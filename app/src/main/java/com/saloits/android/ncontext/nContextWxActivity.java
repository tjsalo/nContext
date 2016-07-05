/*
 * Copyright (C) 2016, Salo IT Solutions, Inc.
 */

/**
 * displays weather data for the current location.
 *
 * Timothy J. Salo, Feruary 26, 2016.
 */

package com.saloits.android.ncontext;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class nContextWxActivity extends AppCompatActivity {

    private final String LOG_TAG = nContextWxActivity.class.getSimpleName();
    private final boolean DEBUG = true;

    /* Request codes used by onActivityResult() */

    public static final int REQUEST_LOCATION_PERMISSION = 1;

    /* onCreate() */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (DEBUG) Log.d(LOG_TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ncontext_wx_activity);
    }


    /* onActivityResult() */

    /** called when another Activity returns a result. It is used to process a request
     * to the user to enable location tracking.  It restarts nContextTidesActivityFragment.
     *
     * @param requestCode: set by the calling nContext Activity, to differentiate various
     *                   nContext uses.
     * @param resultCode: an Activity RESULT code.
     * @param data: Uhh...
     */

    /* TODO: probably move to nContextWxActivityFragment.  Except, IntentService
     * apparently can't return to a Fragment...
     * TODO: support request codes.
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(LOG_TAG, "onActivityResult: requestCode:" + requestCode
                + " resultCode:" + resultCode + " Intent:" + data);

        if (resultCode == RESULT_OK) {
            Log.d(LOG_TAG, "onActivityResult: RESULT_OK");
        } else {
            Log.d(LOG_TAG, "onActivityResult: " + resultCode);
        }

        if (data.getAction() != null) Log.d(LOG_TAG, "onActivityResult: " + data.getAction());

        if (data.getDataString() != null) Log.d(LOG_TAG, "onActivityResult: " + data.getDataString());

        Bundle bundle = data.getExtras();
        if (bundle != null) Log.d(LOG_TAG, "onActivityResult: " + data.toString());

        if (resultCode == RESULT_OK) {
            Log.d(LOG_TAG, "onActivityResult: restarting nContextWxActivityFragment");

            Fragment frg = null;
            frg = getFragmentManager().findFragmentByTag("nContextWxActivityFragment");
            final FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.detach(frg);
            ft.attach(frg);
            ft.commit();

        }

        super.onActivityResult(requestCode, resultCode, data);

    }
}
