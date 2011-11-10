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
package com.google.android.voicemail.example.storage;

import com.google.android.voicemail.example.spec.Omtp;
import com.google.android.voicemail.example.spec.OmtpUtil;

import android.database.Cursor;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An immutable data object representing account related info stored in accounts database.
 * <p>
 * Account phone number is a mandatory field.
 */
@Immutable
public class AccountInfo {
    private final String mAccountId;
    private final Omtp.ProvisioningStatus mProvisioningStatus;
    private final String mSubscriptionUrl;
    private final String mServerAddress;
    private final String mTuiAccessNumber;
    private final String mClientSmsDestinationNumber;
    private final String mImapPort;
    private final String mImapUserName;
    private final String mImapPassword;

    @Override
    public String toString() {
        return "AccountInfo [mAccountPhoneNumber=" + mAccountId
                + ", mProvisioningStatus=" + mProvisioningStatus
                + ", mSubscriptionUrl=" + mSubscriptionUrl
                + ", mServerAddress=" + mServerAddress
                + ", mTuiAccessNumber=" + mTuiAccessNumber
                + ", mClientSmsDestinationNumber=" + mClientSmsDestinationNumber
                + ", mImapPort=" + mImapPort
                + ", mImapUserName=" + mImapUserName
                + ", mImapPassword=" + mImapPassword + "]";
    }

    private AccountInfo(String accountId,
            Omtp.ProvisioningStatus provisioningStatus,
            String subscriptionUrl,
            String serverAddress,
            String tuiAccessNumber,
            String clientSmsDestinationNumber,
            String imapPort,
            String imapUserName,
            String imapPassword) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId must be set in AccountInfo");
        }
        mAccountId = accountId;
        mProvisioningStatus = provisioningStatus;
        mSubscriptionUrl = subscriptionUrl;
        mServerAddress = serverAddress;
        mTuiAccessNumber = tuiAccessNumber;
        mClientSmsDestinationNumber = clientSmsDestinationNumber;
        mImapPort = imapPort;
        mImapUserName = imapUserName;
        mImapPassword = imapPassword;
    }

    public String getAccountId() {
        return mAccountId;
    }

    @Nullable
    public String getSubscriptionUrl() {
        return mSubscriptionUrl;
    }

    public boolean hasSubscriptionUrl() {
        return mSubscriptionUrl != null;
    }

    @Nullable
    public Omtp.ProvisioningStatus getProvisioningStatus() {
        return mProvisioningStatus;
    }

    public boolean hasProvisioningStatus() {
        return mProvisioningStatus != null;
    }

    @Nullable
    public String getServerAddress() {
        return mServerAddress;
    }

    public boolean hasServerAddress() {
        return mServerAddress != null;
    }

    @Nullable
    public String getTuiAccessNumber() {
        return mTuiAccessNumber;
    }

    public boolean hasTuiAccessNumber() {
        return mTuiAccessNumber != null;
    }

    @Nullable
    public String getClientSmsDestinationNumber() {
        return mClientSmsDestinationNumber;
    }

    public boolean hasClientSmsDestinationNumber() {
        return mClientSmsDestinationNumber != null;
    }

    @Nullable
    public String getImapPort() {
        return mImapPort;
    }

    public boolean hasImapPort() {
        return mImapPort != null;
    }

    @Nullable
    public String getImapUserName() {
        return mImapUserName;
    }

    public boolean hasImapUserName() {
        return mImapUserName != null;
    }

    @Nullable
    public String getImapPassword() {
        return mImapPassword;
    }

    public boolean hasImapPassword() {
        return mImapPassword != null;
    }

    /**
     * Builder class for {@link AccountInfo}.
     * <p>
     * Allows creation of immutable {@link AccountInfo} objects either by setting individual fields
     * or directly from a {@link Cursor} object obtained from {@link AccountsDatabase}.
     */
    public static class Builder {
        private String mAccountId;
        private Omtp.ProvisioningStatus mProvisioningStatus;
        private String mSubscriptionUrl;
        private String mServerAddress;
        private String mTuiAccessNumber;
        private String mClientSmsDestinationNumber;
        private String mImapPort;
        private String mImapUserName;
        private String mImapPassword;

        public Builder setAccountId(String accountId) {
            mAccountId = accountId;
            return this;
        }

        public Builder setProvisioningStatus(Omtp.ProvisioningStatus provisioningStatus) {
            mProvisioningStatus = provisioningStatus;
            return this;
        }

        public Builder setSubscriptionUrl(String subscriptionUrl) {
            mSubscriptionUrl = subscriptionUrl;
            return this;
        }

        public Builder setServerAddress(String serverAddress) {
            mServerAddress = serverAddress;
            return this;
        }

        public Builder setTuiAccessNumber(String tuiAccessNumber) {
            mTuiAccessNumber = tuiAccessNumber;
            return this;
        }

        public Builder setClientSmsDestinationNumber(String clientSmsDestinationNumber) {
            mClientSmsDestinationNumber = clientSmsDestinationNumber;
            return this;
        }

        public Builder setImapPort(String imapPort) {
            mImapPort = imapPort;
            return this;
        }

        public Builder setImapUserName(String imapUserName) {
            mImapUserName = imapUserName;
            return this;
        }

        public Builder setImapPassword(String imapPassword) {
            mImapPassword = imapPassword;
            return this;
        }

        public Builder setFieldsFromCursor(Cursor cursor) {
            mProvisioningStatus = OmtpUtil.omtpValueToEnumValue(
                    getCursorStringValue(AccountsDbColumn.PROVISIONING_STATUS, cursor),
                    Omtp.ProvisioningStatus.class);
            mSubscriptionUrl = getCursorStringValue(AccountsDbColumn.SUBSCRIPTION_URL, cursor);
            mServerAddress = getCursorStringValue(AccountsDbColumn.SERVER_ADDRESS, cursor);
            mTuiAccessNumber = getCursorStringValue(AccountsDbColumn.TUI_NUMBER, cursor);
            mClientSmsDestinationNumber =
                    getCursorStringValue(AccountsDbColumn.CLIENT_SMS_NUMBER, cursor);
            mImapPort = getCursorStringValue(AccountsDbColumn.IMAP_PORT, cursor);
            mImapUserName = getCursorStringValue(AccountsDbColumn.IMAP_USER_NAME, cursor);
            mImapPassword = getCursorStringValue(AccountsDbColumn.IMAP_PASSWORD, cursor);
            return this;
        }

        private String getCursorStringValue(AccountsDbColumn column, Cursor cursor) {
            int columnIndex = cursor.getColumnIndexOrThrow(column.getColumnName());
            return cursor.getString(columnIndex);
        }

        public AccountInfo build() {
            return new AccountInfo(mAccountId, mProvisioningStatus, mSubscriptionUrl,
                    mServerAddress, mTuiAccessNumber, mClientSmsDestinationNumber, mImapPort,
                    mImapUserName, mImapPassword);
        }
    }
}
