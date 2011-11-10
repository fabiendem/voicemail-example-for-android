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
package com.google.android.voicemail.example.sms;

import com.google.android.voicemail.example.callbacks.Callbacks;
import com.google.android.voicemail.example.storage.AccountInfo;
import com.google.android.voicemail.example.sync.SyncResolver;
import com.google.android.voicemail.example.sync.VvmStore;
import com.google.android.voicemail.example.sync.VvmStore.Action;
import com.google.android.voicemail.example.sync.VvmStoreActions;
import com.google.android.voicemail.example.util.AccountStoreWrapper;

import android.telephony.SmsMessage;

import com.example.android.voicemail.common.core.Voicemail;
import com.example.android.voicemail.common.core.VoicemailImpl;
import com.example.android.voicemail.common.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Omtp SMS handler. Handles SYNC and STATUS messages and takes appropriate action.
 * <p>
 * This implementation is stateless.
 */
@ThreadSafe
public class OmtpMessageHandlerImpl implements OmtpMessageHandler, OmtpMessage.Visitor {
    private static final Logger logger = Logger.getLogger(OmtpMessageHandler.class);

    private final SmsParser mSmsParser;
    private final AccountStoreWrapper mAccountStore;
    private final SyncResolver mSyncResolver;
    /** Vvm store insert message into. This should be an instance of the local store. */
    private final VvmStore mLocalVvmStore;

    public OmtpMessageHandlerImpl(VvmStore localVvmStore, AccountStoreWrapper accountStore,
            SyncResolver syncResolver, SmsParser smsParser) {
        mLocalVvmStore = localVvmStore;
        mAccountStore = accountStore;
        mSyncResolver = syncResolver;
        mSmsParser = smsParser;
    }

    @Override
    public void process(Object[] omtpSmsPdus) {
        // Notes:
        // 1) OMTP message could be split into multiple messages. Merge them together to build the
        // full OMTP text.
        // 2) The omtpMessage is either included in the userData or in the messageBody. This
        // behavior is
        // likely to vary across different VVM servers. Make sure we can handle both.
        logger.d("Num msgs:" + omtpSmsPdus.length);

        StringBuilder userData = new StringBuilder();
        StringBuilder messageBody = new StringBuilder();
        for (int i = 0; i < omtpSmsPdus.length; i++) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) omtpSmsPdus[i]);
            // TODO: Disable detailed logging after SMS receiver is well tested.
            logMessageDetails(sms);
            messageBody.append(sms.getMessageBody());
            userData.append(extractUserData(sms));
        }

        try {
            mSmsParser.parse(userData.toString()).visit(this);
        } catch (OmtpParseException exceptionUserData) {
            // Failed to parse the user data. Lets try with message body.
            try {
                mSmsParser.parse(messageBody.toString()).visit(this);
            } catch (OmtpParseException exceptionMsgBody) {
                // Failed to parse both. Give up!
                logger.e("Failed to parse userData: " + userData, exceptionUserData);
                logger.e("Failed to parse messageBody: " + messageBody, exceptionMsgBody);
            }
        }
    }

    private String extractUserData(SmsMessage sms) {
        try {
            // OMTP spec does not tell about the encoding. We assume ASCII.
            // UTF-8 sounds safer as it can handle ascii as well as other charsets.
            return new String(sms.getUserData(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("This should have never happened", e);
        }
    }

    @Override
    public void process(String omtpMsgText) {
        try {
            mSmsParser.parse(omtpMsgText).visit(this);
        } catch (OmtpParseException e) {
            logger.e("Error while parsing: " + omtpMsgText, e);
        }
    }

    @Override
    public void visit(SyncMessage syncMessage) {
        logger.d("Received SYNC message:\n" + syncMessage);
        switch (syncMessage.getSyncTriggerEvent()) {
            case NEW_MESSAGE:
                processNewMessage(syncMessage);
                break;
            case MAILBOX_UPDATE:
                mSyncResolver.syncAllMessages(Callbacks.<Void>emptyCallback());
                break;
            case GREETINGS_UPDATE:
                // No action right now.
                break;
        }
    }

    private void processNewMessage(SyncMessage syncMessage) {
        String msgId = syncMessage.getId();
        String sender = syncMessage.getSender();
        long time = syncMessage.getTimestampMillis();
        long duration = syncMessage.getLength();
        // Source name automatically be determined by the content provider.
        String sourcePackageName = null;
        Voicemail voicemail = VoicemailImpl.createForInsertion(time, sender)
                .setDuration(duration)
                .setSourcePackage(sourcePackageName)
                .setSourceData(msgId)
                .build();
        sendInsertRequest(voicemail);
    }

    private void sendInsertRequest(Voicemail voicemail) {
        List<Action> actions = new ArrayList<VvmStore.Action>();
        actions.add(VvmStoreActions.insert(voicemail));
        // TODO: We might want to acquire a wake lock around this operation.
        mLocalVvmStore.performActions(actions, Callbacks.<Void>emptyCallback());
    }

    @Override
    public void visit(StatusMessage statusMessage) {
        logger.d("Received STATUS message:\n" + statusMessage);
        AccountInfo.Builder accountInfoBuilder = new AccountInfo.Builder()
                .setProvisioningStatus(statusMessage.getProvisioningStatus())
                .setSubscriptionUrl(statusMessage.getSubscriptionUrl())
                .setServerAddress(statusMessage.getServerAddress())
                .setTuiAccessNumber(statusMessage.getTuiAccessNumber())
                .setClientSmsDestinationNumber(statusMessage.getClientSmsDestinationNumber())
                .setImapPort(statusMessage.getImapPort())
                .setImapUserName(statusMessage.getImapUserName())
                .setImapPassword(statusMessage.getImapPassword());
        mAccountStore.updateAccountInfo(accountInfoBuilder);
    }

    private void logMessageDetails(SmsMessage sms) {
        StringBuilder sb = new StringBuilder();
        addToString(sb, "sender", sms.getOriginatingAddress());
        addToString(sb, "body", sms.getMessageBody());
        addToString(sb, "pdu", sms.getPdu());
        addToString(sb, "userData", sms.getUserData());
        addToString(sb, "msgClass", sms.getMessageClass().toString());
        addToString(sb, "indexOnIcc", sms.getIndexOnIcc());
        addToString(sb, "serviceCenterAddress", sms.getServiceCenterAddress());
        addToString(sb, "status", sms.getStatus());
        addToString(sb, "isCphsMwiMessage", sms.isCphsMwiMessage());
        addToString(sb, "isEmail", sms.isEmail());
        addToString(sb, "isMWIClearMessage", sms.isMWIClearMessage());
        addToString(sb, "isMWISetMessage", sms.isMWISetMessage());
        addToString(sb, "isMwiDontStore", sms.isMwiDontStore());
        addToString(sb, "isReplace", sms.isReplace());
        addToString(sb, "isReplyPathPresent", sms.isReplyPathPresent());
        addToString(sb, "isStatusReportMessage", sms.isStatusReportMessage());

        logger.d(sb.toString());
    }

    private void addToString(StringBuilder sb, String name, String value) {
        sb.append(name).append(":").append(value).append("\n");
    }

    private void addToString(StringBuilder sb, String name, int value) {
        sb.append(name).append(":").append(value).append("\n");
    }

    private void addToString(StringBuilder sb, String name, boolean value) {
        sb.append(name).append(":").append(value).append("\n");
    }

    private void addToString(StringBuilder sb, String name, byte[] value) {
        sb.append(name).append(":").append(Arrays.toString(value)).append("\n");
    }
}
