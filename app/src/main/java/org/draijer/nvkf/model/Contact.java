package org.draijer.nvkf.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.json.JSONObject;

import android.util.Log;

import com.j256.ormlite.field.DatabaseField;

public class Contact {
	@DatabaseField(id=true)	int mId;	
	@DatabaseField int mActive;
	@DatabaseField boolean mDeleted;
	@DatabaseField String mOpenbaar;
	@DatabaseField String mTitels;
	@DatabaseField String mVoorletters;
	@DatabaseField String mVoornaam;
	@DatabaseField String mTussenvoegsel;
	@DatabaseField String mAchternaam;
	@DatabaseField String mMeisjesnaam;
	@DatabaseField String mFoto;
	@DatabaseField String mGeboortedatum;
	@DatabaseField String mMail;
	@DatabaseField String mTel_1;
	@DatabaseField String mTel_2;
	@DatabaseField String mFax;
	@DatabaseField String mAfdeling;	
	@DatabaseField String mPriveAdres;
	@DatabaseField String mPrivePC;
	@DatabaseField String mPrivePlaats;
	@DatabaseField String mPriveLand;
	@DatabaseField String mPriveMobiel;
	@DatabaseField String mPriveTelefoon;
	@DatabaseField String mPriveMail;
	@DatabaseField String mInstituut;
	@DatabaseField String mAdres;
	@DatabaseField String mPC;
	@DatabaseField String mPlaats;	
	boolean mAKF;
	boolean mAUD;
	boolean mNG;
	boolean mRAD;
	boolean mRTH;
	boolean mOpleider;
	boolean mKlifio;
	int mSoortLid;	
	ArrayList<Integer> mGroupIDs;
	
	public Contact(JSONObject object){
		try {
			mId				= object.optInt("ID");  
			mOpenbaar		= object.has("Openbaar_") ? object.getString("Openbaar_") : null;
			mTitels			= object.has("Titel") ? object.getString("Titel") : null;
			mVoorletters	= object.has("Voorletters") ? object.getString("Voorletters") : null;
			mVoornaam		= object.has("Voornaam") ? object.getString("Voornaam") : null;
			mTussenvoegsel	= object.has("Tussenvoegsel") ? object.getString("Tussenvoegsel") : null;
			mAchternaam		= object.has("Achternaam") ? object.getString("Achternaam") : null;
			mMeisjesnaam	= object.has("Geboortenaam") ? object.getString("Geboortenaam") : null;
			mFoto			= object.has("thumb") ? object.getString("thumb") : null;
			mMail			= object.has("Mail_zakelijk") ? object.getString("Mail_zakelijk") : null;
			mTel_1			= object.has("Doorkiesnummer1") ? object.getString("Doorkiesnummer1") : null;
			mTel_2			= object.has("Doorkiesnummer2") ? object.getString("Doorkiesnummer2") : null;
			mFax			= object.has("Doorkiesfax") ? object.getString("Doorkiesfax") : null;
			mAfdeling		= object.has("Afdeling") ? object.getString("Afdeling") : null;
			mInstituut		= object.has("inst_Naam") ? object.getString("inst_Naam") : null;
			mAdres			= object.has("inst_Adres") ? object.getString("inst_Adres") : null;
			mPC				= object.has("inst_Postcode") ? object.getString("inst_Postcode") : null;
			mPlaats			= object.has("inst_Plaats") ? object.getString("inst_Plaats") : null;
			mKlifio			= object.has("Klifio") ? true : false;
			mAKF			= object.has("RegistratieWerkterrein1DatumIngang") || object.has("OpleidingWerkterrein1BesluitOp") ? true : false;
			mAUD			= object.has("RegistratieWerkterrein2DatumIngang") || object.has("OpleidingWerkterrein2BesluitOp") ? true : false;
			mNG				= object.has("RegistratieWerkterrein3DatumIngang") || object.has("OpleidingWerkterrein3BesluitOp") ? true : false;
			mRAD			= object.has("RegistratieWerkterrein4DatumIngang") || object.has("OpleidingWerkterrein4BesluitOp") ? true : false;
			mRTH			= object.has("RegistratieWerkterrein5DatumIngang") || object.has("OpleidingWerkterrein5BesluitOp") ? true : false;
			mSoortLid		= object.has("SoortLid") ? object.getInt("SoortLid") : null;
			mOpleider		= object.has("OpleiderWerkterrein1") ? true : false;
			mDeleted		= object.has("deleted") ? true : false;
			
			if(mOpenbaar.equalsIgnoreCase("Ja")) {
				mGeboortedatum	= object.has("Geboortedatum") ? object.getString("Geboortedatum") : null;
				mPriveAdres		= object.has("Priveadres") ? object.getString("Priveadres") : null;
				mPrivePC		= object.has("Privepostcode") ? object.getString("Privepostcode") : null;
				mPrivePlaats	= object.has("Priveplaats") ? object.getString("Priveplaats") : null;
				mPriveLand		= object.has("Priveland") ? object.getString("Priveland") : null;
				mPriveMobiel	= object.has("Mobiel") ? object.getString("Mobiel") : null;
				mPriveTelefoon	= object.has("Telefoonnummer") ? object.getString("Telefoonnummer") : null;
				mPriveMail		= object.has("Mail_prive") ? object.getString("Mail_prive") : null;
			} else {
				mGeboortedatum	= null;
				mPriveAdres		= null;
				mPrivePC		= null;
				mPrivePlaats	= null;
				mPriveLand		= null;
				mPriveMobiel	= null;
				mPriveTelefoon	= null;
				mPriveMail		= null;
			}
	    } catch (final Exception ex) {
	    	Log.i("Contacts", "(" + ex.toString() +") "+ object);
	    }
		mActive = 1;
	}
	
