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

import org.apache.commons.io.IOUtils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.CallLog.Calls;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;

import de.ub0r.android.logg0r.Log;

public final class Message {

    static final String TAG = "msg";

    public static final Bitmap BITMAP_PLAY = Bitmap.createBitmap(1, 1, Config.RGB_565);

    public static final String ATTACHMENT_FILE = "mms.";

    private static final int CAHCESIZE = 50;

    private static final LinkedHashMap<Integer, Message> CACHE
            = new LinkedHashMap<>(
            26, 0.9f, true);

    public static final int INDEX_ID = 0;

    public static final int INDEX_READ = 1;

    public static final int INDEX_DATE = 2;

    public static final int INDEX_THREADID = 3;

    public static final int INDEX_TYPE = 4;

    public static final int INDEX_ADDRESS = 5;

    public static final int INDEX_BODY = 6;

    public static final int INDEX_SUBJECT = 7;

    public static final int INDEX_MTYPE = 8;

    public static final int INDEX_MID = 1;

    public static final int INDEX_CT = 2;

    public static final String[] PROJECTION = { //
            "_id", // 0
            "read", // 1
            Calls.DATE, // 2
            "thread_id", // 3
            Calls.TYPE, // 4
            "address", // 5
            "body", // 6
    };
    public static final String[] PROJECTION_SMS = { //
            PROJECTION[INDEX_ID], // 0
            PROJECTION[INDEX_READ], // 1
            PROJECTION[INDEX_DATE], // 2
            PROJECTION[INDEX_THREADID], // 3
            PROJECTION[INDEX_TYPE], // 4
            PROJECTION[INDEX_ADDRESS], // 5
            PROJECTION[INDEX_BODY], // 6
    };
    public static final String[] PROJECTION_JOIN = {
            PROJECTION[INDEX_ID],
            PROJECTION[INDEX_READ],
            PROJECTION[INDEX_DATE],
            PROJECTION[INDEX_THREADID],
            PROJECTION[INDEX_TYPE],
            PROJECTION[INDEX_ADDRESS],
            PROJECTION[INDEX_BODY],
            "sub",
            "m_type",
    };

    public static final String[] PROJECTION_READ = { PROJECTION[INDEX_ID], PROJECTION[INDEX_READ], PROJECTION[INDEX_DATE], PROJECTION[INDEX_THREADID],};

    private static final Uri URI_PARTS = Uri.parse("content://mms/part/");

    public static final String[] PROJECTION_PARTS = {"_id", "mid", "ct",};

    static final String SELECTION_READ_UNREAD = "read = ?";

    static final String[] SELECTION_UNREAD = new String[]{"0"};

    public static final String SORT_USD = Calls.DATE + " ASC";

    public static final int SMS_IN = Calls.INCOMING_TYPE;

    public static final int SMS_OUT = Calls.OUTGOING_TYPE;

    public static final int SMS_DRAFT = 3;

    public static final int MMS_IN = 132;

    public static final int MMS_OUT = 128;

    private final long id;

    private final long threadId;

    private long date;

    private final String address;

    private CharSequence body;

    private int type;

    private int read;

    private String subject;

    private Bitmap picture = null;

    private Intent contentIntent = null;

    private final boolean isMms;

    private Message(final Context context, final Cursor cursor) {
        id = cursor.getLong(INDEX_ID);
        threadId = cursor.getLong(INDEX_THREADID);
        date = cursor.getLong(INDEX_DATE);
        if (date < ConversationListActivity.MIN_DATE) {
            date *= ConversationListActivity.MILLIS;
        }
        if (cursor.getColumnIndex(PROJECTION_JOIN[INDEX_TYPE]) >= 0) {
            address = cursor.getString(INDEX_ADDRESS);
            body = cursor.getString(INDEX_BODY);
        } else {
            body = null;
            address = null;
        }
        type = cursor.getInt(INDEX_TYPE);
        read = cursor.getInt(INDEX_READ);
        if (body == null) {
            isMms = true;
            try {
                fetchMmsParts(context);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "error loading parts", e);
                try {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                } catch (Exception e1) {
                    Log.e(TAG, "error creating Toast", e1);
                }
            }
        } else {
            isMms = false;
        }
        try {
            subject = cursor.getString(INDEX_SUBJECT);
        } catch (IllegalStateException e) {
            subject = null;
        }
        try {
            if (cursor.getColumnCount() > INDEX_MTYPE) {
                final int t = cursor.getInt(INDEX_MTYPE);
                if (t != 0) {
                    type = t;
                }
            }
        } catch (IllegalStateException e) {
            subject = null;
        }

