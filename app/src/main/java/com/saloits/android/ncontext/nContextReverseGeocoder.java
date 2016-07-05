/*
 * Copyright (C) 2016, Salo IT Solutions, Inc.
 */

/**
 * implements an IntentService that provides a reverse geocoder that uses Google
 * Play Services to translate a location into an address.
 *
 * NOTE: this implementation is incomplete.
 *
 * Timothy J. Salo, March 4, 2016.
 */

package com.saloits.android.ncontext;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class nContextReverseGeocoder extends IntentService {


    private final String LOG_TAG = nContextReverseGeocoder.class.getSimpleName();
    private final boolean DEBUG = true;

    /* Result Codes. */

    static public final int RESULT_SUCCESS = 0;
    static public final int RESULT_FAILURE = -1;

    static public final String KEY_RESULT_DATA
            = "com.saloits.android.ncontext.KEY_RESULT_DATA";

    static public final String RECEIVER
            = "com.saloits.android.ncontext.LOCATION_DATA_EXTRA";
    static public final String LOCATION_DATA_EXTRA
            = "com.saloits.android.ncontext.GEOCODER_DATA_EXTRA";


    /* nContextReverseGeocoder(String) */

    public nContextReverseGeocoder(String name) {
        super(name);
    }


    /* nContextReverseGeocoder() */

    public nContextReverseGeocoder() {super("nContextReverseGeocoder");}

    /* onHandleIntent(Intent) */

    @Override
    protected void onHandleIntent(Intent intent) {

        String errorMessage = "";

        Location location = intent.getParcelableExtra(LOCATION_DATA_EXTRA);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = "Service not available.";
            Log.e(LOG_TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = "Invalid lat/lon.";
            Log.e(LOG_TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "No address found.";
                Log.e(LOG_TAG, errorMessage);
            }
            deliverResultToReceiver(RESULT_FAILURE, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(LOG_TAG, "Address found.");
            deliverResultToReceiver(RESULT_SUCCESS,
                    TextUtils.join(System.getProperty("line.separator"),
                            addressFragments));
        }
    }


    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_RESULT_DATA, message);
//        mReceiver.send(resultCode, bundle);
    }

}
