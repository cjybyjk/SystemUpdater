/*
 * Copyright (C) 2017 The LineageOS Project
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
 * limitations under the License.
 */

/*
 * Modify:
 *     add column of table
 * Add:
 *     function cleanUpdates, removeUpdate(Update)
 *     get Update objects by SHA1
 * Remove:
 *     function removeUpdate(rowId), removeUpdate(downloadId)
 */ 
package io.github.cjybyjk.systemupdater.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.cjybyjk.systemupdater.model.Update;
import io.github.cjybyjk.systemupdater.model.UpdateStatus;

public class UpdatesDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "updates.db";

    public static class UpdateEntry implements BaseColumns {
        public static final String TABLE_NAME = "updates";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_VERSION = "version";
        public static final String COLUMN_NAME_REQUIREMENT = "requirement";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_SIZE = "size";
        public static final String COLUMN_NAME_STATUS = "status";
        public static final String COLUMN_NAME_DOWNLOAD_URL = "download_url";
        public static final String COLUMN_NAME_DOWNLOAD_PROG = "download_prog";
        public static final String COLUMN_NAME_PATH = "path";
        public static final String COLUMN_NAME_SHA1 = "sha1";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
    }

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + UpdateEntry.TABLE_NAME + " (" +
                    UpdateEntry._ID + " INTEGER PRIMARY KEY," +
                    UpdateEntry.COLUMN_NAME_NAME + " TEXT," +
                    UpdateEntry.COLUMN_NAME_VERSION + " TEXT," +
                    UpdateEntry.COLUMN_NAME_REQUIREMENT + " INTEGER," +
                    UpdateEntry.COLUMN_NAME_TIMESTAMP + " INTEGER," +
                    UpdateEntry.COLUMN_NAME_TYPE + " TEXT," +
                    UpdateEntry.COLUMN_NAME_SIZE + " INTEGER," +
                    UpdateEntry.COLUMN_NAME_STATUS + " INTEGER," +
                    UpdateEntry.COLUMN_NAME_DOWNLOAD_URL + " TEXT," +
                    UpdateEntry.COLUMN_NAME_DOWNLOAD_PROG + " INTEGER," +
                    UpdateEntry.COLUMN_NAME_PATH + " TEXT UNIQUE," +
                    UpdateEntry.COLUMN_NAME_SHA1 + " TEXT NOT NULL UNIQUE," +
                    UpdateEntry.COLUMN_NAME_DESCRIPTION + " TEXT)";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + UpdateEntry.TABLE_NAME;

    public UpdatesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long addUpdate(Update update) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        Update tUpdate;
        try {
            tUpdate = getUpdate(update.getFileSHA1());
        } catch (Exception e) {
            tUpdate = null;
        }
        values.put(UpdateEntry.COLUMN_NAME_NAME, update.getName());
        values.put(UpdateEntry.COLUMN_NAME_VERSION, update.getVersion());
        values.put(UpdateEntry.COLUMN_NAME_REQUIREMENT, update.getRequirement());
        values.put(UpdateEntry.COLUMN_NAME_TIMESTAMP, update.getTimestamp());
        values.put(UpdateEntry.COLUMN_NAME_TYPE, update.getType());
        values.put(UpdateEntry.COLUMN_NAME_SIZE, update.getFileSize());
        values.put(UpdateEntry.COLUMN_NAME_DOWNLOAD_URL, update.getDownloadUrl());
        values.put(UpdateEntry.COLUMN_NAME_DOWNLOAD_PROG, 0);
        values.put(UpdateEntry.COLUMN_NAME_SHA1, update.getFileSHA1());
        values.put(UpdateEntry.COLUMN_NAME_DESCRIPTION, update.getDescription());
        if (tUpdate != null) {
            String selection = UpdateEntry.COLUMN_NAME_SHA1 + " = ?";
            String[] selectionArgs = {tUpdate.getFileSHA1()};
            values.put(UpdateEntry.COLUMN_NAME_STATUS, tUpdate.getStatus());
            values.put(UpdateEntry.COLUMN_NAME_PATH, tUpdate.getFilePath());
            return db.update(UpdateEntry.TABLE_NAME, values,selection, selectionArgs);
        } else {
            values.put(UpdateEntry.COLUMN_NAME_STATUS, update.getStatus());
            values.put(UpdateEntry.COLUMN_NAME_PATH, "");
            return db.insert(UpdateEntry.TABLE_NAME, null, values);
        }
    }

    public boolean removeUpdate(Update update) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = UpdateEntry.COLUMN_NAME_SHA1 + " = ?";
        String[] selectionArgs = {update.getFileSHA1()};
        return db.delete(UpdateEntry.TABLE_NAME, selection, selectionArgs) != 0;
    }

    public boolean changeUpdateStatus(Update update) {
        String selection = UpdateEntry.COLUMN_NAME_SHA1 + " = ?";
        String[] selectionArgs = {update.getFileSHA1()};
        return changeUpdateStatus(selection, selectionArgs, update.getStatus());
    }

    private boolean changeUpdateStatus(String selection, String[] selectionArgs,
                                       int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UpdateEntry.COLUMN_NAME_STATUS, status);
        try {
            return db.update(UpdateEntry.TABLE_NAME, values, selection, selectionArgs) != 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean changeUpdateFilePath(Update update) {
        String selection = UpdateEntry.COLUMN_NAME_SHA1 + " = ?";
        String[] selectionArgs = {update.getFileSHA1()};
        return changeUpdateFilePath(selection, selectionArgs, update.getFilePath());
    }

    private boolean changeUpdateFilePath(String selection, String[] selectionArgs, String path) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UpdateEntry.COLUMN_NAME_PATH, path);
        return db.update(UpdateEntry.TABLE_NAME, values, selection, selectionArgs) != 0;
    }

    public boolean changeUpdateDownloadProgress(Update update, int prog) {
        String selection = UpdateEntry.COLUMN_NAME_SHA1 + " = ?";
        String[] selectionArgs = {update.getFileSHA1()};
        return changeUpdateDownloadProgress(selection, selectionArgs, update.getDownloadProgress());
    }

    private boolean changeUpdateDownloadProgress(String selection, String[] selectionArgs,
                                                 int prog) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UpdateEntry.COLUMN_NAME_DOWNLOAD_PROG, prog);
        return db.update(UpdateEntry.TABLE_NAME, values, selection, selectionArgs) != 0;
    }

    public Update getUpdate(String sha1) {
        String selection = UpdateEntry.COLUMN_NAME_SHA1 + " = ?";
        String[] selectionArgs = {sha1};
        return getUpdate(selection, selectionArgs);
    }

    private Update getUpdate(String selection, String[] selectionArgs) {
        List<Update> updates = getUpdates(selection, selectionArgs);
        return updates != null ? updates.get(0) : null;
    }

    public List<Update> getUpdates() {
        return getUpdates(null, null);
    }

    public List<Update> getUpdates(String selection, String[] selectionArgs) {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                UpdateEntry.COLUMN_NAME_NAME,
                UpdateEntry.COLUMN_NAME_VERSION,
                UpdateEntry.COLUMN_NAME_REQUIREMENT,
                UpdateEntry.COLUMN_NAME_TIMESTAMP,
                UpdateEntry.COLUMN_NAME_TYPE,
                UpdateEntry.COLUMN_NAME_SIZE,
                UpdateEntry.COLUMN_NAME_STATUS,
                UpdateEntry.COLUMN_NAME_DOWNLOAD_URL,
                UpdateEntry.COLUMN_NAME_DOWNLOAD_PROG,
                UpdateEntry.COLUMN_NAME_PATH,
                UpdateEntry.COLUMN_NAME_SHA1,
                UpdateEntry.COLUMN_NAME_DESCRIPTION
        };
        String sort = UpdateEntry.COLUMN_NAME_TIMESTAMP + " DESC";
        Cursor cursor = db.query(UpdateEntry.TABLE_NAME, projection, selection, selectionArgs,
                null, null, sort);
        List<Update> updates = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Update update = new Update();
                int index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_NAME);
                update.setName(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_VERSION);
                update.setVersion(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_REQUIREMENT);
                update.setRequirement(cursor.getLong(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_TIMESTAMP);
                update.setTimestamp(cursor.getLong(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_TYPE);
                update.setType(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_SIZE);
                update.setFileSize(cursor.getLong(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_STATUS);
                update.setStatus(cursor.getInt(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_DOWNLOAD_URL);
                update.setDownloadUrl(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_DOWNLOAD_PROG);
                update.setDownloadProgress(cursor.getInt(index),0);
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_PATH);
                update.setFilePath(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_SHA1);
                update.setFileSHA1(cursor.getString(index));
                index = cursor.getColumnIndex(UpdateEntry.COLUMN_NAME_DESCRIPTION);
                update.setDescription(cursor.getString(index));
                updates.add(update);
            }
            cursor.close();
        }
        return updates;
    }

    // 根据timestamp清除不符合条件的updates
    // 比对的内容: timestamp(更新发布时的timestamp) requirement(安装更新需要的版本的timestamp)
    public void cleanUpdates(long timeStamp) {
        String selection = UpdateEntry.COLUMN_NAME_TIMESTAMP + " <= " + timeStamp +
                " OR (" + UpdateEntry.COLUMN_NAME_REQUIREMENT + " != " + timeStamp +
                " AND " + UpdateEntry.COLUMN_NAME_REQUIREMENT + " != 0)";
        List<Update> mUpdatesList = getUpdates(selection, null);
        for (int i = 0; i < mUpdatesList.size(); i++) {
            Update tUpdate = mUpdatesList.get(i);
            if (tUpdate.getStatus() == UpdateStatus.UNKNOWN) {
                removeUpdate(tUpdate);
            }
        }
    }

}
