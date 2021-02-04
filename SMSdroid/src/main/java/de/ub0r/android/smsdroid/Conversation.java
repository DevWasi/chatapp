/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of SMSdroid.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.smsdroid;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CallLog.Calls;

import java.util.LinkedHashMap;

import de.ub0r.android.lib.apis.Contact;
import de.ub0r.android.logg0r.Log;

public final class Conversation {
    static final String TAG = "con";
    private static final int CACHESIZE = 50;
    private static final LinkedHashMap<Integer, Conversation> CACHE
            = new LinkedHashMap<>(26, 0.9f, true);
    public static final Bitmap NO_PHOTO = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
    static final Uri URI_SIMPLE = Uri.parse("content://mms-sms/conversations").buildUpon()
            .appendQueryParameter("simple", "true").build();
    public static final String ID = BaseColumns._ID;
    public static final String DATE = Calls.DATE;
    public static final String COUNT = "message_count";
    public static final String NID = "recipient_ids";
    public static final String BODY = "snippet";
    public static final String READ = "read";
    public static final int INDEX_SIMPLE_ID = 0;
    public static final int INDEX_SIMPLE_DATE = 1;
    public static final int INDEX_SIMPLE_COUNT = 2;
    public static final int INDEX_SIMPLE_NID = 3;
    public static final int INDEX_SIMPLE_BODY = 4;
    public static final int INDEX_SIMPLE_READ = 5;
    public static final String[] PROJECTION_SIMPLE = { //
            ID, // 0
            DATE, // 1
            COUNT, // 2
            NID, // 3
            BODY, // 4
            READ, // 5
    };

    static final String DATE_FORMAT = "dd.MM. kk:mm";
    private static long validCache = 0;
    private int id;
    private final int threadId;
    private Contact contact;
    private long date;
    private String body;
    private int read;
    private int count = -1;
    private long lastUpdate = 0L;

    private Conversation(final Context context, final Cursor cursor, final boolean sync) {
        threadId = cursor.getInt(INDEX_SIMPLE_ID);
        date = cursor.getLong(INDEX_SIMPLE_DATE);
        body = cursor.getString(INDEX_SIMPLE_BODY);
        read = cursor.getInt(INDEX_SIMPLE_READ);
        count = cursor.getInt(INDEX_SIMPLE_COUNT);
        contact = new Contact(cursor.getInt(INDEX_SIMPLE_NID));

        AsyncHelper.fillConversation(context, this, sync);
        lastUpdate = System.currentTimeMillis();
    }

    private void update(final Context context, final Cursor cursor, final boolean sync) {
        Log.d(TAG, "update(", threadId, ",", sync, ")");
        if (cursor == null || cursor.isClosed()) {
            Log.e(TAG, "Conversation.update() on null/closed cursor");
            return;
        }
        long d = cursor.getLong(INDEX_SIMPLE_DATE);
        if (d != date) {
            id = cursor.getInt(INDEX_SIMPLE_ID);
            date = d;
            body = cursor.getString(INDEX_SIMPLE_BODY);
        }
        count = cursor.getInt(INDEX_SIMPLE_COUNT);
        read = cursor.getInt(INDEX_SIMPLE_READ);
        final int nid = cursor.getInt(INDEX_SIMPLE_NID);
        if (nid != contact.getRecipientId()) {
            contact = new Contact(nid);
        }
        if (lastUpdate < validCache) {
            AsyncHelper.fillConversation(context, this, sync);
            lastUpdate = System.currentTimeMillis();
        }
    }

    public static Conversation getConversation(final Context context, final Cursor cursor, final boolean sync) {
        Log.d(TAG, "getConversation(", sync, ")");
        synchronized (CACHE) {
            Conversation ret = CACHE.get(cursor.getInt(INDEX_SIMPLE_ID));
            if (ret == null) {
                ret = new Conversation(context, cursor, sync);
                CACHE.put(ret.getThreadId(), ret);
                Log.d(TAG, "cachesize: ", CACHE.size());
                while (CACHE.size() > CACHESIZE) {
                    Integer i = CACHE.keySet().iterator().next();
                    Log.d(TAG, "rm con. from cache: ", i);
                    Conversation cc = CACHE.remove(i);
                    if (cc == null) {
                        Log.w(TAG, "CACHE might be inconsistent!");
                        break;
                    }
                }
            } else {
                ret.update(context, cursor, sync);
            }
            return ret;
        }
    }

    public static Conversation getConversation(final Context context, final int threadId, final boolean forceUpdate) {
        Log.d(TAG, "getConversation(", threadId, ")");
        synchronized (CACHE) {
            Conversation ret = CACHE.get(threadId);
            if (ret == null || ret.getContact().getNumber() == null || forceUpdate) {
                Cursor cursor = context.getContentResolver().query(URI_SIMPLE, PROJECTION_SIMPLE,
                        ID + " = ?", new String[]{String.valueOf(threadId)}, null);
                if (cursor.moveToFirst()) {
                    ret = getConversation(context, cursor, true);
                } else {
                    Log.e(TAG, "did not found conversation: ", threadId);
                }
                cursor.close();
            }
            return ret;
        }
    }

    public static void flushCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
    public static void invalidate() {
        validCache = System.currentTimeMillis();
    }
    public int getId() {
        return id;
    }
    public void setNumberId(final long nid) {
        contact = new Contact(nid);
    }
    public int getThreadId() {
        return threadId;
    }
    public long getDate() {
        return date;
    }
    public Contact getContact() {
        return contact;
    }
    public String getBody() {
        return body;
    }

    public void setBody(final String b) {
        body = b;
    }

    public int getRead() {
        return read;
    }

    public void setRead(final int status) {
        read = status;
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int c) {
        count = c;
    }

    public Uri getUri() {
        return Uri.withAppendedPath(ConversationListActivity.URI, String.valueOf(threadId));
    }
}
