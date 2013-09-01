package org.draijer.nvkf;

import java.util.Calendar;

import org.draijer.nvkf.persistance.SyncResultReceiver;
import org.draijer.nvkf.persistance.SyncService;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TabHost;

public class NVKFTabWidget extends TabActivity {
	Builder mMelding;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    
	    SharedPreferences internalSettings = getSharedPreferences("NVKF_intern", 0);
	    boolean firstRun = internalSettings.getBoolean("firstRun-13_03_02", true);
	    
	    if(firstRun) {
	    	mMelding = new AlertDialog.Builder(this);
	    	mMelding.create();
	    	mMelding.setTitle("Nieuw in versie "+ getString(R.string.aboutVersion));
	    	mMelding.setMessage("Welkom bij de nieuwe versie van de NVKF app.\nTen opzichte van de vorige versie zijn er slechts enkele zaken veranderd : er is hopelijk een bug verholpen in het syncen van de contacten en er is een taalfout verbeterd.\nOm bugs nog beter uit de app te kunnen halen is het handig als ik crash-rapports krijg. Mocht de app dus crashen : graag het rapport versturen, dan kan ik zien in welke regel code er een bug zit.");
	    	mMelding.show();
	    }
	        	
    	SharedPreferences.Editor editor = internalSettings.edit();
        editor.putBoolean("firstRun-13_03_02", false);
        editor.commit();

	    //Resources res = getResources();	// Resource object to get Drawables
	    TabHost tabHost = getTabHost();	// The activity TabHost
	    TabHost.TabSpec spec;			// Resusable TabSpec for each tab
	    Intent intent;					// Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent();
	    //intent.putExtra("INITIALIZE", true);
	    intent.setClass(this, NewsActivity.class);
	    
	    // Do the same for the other tabs
	    spec = tabHost.newTabSpec("news")
	    	.setIndicator("Nieuws")
	    	.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Initialize a TabSpec for each tab and add it to the TabHost
	    intent = new Intent().setClass(this, JobsActivity.class);
	    spec = tabHost.newTabSpec("jobs")
	    		.setIndicator("Vacatures")
	    		.setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, SearchActivity.class);
	    spec = tabHost.newTabSpec("search")
	    		.setIndicator("Ledenlijst")
	    		.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Als deze Activity vanuit de notificatie wordt aangeroepen,
	    // zit in die Intent het id van het tabblad wat geopend moet worden. 
	    int tab = 0;
	    Intent sendIntent = getIntent();
    	Bundle extrasBundle = sendIntent.getExtras();
    	
    	if(extrasBundle != null) {
    		switch (extrasBundle.getInt("id")) {
    			case R.string.tabJobs :
    				tab = 0;
    				break;
    			case R.string.tabNews :
    				tab = 1;
    				break;
    			default :
    				tab = 0;
    		}
    	}
    	
	    tabHost.setCurrentTab(tab);
	    
//	    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//        String prefInterval = settings.getString("prefRefreshRate", "4");
//        int refreshRate = Integer.valueOf(prefInterval);
// 
//        // Stel "diepe" details in van de afspraak
//        SyncResultReceiver receiver = new SyncResultReceiver();
//        
//    	Intent refreshIntent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
//    	refreshIntent.putExtra(SyncService.RECEIVER, receiver);
//    	refreshIntent.putExtra(SyncService.ACTION, SyncService.ACTION_JOBS);
//    	refreshIntent.putExtra("searchParameter", "");
//    	PendingIntent pendingIntent = PendingIntent.getService(this, R.string.tabJobs, refreshIntent, 0);
//        	
//        // Maak de afspraak
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(System.currentTimeMillis());
//        calendar.add(Calendar.HOUR, refreshRate);
//        	        	
//        // En voeg de afspraak toe aan de AlarmManager
//        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);        	
//        	        	
//        if(refreshRate >= 1) {
//        	long interval = refreshRate * 60 * 60 * 1000;
//        	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval, pendingIntent);	
//        }
	}
}
