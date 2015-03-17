/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.draijer.nvkf.sync.client;

import android.accounts.Account;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.draijer.nvkf.model.Contact;
import org.draijer.nvkf.sync.authenticator.AuthenticatorActivity;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides utility methods for communicating with the server.
 */
public class NetworkUtilities {
    private static final String TAG = "NVKF NetworkUtilities";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_UPDATED = "timestamp";
    public static final String USER_AGENT = "AuthenticationService/1.0";
    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
    public static final String BASE_URL = "http://www.nvkf.nl/";
    public static final String AUTH_URI = BASE_URL + "API.php?action=checkCredentials";
	
    public static final String CONTACTS_BASE_URL = "http://apps.draijer.org/nvkf/";
    public static final String CONTACTS_UPDATES_URI = CONTACTS_BASE_URL + "export.php";

    /**
     * Configures the httpClient to connect to the URL provided.
     */   
	private static DefaultHttpClient createHttpClient() {
		HttpParams my_httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(my_httpParams, REGISTRATION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(my_httpParams, REGISTRATION_TIMEOUT);
		ConnManagerParams.setTimeout(my_httpParams, REGISTRATION_TIMEOUT);
		
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		ThreadSafeClientConnManager multiThreadedConnectionManager = new ThreadSafeClientConnManager(my_httpParams, registry);
		DefaultHttpClient httpclient = new DefaultHttpClient(multiThreadedConnectionManager, my_httpParams);
		return httpclient;
	}

    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *        be executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }
    

    /**
     * Connects to the server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean authenticate(String username, String password, Handler handler, final Context context) {
        final HttpResponse resp;
        DefaultHttpClient httpclient = createHttpClient();			
		HttpGet httpget = new HttpGet(AUTH_URI);
		httpget.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password),"UTF-8", false));

        try {
            resp = httpclient.execute(httpget);
            if(EntityUtils.toString(resp.getEntity()).equals("1")) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Successvolle authenticatie");
                }
                sendResult(true, handler, context);
                return true;
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Fout met authenticatie : " + resp.getStatusLine());
                }
                sendResult(false, handler, context);
                return false;
            }
        } catch (final IOException e) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "IOExceptie tijdens autenticatie", e);
            }
            sendResult(false, handler, context);
            return false;
        } finally {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "getAuthtoken completing");
            }
        }
    }

    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding authentication result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     */
    private static void sendResult(final Boolean result, final Handler handler,
        final Context context) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
                ((AuthenticatorActivity) context).onAuthenticationResult(result);
            }
        });
    }

    /**
     * Attempts to authenticate the user credentials on the server.
     * 
     * @param username The user's username
     * @param password The user's password to be authenticated
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptAuth(final String username,
        final String password, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                authenticate(username, password, handler, context);
            }
        };
        // run on background thread.
        return NetworkUtilities.performOnBackgroundThread(runnable);
    }
    
    
  /**
  * Fetches the list of friend data updates from the server
  * 
  * @param account The account being synced.
  * @param authtoken The authtoken stored in AccountManager for this account
  * @param lastUpdated The last time that sync was performed
  * @return list The list of updates received from the server.
  */
	public static List<Contact> fetchContactUpdates(Account account, String authtoken, Date lastUpdated) throws JSONException,ParseException, IOException, AuthenticationException {
		final ArrayList<Contact> contactList = new ArrayList<Contact>();
		final HttpResponse resp;
		String URL;
		String encodeURL;
						
		if (lastUpdated != null) {
			URL = CONTACTS_UPDATES_URI +"?lastSync="+ (lastUpdated.getTime()/1000);
		} else {
			URL = CONTACTS_UPDATES_URI;
		}
		
		//encodeURL = URLEncoder.encode(URL,"UTF-8");		
		encodeURL = URL;
		
		DefaultHttpClient httpclient = createHttpClient();
		HttpGet httpget = new HttpGet(encodeURL);
		httpget.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(account.name, authtoken),"UTF-8", false));
		
		Log.i(TAG, encodeURL.toString());

		resp = httpclient.execute(httpget);
		final String response = EntityUtils.toString(resp.getEntity());
		
		Log.i(TAG, response.toString());
		
		if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			// Succesfully connected to the NVKF-server and authenticated.
			// Extract member data in JSON format.
			final JSONArray contacts = new JSONArray(response);
			Log.d(TAG, response);
			for (int i = 0; i < contacts.length(); i++) {
				contactList.add(new Contact(contacts.getJSONObject(i)));
			}
		} else {
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				Log.e(TAG, "Authenticatie probleem bij ophalen contacts");
				throw new AuthenticationException();
			} else {
				Log.e(TAG, "Server fout in ophalen van de contacten: "+ resp.getStatusLine());
				throw new IOException();
			}
	    }
		
		return contactList;
	}
}
