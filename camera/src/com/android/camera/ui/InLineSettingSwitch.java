/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.camera.ListPreference;
import com.android.camera.R;
import com.android.camera.ui.Switch;
import com.android.camera.ui.Switch.SwitchState;

/* A switch setting control which turns on/off the setting. */
public class InLineSettingSwitch extends InLineSettingItem {
    private Switch mSwitch;

    Switch.OnSwitchChangeListener mSwitchChangeListener = new Switch.OnSwitchChangeListener() {
        @Override
        public void onSwitchChangeListener(SwitchState newState) {
            changeIndex(newState == SwitchState.ON ? 1 : 0);
        }
    };

    public InLineSettingSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSwitch = (Switch) findViewById(R.id.setting_switch);
        mSwitch.setOnSwitchChangeListener(mSwitchChangeListener);
    }

    @Override
    public void initialize(ListPreference preference) {
        super.initialize(preference);
    }

    @Override
    protected void updateView() {
        if (mOverrideValue == null) {
            mSwitch.setSwichState(mIndex == 1 ? SwitchState.ON : SwitchState.OFF);
        } else {
            int index = mPreference.findIndexOfValue(mOverrideValue);
            mSwitch.setSwichState(mIndex == 1 ? SwitchState.ON : SwitchState.OFF);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(mPreference.getTitle());
    }
}
