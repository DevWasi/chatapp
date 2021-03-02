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

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.Contact;
import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.logg0r.Log;

import java.util.Objects;

public class MessageListActivity extends AppCompatActivity implements OnItemClickListener,
        OnItemLongClickListener, OnClickListener, OnLongClickListener {

    private static final String TAG = "ml";

    private static final ContactsWrapper WRAPPER = ContactsWrapper.getInstance();


    private static final int MAX_EDITTEXT_LINES = 10;

    private static String chooserPackage = null;

    private Uri uri;

    private Conversation conv = null;

    static final String URI = "content://mms-sms/conversations/";

    private EditText etText;
    @SuppressWarnings("deprecation")
    private ClipboardManager cbmgr;

    private boolean enableAutosend = true;

    private boolean showTextField = true;

    private boolean showPhoto = false;

    private MenuItem contactItem = null;

    private boolean needContactUpdate = false;

    private ListView getListView() {
        return findViewById(android.R.id.list);
    }

    private void setListAdapter(final ListAdapter la) {
        getListView().setAdapter(la);
    }


    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        enableAutosend = p.getBoolean(PreferencesActivity.PREFS_ENABLE_AUTOSEND, true);
        showTextField = enableAutosend
                || p.getBoolean(PreferencesActivity.PREFS_SHOWTEXTFIELD, true);
        showPhoto = p.getBoolean(PreferencesActivity.PREFS_CONTACT_PHOTO, true);
        final boolean hideSend = p.getBoolean(PreferencesActivity.PREFS_HIDE_SEND, false);
        setTheme(PreferencesActivity.getTheme(this));
        Utils.setLocale(this);
        setContentView(R.layout.messagelist);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Log.d(TAG, "onCreate()");

        if (hideSend) {
            findViewById(R.id.send_).setVisibility(View.GONE);
        }

        cbmgr = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        etText = findViewById(R.id.text);
        int flags = etText.getInputType();
        if (p.getBoolean(PreferencesActivity.PREFS_EDIT_SHORT_TEXT, true)) {
            flags |= InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
        } else {
            flags &= ~InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
        }
        etText.setInputType(flags);

        if (!showTextField) {
            findViewById(R.id.text_layout).setVisibility(View.GONE);
            parseIntent(getIntent());
        } else {
            parseIntent(getIntent());
        }

        final ListView list = getListView();
        list.setOnItemLongClickListener(this);
        list.setOnItemClickListener(this);
        View v = findViewById(R.id.send_);
        v.setOnClickListener(this);
        etText.setMaxLines(MAX_EDITTEXT_LINES);
    }

    @Override
    protected final void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        parseIntent(intent);
    }

    private void parseIntent(final Intent intent) {
        Log.d(TAG, "parseIntent(", intent, ")");
        if (intent == null) {
            return;
        }
        Log.d(TAG, "got action: ", intent.getAction());
        Log.d(TAG, "got uri: ", intent.getData());

        needContactUpdate = true;

        uri = intent.getData();
        if (uri != null) {
            if (!uri.toString().startsWith(URI)) {
                uri = Uri.parse(URI + uri.getLastPathSegment());
            }
        } else {
            final long tid = intent.getLongExtra("thread_id", -1L);
            uri = Uri.parse(URI + tid);
            if (tid < 0L) {
                try {
                    startActivity(ConversationListActivity.getComposeIntent(this, null, false));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "activity not found", e);
                    Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG).show();
                }
                finish();
                return;
            }
        }

        conv = getConversation();
        if (conv == null) {
            finish();
            return;
        }

        final Contact contact = conv.getContact();
        try {
            contact.update(this, false, true);
        } catch (NullPointerException e) {
            Log.e(TAG, "updating contact failed", e);
        }
        boolean showKeyboard = intent.getBooleanExtra("showKeyboard", false);

        Log.d(TAG, "address: ", contact.getNumber());
        Log.d(TAG, "name: ", contact.getName());
        Log.d(TAG, "displayName: ", contact.getDisplayName());
        final int d = Log.d(TAG, "showKeyboard: ", showKeyboard);

        final ListView lv = getListView();
        lv.setStackFromBottom(true);

        MessageAdapter adapter = new MessageAdapter(this, uri);
        setListAdapter(adapter);

        updateHeader(contact);

        final String body = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(body)) {
            etText.setText(body);
            showKeyboard = true;
        }

        if (showKeyboard) {
            etText.requestFocus();
        }
    }

    private void updateHeader(final Contact contact) {
        String displayName = contact.getDisplayName();
        setTitle(displayName);
        String number = contact.getNumber();
        if (displayName.equals(number)) Objects.requireNonNull(getSupportActionBar()).setSubtitle(null);
        else {
            Objects.requireNonNull(getSupportActionBar()).setSubtitle(number);
        }

        setContactIcon(contact);
    }

    @Nullable
    private Conversation getConversation() {
        int threadId;
        try {
            threadId = Integer.parseInt(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            Log.e(TAG, "unable to parse thread id from uri: ", uri, e);
            Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG).show();
            return null;
        }
        if (threadId < 0) {
            Log.e(TAG, "negative thread id from uri: ", uri);
            Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG).show();
            return null;
        }
        try {
            return Conversation.getConversation(this, threadId, true);
        } catch (NullPointerException e) {
            Log.e(TAG, "Fetched null conversation for thread ", threadId, e);
            Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private ImageView findMenuItemView(final int viewId) {
        ImageView view = findViewById(viewId);
        if (view != null) {
            return view;
        }

        if (contactItem != null) {
            return contactItem.getActionView().findViewById(viewId);
        }
        return null;
    }

    private void setContactIcon(final Contact contact) {
        if (contact == null) {
            Log.w(TAG, "setContactIcon(null)");
            return;
        }

        if (contactItem == null) {
            Log.w(TAG, "setContactIcon: contactItem == null");
            return;
        }

        if (!needContactUpdate) {
            Log.i(TAG, "skip setContactIcon()");
            return;
        }

        final String name = contact.getName();
        final boolean showContactItem = showPhoto && name != null;

        if (showContactItem) {
            // photo
            ImageView ivPhoto = findMenuItemView(R.id.photo);
            if (ivPhoto == null) {
                Log.w(TAG, "ivPhoto == null");
            } else {
                ivPhoto.setOnClickListener(WRAPPER.getQuickContact(this, ivPhoto,
                        contact.getLookUpUri(getContentResolver()), 2, null));
            }

            // presence
            ImageView ivPresence = findMenuItemView(R.id.presence);
            if (ivPresence == null) {
                Log.w(TAG, "ivPresence == null");
            } else {
                if (contact.getPresenceState() > 0) {
                    ivPresence.setImageResource(Contact.getPresenceRes(contact.getPresenceState()));
                    ivPresence.setVisibility(View.VISIBLE);
                } else {
                    ivPresence.setVisibility(View.INVISIBLE);
                }
            }
        }

        contactItem.setVisible(showContactItem);
        needContactUpdate = false;
    }

    @Override
    protected final void onResume() {
        super.onResume();

        final ListView lv = getListView();
        lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        lv.setAdapter(new MessageAdapter(this, uri));

        final Button btn = findViewById(R.id.send_);
        if (showTextField) {
            Intent i;
            ActivityInfo ai = null;
            final PackageManager pm = getPackageManager();
            try {
                i = buildIntent(enableAutosend, false);
                if (pm != null && PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                        PreferencesActivity.PREFS_SHOWTARGETAPP, true)) {
                    ai = i.resolveActivityInfo(pm, 0);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "unable to build Intent", e);
            }
            etText.setMaxLines(MAX_EDITTEXT_LINES);

            if (ai == null) {
                btn.setText(null);
                etText.setMinLines(1);
            } else {
                if (chooserPackage == null) {
                    try {
                        final ActivityInfo cai = buildIntent(enableAutosend, true)
                                .resolveActivityInfo(pm, 0);
                        if (cai != null) {
                            chooserPackage = cai.packageName;
                        }
                    } catch (NullPointerException e) {
                        Log.e(TAG, "unable to build Intent", e);
                    }
                }
                if (ai.packageName.equals(chooserPackage)) {
                    btn.setText(R.string.chooser_);
                } else {
                    Log.d(TAG, "ai.pn: ", ai.packageName);
                    btn.setText(ai.loadLabel(pm));
                }
                etText.setMinLines(3);
            }
        } else {
            btn.setText(null);
        }
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.messagelist, menu);
        contactItem = menu.findItem(R.id.item_contact);
        if (conv != null) {
            setContactIcon(conv.getContact());
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in Action Bar clicked; go home
                Intent intent = new Intent(this, ConversationListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.item_contact:
                if (conv != null && contactItem != null) {
                    WRAPPER.showQuickContactFallBack(this, contactItem.getActionView(), conv
                            .getContact().getLookUpUri(getContentResolver()), 2, null);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public final void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        onItemLongClick(parent, view, position, id);
    }

    @SuppressWarnings("deprecation")
    public final void onClick(final View v) {
        switch (v.getId()) {
            case R.id.send_:
                send(true, false);
                return;
            case R.id.text_paste:
                final CharSequence s = cbmgr.getText();
                etText.setText(s);
                return;
            default:
                // should never happen
        }
    }

    public final boolean onLongClick(final View v) {
        if (v.getId() == R.id.send_) {
            send(false, true);
            return true;
        }
        return true;
    }

    private Intent buildIntent(final boolean autosend, final boolean showChooser) {
        if (conv == null || conv.getContact() == null) {
            Log.e(TAG, "buildIntent() without contact: ", conv);
            throw new NullPointerException("conv and conv.getContact() must be not null");
        }
        final String text = etText.getText().toString().trim();
        final Intent i = ConversationListActivity.getComposeIntent(this, conv.getContact()
                .getNumber(), showChooser);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Intent.EXTRA_TEXT, text);
        i.putExtra("sms_body", text);
        if (autosend && enableAutosend && text.length() > 0) {
            i.putExtra("AUTOSEND", "1");
        }
        if (showChooser) {
            return Intent.createChooser(i, getString(R.string.reply));
        } else {
            return i;
        }
    }

    private void send(final boolean autosend, final boolean showChooser) {
        try {
            final Intent i = buildIntent(autosend, showChooser);
            startActivity(i);
            PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .edit()
                    .putString(PreferencesActivity.PREFS_BACKUPLASTTEXT,
                            etText.getText().toString()).commit();
            etText.setText("");
        } catch (ActivityNotFoundException | NullPointerException e) {
            Log.e(TAG, "unable to launch sender app", e);
            Toast.makeText(this, R.string.error_sending_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }
}
