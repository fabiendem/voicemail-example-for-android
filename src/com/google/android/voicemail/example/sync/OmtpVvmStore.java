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
package com.google.android.voicemail.example.sync;

import com.google.android.voicemail.example.callbacks.Callback;
import com.google.android.voicemail.example.callbacks.Callbacks;
import com.google.android.voicemail.example.core.VoicemailIntentUtils;

import android.content.Context;
import android.content.Intent;

import com.example.android.voicemail.common.core.Voicemail;

import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.ThreadSafe;

/**
 * VvmStore implementation backed by an OMTP voicemail service.
 */
@ThreadSafe
public class OmtpVvmStore implements VvmStore {
    /** The {@code action} sent when we wish to fetch the content of a voicemail message. */
    public static final String FETCH_INTENT = "com.google.android.apps.vvm.VOICEMAIL_FETCH";

    private final VoicemailFetcherFactory mVoicemailFetcherFactory;
    private final Executor mExecutor;
    private final Context mContext;

    public OmtpVvmStore(VoicemailFetcherFactory voicemailFetcherFactory,
            Executor executor, Context context) {
        mVoicemailFetcherFactory = voicemailFetcherFactory;
        mExecutor = executor;
        mContext = context;
    }

    @Override
    public void performActions(final List<VvmStore.Action> actions, final Callback<Void> callback) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (VvmStore.Action action : actions) {
                    performSingleAction(action.getOperation(), action.getVoicemail());
                }
                callback.onSuccess(null);
            }
        });
    }

    private void performSingleAction(Operation operation, Voicemail message) {
        switch (operation) {
            case DELETE:
                 mVoicemailFetcherFactory.createVoicemailFetcher().markMessagesAsDeleted(
                         Callbacks.<Void>emptyCallback(), message);
                break;
            case FETCH_CONTENT: {
                Intent intent = new Intent(OmtpVvmStore.FETCH_INTENT, message.getUri());
                VoicemailIntentUtils.storeIdentifierInIntent(intent, message);
                mContext.sendBroadcast(intent);
                break;
            }
            case MARK_AS_READ:
                mVoicemailFetcherFactory.createVoicemailFetcher().markMessagesAsRead(
                        Callbacks.<Void>emptyCallback(), message);
                break;
            case INSERT:
                // Inserting into a remote OMTP store isn't supported.
                throw new UnsupportedOperationException(
                        "Cannot insert new message into remote store");
        }
    }

    @Override
    public void getAllMessages(final Callback<List<Voicemail>> callback) {
        mVoicemailFetcherFactory.createVoicemailFetcher().fetchAllVoicemails(callback);
    }
}
