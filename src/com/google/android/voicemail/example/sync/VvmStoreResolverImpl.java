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

import com.example.android.voicemail.common.core.Voicemail;

import java.util.List;

/**
 * Concrete implementation of the {@link VvmStoreResolver} interface.
 */
public final class VvmStoreResolverImpl implements VvmStoreResolver {
    @Override
    public void resolveFullSync(VvmStore local, VvmStore remote,
            VvmStoreResolver.ResolvePolicy policy, final Callback<Void> result) {
        new InnerVvmStoreResolver(local, remote, policy, result).resolve();
    }

    /**
     * Default implementation of ResolvePolicy, performs typical actions for talking to an OMTP
     * source.
     */
    // TODO: 8. Perhaps this doesn't belong here, perhaps it's specific to OMTP resolves. In which
    // case it probably belongs in the OMTP package.
    public static class DefaultResolvePolicy implements ResolvePolicy {
        @Override
        public void resolveLocalOnlyMessage(Voicemail localMessage,
                List<VvmStore.Action> localActions, List<VvmStore.Action> remoteActions) {
            // If the message is no longer on the server, it should be moved out of local inbox.
            localActions.add(VvmStoreActions.delete(localMessage));
        }

        @Override
        public void resolveRemoteOnlyMessage(Voicemail remoteMessage,
                List<VvmStore.Action> localActions, List<VvmStore.Action> remoteActions) {
            // Voicemails that are available remotely but missing locally need to be inserted.
            localActions.add(VvmStoreActions.insert(remoteMessage));
        }

        @Override
        public void resolveBothLocalAndRemoteMessage(Voicemail localMessage,
                Voicemail remoteMessage, List<VvmStore.Action> localActions,
                List<VvmStore.Action> remoteActions) {
            // Voicemails that are available locally and remotely, but for which content is missing
            // locally, need to have the content fetched from the remote store.
            if (!localMessage.hasContent()) {
                remoteActions.add(VvmStoreActions.fetchContent(remoteMessage));
            }

            // Voicemails that are marked read locally should be marked so on the server and vice
            // versa.
            // Relies on the fact that isRead() would return false if read status is not known.
            if (localMessage.isRead() && !remoteMessage.isRead()) {
                remoteActions.add(VvmStoreActions.markAsRead(remoteMessage));
            } else if (remoteMessage.isRead() && !localMessage.isRead()) {
                localActions.add(VvmStoreActions.markAsRead(localMessage));
            }
        }
    }
}
