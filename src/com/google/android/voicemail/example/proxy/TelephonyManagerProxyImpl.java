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
 * Proxy for {@link TelephonyManager}.
 */
public class TelephonyManagerProxyImpl implements TelephonyManagerProxy {
    private final TelephonyManager mDelegate;

    public TelephonyManagerProxyImpl(TelephonyManager telephonyManager) {
        mDelegate = telephonyManager;
    }

    @Override
    public String getSubscriberId() {
        return mDelegate.getSubscriberId();
    }

    @Override
    public String getNetworkOperator() {
        return mDelegate.getNetworkOperator();
    }

    @Override
    public String getNetworkOperatorName() {
        return mDelegate.getNetworkOperatorName();
    }

    @Override
    public String getSimOperator() {
        return mDelegate.getSimOperator();
    }

    @Override
    public String getSimOperatorName() {
        return mDelegate.getSimOperatorName();
    }
}
