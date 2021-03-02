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

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import de.ub0r.android.lib.IPreferenceContainer;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

public class PreferencesActivity extends PreferenceActivity implements IPreferenceContainer {

    static final String TAG = "prefs";

    static final String PREFS_VIBRATE = "receive_vibrate";

    static final String PREFS_SOUND = "receive_sound";

    private static final String PREFS_LED_COLOR = "receive_led_color";

    private static final String PREFS_LED_FLASH = "receive_led_flash";

    private static final String PREFS_VIBRATOR_PATTERN = "receive_vibrate_mode";

    static final String PREFS_NOTIFICATION_ENABLE = "notification_enable";

    static final String PREFS_NOTIFICATION_PRIVACY = "receive_privacy";

    private static final String PREFS_NOTIFICATION_ICON = "notification_icon";
    
    static final String PREFS_CONTACT_PHOTO = "show_contact_photo";

    static final String PREFS_EMOTICONS = "show_emoticons";

    private static final String PREFS_BUBBLES_IN = "bubbles_in";

    private static final String PREFS_BUBBLES_OUT = "bubbles_out";

    static final String PREFS_FULL_DATE = "show_full_date";

    static final String PREFS_HIDE_SEND = "hide_send";
    
    static final String PREFS_HIDE_PASTE = "hide_paste";

    static final String PREFS_HIDE_WIDGET_LABEL = "hide_widget_label";

    static final String PREFS_HIDE_DELETE_ALL_THREADS = "hide_delete_all_threads";

    static final String PREFS_HIDE_MESSAGE_COUNT = "hide_message_count";

    private static final String PREFS_THEME = "theme";

    private static final String THEME_BLACK = "black";

    private static final String PREFS_TEXTSIZE = "textsizen";

    private static final String PREFS_TEXTCOLOR = "textcolor";

    private static final String PREFS_TEXTCOLOR_IGNORE_CONV = "text_color_ignore_conv";

    public static final String PREFS_ENABLE_AUTOSEND = "enable_autosend";

    public static final String PREFS_MOBILE_ONLY = "mobile_only";

    public static final String PREFS_EDIT_SHORT_TEXT = "edit_short_text";

    public static final String PREFS_SHOWTEXTFIELD = "show_textfield";

    public static final String PREFS_SHOWTARGETAPP = "show_target_app";

    /**
     * Preference's name: backup of last sms.
     */
    public static final String PREFS_BACKUPLASTTEXT = "backup_last_sms";

    /**
     * Preference's name: decode decimal ncr.
     */
    public static final String PREFS_DECODE_DECIMAL_NCR = "decode_decimal_ncr";
    
    public static final String PREFS_FORWARD_SMS_CLEAN = "forwarded_sms_clean";

    private static final String PREFS_REGEX = "regex";

    private static final String PREFS_REPLACE = "replace";
    
    private static final int PREFS_REGEX_COUNT = 3;

    private static final int BLACK = 0xff000000;

    private static final int[] NOTIFICAION_IMG = new int[]{R.drawable.stat_notify_sms,
            R.drawable.stat_notify_sms_gingerbread, R.drawable.stat_notify_email_generic,
            R.drawable.stat_notify_sms_black, R.drawable.stat_notify_sms_green,
            R.drawable.stat_notify_sms_yellow,};

    /**
     * String resources for notification icons.
     */
    private static final int[] NOTIFICAION_STR = new int[]{R.string.notification_default_,
            R.string.notification_gingerbread_, R.string.notification_gingerbread_mail_,
            R.string.notification_black_, R.string.notification_green_,
            R.string.notification_yellow_,};

    /**
     * Drawable resources for bubbles.
     */
    private static final int[] BUBBLES_IMG = new int[]{0, R.drawable.gray_dark,
            R.drawable.gray_light, R.drawable.bubble_old_green_left,
            R.drawable.bubble_old_green_right, R.drawable.bubble_old_turquoise_left,
            R.drawable.bubble_old_turquoise_right, R.drawable.bubble_blue_left,
            R.drawable.bubble_blue_right, R.drawable.bubble_blue2_left,
            R.drawable.bubble_blue2_right, R.drawable.bubble_brown_left,
            R.drawable.bubble_brown_right, R.drawable.bubble_gray_left,
            R.drawable.bubble_gray_right, R.drawable.bubble_green_left,
            R.drawable.bubble_green_right, R.drawable.bubble_green2_left,
            R.drawable.bubble_green2_right, R.drawable.bubble_orange_left,
            R.drawable.bubble_orange_right, R.drawable.bubble_pink_left,
            R.drawable.bubble_pink_right, R.drawable.bubble_purple_left,
            R.drawable.bubble_purple_right, R.drawable.bubble_white_left,
            R.drawable.bubble_white_right, R.drawable.bubble_yellow_left,
            R.drawable.bubble_yellow_right,};

