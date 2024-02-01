package com.qrprojectreader;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }



    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);



            androidx.preference.EditTextPreference editTextPreferenceNumber = getPreferenceManager().findPreference("worker_number");
            editTextPreferenceNumber.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);

                }
            });

//            androidx.preference.EditTextPreference editTextPreferenceAddress = getPreferenceManager().findPreference("address");
//            editTextPreferenceAddress.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
//                @Override
//                public void onBindEditText(@NonNull EditText editText) {
//                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
//
//                }
//            });

            androidx.preference.EditTextPreference editTextPreferencePort = getPreferenceManager().findPreference("port");
            editTextPreferencePort.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);

                }
            });

            androidx.preference.EditTextPreference editTextPreferenceTimeout = getPreferenceManager().findPreference("timeout");
            editTextPreferenceTimeout.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);

                }
            });

        }

//            @SuppressLint("RestrictedApi") PreferenceManager preferenceManager = new PreferenceManager(getContext());
//
//            EditTextPreference editTextPreference = preferenceManager.findPreference("timeout");//<EditTextPreference>("YOUR_PREFERENCE_KEY")
//            editTextPreference.setOnBindEditTextListener( ); //setOnBindEditTextListener { editText ->
//                    editText.inputType = InputType.TYPE_CLASS_NUMBER
//            }


    }


}