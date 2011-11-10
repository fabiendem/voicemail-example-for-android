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
package com.google.android.voicemail.example.util;

import com.google.android.voicemail.example.callbacks.Callback;
import com.google.android.voicemail.example.core.VoicemailPayload;

import com.example.android.voicemail.common.core.Voicemail;

import java.util.List;

/**
 * Interface to interact with a remote voicemail system. This includes fetching of voicemail
 * metadata and content as well as performing various operations (like marking messages as read
 * or deleted) on specific messages.
 */
public interface VoicemailFetcher {
    /**
     * Fetches voicemail metadata for all messages.
     */
    public void fetchAllVoicemails(Callback<List<Voicemail>> callback);

    /**
     * Downloads payload for the voicemail with the given provider identifier from the server.
     */
    public void fetchVoicemailPayload(String providerData, Callback<VoicemailPayload> callback);

    /** Mark the given list of voicemails as read. */
    public void markMessagesAsRead(Callback<Void> callback, Voicemail... voicemails);

    /** Mark the given list of voicemails as deleted. */
    public void markMessagesAsDeleted(Callback<Void> callback, Voicemail... voicemails);
}