        Log.d(TAG, "threadId: ", threadId);
        Log.d(TAG, "address: ", address);
    }

    public void update(final Cursor cursor) {
        read = cursor.getInt(INDEX_READ);
        type = cursor.getInt(INDEX_TYPE);
        try {
            if (cursor.getColumnCount() > INDEX_MTYPE) {
                final int t = cursor.getInt(INDEX_MTYPE);
                if (t != 0) {
                    type = t;
                }
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "wrong projection?", e);
        }
    }

    private CharSequence fetchPart(final InputStream is) {
        Log.d(TAG, "fetchPart(cr, is)");
        String ret = null;
        // get part
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[256];
            int len = is.read(buffer);
            while (len >= 0) {
                baos.write(buffer, 0, len);
                len = is.read(buffer);
            }
            ret = baos.toString();
            Log.d(TAG, ret);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load part data", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close stream", e);
                } // Ignore
            }
        }
        return ret;
    }

    private void fetchMmsParts(final Context context) {
        final ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try{
            cursor = cr.query(URI_PARTS, null, PROJECTION_PARTS[INDEX_MID] + " = ?",
                    new String[]{String.valueOf(id)}, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            final int iID = cursor.getColumnIndex(PROJECTION_PARTS[INDEX_ID]);
            final int iCT = cursor.getColumnIndex(PROJECTION_PARTS[INDEX_CT]);
            final int iText = cursor.getColumnIndex("text");
            do {
                final int pid = cursor.getInt(iID);
                final String ct = cursor.getString(iCT);
                Log.d(TAG, "part: ", pid, " ", ct);

                // get part
                InputStream is = null;

                final Uri uri = ContentUris.withAppendedId(URI_PARTS, pid);
                if (uri == null) {
                    Log.w(TAG, "parts URI=null for pid=", pid);
                    continue;
                }
                try {
                    is = cr.openInputStream(uri);
                } catch (IOException | NullPointerException e) {
                    Log.e(TAG, "Failed to load part data", e);
                }
                if (is == null) {
                    Log.i(TAG, "InputStream for part ", pid, " is null");
                    if (iText >= 0 && ct != null && ct.startsWith("text/")) {
                        body = cursor.getString(iText);
                    }
                    continue;
                }
                if (ct == null) {
                    continue;
                }
                if (ct.startsWith("image/")) {
                    picture = BitmapFactory.decodeStream(is);
                    final Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, ct);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    contentIntent = i;
                    continue; // skip the rest
                } else if (ct.startsWith("video/") || ct.startsWith("audio/")) {
                    picture = BITMAP_PLAY;
                    final Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, ct);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    contentIntent = i;
                    continue; // skip the rest
                } else if (ct.startsWith("text/")) {
                    body = fetchPart(is);
                }

                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close stream", e);
                } // Ignore
            } while (cursor.moveToNext());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public static Message getMessage(final Context context, final Cursor cursor) {
        synchronized (CACHE) {
            String body = cursor.getString(INDEX_BODY);
            int id = cursor.getInt(INDEX_ID);
            if (body == null) { // MMS
                id *= -1;
            }
            Message ret = CACHE.get(id);
            if (ret == null) {
                ret = new Message(context, cursor);
                CACHE.put(id, ret);
                Log.d(TAG, "cachesize: ", CACHE.size());
                while (CACHE.size() > CAHCESIZE) {
                    Integer i = CACHE.keySet().iterator().next();
                    Log.d(TAG, "rm msg. from cache: ", i);
                    Message cc = CACHE.remove(i);
                    if (cc == null) {
                        Log.w(TAG, "CACHE might be inconsistent!");
                        break;
                    }
                }
            } else {
                ret.update(cursor);
            }
            return ret;
        }
    }

    public long getId() {
        return id;
    }

    public long getThreadId() {
        return threadId;
    }

    public long getDate() {
        return date;
    }

    public CharSequence getBody() {
        return body;
    }

    public int getType() {
        return type;
    }

    public int getRead() {
        return read;
    }

    public boolean isMMS() {
        return isMms;
    }

    public String getSubject() {
        return subject;
    }

    public Bitmap getPicture() {
        return picture;
    }

    public Intent getContentIntent() {
        return contentIntent;
    }

    public OnLongClickListener getSaveAttachmentListener(final Activity context) {
        if (contentIntent == null) {
            return null;
        }

        return v -> {
            // check/request permission Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ChatApp.requestPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, 0, R.string.permissions_write_external_storage, null)) {
                return true;
            }

            try {
                Log.d(TAG, "save attachment: ", Message.this.id);
                String fn = ATTACHMENT_FILE;
                final Intent ci = Message.this.contentIntent;
                final String ct = ci.getType();
                Log.d(TAG, "content type: ", ct);
                if (ct == null) {
                    fn += "null";
                } else if (ct.startsWith("image/")) {
                    switch (ct) {
                        case "image/jpeg":
                            fn += "jpg";
                            break;
                        case "image/gif":
                            fn += "gif";
                            break;
                        default:
                            fn += "png";
                            break;
                    }
                } else if (ct.startsWith("audio/")) {
                    switch (ct) {
                        case "audio/3gpp":
                            fn += "3gpp";
                            break;
                        case "audio/mpeg":
                            fn += "mp3";
                            break;
                        case "audio/mid":
                            fn += "mid";
                            break;
                        default:
                            fn += "wav";
                            break;
                    }
                } else if (ct.startsWith("video/")) {
                    if (ct.equals("video/3gpp")) {
                        fn += "3gpp";
                    } else {
                        fn += "avi";
                    }
                } else {
                    fn += "ukn";
                }
                final File file = Message.this.createUniqueFile(
                        Environment.getExternalStorageDirectory(), fn);
                InputStream in = context.getContentResolver().openInputStream(ci.getData());
                OutputStream out = new FileOutputStream(file);
                IOUtils.copy(in, out);
                out.flush();
                out.close();
                in.close();
                assert file != null;
                Log.i(TAG, "attachment saved: ", file.getPath());
                Toast.makeText(context,
                        context.getString(R.string.attachment_saved) + " " + fn,
                        Toast.LENGTH_LONG).show();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "IO ERROR", e);
                Toast.makeText(context, R.string.attachment_not_saved, Toast.LENGTH_LONG)
                        .show();
            } catch (NullPointerException e) {
                Log.e(TAG, "NULL ERROR", e);
                Toast.makeText(context, R.string.attachment_not_saved, Toast.LENGTH_LONG)
                        .show();
            }
            return true;
        };
    }

    public Uri getUri() {
        if (isMms) {
            return Uri.parse("content://mms/" + id);
        } else {
            return Uri.parse("content://sms/" + id);
        }
    }

    private File createUniqueFile(final File directory, final String filename) {
        File file = new File(directory, filename);
        if (!file.exists()) {
            return file;
        }
        // Get the extension of the file, if any.
        int index = filename.lastIndexOf('.');
        String format;
        if (index != -1) {
            String name = filename.substring(0, index);
            String extension = filename.substring(index);
            format = name + "-%d" + extension;
        } else {
            format = filename + "-%d";
        }
        for (int i = 2; i < Integer.MAX_VALUE; i++) {
            file = new File(directory, String.format(format, i));
            if (!file.exists()) {
                return file;
            }
        }
        return null;
    }
}
