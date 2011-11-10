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

import java.util.List;

/**
 * Interface to perform omtp source sync between an application specific remote store (omtp server)
 * and local store (voicemail content provider).
 * <p>
 * This can be viewed as a wrapper on {@link VvmStoreResolver} but simplifies its usage by hiding
 * details like what local store, what remote store and what resolve policy has to be used. For a
 * given application these params never change.
 */
public interface SyncResolver {
    /** Sync all messages on both sides. */
    public void syncAllMessages(Callback<Void> callback);

    /** Sync only the supplied list of messages, identified by their uids. */
    public void syncSpecificMessages(List<String> uids, Callback<Void> callback);
}
