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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.example.android.voicemail.common.logging.Logger;

/**
 * A utility class that enables implementing a simple callback based notification model built on top
 * of an intent based notification model.
 * <p>
 * It registers a broadcast receiver for the given action and invokes the callback when the intent
 * is received. The callback is invoked from within the onReceive() method of a
 * {@link BroadcastReceiver}.
 * <p>
 * @see BroadcastReceiver#onReceive(Context, Intent) for things you may or may not do here.
 */
public class IntentListener {
    private static final Logger logger = Logger.getLogger(IntentListener.class);

    /** Exception thrown when a failure result code is received for the action. */
    public class IntentListenerException extends Exception {
        private static final long serialVersionUID = -6938161560362055785L;

        private final int mResultCode;

        public IntentListenerException(int resultCode, String action) {
            super(String.format("%s failed with result code: %d", action, resultCode));
            mResultCode = resultCode;
        }

        public int getResultCode() {
            return mResultCode;
        }
    }

    private final Context mContext;
    private final String mAction;
    private final int mSuccessReturnCode;
    private BroadcastReceiver mRegisteredReceiver;

    /**
     * A utility method that creates a oneshot IntentListener and starts listening for the returned
     * {@link PendingIntent}. This also makes sure that the listener stops listening immediately
     * after the first matching intent is handled.
     *
     * @see IntentListener
     */
    public static PendingIntent createAndStartListening(Context context, String action,
            final Callback<Void> callback) {
        final IntentListener listener = new IntentListener(context, action, Activity.RESULT_OK);
        PendingIntent pendingIntent = listener.createPendingIntent();
        listener.startListening(new Callback<Void>() {
            @Override
            public void onFailure(Exception error) {
                listener.stopListening();
                callback.onFailure(error);
            }

            @Override
            public void onSuccess(Void result) {
                listener.stopListening();
                callback.onSuccess(result);
            }
        });
        return pendingIntent;
    }

    /**
     * Creates a {@link PendingIntent} that this {@link IntentListener} can listen to.
     * <p>
     * Typically the {@link PendingIntent} obtained through this method is handed over to a system
     * that would fire the intent on completion of some task.
     */
    public PendingIntent createPendingIntent() {
        return PendingIntent.getBroadcast(mContext, 0, new Intent(mAction), 0);
    }

    /**
     * Creates an instance of this class.
     *
     * @param context The context of the calling component (typically an activity)
     * @param action The action to listen to
     * @param successResultCode The resultCode that is considered to be success. On receiving this
     *            result code the callback's onSuccess() method is invoked. For all other result
     *            codes the callback's onFailure() method is invoked with
     *            {@link IntentListenerException}
     */
    public IntentListener(Context context, String action, int successResultCode) {
        mContext = context;
        mAction = action;
        mSuccessReturnCode = successResultCode;
    }

    /**
     * Start listening for the intent, i.e., register the broadcast receiver. It is not allowed to
     * call this method while the listener is already listening to an event. However, once the
     * listener is stopped then it is OK to call this method again to start listening for another
     * event.
     *
     * @param callback The callback to be invoked when an intent with the specified action is
     *            received
     * @throws IllegalStateException if this method is called more than once in succession.
     */
    public synchronized void startListening(Callback<Void> callback) {
        if (mRegisteredReceiver != null) {
            throw new IllegalStateException("Already listening!");
        }
        logger.d("starting to listen");
        mRegisteredReceiver = new IntentReceiver(callback);
        mContext.registerReceiver(mRegisteredReceiver, new IntentFilter(mAction));
    }

    /**
     * Stop listening for the intent, i.e., unregister the broadcast receiver.
     */
    public synchronized void stopListening() {
        logger.d("stopped listening");
        if (mRegisteredReceiver != null) {
            mContext.unregisterReceiver(mRegisteredReceiver);
            mRegisteredReceiver = null;
        }
    }

    private class IntentReceiver extends BroadcastReceiver {
        private final Callback<Void> mCallback;

        public IntentReceiver(Callback<Void> callback) {
            mCallback = callback;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            logger.d("received intent: " + intent);
            if (intent.getAction().equals(mAction)) {
                int resultCode = getResultCode();
                if (mSuccessReturnCode == resultCode) {
                    mCallback.onSuccess(null);
                } else {
                    mCallback.onFailure(new IntentListenerException(resultCode, mAction));
                }
            } else {
                logger.e("Unexpected action: " + intent.getAction());
            }
        }
    }
}