	Contact() {
	}

	public void setInactive() {
		mActive = 0;		
	}
	
	public String getTitels() {
		return mTitels;
	}

	public String getVoorletters() {
		return mVoorletters;
	}

	public String getVoornaam() {
		return mVoornaam;
	}
	
    public String getRoepnaam() {
        if(mVoornaam == null) {
        	return mVoorletters;
        } else {
        	return mVoornaam;
        }
    }

    public String getTussenvoegsel() {
    	return mTussenvoegsel;
    }
    
    public String getAchternaam() {
        return mAchternaam;
    }
	
	public String getVolledigeNaam() {
		String voor = "";
		String achter = "";
		
		if(mTussenvoegsel == null) {
			achter = mAchternaam;	
		} else {
			achter = mTussenvoegsel +" "+ mAchternaam;
		}
		
		if(mVoornaam == null) {
			voor = mVoorletters;	
		} else {
			voor = mVoornaam;
		}
		
		return voor +" "+ achter;		
	}

	public String getInstituut() {
		return mInstituut;
	}

	public String getInstituutPlaats() {
		return mPlaats;
	}

    public String getWerkTelefoon() {
        return mTel_1;
    }

    public String getWerkTelefoon_2() {
        return mTel_2;
    }

    public String getWerkEmail() {
        return mMail;
    }
	
	public String getPasfoto() {
		if(mFoto == null) {
			return null;
		} else {
			mFoto = mFoto.replace("thumb//thumb", "thumb/thumb");
			return "http://www.nvkf.nl/leden_systeem/"+mFoto;
		}
	}	

	public String getAfdeling() {
		return mAfdeling;
	}
	
	public String getGeboortedatum(int outFormat) {		
		if(mGeboortedatum != null) {
			try {
				SimpleDateFormat in = new SimpleDateFormat("yyyy-mm-dd");
				SimpleDateFormat out = new SimpleDateFormat("yyyy-mm-dd");				
				if (outFormat == 1)	out = new SimpleDateFormat("EEE dd-mm-y");							
				return out.format(in.parse(mGeboortedatum));			
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}			
		} else {
			return null;
		}
	}

	public String getPriveAdres() {
		return mPriveAdres;
	}

	public String getPrivePC() {
		return mPrivePC;
	}

	public String getPrivePlaats() {
		return mPrivePlaats;
	}
	
	public String getPriveLand() {
		return mPriveLand;
	}

	public String getPriveTel() {
		return mPriveTelefoon;
	}

	public String getPriveMobiel() {
		return mPriveMobiel;
	}

	public String getPriveEmail() {
		return mPriveMail;
	}

	public boolean isOpenbaar() {
		if(mOpenbaar.equalsIgnoreCase("Ja")) {
			return true;
		} else {
			return false;	
		}
	}

	public int getID() {
		return mId;
	}

	public String getInstituutAdres() {
		return mAdres;
	}

	public String getInstituutPC() {
		return mPC;
	}
	
	public void setGroupID(ArrayList<Integer> groupID) {
		mGroupIDs = groupID;		
	}
	
	public ArrayList<Integer> getGroupID() {
		return mGroupIDs;
	}

	public boolean isDeleted() {
		return mDeleted;
	}

	public ArrayList<String> getContactGroups() {
		ArrayList<String> GroupArray = new ArrayList<String>();
		
		if(mAKF)			GroupArray.add("Algemene Klinische Fysica");						
		if(mNG)				GroupArray.add("Nucleaire Geneeskunde");		
		if(mRAD)			GroupArray.add("Radiologie");		
		if(mRTH)			GroupArray.add("Radiotherapie");	
		if(mAUD)			GroupArray.add("Audiologie");
		if(mKlifio)			GroupArray.add("Klifio");
		if(mOpleider)		GroupArray.add("Opleider");
		if(mSoortLid == 2)	GroupArray.add("Buitengewoon lid");
		if(mSoortLid == 5)	GroupArray.add("Emeritus");		
		return GroupArray;
	}

	public String getMeisjesNaam() {
		return mMeisjesnaam;
	} 
}
