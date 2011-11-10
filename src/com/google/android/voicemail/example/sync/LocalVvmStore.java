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
import com.google.android.voicemail.example.core.VoicemailIntentUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.android.voicemail.common.core.Voicemail;
import com.example.android.voicemail.common.core.VoicemailImpl;
import com.example.android.voicemail.common.core.VoicemailProviderHelper;

import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Implementation of the VvmStore backed by the local voicemail content provider.
 */
@ThreadSafe
public class LocalVvmStore implements VvmStore {
    private final Executor mExecutor;
    private final VoicemailProviderHelper mVoicemailProviderHelper;
    private final Context mContext; // For sending fetch intent broadcast.

    public LocalVvmStore(Executor executor, VoicemailProviderHelper voicemailProviderHelper,
            Context context) {
        mExecutor = executor;
        mVoicemailProviderHelper = voicemailProviderHelper;
        mContext = context;
    }

    @Override
    public void getAllMessages(final Callback<List<Voicemail>> callback) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(mVoicemailProviderHelper.getAllVoicemails());
            }
        });
    }

    @Override
    public void performActions(final List<VvmStore.Action> actions, final Callback<Void> callback) {
        // TODO: 4. Much later down the line, we can optimise this quite heavily, we think.
        // We should group the operations together by the action (deletes, inserts etc).
        // Then we should be able to perform bulk operations locally using the content provider
        // directly.
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

    private void performSingleAction(VvmStore.Operation operation, Voicemail message) {
        switch (operation) {
            case INSERT: {
                Uri newMsgUri = mVoicemailProviderHelper.insert(message);
                // Send intent to fetch the content of the voicemail.
                Intent fetchIntent = new Intent(OmtpVvmStore.FETCH_INTENT, newMsgUri);
                VoicemailIntentUtils.storeIdentifierInIntent(fetchIntent, message);
                mContext.sendBroadcast(fetchIntent);
                break;
            }
            case DELETE:
                // There is no delete in VoicemailProviderHelper, so use our own implementation.
                delete(message.getUri());
                break;
            case MARK_AS_READ:
                mVoicemailProviderHelper.update(message.getUri(),
                        VoicemailImpl.createEmptyBuilder().setIsRead(true).build());
                break;
            case FETCH_CONTENT:
                throw new UnsupportedOperationException("Local store cannot FETCH_CONTENT");
        }
    }

    private int delete(Uri uri) {
        return mContext.getContentResolver().delete(uri, null, null);
    }

}
