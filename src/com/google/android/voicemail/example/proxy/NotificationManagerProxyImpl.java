/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.voicemail.example.proxy;

import android.app.Notification;
import android.app.NotificationManager;

/**
 * Proxy for {@link NotificationManager}.
 */
public class NotificationManagerProxyImpl implements NotificationManagerProxy {
    private final NotificationManager mDelegate;

    public NotificationManagerProxyImpl(NotificationManager delegate) {
        mDelegate = delegate;
    }

    @Override
    public void notify(int id, Notification notification) {
        mDelegate.notify(id, notification);
    }

    @Override
    public void notify(String tag, int id, Notification notification) {
        mDelegate.notify(tag, id, notification);
    }

    @Override
    public void cancel(int id) {
        mDelegate.cancel(id);
    }

    @Override
    public void cancel(String tag, int id) {
        mDelegate.cancel(tag, id);
    }
}
