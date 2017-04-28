/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.searchlauncher;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;

import com.android.launcher3.AppInfo;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.allapps.AllAppsSearchBarController;
import com.android.launcher3.util.ComponentKey;
import com.google.android.libraries.launcherclient.LauncherClient;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link LauncherCallbacks} which integrates the Google -1 screen
 * with launcher
 */
public class SearchLauncherCallbacks implements LauncherCallbacks, OnSharedPreferenceChangeListener {

    private static final String KEY_ENABLE_MINUS_ONE = "pref_enable_minus_one";

    private final Launcher mLauncher;

    private OverlayCallbackImpl mOverlayCallbacks;
    private LauncherClient mLauncherClient;

    private boolean mStarted;
    private boolean mResumed;
    private boolean mAlreadyOnHome;

    public SearchLauncherCallbacks(Launcher launcher) {
        mLauncher = launcher;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mLauncher);
        mOverlayCallbacks = new OverlayCallbackImpl(mLauncher);
        mLauncherClient = new LauncherClient(mLauncher, mOverlayCallbacks,
                preferences.getBoolean(KEY_ENABLE_MINUS_ONE, true));
        mOverlayCallbacks.setClient(mLauncherClient);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetachedFromWindow() {
        mLauncherClient.onDetachedFromWindow();
    }

    @Override
    public void onAttachedToWindow() {
        mLauncherClient.onAttachedToWindow();
    }

    @Override
    public void onHomeIntent() {
        mLauncherClient.hideOverlay(mAlreadyOnHome);
    }

    @Override
    public void onResume() {
        mResumed = true;
        if (mStarted) {
            mAlreadyOnHome = true;
        }
        mLauncherClient.onResume();
    }

    @Override
    public void onPause() {
        mResumed = false;
        mLauncherClient.onPause();
    }

    @Override
    public void onStart() {
        mStarted = true;
    }

    @Override
    public void onStop() {
        mStarted = false;
        if (!mResumed) {
            mAlreadyOnHome = false;
        }
    }

    @Override
    public void onDestroy() {
        mLauncherClient.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(mLauncher)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (KEY_ENABLE_MINUS_ONE.equals(key)) {
            mLauncherClient.setOverlayEnabled(prefs.getBoolean(KEY_ENABLE_MINUS_ONE, true));
        }
    }

    @Override
    public void preOnCreate() { }

    @Override
    public void preOnResume() { }

    @Override
    public void onSaveInstanceState(Bundle outState) { }

    @Override
    public void onPostCreate(Bundle savedInstanceState) { }

    @Override
    public void onNewIntent(Intent intent) { }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) { }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) { }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) { }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void onTrimMemory(int level) { }

    @Override
    public void onLauncherProviderChange() { }

    @Override
    public void finishBindingItems(boolean upgradePath) { }

    @Override
    public void onClickAllAppsButton(View v) { }

    @Override
    public void bindAllApplications(ArrayList<AppInfo> apps) { }

    @Override
    public void onClickFolderIcon(View v) { }

    @Override
    public void onClickAppShortcut(View v) { }

    @Override
    public void onClickPagedViewIcon(View v) { }

    @Override
    public void onClickWallpaperPicker(View v) { }

    @Override
    public void onClickSettingsButton(View v) {
       // mLauncher.startActivity(new Intent(mLauncher, SettingsActivity.class));
    }

    @Override
    public void onClickAddWidgetButton(View v) { }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) { }

    @Override
    public void onWorkspaceLockedChanged() { }

    @Override
    public void onDragStarted(View view) { }

    @Override
    public void onInteractionBegin() { }

    @Override
    public void onInteractionEnd() { }

    @Override
    public boolean forceDisableVoiceButtonProxy() {
        return false;
    }

    @Override
    public boolean providesSearch() {
        return false;
    }

    @Override
    public boolean startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        return false;
    }

    @Override
    public boolean startSearchFromAllApps(String query) {
        return false;
    }

    @Override
    public void startVoice() {

    }

    @Override
    public boolean hasCustomContentToLeft() {
        return false;
    }

    @Override
    public void populateCustomContentContainer() { }

    @Override
    public View getQsbBar() {
        return null;
    }


    @Override
    public Intent getFirstRunActivity() {
        return null;
    }

    @Override
    public boolean hasFirstRunActivity() {
        return false;
    }

    @Override
    public boolean hasDismissableIntroScreen() {
        return false;
    }

    @Override
    public View getIntroScreen() {
        return null;
    }

    @Override
    public boolean shouldMoveToDefaultScreenOnHomeIntent() {
        return false;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public ComponentName getWallpaperPickerComponent() {
        return null;
    }

    @Override
    public boolean overrideWallpaperDimensions() {
        return false;
    }

    @Override
    public boolean isLauncherPreinstalled() {
        return false;
    }

    @Override
    public AllAppsSearchBarController getAllAppsSearchBarController() {
        return null;
    }

    @Override
    public List<ComponentKey> getPredictedApps() {
        return null;
    }

    @Override
    public boolean hasLauncherOverlay() {
        return false;
    }

    @Override
    public Launcher.LauncherOverlay setLauncherOverlayView(InsettableFrameLayout container, Launcher.LauncherOverlayCallbacks callbacks) {
        return null;
    }


    @Override
    public void setLauncherSearchCallback(Object callbacks) { }
}
