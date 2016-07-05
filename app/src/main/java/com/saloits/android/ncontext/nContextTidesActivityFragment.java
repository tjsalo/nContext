/*
 * Copyright (C) 2016, Salo IT Solutions, Inc.
 */

/* nContextTidesActivityFragment */

/**
 * displays tide information for Del Mar, California.
 *
 * nContetTidesActivityFragment determines the current location, then ignores the current
 * location and retrives from a NOAA server tide times for La Jolla, California.  Somday,
 * this code may find tidetables for the current location.  However, it is not at all
 * clear who a current location ought to be mapped to the nearest, or the best, NOAA
 * tides and currents observation station.
 *
 * nContextTidesActivityFragment:
 *
 * o Connects to a GoogleApiClient, created in onCreate(), in onSart().  Control returns
 *   via callback to onConnected(), onConnectionFailed() or onConnectionSuspended().
 *
 * o If the connection is successful, the callback onConnected() checks location settings to
 *   ensure that the user has enabled location tracking.  Control is returned to
 *   ResultsCallbacks.onSuccess, if adequate location permissions are available, or
 *   ResultsCallbacks.onFailure, if adequate location permissions are available.
 *
 * o if adequate locations permissions are available, callback ResultsCallbacks.onSuccess()
 *   asks Google Play Services for the last know location.  The information returned in the
 *   Location is displayed.
 *
 *   - ResultsCallbacks.onSuccess then issues an Intent that requests nContextReverseGeocoder
 *     to find one more addresses associated with this location.  This Intent initiates
 *     nContextReverseGeocoder, an IntentService.  nContextReverseGeocoder returns its
 *     result via a callback to ...
 *
 *   - Control returns somewhere...
 *
 * o If adequate location permissions are not available, callback ResultsCallbacks.onFailure()
 *   asks Google Play Services to ask the user to permit this app to use location services
 *   APIs.  After the user accepts or does not accept enabling location tracking,  callback
 *   nContextWxActivity.onActivityResult() is called.  Note that this callback is in the
 *   calling Activity, not in this Fragment.  If the user agreed to enable location tracking,
 *   onActivityResult() restarts this Fragment (nContextWxActivityFragment).
 *
 * Timothy J. Salo March 8, 2016.
 */


package com.saloits.android.ncontext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import static android.os.SystemClock.elapsedRealtimeNanos;


