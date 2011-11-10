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
package com.google.android.voicemail.example.storage;

import javax.annotation.Nullable;

/**
 * Interface for storing account related data.
 */
public interface AccountStore {
    /**
     * Updates the account store with supplied account info.
     * <p>
     * It is mandatory to include accountId field in {@link AccountInfo} object.
     *
     * @param accountInfo Account info to be updated
     */
    public void updateAccountInfo(AccountInfo accountInfo);

    /**
     * Gets the account info associated with the supplied phone number. Returns null if no data
     * associated to this phone number is found.
     *
     * @param accountId The account id for which account info is requested
     */
    @Nullable
    public AccountInfo getAccountInfo(String accountId);
}
