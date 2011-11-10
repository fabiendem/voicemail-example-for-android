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

import com.google.android.voicemail.example.dependency.DependencyResolver;
import com.google.android.voicemail.example.dependency.DependencyResolverImpl;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.example.android.voicemail.common.core.VoicemailProviderHelper;
import com.example.android.voicemail.common.core.VoicemailProviderHelpers;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Service to download the OMTP voicemail bodies.
 * <p>
 * This service will be triggered by a call to {@link #startService(Intent)} which will have been
 * made by the BroadcastReceiver responsible for handling the fetch intents.
 * <p>
 * This class contains no logic, but just delegates to the {@link OmtpFetchController}.
 * <p>
 * This class extends the {@link IntentService}, which makes the following guaranatees:
 * <ul>
 * <li>requests are handled on a single worker thread</li>
 * <li>only one request will be processed at a time</li>
 * </ul>
 * <p>
 * Unfortunately the {@link IntentService} does <b>not</b> guarantee that this will be the same
 * thread each time, and indeed if one intent is completed before another is sent, then the service
 * will be stopped and restarted, and a different thread will be used. That it guarantees sequential
 * execution of the {@link #onHandleIntent(Intent)} method is great, but that it doesn't guarantee
 * memory visibility between two different calls to {@link #onHandleIntent(Intent)} is annoying. In
 * practice I can see from the implementation that a MessageQueue is used, which contains enough
 * synchronization to guarantee visibility also, but it seems brave to rely on this.
 * <p>
 * I therefore make {@link #onHandleIntent(Intent)} synchronized, which will serve to guarantee
 * memory visibility between successive calls, and coincidentally provides thread safety.
 */
@ThreadSafe
public class OmtpFetchService extends IntentService {
    private static final String WORKER_THREAD_NAME = "OmtpFetchServiceWorkerThread";
    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(0);

    private OmtpFetchController mOmtpFetchController;

    public OmtpFetchService() {
        super(WORKER_THREAD_NAME + "_" + THREAD_NUMBER.incrementAndGet());
    }

    @Override
    protected synchronized void onHandleIntent(Intent intent) {
        if (intent != null) {
            getOmtpFetchController().onHandleFetchIntent(intent);
        }
    }

    /** Lazily initializes the controller. */
    private OmtpFetchController getOmtpFetchController() {
        if (mOmtpFetchController == null) {
            DependencyResolver resolver = DependencyResolverImpl.getInstance();
            Context applicationContext = resolver.getApplicationContext();
            VoicemailProviderHelper voicemailProviderHelper =
                    VoicemailProviderHelpers
                            .createPackageScopedVoicemailProvider(applicationContext);
            mOmtpFetchController = new OmtpFetchController(resolver.getVoicemailFetcherFactory(),
                    voicemailProviderHelper);
        }
        return mOmtpFetchController;
    }
}
