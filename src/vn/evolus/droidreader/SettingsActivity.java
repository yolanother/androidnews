package vn.evolus.droidreader;

import android.os.Bundle;

public class SettingsActivity extends LocalizedPreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
						
		addPreferencesFromResource(R.xml.settings);
	}
}
