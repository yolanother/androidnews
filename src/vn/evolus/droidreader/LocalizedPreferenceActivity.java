package vn.evolus.droidreader;

import java.util.Locale;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.DisplayMetrics;

public class LocalizedPreferenceActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = new Locale(Settings.getLocale(this));
        res.updateConfiguration(conf, dm);
        
		super.onCreate(savedInstanceState);		
	}
}
