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

import com.google.android.voicemail.example.proxy.SmsManagerProxy;
import com.google.android.voicemail.example.spec.Omtp;
import com.google.android.voicemail.example.spec.ProviderConfig;
import com.google.android.voicemail.example.storage.AccountInfo;
import com.google.android.voicemail.example.util.AccountStoreWrapper;

import android.app.PendingIntent;

import com.example.android.voicemail.common.logging.Logger;

import java.io.UnsupportedEncodingException;

import javax.annotation.Nullable;

/**
 * Implementation of {@link OmtpMessageSender} interface.
 * <p>
 * Provides simple APIs to send different types of mobile originated OMTP SMS to the VVM server.
 */
public class OmtpMessageSenderImpl implements OmtpMessageSender {
    private static final Logger logger = Logger.getLogger(OmtpMessageSender.class);

    private final SmsManagerProxy mSmsManager;
    private final AccountStoreWrapper mAccountStore;
    private final short mApplicationPort;
    private final String mDefaultDestinationNumber;
    private final String mClientType;
    private final Omtp.ProtocolVersion mProtocolVersion;
    private final String mClientPrefix;

    /**
     * Creates a provider specific instance of MessageSender with values picked from the supplied
     * providerConfig.
     * <p>
     * Uses {@link Omtp#CLIENT_PREFIX} for clientPrefix.
     */
    public OmtpMessageSenderImpl(SmsManagerProxy smsManager, AccountStoreWrapper accountStore,
            ProviderConfig providerConfig) {
        this(smsManager,
                accountStore,
                providerConfig.getSmsApplicationPort(),
                providerConfig.getSmsDestinationNumber(),
                providerConfig.getClientType(),
                providerConfig.getProtocolVersion(),
                Omtp.CLIENT_PREFIX);
    }

    /**
     * Creates a new instance of MessageSenderImpl.
     *
     * @param smsManager To be used to send SMS across
     * @param accountStore To be used to fetch SMS destination number
     * @param applicationPort If set to a value > 0 then a binary sms is sent to this port number.
     *            Otherwise, a standard text SMS is sent
     * @param defaultDestinationNumber Destination number to be used, if one is not set in the
     *            account store.
     * @param clientType The "ct" field to be set in the MO message. This is the value used by the
     *            VVM server to identify the client. Certain VVM servers require a specific agreed
     *            value for this field.
     * @param clientPrefix The client prefix requested to be used by the server in its MT messages.
     * @param protocolVersion OMTP protocol version.
     */
    public OmtpMessageSenderImpl(SmsManagerProxy smsManager, AccountStoreWrapper accountStore,
            short applicationPort, String defaultDestinationNumber, String clientType,
            Omtp.ProtocolVersion protocolVersion, String clientPrefix) {
        mSmsManager = smsManager;
        mAccountStore = accountStore;
        mApplicationPort = applicationPort;
        mDefaultDestinationNumber = defaultDestinationNumber;
        mClientType = clientType;
        mProtocolVersion = protocolVersion;
        mClientPrefix = clientPrefix;
    }

    @Override
    public void requestVvmActivation(@Nullable PendingIntent sentIntent) {
        sendSms(buildMessageBody(Omtp.MoSmsRequest.ACTIVATE), sentIntent);
    }

    @Override
    public void requestVvmDeactivation(@Nullable PendingIntent sentIntent) {
        sendSms(buildMessageBody(Omtp.MoSmsRequest.DEACTIVATE), sentIntent);
    }

    @Override
    public void requestVvmStatus(@Nullable PendingIntent sentIntent) {
        sendSms(buildMessageBody(Omtp.MoSmsRequest.STATUS), sentIntent);
    }

    private void sendSms(String text, PendingIntent sentIntent) {
        String destinationAddress;
        AccountInfo accountInfo = mAccountStore.getAccountInfo();
        if (accountInfo != null && accountInfo.hasClientSmsDestinationNumber()) {
            destinationAddress = accountInfo.getClientSmsDestinationNumber();
        } else {
            logger.d("Using default destination number.");
            destinationAddress = mDefaultDestinationNumber;
        }

        // If application port is set to 0 then send simple text message, else send data message.
        if (mApplicationPort == 0) {
            logger.d(String.format("Sending TEXT sms '%s' to %s", text, destinationAddress));
            mSmsManager.sendTextMessage(destinationAddress, null, text, sentIntent, null);
        } else {
            byte[] data;
            try {
                data = text.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Failed to encode: " + text);
            }
            logger.d(String.format("Sending BINARY sms '%s' to %s:%d", text, destinationAddress,
                    mApplicationPort));
            mSmsManager.sendDataMessage(destinationAddress, null, mApplicationPort, data,
                    sentIntent, null);
        }
    }

    // Evolution of MO messages.
    //
    // Activate message:
    // V1.1: Activate:pv=<value>;ct=<value>
    // V1.2: Activate:pv=<value>;ct=<value>;pt=<value>;<Clientprefix>
    // V1.3: Activate:pv=<value>;ct=<value>;pt=<value>;<Clientprefix>
    //
    // Deactivate message:
    // V1.1: Deactivate:pv=<value>;ct=<string>
    // V1.2: Deactivate:pv=<value>;ct=<string>
    // V1.3: Deactivate:pv=<value>;ct=<string>
    //
    // Status message:
    // V1.1: STATUS
    // V1.2: STATUS
    // V1.3: STATUS:pv=<value>;ct=<value>;pt=<value>;<Clientprefix>
    private String buildMessageBody(Omtp.MoSmsRequest request) {
        StringBuilder sb = new StringBuilder();

        // Request code.
        sb.append(request.getCode());
        // Other fields, specific to the request and protocol version.
        switch (request) {
            case ACTIVATE:
                appendProtocolVersionAndClientType(sb);
                if (mProtocolVersion.isGreaterOrEqualTo(Omtp.ProtocolVersion.V1_2)) {
                    appendApplicationport(sb);
                    appendClientPrefix(sb);
                }
                break;
            case DEACTIVATE:
                appendProtocolVersionAndClientType(sb);
                break;
            case STATUS:
                if (mProtocolVersion.isGreaterOrEqualTo(Omtp.ProtocolVersion.V1_3)) {
                    appendProtocolVersionAndClientType(sb);
                    appendApplicationport(sb);
                    appendClientPrefix(sb);
                }
                break;
        }
        return sb.toString();
    }

    void appendProtocolVersionAndClientType(StringBuilder sb) {
        sb.append(Omtp.SMS_PREFIX_SEPARATOR);
        appendField(sb, Omtp.MoSmsFields.PROTOCOL_VERSION, mProtocolVersion.getCode());
        sb.append(Omtp.SMS_FIELD_SEPARATOR);
        appendField(sb, Omtp.MoSmsFields.CLIENT_TYPE, mClientType);
    }

    void appendApplicationport(StringBuilder sb) {
        sb.append(Omtp.SMS_FIELD_SEPARATOR);
        appendField(sb, Omtp.MoSmsFields.APPLICATION_PORT, mApplicationPort);
    }

    void appendClientPrefix(StringBuilder sb) {
        sb.append(Omtp.SMS_FIELD_SEPARATOR);
        sb.append(mClientPrefix);
    }

    private void appendField(StringBuilder sb, Omtp.MoSmsFields field, Object value) {
        sb.append(field.getKey())
                .append(Omtp.SMS_KEY_VALUE_SEPARATOR)
                .append(value);
    }
}
