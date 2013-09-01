package org.draijer.nvkf.helper;

import java.sql.SQLException;

import org.draijer.nvkf.model.Contact;
import org.draijer.nvkf.model.Job;
import org.draijer.nvkf.model.News;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
	static final String DATABASE_NAME = "mvkf.sqlite";
	static final int DATABASE_VERSION = 2;
	Dao<News, String> mNewsDao;
	Dao<Job, String> mJobDao;
	Dao<Contact, String> mContactDao;
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		try {
			TableUtils.createTable(connectionSource, News.class);
			TableUtils.createTable(connectionSource, Job.class);
			TableUtils.createTable(connectionSource, Contact.class);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			TableUtils.dropTable(connectionSource, News.class, true);
			TableUtils.dropTable(connectionSource, Job.class, true);
			TableUtils.dropTable(connectionSource, Contact.class, true);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		onCreate(db, connectionSource);
	}
	
	public Dao<News, String> getNewsDao() throws SQLException {
		if (mNewsDao == null) {
			mNewsDao = getDao(News.class);
		}
		return mNewsDao;
	}
	
	public Dao<Job, String> getJobDao() throws SQLException {
		if (mJobDao == null) {
			mJobDao = getDao(Job.class);
		}
		return mJobDao;
	}
	
	public Dao<Contact, String> getContactDao() throws SQLException {
		if (mContactDao == null) {
			mContactDao = getDao(Contact.class);
		}
		return mContactDao;
	}
	
	@Override
	public void close() {
		super.close();
		mNewsDao = null;
		mJobDao = null;
		mContactDao = null;
	}
}
