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

import com.example.android.voicemail.common.core.Voicemail;
import com.example.android.voicemail.common.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Helper class for use by the VvmStoreResolverImpl.
 * <p>
 * One-shot class, constructed by the VvmStoreResolverImpl in response to the full resolve method,
 * and then thrown away afterwards. You should not use this class directly, and you should not
 * attempt to re-use an instance of this class. The VvmStoreResolverImpl class will construct an
 * instance of this class, call the {@link #resolve()} method, and then drop the instance.
 * <p>
 * This class needs to be thread-safe, because the callbacks involved are complex and may happen on
 * arbitrary threads.
 * <p>
 * This class is in one of the following conceptual 'states':
 * <ul>
 * <li>Constructed, but not yet started.</li>
 * <li>Started, waiting for at least one message store to return its results.</li>
 * <li>Resolving the results of both fetches into actions to be performed on both stores.</li>
 * <li>Waiting for the list of actions to be completed on at least one store.</li>
 * <li>Done.</li>
 * </ul>
 * <p>
 * The original callback passed in with this class is guaranteed to be invoked at most once. Success
 * will only be called after both fetches complete, the sync resolves, and the new actions are
 * successfully completed. In any other situation, onFailure will be called as soon as the failure
 * is detected.
 */
@ThreadSafe
/*package*/ final class InnerVvmStoreResolver {
    private static final Logger logger = Logger.getLogger(InnerVvmStoreResolver.class);

    /** The callback to be invoked when the resolve is complete. */
    private final Callback<Void> mCallback;
    /** The local store holding voicemails, expected to be the content provider. */
    private final VvmStore mLocalStore;
    /** The remote store holding voicemails. */
    private final VvmStore mRemoteStore;
    /** The resolve policy to use when handling voicemails. */
    private final ResolvePolicy mResolvePolicy;

    /** Checks that we never call the resolve() method more than once, as per class contract. */
    private final AtomicBoolean mHasResolveBeenCalled;
    /** Container for the result of fetching the local voicemails. */
    private final AtomicReference<List<Voicemail>> mLocalResults;
    /** Container for the result of fetching the remote voicemails. */
    private final AtomicReference<List<Voicemail>> mRemoteResults;

    public InnerVvmStoreResolver(VvmStore localStore, VvmStore remoteStore,
            VvmStoreResolver.ResolvePolicy resolvePolicy, Callback<Void> callback) {
        mCallback = callback;
        mLocalStore = localStore;
        mRemoteStore = remoteStore;
        mResolvePolicy = resolvePolicy;
        mHasResolveBeenCalled = new AtomicBoolean(false);
        mLocalResults = new AtomicReference<List<Voicemail>>();
        mRemoteResults = new AtomicReference<List<Voicemail>>();
    }

    /**
     * Performs the resolve between the two stores supplied in the constructor.
     * <p>
     * See the class documentation for a fuller description of how this method will behave.
     *
     * @throws IllegalStateException if you call this method more than once.
     */
    public void resolve() {
        if (mHasResolveBeenCalled.getAndSet(true)) {
            throw new IllegalStateException("You cannot use this class more than once.");
        }
        AtomicInteger fetchesRemaining = new AtomicInteger(2);
        AtomicBoolean failureReported = new AtomicBoolean(false);
        mRemoteStore.getAllMessages(
                new FetchCallback(mRemoteResults, fetchesRemaining, failureReported));
        mLocalStore.getAllMessages(
                new FetchCallback(mLocalResults, fetchesRemaining, failureReported));
    }

    /**
     * The callback for a fetch of messages, either local or remote fetch.
     * <p>
     * This class is responsible for propagating a failure back to the original callback whilst
     * making sure this happens at most once. It is also responsible for storing the successuful
     * result into the container, and when the right number of successful results have happened, for
     * proceeding with the next task (which is doing the resolve).
     */
    private class FetchCallback implements Callback<List<Voicemail>> {
        private final AtomicReference<List<Voicemail>> mResults;
        private final AtomicInteger mRemaining;
        private final AtomicBoolean mFailureReported;

        /**
         * @param results The container into which we put the results of a successful callback
         * @param remaining The number of successful fetches remaining before we should proceed onto
         *            the next step, i.e. resolving
         * @param failureReported Prevents more than one failure being reported to the original
         *            callback
         */
        public FetchCallback(AtomicReference<List<Voicemail>> results, AtomicInteger remaining,
                AtomicBoolean failureReported) {
            mResults = results;
            mRemaining = remaining;
            mFailureReported = failureReported;
        }

        @Override
        public void onSuccess(List<Voicemail> result) {
            mResults.set(result);
            if (mRemaining.decrementAndGet() == 0) {
                performResolve();
            }
        }

        @Override
        public void onFailure(Exception error) {
            if (!mFailureReported.getAndSet(true)) {
                mCallback.onFailure(error);
            }
        }
    }

    /**
     * Builds a map from provider data to message for the given collection of voicemails.
     */
    private static Map<String, Voicemail> buildMap(Collection<Voicemail> messages) {
        Map<String, Voicemail> map = new HashMap<String, Voicemail>();
        for (Voicemail message : messages) {
            map.put(message.getSourceData(), message);
        }
        return map;
    }

    private void performResolve() {
        // Get the list of messages both locally and remotely.
        // Resolve both lists into three types of operations: a message exists only locally,
        // or a message exists only remotely, or a message exists both locally and remotely.
        // For each case, call the appropriately named method, which may in turn append actions
        // to the localActions or remoteActions lists, which we will later ask the stores to
        // process.
        List<VvmStore.Action> localActions = new ArrayList<VvmStore.Action>();
        List<VvmStore.Action> remoteActions = new ArrayList<VvmStore.Action>();
        Map<String, Voicemail> remoteMap = buildMap(mRemoteResults.get());
        for (Voicemail localMessage : mLocalResults.get()) {
            if (remoteMap.containsKey(localMessage.getSourceData())) {
                Voicemail remoteMessage = remoteMap.remove(localMessage.getSourceData());
                mResolvePolicy.resolveBothLocalAndRemoteMessage(
                        localMessage, remoteMessage, localActions, remoteActions);
            } else {
                mResolvePolicy.resolveLocalOnlyMessage(localMessage, localActions, remoteActions);
            }
        }
        // Because we did a remove() from remoteMap during the loop through the list of local
        // results, we know that remoteMap's values() contains only messages that are missing
        // locally.
        for (Voicemail remoteMessage : remoteMap.values()) {
            mResolvePolicy.resolveRemoteOnlyMessage(remoteMessage, localActions, remoteActions);
        }

        // Perform the list of actions for both stores.
        // I could optimise this to skip the call in the case that the list is empty, but this is
        // handled by the stores in any case since performActions with empty list is a no-op, and
        // the code is much more elegant this way.
        AtomicInteger actionsRemaining = new AtomicInteger(2);
        AtomicBoolean failureReported = new AtomicBoolean(false);
        logger.d("localActions: " + localActions);
        logger.d("remoteActions: " + remoteActions);
        mLocalStore.performActions(localActions,
                new ActionCompletedCallback(actionsRemaining, failureReported));
        mRemoteStore.performActions(remoteActions,
                new ActionCompletedCallback(actionsRemaining, failureReported));
    }

    private class ActionCompletedCallback implements Callback<Void> {
        private final AtomicInteger mActionsRemaining;
        private final AtomicBoolean mFailureReported;

        public ActionCompletedCallback(AtomicInteger actionsRemaining, AtomicBoolean failureReported) {
            mActionsRemaining = actionsRemaining;
            mFailureReported = failureReported;
        }

        @Override
        public void onSuccess(Void result) {
            // A list of actions succeeded.
            // If this is the last list of actions to complete, call the original callback.
            if (mActionsRemaining.decrementAndGet() == 0) {
                mCallback.onSuccess(null);
            }
        }

        @Override
        public void onFailure(Exception error) {
            // A list of actions has failed.
            // Fail the original callback, unless we've already failed once.
            if (!mFailureReported.getAndSet(true)) {
                mCallback.onFailure(error);
            }
        }
    }
}
