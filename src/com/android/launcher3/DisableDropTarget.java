package com.android.launcher3;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Build;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Pair;
import org.fdroid.fdroid.privileged.*;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.util.Thunk;

import java.lang.reflect.InvocationTargetException;

public class DisableDropTarget extends ButtonDropTarget {

    private static String sPermissionControllerPackageName;
    private static String sServicesSystemSharedLibPackageName;
    private static String sSharedSystemSharedLibPackageName;

    public DisableDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DisableDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Get the hover color
        mHoverColor = getResources().getColor(R.color.uninstall_target_hover_tint);

        setDrawable(R.drawable.ic_uninstall_launcher);
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        return supportsDrop(getContext(), info);
    }

    private static boolean isSystemPackage(Object info) {
        return ((getAppInfoFlags(info).second & ApplicationInfo.FLAG_SYSTEM) != 0) ? true
                : false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean supportsDrop(Context context, Object info) {
        if(Launcher.sPriviledgedService==null)
            return false;
        boolean disableable = false;
        boolean cuurentlyDisabled = false;
        Pair<ComponentName, Integer> pair = getAppInfoFlags(info);
        if(pair==null)
            return false;
        ComponentName name = pair.first;
        int enabledSetting =  context.getPackageManager().getComponentEnabledSetting(name);
        // Try to prevent the user from bricking their phone
        // by not allowing disabling of apps signed with the
        // system cert and any launcher app in the system.
        try {
            if (isSystemPackage(info)||isSystemPackage(context.getResources(), context.getPackageManager(), context.getPackageManager().getPackageInfo(name.getPackageName(),PackageManager.GET_SIGNATURES)))
        {
            // Disable button for core system applications.
        } else if (enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
            disableable = true;
        } else {
            disableable = true;
            cuurentlyDisabled = true; //should never happen
        }
    } catch (PackageManager.NameNotFoundException e)

    {
        e.printStackTrace();
    }



        return disableable&&!cuurentlyDisabled;
    }
    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg != null && pkg.signatures != null && pkg.signatures.length > 0) {
            return pkg.signatures[0];
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            final PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return getFirstSignature(sys);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return null;
    }

    public static boolean isSystemPackage(Resources resources, PackageManager pm, PackageInfo pkg) {
        try {
            if (sPermissionControllerPackageName == null) {
                sPermissionControllerPackageName = (String) PackageManager.class.getMethod("getPermissionControllerPackageName").invoke(pm);
            }
            if (sServicesSystemSharedLibPackageName == null) {
                sServicesSystemSharedLibPackageName = (String) PackageManager.class.getMethod("getServicesSystemSharedLibraryPackageName").invoke(pm);
            }
            if (sSharedSystemSharedLibPackageName == null) {
                sSharedSystemSharedLibPackageName = (String) PackageManager.class.getMethod("getSharedSystemSharedLibraryPackageName").invoke(pm);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return (getSystemSignature(pm).equals(getFirstSignature(pkg)))
                || pkg.packageName.equals(sPermissionControllerPackageName)
                || pkg.packageName.equals(sServicesSystemSharedLibPackageName)
                || pkg.packageName.equals(sSharedSystemSharedLibPackageName)
                || pkg.packageName.equals("com.android.printspooler");
    }

    /**
     * @return the component name and flags if {@param info} is an AppInfo or an app shortcut.
     */
    private static Pair<ComponentName, Integer> getAppInfoFlags(Object item) {
        if (item instanceof AppInfo) {
            AppInfo info = (AppInfo) item;
            return Pair.create(info.componentName, info.flags);
        } else if (item instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) item;
            ComponentName component = info.getTargetComponent();
            if (info.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION
                    && component != null) {
                return Pair.create(component, info.flags);
            }
        }
        return null;
    }

    @Override
    public void onDrop(DragObject d) {
        // Differ item deletion
        if (d.dragSource instanceof UninstallSource) {
            ((UninstallSource) d.dragSource).deferCompleteDropAfterUninstallActivity();
        }
        super.onDrop(d);
    }

    @Override
    void completeDrop(final DragObject d) {
        final Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(d.dragInfo);
        final UserHandleCompat user = ((ItemInfo) d.dragInfo).user;
        if (startDisableActivity(mLauncher, d.dragInfo)) {

            final Runnable checkIfUninstallWasSuccess = new Runnable() {
                @Override
                public void run() {
                    String packageName = componentInfo.first.getPackageName();
                    boolean uninstallSuccessful = !AllAppsList.packageHasActivities(
                            getContext(), packageName, user);
                    sendUninstallResult(d.dragSource, uninstallSuccessful);
                }
            };
            mLauncher.addOnResumeCallback(checkIfUninstallWasSuccess);
        } else {
            sendUninstallResult(d.dragSource, false);
        }
    }

    public static boolean startDisableActivity(final Launcher launcher, final Object info) {
        final IPrivilegedService privService = Launcher.sPriviledgedService;
        final IPrivilegedCallback callback = new IPrivilegedCallback.Stub() {
            @Override
            public void handleResult(String packageName, int returnCode) throws RemoteException {
                if(returnCode ==  PackageInstaller.STATUS_SUCCESS){
                    launcher.getModel().forceReload();
                }
            }
        };

        try {
            boolean hasPermissions = privService.hasPrivilegedPermissions();
            if (!hasPermissions) {
                return false;
            }

            privService.disablePackage(getAppInfoFlags(info).first.getPackageName(), callback);
            return true;
        } catch (RemoteException e) {
            return false;
        }


    }

    @Thunk void sendUninstallResult(DragSource target, boolean result) {
        if (target instanceof UninstallSource) {
            ((UninstallSource) target).onUninstallActivityReturned(result);
        }
    }

    /**
     * Interface defining an object that can provide uninstallable drag objects.
     */
    public static interface UninstallSource {

        /**
         * A pending uninstall operation was complete.
         * @param result true if uninstall was successful, false otherwise.
         */
        void onUninstallActivityReturned(boolean result);

        /**
         * Indicates that an uninstall request are made and the actual result may come
         * after some time.
         */
        void deferCompleteDropAfterUninstallActivity();
    }
}
