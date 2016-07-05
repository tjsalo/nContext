/*
 * Copyright (C) 2016, Salo IT Solutions, Inc.
 */

/**
 * demonstrates proof-of-concept implementations for several displays of contextual
 * environmental information.
 *
 * Timothy J. Salo, February 22, 2016.
 */


package com.saloits.android.ncontext;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class nContextActivity extends AppCompatActivity {

    private final String LOG_TAG = nContextActivity.class.getSimpleName();
    private final boolean DEBUG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(DEBUG) Log.d(LOG_TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ncontext_activity);
    }

    public void onWxDemoButton(View view) {

        Log.d(LOG_TAG, "onWxDemoButton()");

        Intent intent = new Intent(this, nContextWxActivity.class);
        startActivity(intent);
    }


    public void onTidesDemoButton(View view) {

        Log.d(LOG_TAG, "onTidesDemoButton()");

        Intent intent = new Intent(this, nContextTidesActivity.class);
        startActivity(intent);
    }
}
