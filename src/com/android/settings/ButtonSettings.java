/*
 * Copyright (C) 2013 The CyanogenMod project
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

package com.android.settings;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.internal.util.cm.ScreenType;

import com.android.settings.cyanogenmod.ButtonBacklightBrightness;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import cyanogenmod.hardware.CMHardwareManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "SystemSettings";

    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String KEY_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String KEY_HOME_DOUBLE_TAP = "hardware_keys_home_double_tap";
    private static final String KEY_MENU_PRESS = "hardware_keys_menu_press";
    private static final String KEY_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String KEY_ASSIST_PRESS = "hardware_keys_assist_press";
    private static final String KEY_ASSIST_LONG_PRESS = "hardware_keys_assist_long_press";
    private static final String KEY_APP_SWITCH_PRESS = "hardware_keys_app_switch_press";
    private static final String KEY_APP_SWITCH_LONG_PRESS = "hardware_keys_app_switch_long_press";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String DISABLE_NAV_KEYS = "disable_nav_keys";
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";
    private static final String KEY_NAVIGATION_RECENTS_LONG_PRESS = "navigation_recents_long_press";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String KEY_VOLUME_MUSIC_CONTROLS = "volbtn_music_controls";
    private static final String KEY_VOLUME_CONTROL_RING_STREAM = "volume_keys_control_ring_stream";

    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";
    private static final String CATEGORY_NAVBAR = "navigation_bar_category";
    private static final String VOLUME_ROCKER_WAKE = "volume_rocker_wake";

    // Available custom actions to perform on a key press.
    // Must match values for KEY_HOME_LONG_PRESS_ACTION in:
    // frameworks/base/core/java/android/provider/Settings.java
    private static final int ACTION_NOTHING = 0;
    private static final int ACTION_MENU = 1;
    private static final int ACTION_APP_SWITCH = 2;
    private static final int ACTION_SEARCH = 3;
    private static final int ACTION_VOICE_SEARCH = 4;
    private static final int ACTION_IN_APP_SEARCH = 5;
    private static final int ACTION_LAUNCH_CAMERA = 6;
    private static final int ACTION_SLEEP = 7;
    private static final int ACTION_LAST_APP = 8;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private SwitchPreference mCameraWakeScreen;
    private SwitchPreference mCameraSleepOnRelease;
    private SwitchPreference mCameraLaunch;
    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreference mVolumeWakeScreen;
    private SwitchPreference mVolumeMusicControls;
    private SwitchPreference mVolumeControlRingStream;
    private SwitchPreference mSwapVolumeButtons;
    private SwitchPreference mDisableNavigationKeys;
    private SwitchPreference mNavigationBarLeftPref;
    private ListPreference mNavigationRecentsLongPressAction;
    private SwitchPreference mPowerEndCall;
    private SwitchPreference mHomeAnswerCall;
    private SwitchPreference mVolumeRockerWake;

    private PreferenceCategory mNavigationPreferencesCat;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final PreferenceScreen prefScreen = getPreferenceScreen();

	// volume rocker wake
        mVolumeRockerWake = (SwitchPreference) findPreference(VOLUME_ROCKER_WAKE);
        mVolumeRockerWake.setOnPreferenceChangeListener(this);
        int volumeRockerWake = Settings.System.getInt(getContentResolver(),
                VOLUME_ROCKER_WAKE, 0);
        mVolumeRockerWake.setChecked(volumeRockerWake != 0);

        // Power button ends calls.
        mPowerEndCall = (SwitchPreference) findPreference(KEY_POWER_END_CALL);

        // Home button answers calls.
        mHomeAnswerCall = (SwitchPreference) findPreference(KEY_HOME_ANSWER_CALL);

        mHandler = new Handler();

        // Force Navigation bar related options
        mDisableNavigationKeys = (SwitchPreference) findPreference(DISABLE_NAV_KEYS);

        mNavigationPreferencesCat = (PreferenceCategory) findPreference(CATEGORY_NAVBAR);

        // Navigation bar left
        mNavigationBarLeftPref = (SwitchPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);

        // Navigation bar recents long press activity needs custom setup
        mNavigationRecentsLongPressAction =
                initRecentsLongPressAction(KEY_NAVIGATION_RECENTS_LONG_PRESS);

        HashMap<String, String> prefsToRemove = (HashMap<String, String>)
                getPreferencesToRemove(this, getActivity());
        for (String key : prefsToRemove.keySet()) {
            String category = prefsToRemove.get(key);
            Preference preference = findPreference(key);
            if (category != null) {
                // Parent is a category
                PreferenceCategory preferenceCategory =
                        (PreferenceCategory) findPreference(category);
                if (preferenceCategory != null) {
                    // Preference category might have already been removed
                    preferenceCategory.removePreference(preference);
                }
            } else {
                // Either parent is preference screen, or remove whole category
                removePreference(key);
            }
        }

        if (Utils.hasVolumeRocker(getActivity())) {
            int cursorControlAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

            int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = (SwitchPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            if (mSwapVolumeButtons != null) {
                mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);
            }
        }

        if (mNavigationPreferencesCat.getPreferenceCount() == 0) {
            // Hide navigation bar category
            prefScreen.removePreference(mNavigationPreferencesCat);
        }

        mCameraWakeScreen = (SwitchPreference) findPreference(Settings.System.CAMERA_WAKE_SCREEN);
        mCameraSleepOnRelease =
                (SwitchPreference) findPreference(Settings.System.CAMERA_SLEEP_ON_RELEASE);
        mCameraLaunch = (SwitchPreference) findPreference(Settings.System.CAMERA_LAUNCH);

        if (mCameraWakeScreen != null) {
            if (mCameraSleepOnRelease != null && !getResources().getBoolean(
                    com.android.internal.R.bool.config_singleStageCameraKey)) {
                mCameraSleepOnRelease.setDependency(Settings.System.CAMERA_WAKE_SCREEN);
            }
        }

        mVolumeWakeScreen = (SwitchPreference) findPreference(Settings.System.VOLUME_WAKE_SCREEN);
        mVolumeMusicControls = (SwitchPreference) findPreference(KEY_VOLUME_MUSIC_CONTROLS);

        mVolumeControlRingStream = (SwitchPreference)
                findPreference(KEY_VOLUME_CONTROL_RING_STREAM);
        int volumeControlRingtone = Settings.System.getInt(getContentResolver(),
                Settings.System.VOLUME_KEYS_CONTROL_RING_STREAM, 1);
        if (mVolumeControlRingStream != null) {
            mVolumeControlRingStream.setChecked(volumeControlRingtone > 0);
        }

        if (mVolumeWakeScreen != null) {
            if (mVolumeMusicControls != null) {
                mVolumeMusicControls.setDependency(Settings.System.VOLUME_WAKE_SCREEN);
                mVolumeWakeScreen.setDisableDependentsState(true);
            }
        }
    }

    private static Map<String, String> getPreferencesToRemove(ButtonSettings settings,
                   Context context) {
        HashMap<String, String> result = new HashMap<String, String>();

        final ContentResolver resolver = context.getContentResolver();
        final Resources res = context.getResources();

        final int deviceKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final int deviceWakeKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareWakeKeys);

        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
        final boolean hasCameraKey = (deviceKeys & KEY_MASK_CAMERA) != 0;

        final boolean showHomeWake = (deviceWakeKeys & KEY_MASK_HOME) != 0;
        final boolean showBackWake = (deviceWakeKeys & KEY_MASK_BACK) != 0;
        final boolean showMenuWake = (deviceWakeKeys & KEY_MASK_MENU) != 0;
        final boolean showAssistWake = (deviceWakeKeys & KEY_MASK_ASSIST) != 0;
        final boolean showAppSwitchWake = (deviceWakeKeys & KEY_MASK_APP_SWITCH) != 0;
        final boolean showCameraWake = (deviceWakeKeys & KEY_MASK_CAMERA) != 0;
        final boolean showVolumeWake = (deviceWakeKeys & KEY_MASK_VOLUME) != 0;

        final CMHardwareManager hardware = CMHardwareManager.getInstance(context);

        // Only visible on devices that does not have a navigation bar already,
        // and don't even try unless the existing keys can be disabled
        boolean needsNavigationBar = false;
        if (hardware.isSupported(CMHardwareManager.FEATURE_KEY_DISABLE)) {
            try {
                IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                needsNavigationBar = wm.needsNavigationBar();
            } catch (RemoteException e) {
            }

            if (needsNavigationBar) {
                result.put(DISABLE_NAV_KEYS, null);
            } else {
                // Remove keys that can be provided by the navbar
                if (settings != null) {
                    settings.updateDisableNavkeysOption();
                    settings.mNavigationPreferencesCat.setEnabled(
                            settings.mDisableNavigationKeys.isChecked());
                    settings.updateDisableNavkeysCategories(
                            settings.mDisableNavigationKeys.isChecked());
                }
            }
        } else {
            result.put(DISABLE_NAV_KEYS, null);
        }

        if (hasPowerKey) {
            if (!Utils.isVoiceCapable(context)) {
                result.put(KEY_POWER_END_CALL, CATEGORY_POWER);
                if (settings != null) {
                    settings.mPowerEndCall = null;
                }
            }
        } else {
            result.put(CATEGORY_POWER, null);
        }

        if (hasHomeKey) {
            if (!showHomeWake) {
                result.put(Settings.System.HOME_WAKE_SCREEN, CATEGORY_HOME);
            }

            if (!Utils.isVoiceCapable(context)) {
                if (settings != null) {
                    settings.mHomeAnswerCall = null;
                }
                result.put(KEY_HOME_ANSWER_CALL, CATEGORY_HOME);
            }

            int defaultLongPressAction = res.getInteger(
                    com.android.internal.R.integer.config_longPressOnHomeBehavior);
            if (defaultLongPressAction < ACTION_NOTHING ||
                    defaultLongPressAction > ACTION_LAST_APP) {
                defaultLongPressAction = ACTION_NOTHING;
            }

            int defaultDoubleTapAction = res.getInteger(
                    com.android.internal.R.integer.config_doubleTapOnHomeBehavior);
            if (defaultDoubleTapAction < ACTION_NOTHING ||
                    defaultDoubleTapAction > ACTION_LAST_APP) {
                defaultDoubleTapAction = ACTION_NOTHING;
            }

            if (settings != null) {
                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_HOME_LONG_PRESS_ACTION,
                        defaultLongPressAction);
                settings.mHomeLongPressAction = settings.initActionList(
                        KEY_HOME_LONG_PRESS, longPressAction);

                int doubleTapAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                        defaultDoubleTapAction);
                settings.mHomeDoubleTapAction = settings.initActionList(
                        KEY_HOME_DOUBLE_TAP, doubleTapAction);
            }

        } else {
            result.put(CATEGORY_HOME, null);
        }

        if (hasBackKey) {
            if (!showBackWake) {
                result.put(Settings.System.BACK_WAKE_SCREEN, CATEGORY_BACK);
                result.put(CATEGORY_BACK, null);
            }
        } else {
            result.put(CATEGORY_BACK, null);
        }

        if (hasMenuKey) {
            if (!showMenuWake) {
                result.put(Settings.System.MENU_WAKE_SCREEN, CATEGORY_MENU);
            }

            if (settings != null) {
                int pressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_MENU_ACTION, ACTION_MENU);
                settings.mMenuPressAction = settings.initActionList(
                        KEY_MENU_PRESS, pressAction);

                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION,
                        hasAssistKey ? ACTION_NOTHING : ACTION_SEARCH);
                settings.mMenuLongPressAction = settings.initActionList(
                        KEY_MENU_LONG_PRESS, longPressAction);
            }
        } else {
            result.put(CATEGORY_MENU, null);
        }

        if (hasAssistKey) {
            if (!showAssistWake) {
                result.put(Settings.System.ASSIST_WAKE_SCREEN, CATEGORY_ASSIST);
            }

            if (settings != null) {
                int pressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_ASSIST_ACTION, ACTION_SEARCH);
                settings.mAssistPressAction = settings.initActionList(
                        KEY_ASSIST_PRESS, pressAction);

                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, ACTION_VOICE_SEARCH);
                settings.mAssistLongPressAction = settings.initActionList(
                        KEY_ASSIST_LONG_PRESS, longPressAction);
            }
        } else {
            result.put(CATEGORY_ASSIST, null);
        }

        if (hasAppSwitchKey) {
            if (!showAppSwitchWake) {
                result.put(Settings.System.APP_SWITCH_WAKE_SCREEN, CATEGORY_APPSWITCH);
            }

            if (settings != null) {
                int pressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_APP_SWITCH_ACTION, ACTION_APP_SWITCH);
                settings.mAppSwitchPressAction = settings.initActionList(
                        KEY_APP_SWITCH_PRESS, pressAction);

                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, ACTION_NOTHING);
                settings.mAppSwitchLongPressAction = settings.initActionList(
                        KEY_APP_SWITCH_LONG_PRESS, longPressAction);
            }
        } else {
            result.put(CATEGORY_APPSWITCH, null);
        }

        if (hasCameraKey) {
            if (!showCameraWake) {
                result.put(Settings.System.CAMERA_WAKE_SCREEN, CATEGORY_CAMERA);
            }
            // Only show 'Camera sleep on release' if the device has a focus key
            if (res.getBoolean(com.android.internal.R.bool.config_singleStageCameraKey)) {
                result.put(Settings.System.CAMERA_SLEEP_ON_RELEASE, CATEGORY_CAMERA);
            }
        } else {
            result.put(CATEGORY_CAMERA, null);
        }

        if (Utils.hasVolumeRocker(context)) {
            if (!showVolumeWake) {
                result.put(Settings.System.VOLUME_WAKE_SCREEN, CATEGORY_VOLUME);
            }
        } else {
            result.put(CATEGORY_VOLUME, null);
        }

        try {
            // Only show the navigation bar category on devices that have a navigation bar
            // unless we are forcing it via development settings
            boolean forceNavbar = android.provider.Settings.Secure.getInt(resolver,
                    android.provider.Settings.Secure.DEV_FORCE_SHOW_NAVBAR, 0) == 1;
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar()
                    || forceNavbar;

            if (!ScreenType.isPhone(context)) {
                result.put(KEY_NAVIGATION_BAR_LEFT, CATEGORY_NAVBAR);
            }

            if (!hasNavBar && (needsNavigationBar ||
                    !hardware.isSupported(CMHardwareManager.FEATURE_KEY_DISABLE))) {
                result.put(CATEGORY_NAVBAR, null);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

        if (!ButtonBacklightBrightness.isButtonSupported(context) &&
                !ButtonBacklightBrightness.isKeyboardSupported(context)) {
            result.put(KEY_BUTTON_BACKLIGHT, null);
        }

        return result;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);
        }

        // Home button answers calls.
        if (mHomeAnswerCall != null) {
            final int incallHomeBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DEFAULT);
            final boolean homeButtonAnswersCall =
                (incallHomeBehavior == Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER);
            mHomeAnswerCall.setChecked(homeButtonAnswersCall);
        }

    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        if (list == null) return null;
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private ListPreference initRecentsLongPressAction(String key) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setOnPreferenceChangeListener(this);

        // Read the componentName from Settings.Secure, this is the user's prefered setting
        String componentString = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.RECENTS_LONG_PRESS_ACTIVITY);
        ComponentName targetComponent = null;
        if (componentString == null) {
            list.setSummary(getString(R.string.hardware_keys_action_last_app));
        } else {
            targetComponent = ComponentName.unflattenFromString(componentString);
        }

        // Dyanamically generate the list array,
        // query PackageManager for all Activites that are registered for ACTION_RECENTS_LONG_PRESS
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_RECENTS_LONG_PRESS);
        List<ResolveInfo> recentsActivities = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (recentsActivities.size() == 0) {
            // No entries available, disable
            list.setSummary(getString(R.string.hardware_keys_action_last_app));
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.RECENTS_LONG_PRESS_ACTIVITY, null);
            list.setEnabled(false);
            return list;
        }

        CharSequence[] entries = new CharSequence[recentsActivities.size() + 1];
        CharSequence[] values = new CharSequence[recentsActivities.size() + 1];
        // First entry is always default last app
        entries[0] = getString(R.string.hardware_keys_action_last_app);
        values[0] = "";
        list.setValue(values[0].toString());
        int i = 1;
        for (ResolveInfo info : recentsActivities) {
            try {
                // Use pm.getApplicationInfo for the label,
                // we cannot rely on ResolveInfo that comes back from queryIntentActivities.
                entries[i] = pm.getApplicationInfo(info.activityInfo.packageName, 0).loadLabel(pm);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Error package not found: " + info.activityInfo.packageName, e);
                // Fallback to package name
                entries[i] = info.activityInfo.packageName;
            }

            // Set the value to the ComponentName that will handle this intent
            ComponentName entryComponent = new ComponentName(info.activityInfo.packageName,
                    info.activityInfo.name);
            values[i] = entryComponent.flattenToString();
            if (targetComponent != null) {
                if (entryComponent.equals(targetComponent)) {
                    // Update the selected value and the preference summary
                    list.setSummary(entries[i]);
                    list.setValue(values[i].toString());
                }
            }
            i++;
        }
        list.setEntries(entries);
        list.setEntryValues(values);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHomeLongPressAction) {
            handleActionListChange(mHomeLongPressAction, newValue,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION);
            return true;
	} else if (preference == mVolumeRockerWake) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(), VOLUME_ROCKER_WAKE,
                    value ? 1 : 0);
            return true; 
        } else if (preference == mHomeDoubleTapAction) {
            handleActionListChange(mHomeDoubleTapAction, newValue,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION);
            return true;
        } else if (preference == mMenuPressAction) {
            handleActionListChange(mMenuPressAction, newValue,
                    Settings.System.KEY_MENU_ACTION);
            return true;
        } else if (preference == mMenuLongPressAction) {
            handleActionListChange(mMenuLongPressAction, newValue,
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mAssistPressAction) {
            handleActionListChange(mAssistPressAction, newValue,
                    Settings.System.KEY_ASSIST_ACTION);
            return true;
        } else if (preference == mAssistLongPressAction) {
            handleActionListChange(mAssistLongPressAction, newValue,
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mAppSwitchPressAction) {
            handleActionListChange(mAppSwitchPressAction, newValue,
                    Settings.System.KEY_APP_SWITCH_ACTION);
            return true;
        } else if (preference == mAppSwitchLongPressAction) {
            handleActionListChange(mAppSwitchLongPressAction, newValue,
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        } else if (preference == mNavigationRecentsLongPressAction) {
            // RecentsLongPressAction is handled differently because it intentionally uses
            // Settings.System
            String putString = (String) newValue;
            int index = mNavigationRecentsLongPressAction.findIndexOfValue(putString);
            CharSequence summary = mNavigationRecentsLongPressAction.getEntries()[index];
            // Update the summary
            mNavigationRecentsLongPressAction.setSummary(summary);
            if (putString.length() == 0) {
                putString = null;
            }
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.RECENTS_LONG_PRESS_ACTIVITY, putString);
            return true;
        }
        return false;
    }

    private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int defaultBrightness = context.getResources().getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.DEV_FORCE_SHOW_NAVBAR, enabled ? 1 : 0);
        CMHardwareManager hardware = CMHardwareManager.getInstance(context);
        hardware.set(CMHardwareManager.FEATURE_KEY_DISABLE, enabled);

        /* Save/restore button timeouts to disable them in softkey mode */
        if (enabled) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.BUTTON_BRIGHTNESS, 0);
        } else {
            int oldBright = prefs.getInt(ButtonBacklightBrightness.KEY_BUTTON_BACKLIGHT,
                    defaultBrightness);
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.BUTTON_BRIGHTNESS, oldBright);
        }
    }

    private void updateDisableNavkeysOption() {
        boolean enabled = Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.DEV_FORCE_SHOW_NAVBAR, 0) != 0;

        mDisableNavigationKeys.setChecked(enabled);
    }

    private void updateDisableNavkeysCategories(boolean navbarEnabled) {
        final PreferenceScreen prefScreen = getPreferenceScreen();

        /* Disable hw-key options if they're disabled */
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);
        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) prefScreen.findPreference(KEY_BUTTON_BACKLIGHT);

        /* Toggle backlight control depending on navbar state, force it to
           off if enabling */
        if (backlight != null) {
            backlight.setEnabled(!navbarEnabled);
            backlight.updateSummary();
        }

        /* Toggle hardkey control availability depending on navbar state */
        if (homeCategory != null) {
            homeCategory.setEnabled(!navbarEnabled);
        }
        if (backCategory != null) {
            backCategory.setEnabled(!navbarEnabled);
        }
        if (menuCategory != null) {
            menuCategory.setEnabled(!navbarEnabled);
        }
        if (assistCategory != null) {
            assistCategory.setEnabled(!navbarEnabled);
        }
        if (appSwitchCategory != null) {
            appSwitchCategory.setEnabled(!navbarEnabled);
        }
    }

    public static void restoreKeyDisabler(Context context) {
        CMHardwareManager hardware = CMHardwareManager.getInstance(context);
        if (!hardware.isSupported(CMHardwareManager.FEATURE_KEY_DISABLE)) {
            return;
        }

        writeDisableNavkeysOption(context, Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.DEV_FORCE_SHOW_NAVBAR, 0) != 0);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSwapVolumeButtons) {
            int value = mSwapVolumeButtons.isChecked()
                    ? (ScreenType.isTablet(getActivity()) ? 2 : 1) : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
        } else if (preference == mVolumeControlRingStream) {
            int value = mVolumeControlRingStream.isChecked() ? 1 : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.VOLUME_KEYS_CONTROL_RING_STREAM, value);
        } else if (preference == mDisableNavigationKeys) {
            mDisableNavigationKeys.setEnabled(false);
            mNavigationPreferencesCat.setEnabled(false);
            writeDisableNavkeysOption(getActivity(), mDisableNavigationKeys.isChecked());
            updateDisableNavkeysOption();
            updateDisableNavkeysCategories(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDisableNavigationKeys.setEnabled(true);
                    mNavigationPreferencesCat.setEnabled(mDisableNavigationKeys.isChecked());
                    updateDisableNavkeysCategories(mDisableNavigationKeys.isChecked());
                }
            }, 1000);
        } else if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.button_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();

                    Map<String, String> items = getPreferencesToRemove(null, context);
                    for (String key : items.keySet()) {
                        result.add(key);
                    }
                    return result;
                }
            };

}
