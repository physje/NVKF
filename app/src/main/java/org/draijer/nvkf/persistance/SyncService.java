package org.draijer.nvkf.persistance;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.draijer.nvkf.Constants;
import org.draijer.nvkf.NVKFTabWidget;
import org.draijer.nvkf.R;
import org.draijer.nvkf.helper.DatabaseHelper;
import org.draijer.nvkf.model.Contact;
import org.draijer.nvkf.model.Job;
import org.draijer.nvkf.model.News;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class SyncService extends IntentService {
	public static final String RECEIVER = "resultreceiver";
	public static final String ACTION = "action";
	public static final String PARAMETER = "parameter";
	
	public static final String BASE_URL = "http://www.nvkf.nl/API.php";
	
	public static final int ACTION_NEWS = 1;
	public static final int ACTION_JOBS = 2;
	public static final int ACTION_SEARCH = 3;
	
	private DatabaseHelper mDatabaseHelper;
	DefaultHttpClient mHttpClient;
	SharedPreferences settings;
	
	public SyncService() {
		super("SyncService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		if (mHttpClient == null){
			mHttpClient = createHttpClient();
		}
		
		// Lees de benodigde info in om de opdracht uit te kunnen voeren
		int action = intent.getIntExtra(ACTION, 0);
		String searchParameter = intent.getStringExtra("searchParameter");
						
		// Ken de voorkeuren toe aan de variabele settings
		settings = PreferenceManager.getDefaultSharedPreferences(this);
    	
		// Voer de opdracht uit
		int result = 0;
		try {
			result = download(action, searchParameter);			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResultReceiver receiver = intent.getParcelableExtra(RECEIVER);
		receiver.send(result, null);
	}

	private int download(int action, String searchParameter) throws SQLException {
		// Haal de accountgevens op uit de AccountManager
		AccountManager accountManager = AccountManager.get(this);
    	Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
    	
    	// Initialiseer username & password
    	String userName = " ";
    	String password = " ";
    	
    	if(accounts.length > 0) {
        	userName = accounts[0].name;
        	password = accountManager.getPassword(accounts[0]);
    	}
		
		int mStatusCode = 0;
		String mResultString;
		String fetchUrl = "";
		
		if(action == ACTION_NEWS) {
			fetchUrl = BASE_URL + "?action=news";
		} else if (action == ACTION_JOBS) {
			fetchUrl = BASE_URL + "?action=jobs";
		} else if (action == ACTION_SEARCH) {
			String query = "";
			try {
				query = URLEncoder.encode(searchParameter,"UTF-8");
				fetchUrl = BASE_URL + "?action=member&searchString="+ query;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}			
		}
				
		DefaultHttpClient httpclient = createHttpClient();		
		HttpGet httpget = new HttpGet(fetchUrl);
		httpget.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(userName, password),"UTF-8", false));
		
		HttpResponse response = null;
		try {
			response = httpclient.execute(httpget);
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (NullPointerException e1) {
			e1.printStackTrace();
		}
				
		try {
			StatusLine statusLine = response.getStatusLine();
			mStatusCode = statusLine.getStatusCode();
					
			if (mStatusCode == SyncResultReceiver.RESULT_OK){
				//mResultString = EntityUtils.toString(response.getEntity());
				HttpEntity entity = response.getEntity();
				mResultString = EntityUtils.toString(entity, HTTP.UTF_8);
				
				Log.d("reciever", mResultString);
				if(action == ACTION_NEWS) {
					saveNews(mResultString);
				} else if (action == ACTION_JOBS) {
					saveJob(mResultString);
				} else if (action == ACTION_SEARCH) {
					saveContact(mResultString);
				}
			}
			return mStatusCode;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return SyncResultReceiver.RESULT_OK;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return SyncResultReceiver.RESULT_OK;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return SyncResultReceiver.RESULT_OK;
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return SyncResultReceiver.RESULT_ERROR;
		}
	}
	
	private void saveContact(String resultString) throws SQLException {
		// Maak verbinding met de dB	
		Dao<Contact, String> ContactDao = getDatabaseHelper().getContactDao();
				
		// Alles opvragen en op inactief zetten	
		List<Contact> ContactList = ContactDao.queryForAll();
		
		for (int counter = 0; counter < ContactList.size(); counter++) {
			Contact tempContact = ContactList.get(counter);
			tempContact.setInactive();
			ContactDao.update(tempContact);
		}
		
		// Nu de nieuwe string verwerken		
		try {
			JSONArray jSONArray = new JSONArray(resultString);
			for (int counter = 0; counter < jSONArray.length(); counter++) {
				JSONObject jSONObject = jSONArray.getJSONObject(counter);
				Contact contact = new Contact(jSONObject);
				ContactDao.createOrUpdate(contact);			
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void saveJob(String resultString) throws SQLException {		
		boolean notificationActive = false;
		
		// Maak verbinding met de dB	
		Dao<Job, String> JobDao = getDatabaseHelper().getJobDao();
		
		// Alles opvragen en op inactief zetten	
		List<Job> Vacancies = JobDao.queryForAll();
		
		for (int counter = 0; counter < Vacancies.size(); counter++) {
			Job tempJob = Vacancies.get(counter);
			tempJob.setInactive();
			JobDao.update(tempJob);
		}
		
		// Nu de nieuwe string verwerken
		try {
			JSONArray jSONArray = new JSONArray(resultString);			
			for (int counter = 0; counter < jSONArray.length(); counter++) {
				JSONObject jSONObject = jSONArray.getJSONObject(counter);
				Job vacature = new Job(jSONObject);
								
				Job dbJob = JobDao.queryForId(vacature.getId());				
				if(dbJob == null && !notificationActive) {
					showCustomNotification(R.string.tabNews, "Nieuwe vacature", vacature.getTitle(), "NVKF : "+ vacature.getTitle());
					notificationActive = true;
				}
				
				JobDao.createOrUpdate(vacature);				
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	private void saveNews(String resultString) throws SQLException {
		// Alles opvragen en op inactief zetten		
		Dao<News, String> NewsDao = getDatabaseHelper().getNewsDao();
		List<News> NewsItems = NewsDao.queryForAll();
		boolean notificationActive = false; 
		
		for (int counter = 0; counter < NewsItems.size(); counter++) {
			News tempNews = NewsItems.get(counter);
			tempNews.setInactive();
			NewsDao.update(tempNews);
		}
		
		// Nu de nieuwe string verwerken
		try {			
			JSONArray jSONArray = new JSONArray(resultString);
			for (int counter = 0; counter < jSONArray.length(); counter++) {				
				News dbNews;
				JSONObject jSONObject = jSONArray.getJSONObject(counter);				
				News news = new News(jSONObject);
								
				// Kijk of een id al bestaat
				dbNews = NewsDao.queryForId(news.getId());				
				if(dbNews == null && !notificationActive) {
					showCustomNotification(R.string.tabJobs, "Nieuw nieuwsbericht", news.getTitle(), "NVKF : "+ news.getTitle());
					notificationActive = true;
				}	
				
				NewsDao.createOrUpdate(news);				
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	private boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		
		if (ni == null) {
			// There are no active networks.
			return false;
		} else {
			return true;
		}
	}
	
	private DefaultHttpClient createHttpClient() {
		HttpParams my_httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(my_httpParams, 3000);
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		ThreadSafeClientConnManager multiThreadedConnectionManager = new ThreadSafeClientConnManager(my_httpParams, registry);
		DefaultHttpClient httpclient = new DefaultHttpClient(multiThreadedConnectionManager, my_httpParams);
		return httpclient;
	}
	
	private void showCustomNotification(int id, CharSequence contentTitle, CharSequence contentText, CharSequence tickerText) {
		// Initialiseer de NotificationManager
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        				
		// Initialiseer de notificatie
		int icon = R.drawable.notification_icon;
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		
		// uitzetten na aanraken
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		// elke keer melding bij nieuw bericht
		notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

		// trillen bij alarm
		//notification.defaults |= Notification.DEFAULT_VIBRATE;
		long[] vibrate = {0,100,200,300};
		notification.vibrate = vibrate;
		
		// knipperende led bij alarm
		notification.ledARGB = 0xff00ff00;
		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		
		// geluid bij alarm
		//notification.defaults |= Notification.DEFAULT_SOUND;
		notification.sound = Uri.parse(settings.getString("prefRingtone", ""));
		
		Context context = getApplicationContext();

		// initialiseer waar we naar toe terugmoeten			
		Intent notificationIntent = new Intent(this, NVKFTabWidget.class);
		notificationIntent.putExtra("id", id);
		PendingIntent contentIntent = PendingIntent.getActivity(this, id, notificationIntent, 0);
		
		// Het feitelijke bericht
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(id, notification);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDatabaseHelper != null) {
			OpenHelperManager.releaseHelper();
			mDatabaseHelper = null;
		}
	}

	private DatabaseHelper getDatabaseHelper() {
		if (mDatabaseHelper == null) {
			mDatabaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
		}
		return mDatabaseHelper;
	}
}
