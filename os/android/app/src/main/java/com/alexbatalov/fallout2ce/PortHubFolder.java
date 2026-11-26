package com.alexbatalov.fallout2ce;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import java.io.File;

/**
 * Game files that PortHub copied to Internal storage/PortHub/Fallout 2. The game runs from that folder
 * in place (saves included), so it needs no import step and the saves survive a reinstall.
 * A copy already imported into the app's own folder keeps working and takes priority.
 */
final class PortHubFolder {
    /** Read by the native code (win32.cc) to choose its working directory. */
    static final String ENV_DATA_PATH = "FALLOUT_DATA_PATH";

    private PortHubFolder() {}

    static File dir() {
        return new File(Environment.getExternalStorageDirectory(), "PortHub/Fallout 2");
    }

    static boolean hasGame(File dir) {
        return find(dir, "master.dat") && find(dir, "critter.dat");
    }

    /** True when the app's own folder has no game, so the PortHub folder should be used. */
    static boolean preferred(Context context) {
        final File own = context.getExternalFilesDir(null);
        if (own != null && (hasGame(own) || new File(own, "fallout2.cfg").exists())) {
            return false;
        }
        return hasGame(dir());
    }

    /**
     * Worth asking for access: without it Android 11+ hides the folder's files from this app,
     * so only the folder itself can be checked until the player allows access.
     */
    static boolean worthAsking(Context context) {
        return !canUse(context) && dir().isDirectory();
    }

    static boolean canUse(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    static void requestAccess(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName())));
            } catch (Exception e) {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            }, requestCode);
        }
    }

    /** Store copies keep their original file names, which may not be lower case. */
    private static boolean find(File dir, String name) {
        final String[] names = dir.list();
        if (names == null) {
            return false;
        }
        for (String n : names) {
            if (n.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
