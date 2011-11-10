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
import com.google.android.voicemail.example.proxies.FolderDelegate;
import com.google.android.voicemail.example.proxies.FolderProxy;
import com.google.android.voicemail.example.util.AccountDetails;
import com.google.android.voicemail.example.util.AccountStoreWrapper;

import android.content.Context;

import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.store.ImapStore;
import com.example.android.voicemail.common.core.Voicemail;
import com.example.android.voicemail.common.logging.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A one shot implementation of {@link ImapHelper} that runs the commands in the caller's thread
 * itself.
 * <p>
 * You must create a new instance of this class for each operation that you would like to execute.
 * If the same object is used for more than one operation then an {@link IllegalStateException} is
 * thrown.
 * <p>
 * For certain operations, the caller thread will block until the network operation is completed. It
 * is therefore important that this class is not used from the main thread of the process.
 */
public class OneshotSyncImapHelper implements ImapHelper {
    private static final Logger logger = Logger.getLogger(OneshotSyncImapHelper.class);
    private static final String INBOX_FOLDER_NAME = "inbox";

    private final Context mContext;
    private final AccountStoreWrapper mAccountStore;
    private final AtomicBoolean mUsed;

    private volatile FolderProxy mFolder;

    public OneshotSyncImapHelper(Context context, AccountStoreWrapper accountStore) {
        mContext = context;
        mAccountStore = accountStore;
        mUsed = new AtomicBoolean(false);
        mFolder = null;
    }

    /** The caller thread will block until the method returns. */
    @Override
    public void markMessagesAsRead(Callback<Void> callback, Voicemail... voicemails) {
        setFlags(voicemails, callback, Flag.SEEN);
    }

    /** The caller thread will block until the method returns. */
    @Override
    public void markMessagesAsDeleted(Callback<Void> callback, Voicemail... voicemails) {
        setFlags(voicemails, callback, Flag.DELETED);
    }

    private void setFlags(Voicemail[] voicemails, Callback<Void> callback, Flag... flags) {
        onOperationRequested();
        if (voicemails.length == 0) {
            throw new IllegalArgumentException("No voicemails to apply operation on.");
        }
        try {
            mFolder = openImapFolder(Folder.OpenMode.READ_WRITE);
            mFolder.setFlags(convertToImapMessages(voicemails), flags, true);
            callback.onSuccess(null);
        } catch (MessagingException e) {
            callback.onFailure(e);
        } catch (IOException e) {
            callback.onFailure(e);
        } finally {
            closeImapFolder();
        }
    }

    /**
     * Opens inbox folder with the account details fetched from account store.
     *
     * @throws IOException if unable to fetch account details from account store
     * @throws MessagingException if could not open IMAP folder using the account details
     */
    private FolderProxy openImapFolder(Folder.OpenMode openMode)
            throws MessagingException, IOException {
        AccountDetails accountDetails = AccountDetails.fetchFromAccountStore(mAccountStore);
        if (accountDetails == null) {
            throw new IOException("Could not fetch imap account details from account store.");
        }
        return openImapFolder(accountDetails, INBOX_FOLDER_NAME, openMode);
    }

    /** Opens the said folder for specified imap account. */
    /* package for testing */FolderProxy openImapFolder(AccountDetails accountDetails,
            String folderName, Folder.OpenMode openMode) throws MessagingException {
        FolderDelegate folder = new FolderDelegate(
                ImapStore.newInstance(accountDetails.getUriString(), mContext, null).getFolder(
                        folderName));
        folder.open(openMode, null);
        return folder;
    }

    /**
     * Must be called before a client request operation is executed. Throws
     * {@link IllegalStateException} if this is called more than once during the lifetime of this
     * object.
     */
    private void onOperationRequested() {
        if (mUsed.getAndSet(true)) {
            throw new IllegalStateException("Already been used for a previous operation.");
        }
    }

    private void closeImapFolder() {
        if (mFolder != null) {
            try {
                // Note that the current implementation of ImapFolder does not really expunge. This
                // means that messages marked as deleted will not be committed on the server side.
                // They will, however, remain marked as deleted.
                mFolder.close(true);
            } catch (MessagingException e) {
                logger.e("Failed to close imap folder.", e);
            }
        }
    }

    /** Converts an array of {@link Voicemail} objects to Imap {@link Message} objects. */
    private Message[] convertToImapMessages(Voicemail[] voicemails) {
        Message[] messages = new Message[voicemails.length];
        for (int i = 0; i < voicemails.length; ++i) {
            messages[i] = new MimeMessage();
            messages[i].setUid(voicemails[i].getSourceData());
        }
        return messages;
    }
}
