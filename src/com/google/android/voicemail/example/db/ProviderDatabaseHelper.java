/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.voicemail.example.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class to create a database helper for a set of columns.
 * <p>
 * This class simply wraps a {@link TableCreator}.
 */
public class ProviderDatabaseHelper extends SQLiteOpenHelper {
    /** The version of the database to create. */
    private final int mVersion;
    /** A helper object to create the table. */
    private final TableCreator mTableCreator;

    public ProviderDatabaseHelper(Context context, String databaseName,
                                int version, String tableName,
                                DatabaseColumn[] columns) {
        super(context, databaseName, null, version);

        mVersion = version;
        mTableCreator = new TableCreator(tableName, columns);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(mTableCreator.getCreateTableQuery(mVersion));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(mTableCreator.getUpgradeTableQuery(oldVersion, newVersion));
    }
}
