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

import com.google.android.voicemail.example.db.ProviderDatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.example.android.voicemail.common.logging.Logger;
import com.example.android.voicemail.common.utils.CloseUtils;

/**
 * Database for storing account status data.
 */
public class AccountsDatabase implements AccountStore {
    // TODO: Think of getting rid of unused acountInfo records.
    private static final Logger logger = Logger.getLogger(AccountsDatabase.class);

    private static final String DB_NAME = "omtpsource.db";
    private static final int DB_VERSION = 1;
    private static final String ACCOUNTS_TABLE_NAME = "accounts";

    private final ProviderDatabaseHelper mDbHelper;

    public AccountsDatabase(Context context) {
        mDbHelper = new ProviderDatabaseHelper(context, DB_NAME, DB_VERSION, ACCOUNTS_TABLE_NAME,
                AccountsDbColumn.values());
    }

    @Override
    public void updateAccountInfo(AccountInfo accountInfo) {
        logger.d("updateAccountInfo: " + accountInfo);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = getContentValues(accountInfo);
        db.replace(ACCOUNTS_TABLE_NAME, null, values);
    }

    @Override
    public AccountInfo getAccountInfo(String accountId) {
        // Read all columns.
        String selection = getEqualityClause(AccountsDbColumn.ACCOUNT_ID, accountId);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(ACCOUNTS_TABLE_NAME, null, selection, null, null, null, null);
            if (cursor.moveToFirst()) {
                AccountInfo accountInfo = new AccountInfo.Builder()
                        .setAccountId(accountId)
                        .setFieldsFromCursor(cursor)
                        .build();
                logger.d("Fetched accountInfo: " + accountInfo);
                return accountInfo;
            } else {
                // No record found.
                return null;
            }
        } finally {
            CloseUtils.closeQuietly(cursor);
        }
    }

    /** Returns ContentValues populated with values from the supplied account info object */
    /* package for testing */
    static ContentValues getContentValues(AccountInfo accountInfo) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(AccountsDbColumn.ACCOUNT_ID.getColumnName(),
                accountInfo.getAccountId());

        if (accountInfo.hasImapUserName()) {
            contentValues.put(AccountsDbColumn.IMAP_USER_NAME.getColumnName(),
                    accountInfo.getImapUserName());
        }
        if (accountInfo.hasImapPassword()) {
            contentValues.put(AccountsDbColumn.IMAP_PASSWORD.getColumnName(),
                    accountInfo.getImapPassword());
        }
        if (accountInfo.hasImapPort()) {
            contentValues.put(AccountsDbColumn.IMAP_PORT.getColumnName(),
                    accountInfo.getImapPort());
        }
        if (accountInfo.hasServerAddress()) {
            contentValues.put(AccountsDbColumn.SERVER_ADDRESS.getColumnName(),
                    accountInfo.getServerAddress());
        }
        if (accountInfo.hasSubscriptionUrl()) {
            contentValues.put(AccountsDbColumn.SUBSCRIPTION_URL.getColumnName(),
                    accountInfo.getSubscriptionUrl());
        }
        if (accountInfo.hasTuiAccessNumber()) {
            contentValues.put(AccountsDbColumn.TUI_NUMBER.getColumnName(),
                    accountInfo.getTuiAccessNumber());
        }
        if (accountInfo.hasClientSmsDestinationNumber()) {
            contentValues.put(AccountsDbColumn.CLIENT_SMS_NUMBER.getColumnName(),
                    accountInfo.getClientSmsDestinationNumber());
        }
        if (accountInfo.hasProvisioningStatus()) {
            // We store the original SMTP code for account status.
            contentValues.put(AccountsDbColumn.PROVISIONING_STATUS.getColumnName(),
                    accountInfo.getProvisioningStatus().getCode());
        }
        return contentValues;
    }

    /** Returns a WHERE clause assert equality of a field to a value. */
    private String getEqualityClause(AccountsDbColumn column, String value) {
        StringBuilder clause = new StringBuilder();
        clause.append("(");
        clause.append(ACCOUNTS_TABLE_NAME);
        clause.append(".");
        clause.append(column.getColumnName());
        clause.append(" = ");
        DatabaseUtils.appendEscapedSQLString(clause, value);
        clause.append(")");
        return clause.toString();
    }
}
