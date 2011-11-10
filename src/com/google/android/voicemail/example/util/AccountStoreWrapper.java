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

import com.google.android.voicemail.example.storage.AccountInfo;
import com.google.android.voicemail.example.storage.AccountStore;

import javax.annotation.Nullable;

/**
 * A wrapper on {@link AccountStore} that automatically determines the accountId to be used based on
 * the currently inserted SIM and uses that to call respective methods of {@link AccountStore}.
 */
public interface AccountStoreWrapper {
    /**
     * Updates the account store with the account info data set in the supplied
     * {@link com.google.android.voicemail.example.storage.AccountInfo.Builder}.
     * All necessary fields other than the accountId must be already
     * set in the builder. This method will internally set the accountId for you.
     *
     * @see AccountStore#updateAccountInfo(AccountInfo)
     */
    public void updateAccountInfo(AccountInfo.Builder accountInfoBuilder);

    /** @see AccountStore#getAccountInfo(String) */
    @Nullable
    public AccountInfo getAccountInfo();
}