public class nContextTidesActivityFragment
        extends Fragment
        implements GoogleApiClient.ConnectionCallbacks, OnConnectionFailedListener {

    private final String LOG_TAG = nContextTidesActivityFragment.class.getSimpleName();
    private final boolean DEBUG = true;

    GoogleApiClient mGoogleApiClient = null;    // API for Google Play Services
    Location mLastLocation = null;              // Location returned by Google Play Services
    public TideResultReceiver mReceiver;        // ResultReceiver for nContextFetchTideData


    /* onCreate(Bundle) */

    @Override
    public void onCreate(Bundle savedInstanceState) {

        if (DEBUG) Log.d(LOG_TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        mReceiver = new TideResultReceiver(new Handler());

        /* Get a GoogleApiClient; add LocationServices. */

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }


    /* onCreateView() */

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (DEBUG) Log.d(LOG_TAG, "onCreateView()");

        return inflater.inflate(R.layout.ncontext_tides_activity_fragment, container, false);
    }


    /* onStart() */

    @Override
    public void onStart() {

        if (DEBUG) Log.d(LOG_TAG, "onStart()");

        mGoogleApiClient.connect();     // connect to Google Play Services
                                        // returns to onConnected(Bundle),
                                        // onConnectionFailed() or onConnectionSuspended()
        super.onStart();
    }


    /* onStop() */

    @Override
    public void onStop() {

        if (DEBUG) Log.d(LOG_TAG, "onStop()");

        mGoogleApiClient.disconnect();  // disconnect from Google Play Services

        super.onStop();
    }


    /* onConnected() */

    /** receives control when GoogleApiClient is connected.
     *
     * @param bundle
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (DEBUG) Log.d(LOG_TAG, "onConnected()");

        if (bundle != null) Log.d(LOG_TAG, "onConnected bundle: " + bundle.toString());

        /* Build LocationRequest. (Both HIGH_ACCURACY and BALANCED_POWER_ACCURACY built.) */

        LocationRequest mLocationRequestFine = new LocationRequest();
        mLocationRequestFine.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationRequest mLocationRequestCoarse = new LocationRequest();
        mLocationRequestCoarse.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        /* Build LocationSettingReequest, currently with PRIORITY_HIGH_ACCURACY */

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequestFine);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallbacks<LocationSettingsResult>() {

            /* ResultCallbacks.onSuccess(LocationSettingResult) */

            /**
             * is called back when Google Play Services with the results of a location request.
             *
             * @param locationSettingsResult
             */

            @Override
            public void onSuccess(@NonNull LocationSettingsResult locationSettingsResult) {
                Log.d(LOG_TAG, "ResultCallback:onSuccess()");

                LocationSettingsStates states = locationSettingsResult.getLocationSettingsStates();
                Log.d(LOG_TAG, "onSuccess:states: BlePresent:" + states.isBlePresent()
                        + " BleUsable:" + states.isBleUsable()
                        + " GpsPresent:" + states.isGpsPresent()
                        + " GpsUsable:" + states.isGpsUsable()
                        + " LocationPresent:" + states.isLocationPresent()
                        + " LocationUsable:" + states.isLocationUsable()
                        + " NetworkLocationPresent:" + states.isNetworkLocationPresent()
                        + " NetworkLocationUsable:" + states.isNetworkLocationUsable()
                );

                /* Check for necessary permissions.  This seems redundant, but Android Studio
                 * complains if this check isn't made (even though we just asked Google Play
                 * Services).
                 */

                if ((ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)
                        && (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)) {

                    Log.d(LOG_TAG, "onConnected(): permission not available");

                    /* user already had an opportunity to grant permissions, so give up. */

                    return;
                }

                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
                    Log.d(LOG_TAG, String.format("Location: (%.6f, %.6f)",
                            mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                    Log.d(LOG_TAG, String.format("Accuracy: +/- %.2fm (68%% confidence)",
                            mLastLocation.getAccuracy()));
                    Log.d(LOG_TAG, String.format("Altitude: %.2fm", mLastLocation.getAltitude()));
                    Log.d(LOG_TAG, String.format("Bearing: %.0f", mLastLocation.getBearing()));
                    Log.d(LOG_TAG, String.format("Time since last fix: %d sec",
                            (elapsedRealtimeNanos() - mLastLocation.getElapsedRealtimeNanos())/1000000000 ));
                    String provider = mLastLocation.getProvider();
                    Log.d(LOG_TAG, "Provider: " + ((provider != null) ? provider : "null"));
                    Log.d(LOG_TAG, String.format("Speed: %.1f m/s", mLastLocation.getSpeed()));
                } else {
                    Log.d(LOG_TAG, "onConnected(): LastLocation null");
                }

                /* Request from NOAA.gov tide information for Del Mar, California. */

                /* Build Location for Del Mar, California lifeguard tower. */

                Location location = new Location("nContext");
                location.setLatitude(32.9624784d);
                location.setLongitude(-117.2703195d);

                /* Request tide information from nContextFetchTideData. */

                Log.d(LOG_TAG, "mReceiver: " + mReceiver);

                Intent intent = new Intent(getActivity(), nContextFetchTideData.class);
                intent.putExtra(nContextFetchTideData.RECEIVER, mReceiver);
                intent.putExtra(nContextFetchTideData.LOCATION_DATA_EXTRA, location);
                getActivity().startService(intent);
            }

            /* onFailure(Status) */

            @Override
            public void onFailure(@NonNull Status status) {
                Log.d(LOG_TAG, "ResultCallback:onFailure()");
                if (status.hasResolution()
                        && (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED)) {
                    try {
                        status.startResolutionForResult(getActivity(), nContextWxActivity.REQUEST_LOCATION_PERMISSION);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(LOG_TAG, "ResultCallback:onFailure: unrecoverable error");
                }
            }
        } );

    }


    /* onConnectionFailed() */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        if (DEBUG) Log.d(LOG_TAG, "onConnecctionFailed()");

        // TODO: https://developers.google.com/android/guides/api-client#handle_connection_failures

    }


    /* onConnectionSuspended() */

    @Override
    public void onConnectionSuspended(int i) {

        if (DEBUG) Log.d(LOG_TAG, "onConnectionSuspended(), code: " + i);
    }


    /* Note: the Activity, not this Fragment, gets called... */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult: requestCode:" + requestCode
                + " resultCode:" + resultCode + " Intent:" + data);
        super.onActivityResult(requestCode, resultCode, data);
    }


    @SuppressLint("ParcelCreator")
    class TideResultReceiver extends ResultReceiver {

        private final String LOG_TAG = ResultReceiver.class.getSimpleName();

        public TideResultReceiver(Handler handler) {
            super(handler);
            Log.d(LOG_TAG, "TideResultsReceiver()");
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            Log.d(LOG_TAG, "onReceiveResult()");

            String tideTimes = resultData.getString(nContextFetchTideData.KEY_RESULT_DATA);
            Log.d(LOG_TAG, tideTimes);

            TextView tideView = (TextView) getActivity().findViewById(R.id.tide_text_view);
            tideView.setText(tideTimes);
        }
    }
}
