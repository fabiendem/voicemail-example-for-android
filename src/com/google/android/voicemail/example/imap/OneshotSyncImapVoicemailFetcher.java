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
package com.google.android.voicemail.example.imap;

import com.google.android.voicemail.example.callbacks.Callback;
import com.google.android.voicemail.example.core.VoicemailPayload;
import com.google.android.voicemail.example.core.VoicemailPayloadImpl;
import com.google.android.voicemail.example.proxies.FolderDelegate;
import com.google.android.voicemail.example.proxies.FolderProxy;
import com.google.android.voicemail.example.util.AccountDetails;
import com.google.android.voicemail.example.util.VoicemailFetcher;

import android.content.Context;
import android.util.Base64;

import com.android.email.Email;
import com.android.email.mail.Address;
import com.android.email.mail.Body;
import com.android.email.mail.BodyPart;
import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Multipart;
import com.android.email.mail.Store;
import com.android.email.mail.store.ImapStore;
import com.example.android.voicemail.common.core.Voicemail;
import com.example.android.voicemail.common.core.VoicemailImpl;
import com.example.android.voicemail.common.logging.Logger;
import com.example.android.voicemail.common.utils.CloseUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.concurrent.ThreadSafe;

/**
 * One-shot synchronous fetcher for voicemail from an IMAP server.
 * <p>
 * A one-shot class, construct this and then call either {@link #fetchAllVoicemails(Callback)} or
 * {@link #fetchVoicemailPayload(String, Callback)}. Subsequent calls to
 * {@link #fetchAllVoicemails(Callback)} or {@link #fetchVoicemailPayload(String, Callback)} will
 * immediately fail.
 */
