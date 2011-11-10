/*
 * Copyright (C) 2011 Google Inc. All Rights Reserved.
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

import com.google.android.voicemail.example.proxy.TelephonyManagerProxy;
import com.google.android.voicemail.example.storage.AccountInfo;
import com.google.android.voicemail.example.storage.AccountStore;

import javax.annotation.Nullable;

/**
 * Implementation of {@link AccountStoreWrapper}.
 */
public class AccountStoreWrapperImpl implements AccountStoreWrapper {
    private final AccountStore mAccountStore;
    private final TelephonyManagerProxy mTelephonyManager;

    public AccountStoreWrapperImpl(AccountStore accountStore,
            TelephonyManagerProxy telephonyManager) {
        mAccountStore = accountStore;
        mTelephonyManager = telephonyManager;
    }

    @Override
    public void updateAccountInfo(AccountInfo.Builder accountInfoBuilder) {
        accountInfoBuilder.setAccountId(getAccountIdFromSim());
        mAccountStore.updateAccountInfo(accountInfoBuilder.build());
    }

    @Override
    @Nullable
    public AccountInfo getAccountInfo() {
        String accountId = getAccountIdFromSim();
        if (accountId == null) {
            return null;
        }
        return mAccountStore.getAccountInfo(accountId);
    }

    private String getAccountIdFromSim() {
        return mTelephonyManager.getSubscriberId();
    }
}
