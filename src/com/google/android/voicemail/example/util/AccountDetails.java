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

import com.google.android.voicemail.example.storage.AccountInfo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Account details for an email account.
 * <p>
 * Used to create an IMAP connection to fetch voicemails.
 */
@Immutable
public final class AccountDetails {
    // TODO: Whether to use SSL or not should probably come from provider config. Right now
    // using a constant.
    private static final boolean USE_SSL = false;

    /**
     * Default account details to be used for IMAP connection. Used when no account info is
     * populated in the accounts database. Accounts database is typically populated through an
     * account status SMS sent from the server.
     */
    private static AccountDetails sDefaultAccountDetails = new AccountDetails(
            "user@example.com",         // IMAP user login
            "my-secret-passwd",         // IMAP password
            "imap_server.example.com",  // IMAP server address
            993,                        // IMAP port number.
            USE_SSL);

    /** Returns the default account details to be used if no account is configured. */
    public static AccountDetails getDefaultAccountDetails() {
        return sDefaultAccountDetails;
    }

    /** Changes the default account details to be used if no account is configured. */
    public static void setDefaultAccountDetails(AccountDetails defaultAccountDetails) {
        sDefaultAccountDetails = defaultAccountDetails;
    }

    /**
     * Utility method to fetch account details from the given account store.
     * <p>
     * Returns the default account details if the account details did not exist.
     */
    @Nullable
    public static AccountDetails fetchFromAccountStore(AccountStoreWrapper accountStore) {
        AccountInfo accountInfo = accountStore.getAccountInfo();
        if (accountInfo == null) {
            return getDefaultAccountDetails();
        }
        return new AccountDetails(accountInfo.getImapUserName(),
                accountInfo.getImapPassword(),
                accountInfo.getServerAddress(),
                accountInfo.hasImapPort() ? Integer.valueOf(accountInfo.getImapPort()) : 0,
                USE_SSL);
    }

    private final String mUsername;
    private final String mPassword;
    private final String mServerAddress;
    private final int mServerPort;
    private final boolean mUseSsl;

    /**
     * Standard constructor for an AccountDetails.
     *
     * @param username Account username, of the format {@code user@domain.com}
     * @param password Account password, for example {@code foo38792}
     * @param serverAddress Server address, for example {@code imap.domain.com}
     * @param serverPort Server port, for example {@code 993}
     */
    public AccountDetails(String username, String password, String serverAddress,
            int serverPort, boolean useSsl) {
        mUsername = username;
        mPassword = password;
        mServerAddress = serverAddress;
        mServerPort = serverPort;
        mUseSsl = useSsl;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getServerAddress() {
        return mServerAddress;
    }

    public int getServerPort() {
        return mServerPort;
    }

    private String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("Not possible, all JVMs support UTF-8");
        }
    }

    public String getUriString() {
        String scheme = mUseSsl ? "imap+ssl" : "imap";
        return new StringBuilder()
                .append(scheme)
                .append("://")
                .append(urlEncode(getUsername()))
                .append(":")
                .append(getPassword())
                .append("@")
                .append(getServerAddress())
                .append(":")
                .append(getServerPort())
                .toString();
    }
}
