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

import com.google.android.voicemail.example.db.DatabaseColumn;

/**
 * Database columns in accounts database.
 */
public enum AccountsDbColumn implements DatabaseColumn {
    ACCOUNT_ID("account_id", "TEXT PRIMARY KEY", 1),
    PROVISIONING_STATUS("status", "TEXT", 1),
    IMAP_USER_NAME("imp_uname", "TEXT", 1),
    IMAP_PASSWORD("imp_passwd", "TEXT", 1),
    IMAP_PORT("imp_port", "TEXT", 1),
    SERVER_ADDRESS("srv_address", "TEXT", 1),
    CLIENT_SMS_NUMBER("clt_sms_num", "TEXT", 1),
    TUI_NUMBER("tui_number", "TEXT", 1),
    SUBSCRIPTION_URL("subn_url", "TEXT", 1);

    private final String mName;
    private final String mSqlType;
    private final int mSinceVersion;

    private AccountsDbColumn(String name, String sqlType, int sinceVersion) {
        mName = name;
        mSqlType = sqlType;
        mSinceVersion = sinceVersion;
    }

    @Override
    public String getColumnName() {
        return mName;
    }

    @Override
    public String getColumnType() {
        return mSqlType;
    }

    @Override
    public int getSinceVersion() {
        return mSinceVersion;
    }
}
