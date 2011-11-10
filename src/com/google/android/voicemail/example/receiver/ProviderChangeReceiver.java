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

import com.google.android.voicemail.example.callbacks.Callbacks;
import com.google.android.voicemail.example.dependency.DependencyResolverImpl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.VoicemailContract;

import com.example.android.voicemail.common.logging.Logger;

/**
 * A broadcast receiver that listens to provider change events from vvm content provider and if
 * needed, triggers a sync of local changes to the server.
 */
public class ProviderChangeReceiver extends BroadcastReceiver {
    private static final Logger logger = Logger.getLogger(ProviderChangeReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        logger.d(String.format("New intent received: %s", intent));
        if (Intent.ACTION_PROVIDER_CHANGED.equals(intent.getAction())) {
            if (!intent.hasExtra(VoicemailContract.EXTRA_SELF_CHANGE)) {
                logger.e(String.format("Extra %s not found in intent. Ignored!",
                        VoicemailContract.EXTRA_SELF_CHANGE));
                return;
            }
            // Sync is required only if the change was not triggered by self.
            if (!intent.getBooleanExtra(VoicemailContract.EXTRA_SELF_CHANGE, false)) {
                // TODO: We can optimize this to perform sync of only affected messages.
                DependencyResolverImpl.getInstance().createSyncResolver().syncAllMessages(
                        Callbacks.<Void>emptyCallback());
            } else {
                logger.d("Changed by self. Ignored!");
            }
        }
    }
}
