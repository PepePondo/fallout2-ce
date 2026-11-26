package com.alexbatalov.fallout2ce;

import android.content.Intent;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

import org.libsdl.app.SDLActivity;

import java.io.File;

public class MainActivity extends SDLActivity {
    private boolean noExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Decide where the game runs before SDL starts the native code.
        final boolean usePortHub = PortHubFolder.preferred(this) && PortHubFolder.canUse(this);
        if (usePortHub) {
            try {
                Os.setenv(PortHubFolder.ENV_DATA_PATH, PortHubFolder.dir().getAbsolutePath(), true);
            } catch (ErrnoException e) {
                // Falls back to the app's own folder.
            }
        }

        super.onCreate(savedInstanceState);

        if (usePortHub) {
            return;
        }

        final File externalFilesDir = getExternalFilesDir(null);

        final File configFile = new File(externalFilesDir, "fallout2.cfg");
        if (!configFile.exists()) {
            final File masterDatFile = new File(externalFilesDir, "master.dat");
            final File critterDatFile = new File(externalFilesDir, "critter.dat");
            if (!masterDatFile.exists() || !critterDatFile.exists()) {
                final Intent intent = new Intent(this, ImportActivity.class);
                startActivity(intent);

                noExit = true;
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!noExit) {
            // Needed to make sure libc calls exit handlers, which releases
            // in-game resources.
            System.exit(0);
        }
    }

    @Override
    protected String[] getLibraries() {
        return new String[]{
            "fallout2-ce",
        };
    }
}
