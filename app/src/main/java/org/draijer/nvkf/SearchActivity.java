package org.draijer.nvkf;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.draijer.nvkf.helper.ContactAdapter;
import org.draijer.nvkf.helper.DatabaseHelper;
import org.draijer.nvkf.helper.EditPreferences;
import org.draijer.nvkf.model.Contact;
import org.draijer.nvkf.persistance.SyncCallbackReceiver;
import org.draijer.nvkf.persistance.SyncResultReceiver;
import org.draijer.nvkf.persistance.SyncService;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends Activity implements SyncCallbackReceiver {
    //Button mBtnZoeken;
	Builder mMelding;
	TextView mSearch;
    ProgressDialog mProgressDialog;
	ArrayList<Contact> mContactList;
	ListView mLvContacts;
	DatabaseHelper mDatabaseHelper;
	SharedPreferences settings;
	
	TextView mTitel;
	TextView mVoorletters;
	TextView mVoornaam;
	TextView mTussen;
	TextView mAchternaam;
	TextView mInstituut;
	TextView mAdres;
	TextView mAfdeling;
	TextView mPC;
	TextView mPlaats;
	TextView mwerkMail;
	TextView mwerkTel1;
	TextView mwerkTel2;
	TextView mGeboorte;
	TextView mpriveAdres;
	TextView mprivePC;
	TextView mprivePlaats;
	TextView mpriveTel;
	TextView mpriveMobiel;
	TextView mpriveMail;
	Button mKnopPrev;
	Button mKnopUp;
	Button mKnopNext;
	
	int mShownPerson;
    
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leden);
        
        //mBtnZoeken = (Button) findViewById(R.id.btnZoekLeden);
        mSearch = (TextView) findViewById(R.id.zoektermLedenlijst);
    }
	
	public void onResume() {
        super.onResume();
        setContentView(R.layout.leden);
        
        mSearch = (TextView) findViewById(R.id.zoektermLedenlijst);
    }    
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuInstellingen:
                startActivity(new Intent(this, EditPreferences.class));
                return true;
            case R.id.menuAbout:
            	mMelding = new AlertDialog.Builder(SearchActivity.this);
            	mMelding.create();
            	mMelding.setTitle(getString(R.string.about_title) +", versie "+ getString(R.string.aboutVersion));
            	mMelding.setMessage(R.string.about_message);
            	mMelding.show();
            	return true;                
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void zoekLeden(View view) {
    	String searchString = mSearch.getText().toString().trim();

    	// Even controleren of er een account is
    	AccountManager accountManager = AccountManager.get(this);
    	Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
    	    	
    	if((accounts.length == 0)) {
    		Toast.makeText(this, "Het zoeken in de ledenlijst is beveiligd. Voer eerst uw NVKF-account in. Ga daarvoor naar Settings -> Accounts & Sync -> Add account en kies dan '"+ getString(R.string.label) +"' en vul uw NVKF-gegevens in.", Toast.LENGTH_LONG).show();
    	} else if(searchString.length() < 3) {
    		Toast.makeText(this, "Voer minimaal 3 letters in.", Toast.LENGTH_LONG).show();
    	} else {    	
    		mProgressDialog = new ProgressDialog(this);
    		mProgressDialog.setMessage("Bezig met het zoeken van leden met \""+ searchString +"\"...");
    		mProgressDialog.show();
    	
    		SyncResultReceiver receiver = new SyncResultReceiver();
    		receiver.setSyncCallbackReceiver(this);
    	
    		Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
    		intent.putExtra(SyncService.RECEIVER, receiver);
    		intent.putExtra(SyncService.ACTION, SyncService.ACTION_SEARCH);
    		intent.putExtra("searchParameter", searchString);
			startService(intent);
    	}
    }
    
    public void onClickMail(View view) {    	
    	TextView werkMail = (TextView) findViewById(R.id.txtMail);
    	TextView priveMail = (TextView) findViewById(R.id.txtPriveMail);
    	String mailadres;
    	
    	if(view.equals(werkMail)) {
    		mailadres = werkMail.getText().toString();
    	} else {
    		mailadres = priveMail.getText().toString();
    	}
    	
    	Intent intent = new Intent(Intent.ACTION_SEND); 
    	intent.setType("plain/text");
    	intent.putExtra(Intent.EXTRA_EMAIL, new String[] {mailadres});  
    	startActivity(Intent.createChooser(intent, "Kies applicatie :"));    	
    }
    
    /*
    public void onClickCall(View view) {    	
    	TextView werkPhone_1 = (TextView) findViewById(R.id.txtTel1);
    	TextView werkPhone_2 = (TextView) findViewById(R.id.txtTel2);
    	TextView thuisPhone = (TextView) findViewById(R.id.txtPriveTel);
    	TextView thuisMobiel = (TextView) findViewById(R.id.txtPriveMobiel);
    	String telefoon = null;
    	
    	if(view.equals(werkPhone_1)) {
    		telefoon = werkPhone_1.getText().toString();
    	}
    	if(view.equals(werkPhone_2)) {
    		telefoon = werkPhone_2.getText().toString();
    	}
    	if(view.equals(thuisPhone)) {
    		telefoon = thuisPhone.getText().toString();
    	}
    	if(view.equals(thuisMobiel)) {
    		telefoon = thuisMobiel.getText().toString();
    	}
    	
    	Intent intent = new Intent(Intent.ACTION_DIAL); 
    	intent.setData(Uri.parse("tel:"+ telefoon));  
    	startActivity(intent);
    }*/

	@Override
	public void onSyncCallback(int resultCode, Bundle resultData) {
		mProgressDialog.dismiss();
				
		switch(resultCode){
			case SyncResultReceiver.RESULT_OK:				
				showFromDb();
				break;
			case SyncResultReceiver.RESULT_DOES_NOT_EXIST:
				Toast.makeText(this, "Neem contact op met de ontwikkelaar, de lijst met NVKF-ledengegevens kan niet gevonden worden.", Toast.LENGTH_LONG).show();
				break;
			case SyncResultReceiver.RESULT_ERROR:
				Toast.makeText(this, "Ledengegevens konden niet worden opgehaald. Controleer uw internetverbinding en probeer het opnieuw." , Toast.LENGTH_LONG).show();
				break;
			default:
				Toast.makeText(this, "Er is in verbindingsfout opgetreden met foutcode " + resultCode, Toast.LENGTH_LONG).show();
				break;
		}		
	}
	
	private DatabaseHelper getDatabaseHelper() {
		if (mDatabaseHelper == null) {
    		mDatabaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
    	}
    	return mDatabaseHelper;
    }
	
	private void showFromDb() {
    	Dao<Contact, String> ContactDao = null;
    	List<Contact> ContactList = null;
    	
		try {
			ContactDao = getDatabaseHelper().getContactDao();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			ContactList = ContactDao.queryForEq("mActive", 1);
			//ContactList = ContactDao.queryForAll();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mContactList = (ArrayList<Contact>) ContactList;
				
		mLvContacts = new ListView(SearchActivity.this);
		setContentView(mLvContacts); 

		// Haal instellingen op (met name vanwege het tonen van de foto in de lijst)
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		ContactAdapter contactAdapter = new ContactAdapter(this, mContactList, settings);
		
		mLvContacts.setAdapter(contactAdapter);
		mLvContacts.setTextFilterEnabled(true);

		mLvContacts.setOnItemClickListener(				
			new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
					setContentView(R.layout.leden_detail);					
				
					mTitel = (TextView) findViewById(R.id.txtTitel);
					mVoorletters = (TextView) findViewById(R.id.txtVoorletters);
					mVoornaam = (TextView) findViewById(R.id.txtVoornaam);					
					mTussen = (TextView) findViewById(R.id.txtTussenvoegsel);					
					mAchternaam = (TextView) findViewById(R.id.txtAchternaam);					
					
					mInstituut = (TextView) findViewById(R.id.txtInstituutNaam);
					mAdres = (TextView) findViewById(R.id.txtInstituutAdres);					
					mAfdeling = (TextView) findViewById(R.id.txtInstituutAfdeling);
					mPC = (TextView) findViewById(R.id.txtInstituutPostcode);
					mPlaats = (TextView) findViewById(R.id.txtInstituutPlaats);
					
					mwerkMail = (TextView) findViewById(R.id.txtMail);
					mwerkTel1 = (TextView) findViewById(R.id.txtTel1);
					mwerkTel2 = (TextView) findViewById(R.id.txtTel2);
					
					mGeboorte = (TextView) findViewById(R.id.txtGeboorte);
					mpriveAdres = (TextView) findViewById(R.id.txtPriveAdres);
					mprivePC = (TextView) findViewById(R.id.txtPrivePC);
					mprivePlaats = (TextView) findViewById(R.id.txtPrivePlaats);
					mpriveTel = (TextView) findViewById(R.id.txtPriveTel);
					mpriveMobiel = (TextView) findViewById(R.id.txtPriveMobiel);
					mpriveMail = (TextView) findViewById(R.id.txtPriveMail);
					
					mKnopPrev = (Button) findViewById(R.id.btnPrev);
					mKnopNext = (Button) findViewById(R.id.btnNext);
					mKnopUp = (Button) findViewById(R.id.btnUp);
					
					showDetails(position);
					
					//ImageView mFoto = (ImageView) findViewById(R.id.imgFoto);
					
//					String foto = selectedContact.getPasfoto();
//					if(foto != null && settings.getBoolean("prefFotoLedenlijst", false)) {
//						URL myUrl = null;
//						InputStream inputStream = null;
//												
//						try {
//							myUrl = new URL(foto);
//							// De volgende regel geeft error... ben er nog niet uit hoezo...			
//							inputStream = (InputStream) myUrl.getContent();
//							Drawable drawable = null;
//							drawable = Drawable.createFromStream(inputStream, null);
//							mFoto.setImageDrawable(drawable);
//						} catch (MalformedURLException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//									
//					} else {
//						mFoto.setVisibility(View.INVISIBLE);
//					}
				}
			}
		);
	}
	
	public void showDetails(int position) {
		Contact selectedContact = mContactList.get(position);
		mShownPerson = position;
		
		if(mShownPerson == 0) {
			mKnopPrev.setVisibility(View.INVISIBLE);
		} else {
			mKnopPrev.setVisibility(View.VISIBLE);
		}
		
		if(mShownPerson == (mContactList.size()-1)) {
			mKnopNext.setVisibility(View.INVISIBLE);
		} else {
			mKnopNext.setVisibility(View.VISIBLE);
		}
		
		mTitel.setText(selectedContact.getTitels());
		mVoorletters.setText(selectedContact.getVoorletters());
		mVoornaam.setText(selectedContact.getVoornaam());					
		mTussen.setText(selectedContact.getTussenvoegsel());
		mAchternaam.setText(selectedContact.getAchternaam());					
		
		mInstituut.setText(selectedContact.getInstituut());
		mAdres.setText(selectedContact.getInstituutAdres());
		mAfdeling.setText(selectedContact.getAfdeling());
		mPC.setText(selectedContact.getInstituutPC());
		mPlaats.setText(selectedContact.getInstituutPlaats());
		
		mwerkMail.setText(selectedContact.getWerkEmail());
		mwerkTel1.setText(selectedContact.getWerkTelefoon());
		mwerkTel2.setText(selectedContact.getWerkTelefoon_2());
		
		mGeboorte.setText(selectedContact.getGeboortedatum(1));
		mpriveAdres.setText(selectedContact.getPriveAdres());
		mprivePC.setText(selectedContact.getPrivePC());
		mprivePlaats.setText(selectedContact.getPrivePlaats());
		mpriveTel.setText(selectedContact.getPriveTel());
		mpriveMobiel.setText(selectedContact.getPriveMobiel());
		mpriveMail.setText(selectedContact.getPriveEmail());
		
		if(!selectedContact.isOpenbaar()) {						
			findViewById(R.id.textView15).setVisibility(View.INVISIBLE);
			findViewById(R.id.textView16).setVisibility(View.INVISIBLE);
			findViewById(R.id.textView17).setVisibility(View.INVISIBLE);
			findViewById(R.id.textView18).setVisibility(View.INVISIBLE);
			findViewById(R.id.textView19).setVisibility(View.INVISIBLE);
			findViewById(R.id.textView20).setVisibility(View.INVISIBLE);
			findViewById(R.id.textView21).setVisibility(View.INVISIBLE);
			findViewById(R.id.textView22).setVisibility(View.INVISIBLE);
		} else {
			findViewById(R.id.textView15).setVisibility(View.VISIBLE);
			findViewById(R.id.textView16).setVisibility(View.VISIBLE);
			findViewById(R.id.textView17).setVisibility(View.VISIBLE);
			findViewById(R.id.textView18).setVisibility(View.VISIBLE);
			findViewById(R.id.textView19).setVisibility(View.VISIBLE);
			findViewById(R.id.textView20).setVisibility(View.VISIBLE);
			findViewById(R.id.textView21).setVisibility(View.VISIBLE);
			findViewById(R.id.textView22).setVisibility(View.VISIBLE);
		}
	}
	
	public void showPrev(View view) {
		showDetails(mShownPerson - 1);
	}
	
	public void showNext(View view) {
		showDetails(mShownPerson + 1);
	}
	
	public void showUp(View view) {
		showFromDb();
	}
}