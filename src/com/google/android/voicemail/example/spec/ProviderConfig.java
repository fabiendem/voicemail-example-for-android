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
package com.google.android.voicemail.example.spec;

import javax.annotation.concurrent.Immutable;

/**
 * A dumb data object that holds static parameters that are specific to VVM provider.
 * <p>
 * This object must only holds parameters that fall under one of the following 2 categories - 1)
 * Parameters needed to boot strap communication with the VVM server. 2) Parameters that cannot be
 * obtained dynamically from the server. Other details (like IMAP connection details) should be
 * obtained dynamically and stored in in account store.
 */
@Immutable
public class ProviderConfig {
    private final Omtp.ProtocolVersion mProtocolVersion;
    private final String mClientType;
    private final String mSmsDestinationNumber;
    private final short mSmsApplicationPort;
    private final String mDateFormat;

    public ProviderConfig(Omtp.ProtocolVersion protocolVersion, String clientType,
            String smsDestinationNumber, short smsApplicationPort, String dateFormat) {
        mProtocolVersion = protocolVersion;
        mClientType = clientType;
        mSmsDestinationNumber = smsDestinationNumber;
        mSmsApplicationPort = smsApplicationPort;
        mDateFormat = dateFormat;
    }

    /** Returns the client type ("pv") parameter to be used in various Mobile Originated SMS. */
    public Omtp.ProtocolVersion getProtocolVersion() {
        return mProtocolVersion;
    }

    /** Returns the client type ("ct") parameter to be used in various Mobile Originated SMS. */
    public String getClientType() {
        return mClientType;
    }

    /** Returns the destination number to send MO SMS. */
    public String getSmsDestinationNumber() {
        return mSmsDestinationNumber;
    }

    /**
     * The agreed binary SMS port number. The same port number is expected to be used for both MT as
     * well as MO messages.
     */
    public short getSmsApplicationPort() {
        return mSmsApplicationPort;
    }

    /** Returns the date format as used by this provider */
    public String getDateFormat() {
        return mDateFormat;
    }
}
