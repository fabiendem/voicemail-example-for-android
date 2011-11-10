/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.voicemail.example.proxy;

import android.telephony.TelephonyManager;

/**
 * Wrapper interface around {@link TelephonyManager}. This interface makes it simpler to unit test
 * classes that uses {@link TelephonyManager}.
 */
public interface TelephonyManagerProxy {
    /** @see TelephonyManagerProxy#getSubscriberId() */
    public String getSubscriberId();

    /** @see TelephonyManager#getNetworkOperator() */
    public String getNetworkOperator();

    /** @see TelephonyManager#getNetworkOperatorName() */
    public String getNetworkOperatorName();

    /** @see TelephonyManager#getSimOperator() */
    public String getSimOperator();

    /** @see TelephonyManager#getSimOperatorName() */
    public String getSimOperatorName();
}
