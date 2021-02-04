package de.ub0r.android.smsdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.view.View.OnClickListener;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.ads.MobileAds;
import de.ub0r.android.logg0r.Log;

public final class ChatApp extends Application {

    private static final String TAG = "app";

    static final String NOTIFICATION_CHANNEL_ID_MESSAGES = "messages";
    static final String NOTIFICATION_CHANNEL_ID_FAILD_SENDING_MESSAGE = "failed_sending_message";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "init Chat App v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        MobileAds.initialize(this, getString(R.string.admob_app_id));
    }
    static OnClickListener getOnClickStartActivity(final Context context, final Intent intent) {
        if (intent == null) {
            return null;
        }
        return v -> {
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "activity not found", e);
                Toast.makeText(context, "no activity for data: " + intent.getType(),
                        Toast.LENGTH_LONG).show();
            }
        };
    }

    static boolean isDefaultApp(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true;
        }

        try {
            final String smsPackage = Telephony.Sms.getDefaultSmsPackage(context);
            return smsPackage == null || smsPackage.equals(BuildConfig.APPLICATION_ID);
        } catch (SecurityException e) {
            Log.e(TAG, "failed to query default SMS app", e);
            return true;
        }
    }

    static boolean hasPermission(final Context context, final String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean requestPermission(final Activity activity, final String permission, final int requestCode, final int message, final DialogInterface.OnClickListener onCancelListener) {
        Log.i(TAG, "requesting permission: " + permission);
        if (!hasPermission(activity, permission)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.permissions_)
                        .setMessage(message)
                        .setCancelable(false)
                        .setNegativeButton(android.R.string.cancel, onCancelListener)
                        .setPositiveButton(android.R.string.ok,
                                (dialogInterface, i) -> ActivityCompat.requestPermissions(activity,
                                        new String[]{permission}, requestCode))
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            }
            return true;
        } else {
            return false;
        }
    }
}
