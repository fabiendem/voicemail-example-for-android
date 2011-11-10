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
package com.google.android.voicemail.example.activity.fetch;

import com.google.android.voicemail.example.R;
import com.google.android.voicemail.example.callbacks.Callback;
import com.google.android.voicemail.example.callbacks.Callbacks;
import com.google.android.voicemail.example.sms.OmtpMessageSender;
import com.google.android.voicemail.example.sync.SyncResolver;
import com.google.android.voicemail.example.util.AccountDetails;
import com.google.android.voicemail.example.util.AccountStoreWrapper;
import com.google.android.voicemail.example.util.IntentListener;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import com.android.email.Email;
import com.android.email.mail.Address;
import com.android.email.mail.Body;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;
import com.android.email.mail.internet.MimeBodyPart;
import com.android.email.mail.internet.MimeMultipart;
import com.android.email.mail.internet.TextBody;
import com.android.email.mail.store.ImapStore;
import com.example.android.voicemail.common.core.VoicemailProviderHelper;
import com.example.android.voicemail.common.logging.Logger;
import com.example.android.voicemail.common.ui.DialogHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Presentation logic for the test IMAP fetching application.
 * <p>
 * This class is not thread safe. It handles concurrency via thread-confinement - all method calls
 * are made into this class on the ui thread.
 */
