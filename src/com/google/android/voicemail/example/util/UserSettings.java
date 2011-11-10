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
 * limitations under the License
 */
package com.google.android.voicemail.example.util;

import com.google.android.voicemail.example.R;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * An object representing the user settings.
 */
public class UserSettings {
    private final SharedPreferences mPreferences;
    private final Context mContext;

    public UserSettings(SharedPreferences preferences, Context context) {
        mPreferences = preferences;
        mContext = context;
    }

    /**
     * Returns if fake mode of operation in enabled. If fake mode is enabled then the application
     * accepts fake OMTP messages through a test intent and fetches audio content from and syncs
     * against data stored locally in the sdcard.
     */
    public boolean isFakeModeEnabled() {
        return mPreferences.getBoolean(mContext.getString(R.string.prefkey_fake_mode), false);
    }
}
