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
package com.google.android.voicemail.example.service.fetch;

import com.google.android.voicemail.example.callbacks.Callback;
import com.google.android.voicemail.example.core.VoicemailIntentUtils;
import com.google.android.voicemail.example.core.VoicemailPayload;
import com.google.android.voicemail.example.sync.VoicemailFetcherFactory;

import android.content.Intent;

import com.example.android.voicemail.common.core.Voicemail;
import com.example.android.voicemail.common.core.VoicemailProviderHelper;
import com.example.android.voicemail.common.logging.Logger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Contains the logic for handling fetch requests on behalf of the {@link OmtpFetchService}.
 * <p>
 * This class is not thread safe for concurrent access. The calls to the
 * {@link #onHandleFetchIntent(Intent)} method will be made sequentially with a guarantee of memory
 * visibility between calls, see the {@link OmtpFetchService} class documentation for more details.
 * <p>
 * Note also that the fetch operation should complete before {@link #onHandleFetchIntent(Intent)}
 * method returns, i.e. should not be performed by yet another background thread, else our original
 * caller will think that we are done and may begin performing the next fetch immediately.
 */
@NotThreadSafe
public class OmtpFetchController {
    private static final Logger logger = Logger.getLogger(OmtpFetchController.class);

    /** Over a 3G network, fetching one message by IMAP can take > 10s. */
    private static final long TIME_TO_WAIT_FOR_RESULT_MS = 20000;

    private final VoicemailFetcherFactory mVoicemailFetcherFactory;
    private final VoicemailProviderHelper mVoicemailProviderHelper;

    public OmtpFetchController(VoicemailFetcherFactory voicemailFetcherFactory,
            VoicemailProviderHelper voicemailProviderHelper) {
        mVoicemailFetcherFactory = voicemailFetcherFactory;
        mVoicemailProviderHelper = voicemailProviderHelper;
    }

    public void onHandleFetchIntent(Intent intent) {
        logger.d("Received onHandleFetchIntent(" + intent + ")");
        // Work out which Voicemail this intent corresponds to fetching.
        String identifier = VoicemailIntentUtils.extractIdentifierFromIntent(intent);
        if (identifier == null) {
            // We don't know what message we are supposed to be fetching. Can't do much.
            logger.e("Asked to fetch for intent without identifier: " + intent);
            return;
        }

        // Fire off a fetch request. Then wait for the result.
        // The wait is required as explained in the class documentation.
        FetchAttachmentCallback callback = new FetchAttachmentCallback();
        mVoicemailFetcherFactory.createVoicemailFetcher().fetchVoicemailPayload(
                identifier, callback);
        Voicemail voicemail = mVoicemailProviderHelper.findVoicemailBySourceData(identifier);
        VoicemailPayload fetchedPayload = callback.waitForResult();

        if (check(fetchedPayload != null, "Missing payload", voicemail)) {
            // TODO: 3. Determine what we are going to do about duration.
            // TODO: 1. What if we didn't find a voicemail with that id? Would it be null? Should we
            // then throw an IOException?
            try {
                // Eclipse thinks that payload may be null at this location.
                // Eclipse is wrong. But I don't mind this extra check, it's a no-op.
                if (fetchedPayload != null) {
                    mVoicemailProviderHelper.setVoicemailContent(voicemail.getUri(),
                            fetchedPayload.getBytes(), fetchedPayload.getMimeType());
                }
            } catch (IOException e) {
                logger.e("Couldn't write payload to content provider", e);
                return;
            }
        }
    }

    private boolean check(boolean check, String message, Voicemail voicemail) {
        if (!check) {
            logger.e(message + ": " + voicemail);
            return false;
        }
        return true;
    }

    /**
     * Helper class used as a callback that also allows a thread to wait for the result.
     */
    private class FetchAttachmentCallback implements Callback<VoicemailPayload> {
        private final CountDownLatch mIsComplete = new CountDownLatch(1);
        private volatile VoicemailPayload mResult;

        @Override
        public void onFailure(Exception error) {
            mIsComplete.countDown();
        }

        @Override
        public void onSuccess(VoicemailPayload result) {
            mResult = result;
            mIsComplete.countDown();
        }

        /**
         * Waits for the asynchronous result of the callback to complete.
         * <p>
         * Returns the voicemail and the payload that we retrieved. Returns null if the thread was
         * interrupted, if there was an exception of any sort fetching the data from the server, or
         * if the timeout expired (i.e. the fetch took too long).
         */
        @Nullable
        private VoicemailPayload waitForResult() {
            try {
                mIsComplete.await(TIME_TO_WAIT_FOR_RESULT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // Restore interrupt status and fall through.
                Thread.currentThread().interrupt();
            }
            return mResult;
        }
    }
}
