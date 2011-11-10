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

import com.google.android.voicemail.example.dependency.DependencyResolverImpl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.android.voicemail.common.logging.Logger;

/**
 * OMTP-related binary SMS receiver.
 */
public class OmtpSmsReceiver extends BroadcastReceiver {
    private static final Logger logger = Logger.getLogger(OmtpSmsReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: 7. I think that this will violate strict mode - we shouldn't be writing
        // entries into the content resolver during a receive of a broadcast.
        // I guess it depends upon how much work the handler is going to really do, perhaps we can
        // get away with using the new goAsync() here, but I think really this should be handed
        // off to a service.
        logger.i(intent.getAction() + ", Port: " + intent.getData().getPort());
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            DependencyResolverImpl.getInstance().createOmtpMessageHandler()
                    .process((Object[]) bundle.get("pdus"));
        }
    }
}
