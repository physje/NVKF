package org.draijer.nvkf.model;

import org.json.JSONObject;

import android.os.SystemClock;
import com.j256.ormlite.field.DatabaseField;

public class News {
	@DatabaseField(id=true)
	String mId;
	@DatabaseField
	int mActive;
	@DatabaseField
	String mTitle;
	@DatabaseField
	String mDescription;
	@DatabaseField
	String mURL;
	@DatabaseField
	long mLastUpdate = 0;
		
	public News(JSONObject object){		
		mTitle = object.optString("title");
		mURL = object.optString("url");
		mId = object.optString("id");
		mActive = 1;
		mDescription = object.optString("descr");
		mLastUpdate = SystemClock.elapsedRealtime();
	}
	
	News(){		
	}
		
//	public News(Cursor c){
//		mTitle = c.getString(c.getColumnIndex(DBAdapter.KEY_NEWS_ID));
//		mId = c.getString(c.getColumnIndex(DBAdapter.KEY_NEWS_ID));
//		mDescription = c.getString(c.getColumnIndex(DBAdapter.KEY_NEWS_DESCR));		
//	}
	
	public void setInactive(){
		mActive = 0;
	}
	
	public String getId(){
		return mId;
	}
	
	public String getTitle(){
		return mTitle;
	}

	public String getURL(){
		return mURL;
	}
	
	public String getDescription(){
		return mDescription;
	}
	
	public long getLastNewsUpdate () {
		return mLastUpdate;		
	}
}
