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
package com.google.android.voicemail.example.imap;

import com.google.android.voicemail.example.callbacks.Callback;
import com.google.android.voicemail.example.core.VoicemailPayload;
import com.google.android.voicemail.example.util.AccountDetails;
import com.google.android.voicemail.example.util.AccountStoreWrapper;
import com.google.android.voicemail.example.util.VoicemailFetcher;

import android.content.Context;

import com.example.android.voicemail.common.core.Voicemail;

import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Asynchronous fetcher for voicemail from an IMAP server.
 */
@ThreadSafe
public class AsyncImapVoicemailFetcher implements VoicemailFetcher {
    private final Context mContext;
    private final Executor mExecutor;
    private final AccountStoreWrapper mAccountStore;

    /**
     * The {@link Context} is required for handing to the underlying imap code, any context will do,
     * the application context is fine.
     */
    public AsyncImapVoicemailFetcher(Context context, Executor executor,
            AccountStoreWrapper accountStore) {
        mContext = context;
        mExecutor = executor;
        mAccountStore = accountStore;
    }

    @Override
    public void fetchAllVoicemails(final Callback<List<Voicemail>> callback) {
        final AccountDetails accountDetails = getAccountDetails();
        if (accountDetails == null) {
            // Could not fetch account details. Fail!
            callback.onFailure(new Exception(
                    "fetchAllVoicemails() failed, we can't get AccountDetails"));
            return;
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                new OneshotSyncImapVoicemailFetcher(mContext, accountDetails, createImapHelper())
                        .fetchAllVoicemails(callback);
            }
        });
    }

    @Override
    public void fetchVoicemailPayload(final String providerData,
            final Callback<VoicemailPayload> callback) {
        final AccountDetails accountDetails = getAccountDetails();
        if (accountDetails == null) {
            // Could not fetch account details. Fail!
            callback.onFailure(
                    new Exception("fetchVoicemailsPayload() failed, we can't get AccountDetails"));
            return;
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                new OneshotSyncImapVoicemailFetcher(mContext, accountDetails, createImapHelper())
                        .fetchVoicemailPayload(providerData, callback);
            }
        });
    }

    @Override
    public void markMessagesAsRead(final Callback<Void> callback, final Voicemail... voicemails) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                createImapHelper().markMessagesAsRead(callback, voicemails);
            }
        });
    }

    @Override
    public void markMessagesAsDeleted(final Callback<Void> callback,
            final Voicemail... voicemails) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                createImapHelper().markMessagesAsDeleted(callback, voicemails);
            }
        });
    }

    private AccountDetails getAccountDetails() {
        return AccountDetails.fetchFromAccountStore(mAccountStore);
    }

    private ImapHelper createImapHelper() {
        return new OneshotSyncImapHelper(mContext, mAccountStore);
    }

}
