/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.systemui.tuner;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.System;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.settings.SettingConfirmationHelper;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService.Tunable;

public class TunerFragment extends PreferenceFragment {

    private static final String TAG = "TunerFragment";

    private static final String KEY_STATUSBAR_BLACKLIST = "statusbar_icon_blacklist";
    private static final String KEY_DEMO_MODE = "demo_mode";
	private static final String KEY_QUICK_PULL_DOWN = "quick_settings_quick_pull_down";

    public static final String SETTING_SEEN_TUNER_WARNING = "seen_tuner_warning";

    private static final int MENU_REMOVE = Menu.FIRST + 1;

	private SwitchPreference mQuickPullDown;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.tuner_prefs);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        setHasOptionsMenu(true);

        findPreference(KEY_STATUSBAR_BLACKLIST).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(android.R.id.content, new StatusBarIconBlacklistFragment(),
                        "StatusBarBlacklist");
                ft.addToBackStack(null);
                ft.commit();
                return true;
            }
        });
        findPreference(KEY_DEMO_MODE).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(android.R.id.content, new DemoModeFragment(), "DemoMode");
                ft.addToBackStack(null);
                ft.commit();
                return true;
            }
        });
		mQuickPullDown = (SwitchPreference) findPreference(KEY_QUICK_PULL_DOWN);
        if (Settings.Secure.getInt(getContext().getContentResolver(), SETTING_SEEN_TUNER_WARNING,
                0) == 0) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.tuner_warning_title)
                    .setMessage(R.string.tuner_warning)
                    .setPositiveButton(R.string.got_it, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getContext().getContentResolver(),
                                    SETTING_SEEN_TUNER_WARNING, 1);
                        }
                    }).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
		
		updateQuickPullDown();
        getContext().getContentResolver().registerContentObserver(
                Secure.getUriFor(QUICK_SETTINGS_QUICK_PULL_DOWN), false, mSettingObserver);

        registerPrefs(getPreferenceScreen());
        MetricsLogger.visibility(getContext(), MetricsLogger.TUNER, true);
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterPrefs(getPreferenceScreen());
        MetricsLogger.visibility(getContext(), MetricsLogger.TUNER, false);
    }

    private void registerPrefs(PreferenceGroup group) {
        TunerService tunerService = TunerService.get(getContext());
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof StatusBarSwitch) {
                tunerService.addTunable((Tunable) pref, StatusBarIconController.ICON_BLACKLIST);
            } else if (pref instanceof PreferenceGroup) {
                registerPrefs((PreferenceGroup) pref);
            }
        }
    }

    private void unregisterPrefs(PreferenceGroup group) {
        TunerService tunerService = TunerService.get(getContext());
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof Tunable) {
                tunerService.removeTunable((Tunable) pref);
            } else if (pref instanceof PreferenceGroup) {
                registerPrefs((PreferenceGroup) pref);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_REMOVE, Menu.NONE, R.string.remove_from_settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case MENU_REMOVE:
                TunerService.showResetRequest(getContext(), new Runnable() {
                    @Override
                    public void run() {
                        getActivity().finish();
                    }
                });
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
	private void updateQuickPullDown() {
        mQuickPullDown.setOnPreferenceChangeListener(null);
        mQuickPullDown.setChecked(SettingConfirmationHelper.get(
                getContext().getContentResolver(),
                QUICK_SETTINGS_QUICK_PULL_DOWN, false));
        mQuickPullDown.setOnPreferenceChangeListener(mQuickPullDownChange);
    }
	
	private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }
		
	@Override
    public void onChange(boolean selfChange, Uri uri, int userId) {
        super.onChange(selfChange, uri, userId);
        updateBatteryPct();
        updateQuickPullDown();
    }
	
	private final OnPreferenceChangeListener mQuickPullDownChange = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final boolean v = (Boolean) newValue;
            SettingConfirmationHelper.set(
                    getContext().getContentResolver(),
                    QUICK_SETTINGS_QUICK_PULL_DOWN, v);
            return true;
        }
    };
}
