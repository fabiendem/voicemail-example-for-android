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
package com.google.android.voicemail.example.receiver;

import com.google.android.voicemail.example.core.VoicemailIntentUtils;
import com.google.android.voicemail.example.service.fetch.OmtpFetchService;
import com.google.android.voicemail.example.sync.OmtpVvmStore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.android.voicemail.common.logging.Logger;

/**
 * Receives and handles broadcasts sent to indicate that we should download voicemails.
 * <p>
 * BroadcastReceivers can't do any long running work in their {@link #onReceive(Context, Intent)}
 * methods, so we just send an Intent on to the
 * {@link com.google.android.voicemail.example.service.fetch.OmtpFetchService} which will take
 * care of downloading for us.
 * <p>
 * This class is thread-confined, it will only be called from the application main thread.
 */
public class OmtpFetchReceiver extends BroadcastReceiver {
    private static final Logger logger = Logger.getLogger(OmtpFetchReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        logger.d("Received intent: " + intent);
        if (intent != null && OmtpVvmStore.FETCH_INTENT.equals(intent.getAction())) {
            Intent outgoing = new Intent(intent.getAction(), intent.getData(),
                    context, OmtpFetchService.class);
            VoicemailIntentUtils.copyExtrasBetween(intent, outgoing);
            context.startService(outgoing);
        }
    }
}
