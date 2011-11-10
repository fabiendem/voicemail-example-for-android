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

import com.google.android.voicemail.example.sync.VvmStore.Operation;

import com.example.android.voicemail.common.core.Voicemail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for working with {@link VvmStore.Action} objects.
 */
public class VvmStoreActions {
    private static class RealAction implements VvmStore.Action {
        private final Voicemail mVoicemail;
        private final Operation mOperation;

        public RealAction(Voicemail voicemail, VvmStore.Operation operation) {
            mVoicemail = voicemail;
            mOperation = operation;
        }

        @Override
        public Voicemail getVoicemail() {
            return mVoicemail;
        }

        @Override
        public Operation getOperation() {
            return mOperation;
        }

        @Override
        public String toString() {
            return "RealAction [op=" + mOperation + ", msg=" + mVoicemail + "]";
        }
    }

    public static List<VvmStore.Action> createActions(List<Voicemail> messages,
            VvmStore.Operation operation) {
        List<VvmStore.Action> actions = new ArrayList<VvmStore.Action>();
        for (Voicemail message : messages) {
            actions.add(createAction(message, operation));
        }
        return actions;
    }

    public static VvmStore.Action delete(Voicemail message) {
        return createAction(message, VvmStore.Operation.DELETE);
    }

    public static VvmStore.Action insert(Voicemail message) {
        return createAction(message, VvmStore.Operation.INSERT);
    }

    public static VvmStore.Action fetchContent(Voicemail message) {
        return createAction(message, VvmStore.Operation.FETCH_CONTENT);
    }

    public static VvmStore.Action markAsRead(Voicemail message) {
        return createAction(message, VvmStore.Operation.MARK_AS_READ);
    }

    public static VvmStore.Action createAction(Voicemail message, VvmStore.Operation operation) {
        return new RealAction(message, operation);
    }

    /**
     * Builds a map from the {@link VvmStore.Operation} to the set of message ids for a given
     * collection of actions.
     * <p>
     * This is currently used as a very simple way to test if two lists of actions are equal, that
     * is to say if they perform the same actions on the same groups of messages.
     */
    public static Map<VvmStore.Operation, Set<String>> buildOperationMap(
            Collection<VvmStore.Action> actions) {
        Map<VvmStore.Operation, Set<String>> map = new HashMap<VvmStore.Operation, Set<String>>();
        for (VvmStore.Action action : actions) {
            Set<String> messageIds = map.get(action.getOperation());
            if (messageIds == null) {
                messageIds = new HashSet<String>();
                map.put(action.getOperation(), messageIds);
            }
            messageIds.add(action.getVoicemail().getSourceData());
        }
        return map;
    }

    public static boolean areActionsEqual(Collection<VvmStore.Action> first,
            Collection<VvmStore.Action> second) {
        return buildOperationMap(first).equals(buildOperationMap(second));
    }
}