@ThreadSafe
/*package*/ class OneshotSyncImapVoicemailFetcher implements VoicemailFetcher {
    private static final Logger logger = Logger.getLogger(OneshotSyncImapVoicemailFetcher.class);

    private final Context mContext;
    private final AccountDetails mAccountDetails;
    private final AtomicBoolean mStarted;
    private final AtomicBoolean mFinished;
    // TODO: Merge functionality of ImapHelper into this class.
    private final ImapHelper mImapHelper;
    private volatile FolderProxy mFolder;

    public OneshotSyncImapVoicemailFetcher(Context context, AccountDetails accountDetails,
            ImapHelper imapHelper) {
        mContext = context;
        mAccountDetails = accountDetails;
        mImapHelper = imapHelper;
        mStarted = new AtomicBoolean(false);
        mFinished = new AtomicBoolean(false);
        mFolder = null;
    }

    @Override
    public void fetchAllVoicemails(final Callback<List<Voicemail>> callback) {
        executeWithFolder(
                new Callable<Void>() {
                    @Override
                    public Void call() throws MessagingException {
                        List<Voicemail> result = new ArrayList<Voicemail>();
                        Message[] messages = mFolder.getMessages(null);
                        for (Message message : messages) {
                            Voicemail voicemail = fetchVoicemail(message, callback);
                            if (voicemail != null) {
                                result.add(voicemail);
                            }
                        }
                        if (!mFinished.getAndSet(true)) {
                            callback.onSuccess(result);
                        }
                        return null;
                    }
                },
                callback);
    }

    @Override
    public void fetchVoicemailPayload(final String uid, final Callback<VoicemailPayload> callback) {
        executeWithFolder(
                new Callable<Void>() {
                    @Override
                    public Void call() throws MessagingException {
                        Message message = mFolder.getMessage(uid);
                        VoicemailPayload voicemailPayload = fetchVoicemailPayload(message, callback);
                        if (!mFinished.getAndSet(true)) {
                            callback.onSuccess(voicemailPayload);
                        }
                        return null;
                    }
                },
                callback);
    }

    @Override
    public void markMessagesAsRead(Callback<Void> callback, Voicemail... voicemails) {
        mImapHelper.markMessagesAsRead(callback, voicemails);
    }

    @Override
    public void markMessagesAsDeleted(Callback<Void> callback, Voicemail... voicemails) {
        mImapHelper.markMessagesAsDeleted(callback, voicemails);
    }

    /**
     * Executes the given runnable while the inbox folder is open.
     * <p>
     * It takes care of handling failures with opening the folder and closing the folder after the
     * operation completed.
     *
     * @param callable the code to run while the folder is open
     * @param failureCallback the callback to notify the first time a failure occurs
     */
    private void executeWithFolder(Callable<Void> callable, Callback<?> failureCallback) {
        if (mStarted.getAndSet(true)) {
            throw new IllegalStateException("Already have a fetch in progress");
        }
        try {
            mFolder = openFolder("inbox");
            mFolder.open(Folder.OpenMode.READ_ONLY, null);
            callable.call();
            closeMailbox();
        } catch (Exception e) {
            handleFailure(e, failureCallback);
        }
    }

    private void closeMailbox() {
        FolderProxy folder = mFolder;
        mFolder = null;

        if (folder != null) {
            try {
                folder.close(false);
            } catch (MessagingException e) {
                logger.e("failure while closing folder", e);
            }
        }
    }

    // Visible for testing.
    protected FolderProxy openFolder(String name) throws MessagingException {
        Email.setTempDirectory(mContext);
        Store store = ImapStore.newInstance(mAccountDetails.getUriString(), mContext, null);
        return new FolderDelegate(store.getFolder(name));
    }

    private void handleFailure(Exception e, Callback<?> callback) {
        if (!mFinished.getAndSet(true)) {
            closeMailbox();
            callback.onFailure(e);
        }
    }

    private Voicemail getVoicemailFromMessage(Message message) throws MessagingException {
        if (!message.getMimeType().startsWith("multipart/")) {
            logger.w("Ignored non multi-part message");
            return null;
        }
        Multipart multipart = (Multipart) message.getBody();
        logger.d("Num body parts: " + multipart.getCount());
        for (int i = 0; i < multipart.getCount(); ++i) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String bodyPartMimeType = bodyPart.getMimeType().toLowerCase();
            logger.d("bodyPart mime type: " + bodyPartMimeType);
            if (bodyPartMimeType.startsWith("audio/")) {
                // Found an audio attachment, this is a valid voicemail.
                VoicemailImpl.Builder voicemailBuilder = VoicemailImpl.createEmptyBuilder()
                        .setTimestamp(message.getSentDate().getTime())
                        .setSourceData(message.getUid());
                setSender(voicemailBuilder, message.getFrom());
                setMailBoxAndReadStatus(voicemailBuilder, message.getFlags());
                return voicemailBuilder.build();
            }
        }
        // No attachment found, this is not a voicemail.
        return null;
    }

    private VoicemailPayload getVoicemailPayloadFromMessage(Message message)
            throws MessagingException, IOException {
        Multipart multipart = (Multipart) message.getBody();
        logger.d("Num body parts: " + multipart.getCount());
        for (int i = 0; i < multipart.getCount(); ++i) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String bodyPartMimeType = bodyPart.getMimeType().toLowerCase();
            logger.d("bodyPart mime type: " + bodyPartMimeType);
            if (bodyPartMimeType.startsWith("audio/")) {
                byte[] bytes = getAudioDataFromBody(bodyPart.getBody());
                logger.d(String.format("Fetched %s bytes of data", bytes.length));
                return new VoicemailPayloadImpl(bodyPartMimeType, bytes);
            }
        }
        throw new MessagingException("No audio attachment found on this voicemail");
    }

    /** Sets the mailbox and read status */
    private void setMailBoxAndReadStatus(VoicemailImpl.Builder voicemailBuilder, Flag[] flags) {
        List<Flag> flagList = Arrays.asList(flags);
        voicemailBuilder.setIsRead(flagList.contains(Flag.SEEN));
    }

    private void setSender(VoicemailImpl.Builder voicemailBuilder, Address[] fromAddresses) {
        if (fromAddresses != null && fromAddresses.length > 0) {
            if (fromAddresses.length != 1) {
                logger.w("More than one from addresses found. Using the first one.");
            }
            String sender = fromAddresses[0].getAddress();
            int atPos = sender.indexOf('@');
            if (atPos != -1) {
                // Strip domain part of the address.
                sender = sender.substring(0, atPos);
            }
            voicemailBuilder.setNumber(sender);
        }
    }

    private String debugStringForMessage(Message message) {
        return new StringBuilder()
                .append("## Message Details: \n")
                .append("UID: " + message.getUid() + "\n")
                .append("FLAGS: " + Arrays.toString(message.getFlags()) + "\n")
                .append(debugStringForHeader(message, "From")).append("\n")
                .append(debugStringForHeader(message, "To")).append("\n")
                .append(debugStringForHeader(message, "Content-Type")).append("\n")
                .append(debugStringForHeader(message, "Date")).append("\n")
                .append(debugStringForHeader(message, "Message-Id"))
                .toString();
    }

    private String debugStringForHeader(Message message, String name) {
        try {
            return name + ": " + Arrays.toString(message.getHeader(name));
        } catch (MessagingException e) {
            return name + ": null";
        }
    }

    private byte[] getAudioDataFromBody(Body body) throws IOException, MessagingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedOutputStream bufferedOut = new BufferedOutputStream(out);
        try {
            body.writeTo(bufferedOut);
        } finally {
            CloseUtils.closeQuietly(bufferedOut);
        }
        return Base64.decode(out.toByteArray(), Base64.DEFAULT);
    }

    /**
     * Fetches the structure of the given message and returns the voicemail parsed from it.
     *
     * @throws MessagingException if fetching the structure of the message fails
     */
    private Voicemail fetchVoicemail(Message message, Callback<?> failureCallback)
            throws MessagingException {
        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.addAll(Arrays.asList(FetchProfile.Item.FLAGS, FetchProfile.Item.ENVELOPE,
                FetchProfile.Item.STRUCTURE));
        logger.d("Fetching message structure for " + message.getUid());
        MessageStructureFetchedListener listener =
                new MessageStructureFetchedListener(failureCallback);
        mFolder.fetch(new Message[] {message}, fetchProfile, listener);
        return listener.getVoicemail();
    }

    /**
     * Fetches the body of the given message and returns the parsed voicemail payload.
     *
     * @throws MessagingException if fetching the body of the message fails
     */
    private VoicemailPayload fetchVoicemailPayload(Message message, Callback<?> failureCallback)
            throws MessagingException {
        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.BODY);
        logger.d("Fetching message body for " + message.getUid());
        MessageBodyFetchedListener listener = new MessageBodyFetchedListener(failureCallback);
        mFolder.fetch(new Message[] {message}, fetchProfile, listener);
        return listener.getVoicemailPayload();
    }

    /**
     * Listener for the message structure being fetched.
     * <p>
     * In case of failure, it calls {@link #handleFailure(Exception, Callback)}.
     */
    private final class MessageStructureFetchedListener implements Folder.MessageRetrievalListener {
        private final Callback<?> mFailureCallback;

        private Voicemail mVoicemail;

        public MessageStructureFetchedListener(Callback<?> failureCallback) {
            mFailureCallback = failureCallback;
        }

        public Voicemail getVoicemail() {
            return mVoicemail;
        }

        @Override
        public void messageRetrieved(Message message) {
            logger.d("Fetched message structure for " + message.getUid());
            logger.d("Message retrieved: " + message);
            // TODO: Get rid of the detailed message logging when we are done with testing.
            logger.d(debugStringForMessage(message));
            try {
                mVoicemail = getVoicemailFromMessage(message);
                if (mVoicemail == null) {
                    logger.d("This voicemail does not have an attachment...");
                    return;
                }
            } catch (MessagingException e) {
                handleFailure(e, mFailureCallback);
            }
        }
    }

    /**
     * Listener for the message body being fetched.
     * <p>
     * In case of failure, it calls {@link #handleFailure(Exception, Callback)}.
     */
    private final class MessageBodyFetchedListener implements Folder.MessageRetrievalListener {
        private final Callback<?> mFailureCallback;

        private VoicemailPayload mVoicemailPayload;

        public MessageBodyFetchedListener(Callback<?> failureCallback) {
            mFailureCallback = failureCallback;
        }

        /** Returns the fetch voicemail payload. */
        public VoicemailPayload getVoicemailPayload() {
            return mVoicemailPayload;
        }

        @Override
        public void messageRetrieved(Message message) {
            logger.d("Fetched message body for " + message.getUid());
            logger.d("Message retrieved: " + message);
            // TODO: Get rid of the detailed message logging when we are done with testing.
            logger.d(debugStringForMessage(message));
            if (mFinished.get()) {
                // Once we've finished, i.e. reported a callback, we ignore further messages.
                return;
            }
            try {
                mVoicemailPayload = getVoicemailPayloadFromMessage(message);
            } catch (MessagingException e) {
                handleFailure(e, mFailureCallback);
            } catch (IOException e) {
                handleFailure(e, mFailureCallback);
            }
        }
    }
}
