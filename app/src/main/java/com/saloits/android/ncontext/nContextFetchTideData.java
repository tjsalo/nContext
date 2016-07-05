/*
 * Copyright (C) 2016, Salo IT Solutions, Inc.
 */

/** implements an IntentService that retrieves tide information from NOAA.gov.
 *
 * Currently, this proof-of-concept implementation obtains the current location, and
 * then retrieves tide times for La Jolla, California.  The NOAA tide times API
 * requires a NOAA tides & currents obsertation station ID. This proof-of-concept
 * implementation requires the user to specify a location, but this IntentService
 * returns tide times for La Jolla, California, no mater what location the caller
 * specifies.
 *
 * Timothy J. Salo, March 8, 2016.
 */

package com.saloits.android.ncontext;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class nContextFetchTideData extends IntentService {

    private final String LOG_TAG = nContextFetchTideData.class.getSimpleName();
    private final boolean DEBUG = true;

    /* Result Codes. */

    static public final int RESULT_SUCCESS = 0;
    static public final int RESULT_FAILURE = -1;

    static public final String KEY_RESULT_DATA
            = "com.saloits.android.ncontext.KEY_RESULT_DATA";

    static public final String RECEIVER
            = "com.saloits.android.ncontext.RECEIVER";
    static public final String LOCATION_DATA_EXTRA
            = "com.saloits.android.ncontext.TIDES_DATA_EXTRA";

    protected ResultReceiver mReceiver;     // ResultReceiver used to return result String


//    /* nContextFetchTideData(String) */
//
//    public nContextFetchTideData(String name) {
//        super(name);
//    }
//
//
    /* nContextFetchTideData() */

    public nContextFetchTideData() {
        super("nContextFetchTideData");
    }


    /* onHandleIntent(Intent) */

    /**
     * fetches from NOAA.gov tide information for the location specified in the Intent.
     *
     * @param intent - contains Location (as a Parcel).
     */

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(LOG_TAG, "onHandleIntent()");

        mReceiver = intent.getParcelableExtra(RECEIVER);    // Save ResultReceiver

        Log.d(LOG_TAG, "mReceiver: " + mReceiver);

        Location location = intent.getParcelableExtra(LOCATION_DATA_EXTRA);

        Log.d(LOG_TAG, String.format("Location: (%.6f, %.6f)",
                location.getLatitude(), location.getLongitude()));

        /* The location should be translated into a NOAA tides and currents station ID.
         * Currently, we just give up and request data for the La Jolla station.
         */

        /* Build URL to request tide information.
        * Request time times  from 12 hours ago to 48 hours from now.  The CO-OPS
        * website wants times in UTC.
        */

        /* Build times for request. */

        SimpleDateFormat dateFormat
                = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

        Calendar calendarNow = Calendar.getInstance();

        Calendar calendarMinus12 = Calendar.getInstance();  // 12+ hours ago
        calendarMinus12.set(Calendar.MINUTE, 0);
        calendarMinus12.set(Calendar.SECOND, 0);
        calendarMinus12.set(Calendar.MILLISECOND, 0);
        calendarMinus12.add(Calendar.HOUR_OF_DAY, -12);

        Calendar calendarPlus48 = Calendar.getInstance();   // 48 hours from now
        calendarPlus48.set(Calendar.MINUTE, 59);
        calendarPlus48.set(Calendar.SECOND, 0);
        calendarPlus48.set(Calendar.MILLISECOND, 0);
        calendarPlus48.add(Calendar.HOUR_OF_DAY, 48);

        /*
          http://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS
          ?service=SOS
          &request=GetObservation
          &version=1.0.0
          &observedProperty=sea_surface_height_amplitude_due_to_equilibrium_ocean_tide
          &offering=urn:ioos:station:NOAA.NOS.CO-OPS:9410230
          &responseFormat=text%2Fxml%3Bsubtype%3D%22om/1.0.0/profiles/ioos_sos/1.0%22
          &eventTime=2016-03-09T00:00:00Z/2016-04-09T23:59:00Z
          &dataType=HighLowTidePredictions
          &unit=Feet
        */

        String TIDES_BASE_URL = "http://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS";
        URL url = null;

        calendarMinus12.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.setCalendar(calendarMinus12);
        String startTime = dateFormat.format(calendarMinus12.getTime());

        calendarPlus48.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.setCalendar(calendarPlus48);
        String endTime = dateFormat.format(calendarPlus48.getTime());

        Uri builtUri = Uri.parse(TIDES_BASE_URL).buildUpon()
                .appendQueryParameter("service", "SOS")
                .appendQueryParameter("request", "GetObservation")
                .appendQueryParameter("version", "1.0.0")
                .appendQueryParameter("observedProperty",
                        "sea_surface_height_amplitude_due_to_equilibrium_ocean_tide")
                .appendQueryParameter("offering", "urn:ioos:station:NOAA.NOS.CO-OPS:9410230")
//                .appendQueryParameter("responseFormat",
//                        "text%2Fxml%3Bsubtype%3D%22om/1.0.0/profiles/ioos_sos/1.0%22")
//                .appendQueryParameter("responseFormat",
//                        "text%2Fxml%3Bschema%3D%22ioos/0.6.1%22")
//                .appendQueryParameter("responseFormat",
//                        "application%2Fioos%2Bxml%3Bversion%3D0.6.1")
                .appendQueryParameter("responseFormat",
                        "text%2Fcsv")
                .appendQueryParameter("eventTime", startTime + "/" + endTime)
                .appendQueryParameter("dataType", "HighLowTidePredictions")
                .appendQueryParameter("unit", "Feet")
                .build();

        try {
            url = new URL(builtUri.toString());
            url = new URL(Uri.decode(builtUri.toString()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (url != null) {
            Log.d(LOG_TAG, "Decoded URL: " + Uri.decode(url.toString()));
        } else {
            Log.d(LOG_TAG, "URL: null");
        }

        // 32.9624784,-117.2703195 Del Mar

        if (url == null) return;        // on error

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        InputStream inputStream = null;
        StringBuilder buffer = new StringBuilder();   // retrieved CVS (or XML) string

        try {

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            /* Read input stream into a String */

            inputStream = urlConnection.getInputStream();

            if (inputStream != null) {

                reader = new BufferedReader(new InputStreamReader(inputStream));

                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line).append("\n");
                    }
                } catch (IOException e) {
                    Log.d(LOG_TAG, "fetchMovieData: readLine() failed");
                }
            }

        } catch (MalformedURLException e) {
            Log.d(LOG_TAG, e.toString());
        } catch (UnknownHostException e) {
            Log.d(LOG_TAG, e.toString());
        } catch (IOException e) {
            Log.d(LOG_TAG, e.toString());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        if (buffer.length() == 0) return;   // fail silently, if no tide data

        /* Parse CSV string. */

        String string = buffer.toString();
        Boolean skipFirst = false;
        SimpleDateFormat tideDateFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        Date tideDate = null;
        tideDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar tideCalendar = Calendar.getInstance();
        SimpleDateFormat tideOutFormat = new SimpleDateFormat("MM/dd EEE hh:mm a");

        Calendar previousTideCalendar = null;
        String previousTideInfo = null;
        Calendar nextTideCalendar = null;
        String nextTideInfo = null;

        /* For each line. */

        /* station_id
         * sensor_id
         * latitude (degree)
         * longitude (degree)
         * date_time
         * sea_surface_height_amplitude_due_to_equilibrium_ocean_tide (feet)
         * type
         * datum_id
         * vertical_position (feet)
         */

        String date_time;
        String sea_surface_height;
        String type;
        String tideTimes = "";

        String[] lines = string.split("\n");

        for (String line: lines) {

            if (skipFirst) {

                String[] tokens = line.split(",");

                date_time = tokens[4];
                sea_surface_height = tokens[5];
                type = tokens[6];

                try {
                    tideDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    tideDate = tideDateFormat.parse(date_time);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                /* Build String for display */

                if (tideDate != null) {
                    tideCalendar.setTime(tideDate);
                    tideCalendar.setTimeZone(TimeZone.getTimeZone("US/Pacific"));
                    tideOutFormat.setCalendar(tideCalendar);
                    tideTimes += tideOutFormat.format(tideCalendar.getTime())
                            + " " + sea_surface_height + " " + type + "\n";

                    /* Retain info for Notification. */

                    float height = Float.parseFloat(sea_surface_height);
                    sea_surface_height = String.format("%.1f", height);
                    if (tideCalendar.before(calendarNow)) {
                        previousTideCalendar = (Calendar) tideCalendar.clone();
                        previousTideInfo = sea_surface_height + " " + type;
                    } else if (nextTideCalendar == null) {
                        nextTideCalendar = (Calendar) tideCalendar.clone();
                        nextTideInfo = sea_surface_height + " " + type;
                    }
                }

            } else {
                skipFirst = true;
            }
        }

        /* Create notification. */

        /*
         * Time times: DDD mm/dd, La Jolla, Ca
         * 7:08 am: High (5.84) 2:12 pm: Low (-0.95)
         */

        SimpleDateFormat notificationDateFormat1 = new SimpleDateFormat("EEE MM/dd");
        SimpleDateFormat notificationDateFormat2 = new SimpleDateFormat("EEE hh:mm a");

        calendarNow.setTimeZone(TimeZone.getTimeZone("US/Pacific"));

        notificationDateFormat1.setCalendar(calendarNow);
        String notificationLine1 = "Tides: "
                + notificationDateFormat1.format(calendarNow.getTime())
                + ", La Jolla, CA";
        notificationDateFormat2.setCalendar(previousTideCalendar);
        String notificationLine2
                = notificationDateFormat2.format(previousTideCalendar.getTime())
                + " " + previousTideInfo;
        notificationDateFormat2.setCalendar(nextTideCalendar);
        notificationLine2 += " / "
                + notificationDateFormat2.format(nextTideCalendar.getTime())
                + " " + nextTideInfo;
        Log.d(LOG_TAG, notificationLine1);
        Log.d(LOG_TAG, notificationLine2);

//        Log.d(LOG_TAG, "Tide times:\n" + tideTimes);

        /* Return results to calling Activity using ResultReceiver passed by caller. */

        Bundle bundle = new Bundle();
        bundle.putString(KEY_RESULT_DATA, tideTimes);
        Log.d(LOG_TAG, "mReceiver: " + mReceiver);
        mReceiver.send(RESULT_SUCCESS, bundle);

        /* Parse XML */
        /* Note that it appears that XmlPullParser cannot parse the XML used by the
         * NOAA CO-OPS site.
         */

//        if (buffer.length() != 0){
//
//            String ns = null;
//            StringReader in = new StringReader( buffer.toString() );
//
//            try {
//                XmlPullParser parser = Xml.newPullParser();
//                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//                parser.setInput( in );
//                parser.nextTag();
//
//                /* Do something useful. */
//
//                parser.setInput( in );
//                int eventType = parser.getEventType();
//                while (eventType != XmlPullParser.END_DOCUMENT) {
//                    if(eventType == XmlPullParser.START_DOCUMENT) {
//                        System.out.println("Start document");
//                    } else if(eventType == XmlPullParser.START_TAG) {
//                        System.out.println("Start tag " + parser.getName());
//                    } else if(eventType == XmlPullParser.END_TAG) {
//                        System.out.println("End tag " + parser.getName());
//                    } else if(eventType == XmlPullParser.TEXT) {
//                        System.out.println("Text " + parser.getText());
//                    }
//                    eventType = parser.next();
//                }
//                System.out.println("End document");
//
////                List entries = new ArrayList();
////
////                parser.require(XmlPullParser.START_TAG, ns, "feed");
////                while (parser.next() != XmlPullParser.END_TAG) {
////                    if (parser.getEventType() != XmlPullParser.START_TAG) {
////                        continue;
////                    }
////                    String name = parser.getName();
////                    // Starts by looking for the entry tag
////                    if (name.equals("entry")) {
////                        entries.add(readEntry(parser));
////                    } else {
////                        skip(parser);
////                    }
////                }
//
//            } catch (XmlPullParserException e) {
//                Log.d(LOG_TAG, e.toString());
//            } catch (IOException e) {
//                Log.d(LOG_TAG, e.toString());
//            } finally {
//                in.close();
//            }
//        }
    }
}

