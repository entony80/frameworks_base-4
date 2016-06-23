/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.systemui.qs.tiles;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import org.cyanogenmod.internal.logging.CMMetricsLogger;

public class EditTile extends QSTile<QSTile.BooleanState> implements KeyguardMonitor.Callback {

    private boolean mListening;

    public EditTile(Host host) {
        super(host);
        refreshState();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        getHost().setEditing(!mState.value);
        refreshState(!mState.value);
    }

    @Override
    protected void handleLongClick() {
        getHost().goToSettingsPage();
        refreshState(true);
    }

    @Override
    public int getMetricsCategory() {
        return CMMetricsLogger.TILE_EDIT;
    }

    @Override
    protected String composeChangeAnnouncement() {
        // TODO
        return null;
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (mListening) {
            mHost.getKeyguardMonitor().addCallback(this);
        } else {
            mHost.getKeyguardMonitor().removeCallback(this);
        }
        refreshState();
    }

    @Override
    public void onKeyguardChanged() {
        refreshState();
    }
}