@NotThreadSafe
/*package*/ class FetchVoicemailPresenter {
    /**
     * Abstraction of the view the presenter is controlling.
     */
    public interface VVMImapView {
        public void setUploadButtonListener(View.OnClickListener listener);
        public void setSyncMessagesButtonListener(View.OnClickListener listener);
        public void goToVoicemailListAndFinish();
        public void setSyncInProgress();
        public void setSyncCompleted();
        public void setActivateVvmButtonListener(View.OnClickListener listener);
        public void setDeactivateVvmButtonListener(View.OnClickListener listener);
        public void setGetVvmStatusButtonListener(View.OnClickListener listener);
        public void setSmsSendInProgress();
        public void setSmsSendCompleted();
    }

    private static final Logger logger = Logger.getLogger(FetchVoicemailPresenter.class);
    private static final String ACTION_SMS_SENT = "com.google.android.voicemail.example.SMS_SENT";

    // TODO: 3. Upload headers, make them the same as the greeting example from OMTP.
    // TODO: 3. Fetching messages, resolve their uids against messages already fetched.
    // TODO: 3. Store the uids of fetched messages into the content provider.

    private final Activity mActivity;
    private final VVMImapView mView;
    private final DialogHelper mDialogHelper;
    private final VoicemailProviderHelper mVoicemailProviderHelper;
    private final SyncResolver mSyncResolver;
    private final AccountStoreWrapper mAccountStore;
    private final OmtpMessageSender mMessageSender;

    public FetchVoicemailPresenter(Activity activity, VVMImapView view,
            DialogHelper dialogHelper, VoicemailProviderHelper voicemailProviderHelper,
            SyncResolver syncResolver, AccountStoreWrapper accountStore,
            OmtpMessageSender messageSender) {
        mActivity = activity;
        mView = view;
        mDialogHelper = dialogHelper;
        mVoicemailProviderHelper = voicemailProviderHelper;
        mSyncResolver = syncResolver;
        mAccountStore = accountStore;
        mMessageSender = messageSender;
    }

    // TODO: 2. This method should not be public, and should not be on this class.
    // TODO: 3. Revisit the mime type as well, audio/mpeg I think.
    public void put(Context context, AccountDetails accountDetails) {
        Email.setTempDirectory(context);
        try {
            Store store = ImapStore.newInstance(accountDetails.getUriString(), context, null);
            String folderName = "Greeting";
            Folder folder = store.getFolder(folderName);
            folder.open(Folder.OpenMode.READ_WRITE, null);
            // TODO: 4. Is leaving the message-id blank the right thing to do here? It seems to
            // create a good message id for me on the uploaded message, an example of such a message
            // id follows:
            // Message-ID: <0n6unev1ht2pt35k7u55u2am.1297885654292@email.android.com>
            // But is this message id perfectly safe? Is there danger in leaving it blank?
            Message message = folder.createMessage("");
            message.setFrom(new Address(accountDetails.getUsername()));
            message.addHeader("X-CNS-Greeting-Type", "normal-greeting");
            message.setSubject("uploaded greeting at " + System.currentTimeMillis());
            message.setRecipient(Message.RecipientType.TO,
                    new Address(accountDetails.getUsername()));

            // The following adds a text body to the email, as the first part of a mime multipart
            // message.
            // The text part is encoded in base64.
            TextBody textBody = new TextBody("This is some body text.  That is all.");
            MimeBodyPart textPart = new MimeBodyPart(textBody, "text/plain");
            Body attachment = createFakeAudioAttachment();
            MimeBodyPart attachmentPart = new MimeBodyPart(attachment,
                    "audio/mp3; name=message.mp3");
            attachmentPart.addHeader("Content-Disposition", "attachment; filename=message.mp3");
            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);
            message.setBody(multipart);

            folder.appendMessages(new Message[] {message});
        } catch (MessagingException e) {
            handleException(e);
        }
    }

    private Body createFakeAudioAttachment() {
        final byte[] fakeAudioData = new byte[10];
        return new Body() {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(fakeAudioData);
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                out.write(Base64.encode(fakeAudioData, Base64.CRLF));
            }
        };
    }

    public void onCreate(Bundle savedInstanceState) {
        mView.setUploadButtonListener(new UploadButtonListener());
        mView.setSyncMessagesButtonListener(new SyncMessagesListener());
        // Send message buttons.
        mView.setActivateVvmButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView.setSmsSendInProgress();
                mMessageSender.requestVvmActivation(createSmsPendingIntent());
            }
        });
        mView.setDeactivateVvmButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView.setSmsSendInProgress();
                mMessageSender.requestVvmDeactivation(createSmsPendingIntent());
            }
        });
        mView.setGetVvmStatusButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView.setSmsSendInProgress();
                mMessageSender.requestVvmStatus(createSmsPendingIntent());
            }
        });
    }

    private PendingIntent createSmsPendingIntent() {
        return IntentListener.createAndStartListening(mActivity, ACTION_SMS_SENT,
                Callbacks.uiThreadCallback(mActivity, new SmsSentCallback()));
    }

    private class SmsSentCallback implements Callback<Void> {
        @Override
        public void onFailure(final Exception error) {
            logger.d("Failed to send SMS.");
            mView.setSmsSendCompleted();
            handleException(error);
        }

        @Override
        public void onSuccess(Void result) {
            logger.d("Sms sent succesfully.");
            mView.setSmsSendCompleted();
        }
    }

    private class UploadButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AccountDetails accountDetails =
                    AccountDetails.fetchFromAccountStore(mAccountStore);
            if (accountDetails == null) {
                handleException(new Exception("Couldn't get account details"));
                return;
            }
            put(mActivity, accountDetails);
        }
    }

    private class SyncMessagesListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AccountDetails accountDetails =
                AccountDetails.fetchFromAccountStore(mAccountStore);
            if (accountDetails == null) {
                handleException(new Exception("Couldn't get account details"));
                return;
            }
            mView.setSyncInProgress();
            mSyncResolver.syncAllMessages(new SyncResultCallback());
        }
    }

    private class SyncResultCallback implements Callback<Void> {
        @Override
        public void onFailure(final Exception error) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mView.setSyncCompleted();
                    handleException(error);
                }
            });
        }

        @Override
        public void onSuccess(Void result) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mView.setSyncCompleted();
                    mView.goToVoicemailListAndFinish();
                }
            });
        }
    }

    private void handleException(Exception e) {
        logger.e(e.getMessage(), e);
        mDialogHelper.showErrorMessageDialog(R.string.error_title, e);
    }

    public void menuClearItemsSelected() {
        mVoicemailProviderHelper.deleteAll();
        mView.goToVoicemailListAndFinish();
    }
}
