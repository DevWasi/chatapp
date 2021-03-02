
package de.ub0r.android.smsdroid;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.telephony.SmsManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URLDecoder;
import java.util.ArrayList;

import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.logg0r.Log;

public final class SenderActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = "send";

    private static final Uri URI_SMS = Uri.parse("content://sms");

    public static final Uri URI_SENT = Uri.parse("content://sms/sent");

    private static final String[] PROJECTION_ID = new String[]{BaseColumns._ID};

    private static final String ADDRESS = "address";

    private static final String READ = "read";

    public static final String TYPE = "type";

    private static final String BODY = "body";

    private static final String DATE = "date";

    public static final String MESSAGE_SENT_ACTION = "com.android.mms.transaction.MESSAGE_SENT";

    private String to, text;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @SuppressWarnings("deprecation")
    private void handleIntent(final Intent intent) {
        if (parseIntent(intent)) {
            setTheme(android.R.style.Theme_Translucent_NoTitleBar);
            send();
            finish();
        } else {
            int tid = getThreadId();
            if (tid >= 0) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(
                        ConversationListActivity.URI, String.valueOf(tid)), this,
                        MessageListActivity.class);
                i.putExtra("showKeyboard", true);
                startActivity(i);
                finish();
            } else {
                setTheme(PreferencesActivity.getTheme(this));
                setContentView(R.layout.sender);
                findViewById(R.id.text_paste).setOnClickListener(this);
                final EditText et = findViewById(R.id.text);
                et.setText(text);
                final MultiAutoCompleteTextView mtv = this
                        .findViewById(R.id.to);
                final MobilePhoneAdapter mpa = new MobilePhoneAdapter(this);
                final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
                MobilePhoneAdapter.setMobileNumbersOnly(p.getBoolean(
                        PreferencesActivity.PREFS_MOBILE_ONLY, false));
                mtv.setAdapter(mpa);
                mtv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
                mtv.setText(to);
                if (!TextUtils.isEmpty(to)) {
                    to = to.trim();
                    if (to.endsWith(",")) {
                        to = to.substring(0, to.length() - 1).trim();
                    }
                    if (to.indexOf('<') < 0) {
                        try {
                            // try to fetch recipient's name from phone book
                            String n = ContactsWrapper.getInstance().getNameForNumber(
                                    getContentResolver(), to);
                            if (n != null) {
                                to = n + " <" + to + ">, ";
                            }
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Null pointer while resolving number: ", to);
                        }
                    }
                    mtv.setText(to);
                    et.requestFocus();
                } else {
                    mtv.requestFocus();
                }
                int flags = et.getInputType();
                if (p.getBoolean(PreferencesActivity.PREFS_EDIT_SHORT_TEXT, true)) {
                    flags |= InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
                } else {
                    flags &= ~InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
                }
                et.setInputType(flags);
            }
        }
    }

    private boolean parseIntent(final Intent intent) {
        Log.d(TAG, "parseIntent(", intent, ")");
        if (intent == null) {
            return false;
        }
        Log.d(TAG, "got action: ", intent.getAction());

        to = null;
        String u = intent.getDataString();
        try {
            if (!TextUtils.isEmpty(u) && u.contains(":")) {
                String t = u.split(":")[1];
                if (t.startsWith("+")) {
                    to = "+" + URLDecoder.decode(t.substring(1));
                } else {
                    to = URLDecoder.decode(t);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, "could not split at :", e);
        }

        CharSequence cstext = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        text = null;
        if (cstext != null) {
            text = cstext.toString();
        }
        if (TextUtils.isEmpty(text)) {
            Log.i(TAG, "text missing");
            return false;
        }
        if (TextUtils.isEmpty(to)) {
            Log.i(TAG, "recipient missing");
            return false;
        }

        return true;
    }

    private int getThreadId() {
        if (TextUtils.isEmpty(to)) {
            return -1;
        }
        String filter = to.replaceAll("[-()/ ]", "");
        if (filter.length() > 6) {
            filter = filter.substring(filter.length() - 6);
        }
        Cursor c = getContentResolver().query(Uri.parse("content://sms"),
                new String[]{"thread_id"}, "address like '%" + filter + "'", null, null);
        int threadId = -1;
        if (c.moveToFirst()) {
            threadId = c.getInt(0);
        }
        c.close();
        return threadId;
    }

    private void send(final String recipient, final String message) {
        Log.d(TAG, "text: ", recipient);

        final ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(TYPE, Message.SMS_DRAFT);
        values.put(BODY, message);
        values.put(READ, 1);
        values.put(ADDRESS, recipient);
        Uri draft = null;
        // save sms to content://sms/sent
        Cursor cursor = cr.query(URI_SMS, PROJECTION_ID,
                TYPE + " = " + Message.SMS_DRAFT + " AND " + ADDRESS + " = '" + recipient
                        + "' AND " + BODY + " like '" + message.replace("'", "_") + "'", null, DATE
                        + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            draft = URI_SENT.buildUpon().appendPath(cursor.getString(0)).build();
            Log.d(TAG, "skip saving draft: ", draft);
        } else {
            try {
                draft = cr.insert(URI_SENT, values);
                Log.d(TAG, "draft saved: ", draft);
            } catch (IllegalArgumentException | SQLiteException | NullPointerException e) {
                Log.e(TAG, "unable to save draft", e);
            }
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        Log.d(TAG, "send messages to: ", recipient);

        final ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        try {
            SmsManager smsmgr = SmsManager.getDefault();

            final ArrayList<String> messages = smsmgr.divideMessage(message);
            for (String m : messages) {
                Log.d(TAG, "divided messages: ", m);

                final Intent sent = new Intent(MESSAGE_SENT_ACTION, draft, this, SmsReceiver.class);
                sentIntents.add(PendingIntent.getBroadcast(this, 0, sent, 0));
            }
            smsmgr.sendMultipartTextMessage(recipient, null, messages, sentIntents, null);
            Log.i(TAG, "message sent");
        } catch (Exception e) {
            Log.e(TAG, "unexpected error", e);
            for (PendingIntent pi : sentIntents) {
                if (pi != null) {
                    try {
                        pi.send();
                    } catch (CanceledException e1) {
                        Log.e(TAG, "unexpected error", e1);
                    }
                }
            }
        }
    }

    /**
     * Send a message.
     *
     * @return true, if message was sent
     */
    private boolean send() {
        if (TextUtils.isEmpty(to) || TextUtils.isEmpty(text)) {
            return false;
        }
        for (String r : to.split(",")) {
            r = MobilePhoneAdapter.cleanRecipient(r);
            if (TextUtils.isEmpty(r)) {
                Log.w(TAG, "skip empty recipient: ", r);
                continue;
            }
            try {
                send(r, text);
            } catch (Exception e) {
                Log.e(TAG, "unable to send message: ", to, e);
                Toast.makeText(this, R.string.error_sending_failed, Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }

    @Override
    public void onClick(final View v) {}

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.sender, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in Action Bar clicked; go home
                Intent intent = new Intent(this, ConversationListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.item_send:
                EditText et = findViewById(R.id.text);
                text = et.getText().toString();
                et = findViewById(R.id.to);
                to = et.getText().toString();
                if (send()) {
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
