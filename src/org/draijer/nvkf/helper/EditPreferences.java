package org.draijer.nvkf.helper;

import org.draijer.nvkf.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class EditPreferences extends PreferenceActivity {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
     
        @Override
        protected void onResume() {
            super.onResume();
            addPreferencesFromResource(R.xml.preferences);
        }
    }