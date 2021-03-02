/*
 * Copyright (C) 2009-2015 Felix Bechstein
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

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

public final class ConversationListActivity extends AppCompatActivity implements
        OnItemClickListener, OnItemLongClickListener {

    public static final String TAG = "main";
    static final Uri URI = Uri.parse("content://mms-sms/conversations/");
    public static final long MIN_DATE = 10000000000L;
    public static final long MILLIS = 1000L;
    public static boolean showContactPhoto = false;

    public static boolean showEmoticons = false;
    private ConversationAdapter adapter = null;

    private static final Calendar CAL_DAYAGO = Calendar.getInstance();

    static {
        // Get time for now - 24 hours
        CAL_DAYAGO.add(Calendar.DAY_OF_MONTH, -1);
    }

    private static final int PERMISSIONS_REQUEST_READ_SMS = 1;

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 2;

    @Override
    public void onStart() {
        super.onStart();
        AsyncHelper.setAdapter(adapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        AsyncHelper.setAdapter(null);
    }

    private AbsListView getListView() {
        return findViewById(android.R.id.list);
    }

    private void setListAdapter(final ListAdapter la) {
        AbsListView v = getListView();
        if (v instanceof GridView) {
            //noinspection RedundantCast
            ((GridView) v).setAdapter(la);
        } else if (v instanceof ListView) {
            //noinspection RedundantCast
            ((ListView) v).setAdapter(la);
        }

    }

    @Override
    public void onNewIntent(final Intent intent) {
        if (intent != null) {
            Log.d(TAG, "got intent: ", intent.getAction());
            Log.d(TAG, "got uri: ", intent.getData());
            final Bundle b = intent.getExtras();
            if (b != null) {
                Log.d(TAG, "user_query: ", b.get("user_query"));
                Log.d(TAG, "got extra: ", b);
            }
            final String query = intent.getStringExtra("user_query");
            Log.d(TAG, "user query: ", query);
            // TODO: do something with search query
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        final Intent i = getIntent();
        Log.d(TAG, "got intent: ", i.getAction());
        Log.d(TAG, "got uri: ", i.getData());
        Log.d(TAG, "got extra: ", i.getExtras());

        setTheme(PreferencesActivity.getTheme(this));
        Utils.setLocale(this);
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("use_gridlayout", false)) {
            setContentView(R.layout.conversationgrid);
        } else {
            setContentView(R.layout.conversationlist);
        }

        final AbsListView list = getListView();
        list.setOnItemClickListener(this);

        if (ChatApp.isDefaultApp(this)) {
            initAdapter();
        } else {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(R.string.not_default_app);
            b.setMessage(R.string.not_default_app_message);
            b.setCancelable(false);
            b.setNegativeButton(android.R.string.cancel, (dialogInterface, i12) -> ConversationListActivity.this.finish());
            b.setPositiveButton(android.R.string.ok, (dialogInterface, i1) -> {
                Intent intent =
                        new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                        BuildConfig.APPLICATION_ID);
                startActivity(intent);
            });
            b.show();
        }
    }

    private void initAdapter() {
        if (ChatApp.requestPermission(this, Manifest.permission.READ_SMS,
                PERMISSIONS_REQUEST_READ_SMS, R.string.permissions_read_sms,
                (dialogInterface, i) -> finish())) {
            return;
        }

        if (ChatApp.requestPermission(this, Manifest.permission.READ_CONTACTS,
                PERMISSIONS_REQUEST_READ_CONTACTS, R.string.permissions_read_contacts,
                (dialogInterface, i) -> finish())) {
            return;
        }

        adapter = new ConversationAdapter(this);
        setListAdapter(adapter);
        adapter.startMsgListQuery();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adapter == null && ChatApp.isDefaultApp(this)) {
            // after setting SMSdroid to default SMS app
            initAdapter();
        }

        CAL_DAYAGO.setTimeInMillis(System.currentTimeMillis());
        CAL_DAYAGO.add(Calendar.DAY_OF_MONTH, -1);

        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        showContactPhoto = p.getBoolean(PreferencesActivity.PREFS_CONTACT_PHOTO, true);
        showEmoticons = p.getBoolean(PreferencesActivity.PREFS_EMOTICONS, false);
        if (adapter != null) {
            adapter.startMsgListQuery();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.conversationlist, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        PreferenceManager.getDefaultSharedPreferences(this);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // just try again.
                    initAdapter();
                } else {
                    // this app is useless without permission for reading sms
                    Log.e(TAG, "permission for reading sms denied, exit");
                    finish();
                }
                return;
            }
            case PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // just try again.
                    initAdapter();
                } else {
                    // this app is useless without permission for reading sms
                    Log.e(TAG, "permission for reading contacts denied, exit");
                    finish();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_compose:
                final Intent i = getComposeIntent(this, null, false);
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "error launching intent: ", i.getAction(), ", ", i.getData());
                    Toast.makeText(this,
                            "error launching messaging app!\nPlease contact the developer.",
                            Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.item_settings: // start settings activity
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            case R.id.item_mark_all_read:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    static Intent getComposeIntent(final Context context, final String address, final boolean showChooser) {
        Intent i = null;

        if (!showChooser && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Search for WebSMS
            PackageManager pm = context.getPackageManager();
            i = pm == null ? null : pm.getLaunchIntentForPackage("de.ub0r.android.websms");
        }

        if (i == null) {
            Log.d(TAG, "WebSMS is not installed!");
            i = new Intent(Intent.ACTION_SENDTO);
        }

        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (address == null) {
            i.setData(Uri.parse("sms:"));
        } else {
            i.setData(Uri.parse("smsto:" + PreferencesActivity.fixNumber(context, address)));
        }

        return i;
    }

    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        final Conversation c = Conversation.getConversation(this,
                (Cursor) parent.getItemAtPosition(position), false);
        final Uri target = c.getUri();
        final Intent i = new Intent(this, MessageListActivity.class);
        i.setData(target);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "error launching intent: ", i.getAction(), ", ", i.getData());
            Toast.makeText(this,
                    "error launching messaging app!\n" + "Please contact the developer.",
                    Toast.LENGTH_LONG).show();
        }
    }

    static String getDate(final Context context, final long time) {
        long t = time;
        if (t < MIN_DATE) {
            t *= MILLIS;
        }
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                PreferencesActivity.PREFS_FULL_DATE, false)) {
            return DateFormat.getTimeFormat(context).format(t) + " "
                    + DateFormat.getDateFormat(context).format(t);
        } else if (t < CAL_DAYAGO.getTimeInMillis()) {
            return DateFormat.getDateFormat(context).format(t);
        } else {
            return DateFormat.getTimeFormat(context).format(t);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }
}
