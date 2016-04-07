package com.alexey.spyrecorder;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by Leha on 07-Apr-16.
 */
public class SettingsActivity extends PreferenceActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
}
