package org.draijer.nvkf.helper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;
import org.draijer.nvkf.model.Contact;
import org.draijer.nvkf.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactAdapter extends BaseAdapter {
	ArrayList<Contact> mContacts;
	LayoutInflater mInflater;
	SharedPreferences mSettings;
	
	public ContactAdapter(Context context, ArrayList<Contact> contacts, SharedPreferences settings){
		mContacts = new ArrayList<Contact>();
		mContacts.addAll(contacts);
		notifyDataSetChanged();
        mInflater = LayoutInflater.from(context);
        
        mSettings = settings;
	}
	
	@Override
	public int getCount() {
		return mContacts.size();
	}

	@Override
	public Contact getItem(int position) {
		return mContacts.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {		
		if(convertView == null) {
			convertView = mInflater.inflate(R.layout.leden_list, null);			
		}
		
		Contact contact = getItem(position);
				
		TextView mNaam = (TextView) convertView.findViewById(R.id.tvNaam);
		TextView mZiekenhuis = (TextView) convertView.findViewById(R.id.tvZiekenhuis);
		TextView mPlaats = (TextView) convertView.findViewById(R.id.tvPlaats);
		ImageView mFoto = (ImageView) convertView.findViewById(R.id.imPasfoto);
		
		mNaam.setText(contact.getVolledigeNaam());
		mZiekenhuis.setText(contact.getInstituut());
		mPlaats.setText(contact.getInstituutPlaats());
		
		//String foto = contact.getPasfoto();
		//String filename = contact.getID()+".jpg";
		//if(foto != null) {
		//	DownloadFromUrl(foto, filename);
		//}
		//mFoto.setImageURI(filename);
		//mFoto.setVisibility(View.INVISIBLE);
		
		String foto = contact.getPasfoto();
		if(foto != null && mSettings.getBoolean("prefFotoLedenlijst", false)) {
			URL myUrl = null;
			InputStream inputStream = null;
			
			try {
				myUrl = new URL(foto);
				// De volgende regel geeft error... ben er nog niet uit hoezo...			
				inputStream = (InputStream) myUrl.getContent();
				Drawable drawable = null;
				drawable = Drawable.createFromStream(inputStream, null);
				mFoto.setImageDrawable(drawable);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
						
		} else {
			mFoto.setVisibility(View.INVISIBLE);
		}
		
		return convertView;
	}
	
	//this is the downloader method
	public void DownloadFromUrl(String imageURL, String fileName) {
		final String PATH = "/data/data/org.draijer.nvkf/";
		
		try {
			URL url = new URL(imageURL); //you can write here any link
			File file = new File(PATH+fileName);
		 
			long startTime = System.currentTimeMillis();
			Log.d("ImageManager", "download begining");
			Log.d("ImageManager", "download url:" + url);
			Log.d("ImageManager", "downloaded file name:" + fileName);
		
			/* Open a connection to that URL. */
			URLConnection ucon = url.openConnection();
		 
			/*
			 * Define InputStreams to read from the URLConnection.
			 */
			BufferedInputStream bis = null;
			try {
				InputStream is = ucon.getInputStream();
				bis = new BufferedInputStream(is);
			} catch (Exception e) {				
				e.printStackTrace();
				Log.d("ImageManager", "Error: " + e);
			}			
			
			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1).
			 */
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			try {
				int current = 0;
				while ((current = bis.read()) != -1) {
				        baf.append((byte) current);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d("ImageManager", "Error: " + e);
			}
			
			/* Convert the Bytes read to a String. */
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(baf.toByteArray());
			fos.close();
			Log.d("ImageManager", "download ready in "+ ((System.currentTimeMillis() - startTime) / 1000) + " sec");
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("ImageManager", "Error: " + e);
		}
	}
	
}
