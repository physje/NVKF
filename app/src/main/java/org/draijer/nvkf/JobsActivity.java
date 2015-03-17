package org.draijer.nvkf;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.draijer.nvkf.helper.DatabaseHelper;
import org.draijer.nvkf.helper.EditPreferences;
import org.draijer.nvkf.helper.JobAdapter;
import org.draijer.nvkf.model.Job;
import org.draijer.nvkf.persistance.SyncCallbackReceiver;
import org.draijer.nvkf.persistance.SyncResultReceiver;
import org.draijer.nvkf.persistance.SyncService;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class JobsActivity extends Activity implements SyncCallbackReceiver {
	ProgressDialog mProgressDialog;
	Builder mMelding;
	ArrayList<Job> mJobs;
	DatabaseHelper mDatabaseHelper;
    String prefInterval;

    private boolean runOnce = false;    
    private PendingIntent pendingIntent;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if(!runOnce) {
        	// Haal op de achtergrond de gegevens op
    		callSyncService();
    		runOnce = true;
        }
        
        // Voorkeuren ophalen
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        prefInterval = settings.getString("prefRefreshRate", "4");
        int refreshRate = Integer.valueOf(prefInterval);
 
        // Stel "diepe" details in van de afspraak
    	SyncResultReceiver receiver = new SyncResultReceiver();
    	receiver.setSyncCallbackReceiver(this);
    	
    	Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
    	intent.putExtra(SyncService.RECEIVER, receiver);
    	intent.putExtra(SyncService.ACTION, SyncService.ACTION_JOBS);
    	intent.putExtra("searchParameter", "");
        pendingIntent = PendingIntent.getService(this, R.string.tabJobs, intent, 0);
        	
        // Maak de afspraak
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.HOUR, refreshRate);
        	        	
        // En voeg de afspraak toe aan de AlarmManager
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);        	
        	        	
        if(refreshRate >= 1) {
        	long interval = refreshRate * 60 * 60 * 1000;
        	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval, pendingIntent);	
        } else {
        	alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
    
    public void onResume() {
    	super.onResume();      	
    	showFromDb();
    }
    
    private void callSyncService() {    	
    	mProgressDialog = new ProgressDialog(this);
    	mProgressDialog.setMessage("Bezig met het ophalen van vacatures...");
    	mProgressDialog.show();
    	
    	SyncResultReceiver receiver = new SyncResultReceiver();
    	receiver.setSyncCallbackReceiver(this);
    	
    	Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
    	intent.putExtra(SyncService.RECEIVER, receiver);
    	intent.putExtra(SyncService.ACTION, SyncService.ACTION_JOBS);
    	intent.putExtra("searchParameter", "");
		startService(intent);
	}
    

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_news_job, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuInstellingen:
                startActivity(new Intent(this, EditPreferences.class));
                return true;
            case R.id.menuRefresh:    			
    			callSyncService();
    			return true;
            case R.id.menuAbout:
            	mMelding = new AlertDialog.Builder(JobsActivity.this);
            	mMelding.create();
            	mMelding.setTitle(getString(R.string.about_title) +", versie "+ getString(R.string.aboutVersion));
            	mMelding.setMessage(R.string.about_message);
            	mMelding.show();
            	return true;                
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void showFromDb() {
    	Dao<Job, String> JobDao = null;
    	List<Job> Jobs = null;
    	
		try {
			JobDao = getDatabaseHelper().getJobDao();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			QueryBuilder<Job, String> queryBuilder = JobDao.queryBuilder();
			queryBuilder.where().eq("mActive", 1);
			queryBuilder.orderBy("mId", false);
			Jobs = JobDao.query(queryBuilder.prepare());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mJobs = (ArrayList<Job>) Jobs;
		ListView mLvJobs = new ListView(JobsActivity.this);
		setContentView(mLvJobs); 
		JobAdapter jobAdapter = new JobAdapter(this, mJobs);
		mLvJobs.setAdapter(jobAdapter);
		
		mLvJobs.setTextFilterEnabled(true);
		
		mLvJobs.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				Job selectedJob = mJobs.get(position);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(selectedJob.getURL()));
				startActivity(intent);
			}
		});
		
		
		
	}

	private DatabaseHelper getDatabaseHelper() {
		if (mDatabaseHelper == null) {
    		mDatabaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
    	}
    	return mDatabaseHelper;
    }

	@Override
	public void onSyncCallback(int resultCode, Bundle resultData) {
		if(mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		
		switch(resultCode){
			case SyncResultReceiver.RESULT_OK:
				showFromDb();
				break;
			case SyncResultReceiver.RESULT_DOES_NOT_EXIST:
				Toast.makeText(this, "Neem contact op met de ontwikkelaar, de lijst met vacatures kan niet gevonden worden.", Toast.LENGTH_LONG).show();
				break;
			case SyncResultReceiver.RESULT_ERROR:
				Toast.makeText(this, "Gegevens konden niet worden opgehaald. Controleer uw internetverbinding en probeer het opnieuw." , Toast.LENGTH_LONG).show();
				break;
			default:
				Toast.makeText(this, "Er is in verbindingsfout opgetreden met foutcode " + resultCode, Toast.LENGTH_LONG).show();
				break;
		}	
	}
    
}