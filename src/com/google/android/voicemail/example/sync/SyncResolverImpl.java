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
import com.google.android.voicemail.example.sync.VvmStoreResolver.ResolvePolicy;

import com.example.android.voicemail.common.logging.Logger;

import java.util.List;

/**
 * Implementation of {@link SyncResolver}.
 */
public class SyncResolverImpl implements SyncResolver {
    private static final Logger logger = Logger.getLogger(SyncResolverImpl.class);

    private final VvmStoreResolver mResolver;
    private final ResolvePolicy mResolvePolicy;
    private final VvmStore mRemoteStore;
    private final VvmStore mLocalStore;

    public SyncResolverImpl(VvmStoreResolver resolver, ResolvePolicy resolvePolicy,
            VvmStore remoteStore, VvmStore localStore) {
        mResolver = resolver;
        mResolvePolicy = resolvePolicy;
        mRemoteStore = remoteStore;
        mLocalStore = localStore;
    }

    @Override
    public void syncAllMessages(Callback<Void> callback) {
        logger.d("Performing full sync.");
        mResolver.resolveFullSync(mLocalStore, mRemoteStore, mResolvePolicy, callback);
    }

    @Override
    public void syncSpecificMessages(List<String> uids, Callback<Void> callback) {
        logger.d(String.format("Performing sync for %d messages.", uids.size()));
        if (uids.size() == 0) {
            // Nothing to resolve.
            callback.onSuccess(null);
            return;
        }
        // TODO: For now a request to sync single message results in full sync. Change this to
        // resolve only the target messages when VvmStoreResolver supports this functionality.
        mResolver.resolveFullSync(mLocalStore, mRemoteStore, mResolvePolicy, callback);
    }
}