    /**
     * String resources for bubbles.
     */
    private static final int[] BUBBLES_STR = new int[]{R.string.bubbles_nothing,
            R.string.bubbles_plain_dark_gray, R.string.bubbles_plain_light_gray,
            R.string.bubbles_old_green_left, R.string.bubbles_old_green_right,
            R.string.bubbles_old_turquoise_left, R.string.bubbles_old_turquoise_right,
            R.string.bubbles_blue_left, R.string.bubbles_blue_right, R.string.bubbles_blue2_left,
            R.string.bubbles_blue2_right, R.string.bubbles_brown_left,
            R.string.bubbles_brown_right, R.string.bubbles_gray_left, R.string.bubbles_gray_right,
            R.string.bubbles_green_left, R.string.bubbles_green_right,
            R.string.bubbles_green2_left, R.string.bubbles_green2_right,
            R.string.bubbles_orange_left, R.string.bubbles_orange_right,
            R.string.bubbles_pink_left, R.string.bubbles_pink_right, R.string.bubbles_purple_left,
            R.string.bubbles_purple_right, R.string.bubbles_white_left,
            R.string.bubbles_white_right, R.string.bubbles_yellow_left,
            R.string.bubbles_yellow_right,};

    /**
     * Listen to clicks on "notification icon" preferences.
     *
     * @author flx
     */
    private static class OnNotificationIconClickListener implements
            Preference.OnPreferenceClickListener {

        private final Context ctx;

        public OnNotificationIconClickListener(final Context context) {
            ctx = context;
        }

        @Override
        public boolean onPreferenceClick(final Preference preference) {
            final Builder b = new Builder(ctx);
            final int l = NOTIFICAION_STR.length;
            final String[] cols = new String[]{"icon", "text"};
            final ArrayList<HashMap<String, Object>> rows
                    = new ArrayList<>();
            for (int i = 0; i < l; i++) {
                final HashMap<String, Object> m = new HashMap<>(2);
                m.put(cols[0], NOTIFICAION_IMG[i]);
                m.put(cols[1], ctx.getString(NOTIFICAION_STR[i]));
                rows.add(m);
            }
            b.setAdapter(new SimpleAdapter(ctx, rows, R.layout.notification_icons_item, cols,
                            new int[]{android.R.id.icon, android.R.id.text1}),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            preference.getEditor().putInt(preference.getKey(), which).commit();
                        }
                    });
            b.setNegativeButton(android.R.string.cancel, null);
            b.show();
            return true;
        }
    }

    private static class OnBubblesClickListener implements Preference.OnPreferenceClickListener {

        private final Context ctx;
        
        public OnBubblesClickListener(final Context context) {
            ctx = context;
        }

        @Override
        public boolean onPreferenceClick(final Preference preference) {
            final Builder b = new Builder(ctx);
            final int l = BUBBLES_STR.length;
            final String[] cols = new String[]{"icon", "text"};
            final ArrayList<HashMap<String, Object>> rows
                    = new ArrayList<>();
            for (int i = 0; i < l; i++) {
                final HashMap<String, Object> m = new HashMap<>(2);
                m.put(cols[0], BUBBLES_IMG[i]);
                m.put(cols[1], ctx.getString(BUBBLES_STR[i]));
                rows.add(m);
            }
            b.setAdapter(new SimpleAdapter(ctx, rows, R.layout.bubbles_item, cols, new int[]{
                            android.R.id.icon, android.R.id.text1}),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            preference.getEditor().putInt(preference.getKey(), which).commit();
                        }
                    });
            b.setNegativeButton(android.R.string.cancel, null);
            b.show();
            return true;
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings);
        Utils.setLocale(this);
    }

    @Override
    public void onBuildHeaders(final List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    protected boolean isValidFragment(final String fragmentName) {
        return HeaderPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * Register {@link OnPreferenceClickListener}.
     *
     * @param pc {@link IPreferenceContainer}
     */
    static void registerOnPreferenceClickListener(final IPreferenceContainer pc) {
        Preference p = pc.findPreference(PREFS_NOTIFICATION_ICON);
        if (p != null) {
            p.setOnPreferenceClickListener(new OnNotificationIconClickListener(pc.getContext()));
        }

        Preference pbi = pc.findPreference(PREFS_BUBBLES_IN);
        Preference pbo = pc.findPreference(PREFS_BUBBLES_OUT);
        if (pbi != null || pbo != null) {
            final OnBubblesClickListener obcl = new OnBubblesClickListener(pc.getContext());

            if (pbi != null) {
                pbi.setOnPreferenceClickListener(obcl);
            }
            if (pbo != null) {
                pbo.setOnPreferenceClickListener(obcl);
            }
        }

        p = pc.findPreference(PREFS_TEXTCOLOR);
        if (p != null) {
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(pc.getContext());

                    int c = prefs.getInt(PREFS_TEXTCOLOR, 0);
                    if (c == 0) {
                        c = BLACK;
                    }

                    return true;
                }
            });
        }
    }
    static int getTheme(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_THEME, null);
        if (THEME_BLACK.equals(s)) {
            return R.style.Theme_SMSdroid;
        } else {
            return R.style.Theme_SMSdroid_Light;
        }
    }

    static int getTextsize(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_TEXTSIZE, null);
        Log.d(TAG, "text size: ", s);
        return Utils.parseInt(s, 0);
    }

    static int getTextcolor(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        if (context instanceof ConversationListActivity
                && p.getBoolean(PREFS_TEXTCOLOR_IGNORE_CONV, false)) {
            return 0;
        }
        final int ret = p.getInt(PREFS_TEXTCOLOR, 0);
        Log.d(TAG, "text color: ", ret);
        return ret;
    }

    static int getLEDcolor(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_LED_COLOR, "65280");
        return Integer.parseInt(s);
    }

    static int[] getLEDflash(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_LED_FLASH, "500_2000");
        final String[] ss = s.split("_");
        final int[] ret = new int[2];
        ret[0] = Integer.parseInt(ss[0]);
        ret[1] = Integer.parseInt(ss[1]);
        return ret;
    }

    static long[] getVibratorPattern(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_VIBRATOR_PATTERN, "0");
        final String[] ss = s.split("_");
        final int l = ss.length;
        final long[] ret = new long[l];
        for (int i = 0; i < l; i++) {
            ret[i] = Long.parseLong(ss[i]);
        }
        return ret;
    }

    static int getNotificationIcon(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final int i = p.getInt(PREFS_NOTIFICATION_ICON, R.drawable.stat_notify_sms);
        if (i >= 0 && i < NOTIFICAION_IMG.length) {
            return NOTIFICAION_IMG[i];
        }
        return R.drawable.stat_notify_sms;
    }

    static int getBubblesIn(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final int i = p.getInt(PREFS_BUBBLES_IN, R.drawable.bubble_old_turquoise_left);
        if (i >= 0 && i < BUBBLES_IMG.length) {
            return BUBBLES_IMG[i];
        }
        return R.drawable.bubble_old_turquoise_left;
    }

    static int getBubblesOut(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final int i = p.getInt(PREFS_BUBBLES_OUT, R.drawable.bubble_old_green_right);
        if (i >= 0 && i < BUBBLES_IMG.length) {
            return BUBBLES_IMG[i];
        }
        return R.drawable.bubble_old_green_right;
    }

    static boolean decodeDecimalNCR(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean b = p.getBoolean(PREFS_DECODE_DECIMAL_NCR, true);
        Log.d(TAG, "decode decimal ncr: ", b);
        return b;
    }
    static boolean showEmoticons(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        return p.getBoolean(PREFS_EMOTICONS, true);
    }
    
    static String fixNumber(final Context context, final String number) {
        String ret = number;
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        for (int i = 1; i <= PREFS_REGEX_COUNT; i++) {
            final String regex = p.getString(PREFS_REGEX + i, null);
            if (!TextUtils.isEmpty(regex)) {
                try {
                    Log.d(TAG, "search for '", regex, "' in ", ret);
                    ret = ret.replaceAll(regex, p.getString(PREFS_REPLACE + i, ""));
                    Log.d(TAG, "new number: ", ret);
                } catch (PatternSyntaxException e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
        return ret;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {// app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, ConversationListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public final Activity getActivity() {
        return this;
    }

    @Override
    public final Context getContext() {
        return this;
    }
}
