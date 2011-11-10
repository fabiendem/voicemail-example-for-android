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
 * Wrapper interface around {@link NotificationManager}. This interface makes it simpler to unit
 * test classes that uses {@link NotificationManager}.
 */
public interface NotificationManagerProxy {
    /** @see NotificationManagerProxy#notify(int, Notification) */
    public void notify(int id, Notification notification);

    /** @see NotificationManagerProxy#notify(String tag, int id, Notification notification) */
    public void notify(String tag, int id, Notification notification);

    /** @see NotificationManagerProxy#cancel(int) */
    public void cancel(int id);

    /** @see NotificationManagerProxy#cancel(String, int) */
    public void cancel(String tag, int id);
}
