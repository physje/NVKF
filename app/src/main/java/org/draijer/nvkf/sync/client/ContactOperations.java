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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.draijer.nvkf.Constants;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.text.TextUtils;
import android.util.Log;


/**
 * Helper class for storing data in the platform content providers.
 */
public class ContactOperations {

    private final ContentValues mValues;
    private ContentProviderOperation.Builder mBuilder;
    private final BatchOperation mBatchOperation;
    private final Context mContext;
    private boolean mYield;
    private long mRawContactId;
    private int mBackReference;
    private boolean mIsNewContact;

    /**
     * Returns an instance of ContactOperations instance for adding new contact
     * to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param userId the userId of the sample SyncAdapter user object
     * @param accountName the username of the current login
     * @return instance of ContactOperations
     */
    public static ContactOperations createNewContact(Context context, int userId, String accountName, BatchOperation batchOperation) {
        return new ContactOperations(context, userId, accountName, batchOperation);
    }

    /**
     * Returns an instance of ContactOperations for updating existing contact in
     * the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id of the existing rawContact
     * @return instance of ContactOperations
     */
    public static ContactOperations updateExistingContact(Context context,
        long rawContactId, BatchOperation batchOperation) {
        return new ContactOperations(context, rawContactId, batchOperation);
    }

    public ContactOperations(Context context, BatchOperation batchOperation) {
        mValues = new ContentValues();
        mYield = true;
        mContext = context;
        mBatchOperation = batchOperation;
    }

    public ContactOperations(Context context, int userId, String accountName,
        BatchOperation batchOperation) {
        this(context, batchOperation);
        mBackReference = mBatchOperation.size();
        mIsNewContact = true;
        mValues.put(RawContacts.SOURCE_ID, userId);
        mValues.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        mValues.put(RawContacts.ACCOUNT_NAME, accountName);
        mBuilder = newInsertCpo(RawContacts.CONTENT_URI, true).withValues(mValues);
        mBatchOperation.add(mBuilder.build());
    }

    public ContactOperations(Context context, long rawContactId,
        BatchOperation batchOperation) {
        this(context, batchOperation);
        mIsNewContact = false;
        mRawContactId = rawContactId;
    }
    
    public byte[] photoURLtoByte(String photoURL) {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();    	
    	
    	try {
    		URL url = new URL(photoURL);	    		
    		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    		connection.setDoInput(true);
    	    connection.connect();
    	    InputStream input = connection.getInputStream();
    	    Bitmap photoBitmap = BitmapFactory.decodeStream(input);	        	  
        	photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
    	    //photoBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        	return baos.toByteArray();
   	    } catch (IOException e) {
   	    	e.printStackTrace();
    	}
    	
    	return null;    	   	
    }

    /**
     * Adds a contact name
     * 
     * @param name Name of contact
     * @param nameType type of name: family name, given name, etc.
     * @return instance of ContactOperations
     */
    public ContactOperations addName(String prefix, String firstName, String lastName, String middleName) {
    	mValues.clear();
        if (!TextUtils.isEmpty(prefix)) {
            mValues.put(StructuredName.PREFIX, prefix);
        }
    	
    	if (!TextUtils.isEmpty(firstName)) {
            mValues.put(StructuredName.GIVEN_NAME, firstName);
        }
        
        if (!TextUtils.isEmpty(middleName)) {
            mValues.put(StructuredName.MIDDLE_NAME, middleName);
        }
        
        if (!TextUtils.isEmpty(lastName)) {
            mValues.put(StructuredName.FAMILY_NAME, lastName);            
        }
        if (mValues.size() > 0) {
        	mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            addInsertOp();
            Log.i("ContactOperations", "toevoegen "+ lastName +", "+ firstName);
        }
        return this;
    }
    
    /**
     * Adds a contact name
     * 
     * @param name Address
     * @param nameType type of address: home, work, etc.
     * @return instance of ContactOperations
     */
    public ContactOperations addAddress(String street, String zip, String city, String land, int type) {
    	mValues.clear();
        if (!TextUtils.isEmpty(street)) {
            mValues.put(StructuredPostal.STREET, street);
        }
        
        if (!TextUtils.isEmpty(zip)) {
            mValues.put(StructuredPostal.POSTCODE, zip);
        }
        
        if (!TextUtils.isEmpty(city)) {
            mValues.put(StructuredPostal.CITY, city);            
        }
        
        if (!TextUtils.isEmpty(land)) {
            mValues.put(StructuredPostal.COUNTRY, land);            
        }
        
        if (mValues.size() > 0) {
        	mValues.put(StructuredPostal.TYPE, type);
        	mValues.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
        	
            addInsertOp();
            Log.i("ContactOperations", "toevoegen adres (" + type + ") "+ street +", "+ zip +", "+ city +", "+ land);
        }
        return this;
    }
    
    
    /**
     * Adds a contact photo
     * 
     * @param photoURL URL of contact-photo
     * @return instance of ContactOperations
     */
    public ContactOperations addPhoto(String photoURL) {
    	mValues.clear();
    	
    	if(photoURL != null) {  	
	    	byte[] b = photoURLtoByte(photoURL);
            mValues.put(Photo.PHOTO, b);
    		mValues.put(Photo.DATA6, photoURL);
            mValues.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
    	}    	    	

        if (mValues.size() > 0) {
            addInsertOp();
            Log.i("ContactOperations", "toevoegen foto (" + photoURL + ")");  
        }
        return this;
    }

    
    /**
     * Adds an email
     * 
     * @param new email for user
     * @return instance of ContactOperations
     */
    public ContactOperations addEmail(String email, int mailType) {
    	mValues.clear();
        if (!TextUtils.isEmpty(email)) {
            mValues.put(Email.DATA, email);
            mValues.put(Email.TYPE, mailType);
            mValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            addInsertOp();
            Log.i("ContactOperations", "toevoegen email (" + mailType + ") "+ email);            
        }
        return this;
    }

    /**
     * Adds a phone number
     * 
     * @param phone new phone number for the contact
     * @param phoneType the type: cell, home, etc.
     * @return instance of ContactOperations
     */
    public ContactOperations addPhone(String phone, int phoneType) {
        mValues.clear();
        if (!TextUtils.isEmpty(phone)) {
            mValues.put(Phone.NUMBER, phone);
            mValues.put(Phone.TYPE, phoneType);
            mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            addInsertOp();
            Log.i("ContactOperations", "toevoegen telefoon (" + phoneType + ") "+ phone);        	
        }
        return this;
    }
     
	public ContactOperations addEvent(String event, int typeEvent) {
		mValues.clear();
        if (event != null) {
        	mValues.put(Event.START_DATE, event);
            mValues.put(Event.TYPE, typeEvent);
            mValues.put(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE);
            addInsertOp();
            Log.i("ContactOperations", "toevoegen event (" + typeEvent + ") "+ event);        	
        }
        return this;
	}
	
	public ContactOperations addGroup(String groupTitle) {
		mValues.clear();
		if (!TextUtils.isEmpty(groupTitle)) {
            mValues.put(Groups.TITLE, groupTitle);
            mValues.put(Groups.GROUP_VISIBLE, true);    		
            addInsertOp();
            Log.i("ContactOperations", "toevoegen groep "+ groupTitle);
        }
        return this;
	}
	
	public ContactOperations addOrganization(String instituut, String afdeling, int type) {
		mValues.clear();
		if (!TextUtils.isEmpty(instituut)) {
            mValues.put(Organization.COMPANY, instituut);
		}
		
		if (!TextUtils.isEmpty(afdeling)) {
            mValues.put(Organization.DEPARTMENT, afdeling);
		}
		
		if (mValues.size() > 0) {
            mValues.put(Organization.TYPE, type);
            mValues.put(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
            addInsertOp();
            Log.i("ContactOperations", "toevoegen instituut "+ instituut);
        }
        return this;		
	}
		
	public ContactOperations asignGroup(int groupID) {
		mValues.clear();
        if (groupID != 0) {
        	mValues.put(GroupMembership.GROUP_ROW_ID, groupID);
        	mValues.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
            addInsertOp();
            Log.i("ContactOperations", "toevoegen aan groep "+ groupID);        	
        }
        return this;
	}
    
	public ContactOperations addAdditionalNames(String naam, int type) {
		mValues.clear();
		if (!TextUtils.isEmpty(naam)) {
        	mValues.put(Nickname.NAME, naam);
            mValues.put(Nickname.TYPE, type);
            mValues.put(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
            addInsertOp();
            Log.i("ContactOperations", "toevoegen event (" + type + ") "+ naam);
        }
        return this;
	}

    /**
     * Adds a profile action
     * 
     * @param userId the userId of the sample SyncAdapter user object
     * @return instance of ContactOperations
     */
/*    public ContactOperations addProfileAction(long userId) {
        mValues.clear();
        if (userId != 0) {
            mValues.put(SampleSyncAdapterColumns.DATA_PID, userId);
            mValues.put(SampleSyncAdapterColumns.DATA_SUMMARY, mContext.getString(R.string.profile_action));
            mValues.put(SampleSyncAdapterColumns.DATA_DETAIL, mContext.getString(R.string.view_profile));
            mValues.put(Data.MIMETYPE, SampleSyncAdapterColumns.MIME_PROFILE);
            addInsertOp();
        }
        return this;
    }*/

    /**
     * Updates contact's email
     * 
     * @param email email id of the sample SyncAdapter user
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateEmail(String existingEmail, String email, int mailType, Uri uri) {
    	if (!TextUtils.equals(existingEmail, email)) {
            mValues.clear();
            mValues.put(Email.DATA, email);
            mValues.put(Email.TYPE, mailType);
            addUpdateOp(uri);
            Log.i("ContactOperations", "wijzigen email (" + mailType + ") "+ email);
        }
        return this;
    }

    /**
     * Updates contact's name
     * 
     * @param name Name of contact
     * @param existingName Name of contact stored in provider
     * @param nameType type of name: family name, given name, etc.
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateName(String existingPrefix, String existingFirstName, String existingLastName, String existingMiddleName, String prefix, String firstName, String lastName, String middleName, Uri uri) {
        mValues.clear();
        if (!TextUtils.equals(existingPrefix, prefix)) {
            mValues.put(StructuredName.PREFIX, prefix);
            Log.i("ContactOperations", "titels : |" + existingPrefix +"|" + prefix +"|");
        }
        
        if (!TextUtils.equals(existingFirstName, firstName)) {
            mValues.put(StructuredName.GIVEN_NAME, firstName);
            Log.i("ContactOperations", "voornaam : |" + existingFirstName +"|" + firstName +"|");
        }

        if (!TextUtils.equals(existingMiddleName, middleName)) {
            mValues.put(StructuredName.MIDDLE_NAME, middleName);
            Log.i("ContactOperations", "tussen : |" + existingMiddleName +"|" + middleName +"|");
        }
        
        if (!TextUtils.equals(existingLastName, lastName)) {
            mValues.put(StructuredName.FAMILY_NAME, lastName);
            Log.i("ContactOperations", "achternaam : |" + existingLastName +"|" + lastName +"|");
        }
        
        if (mValues.size() > 0) {
            addUpdateOp(uri);
            Log.i("ContactOperations", "wijzigen naam "+ firstName +";"+ lastName +";"+ middleName);
        }
        return this;
    }
        
    public ContactOperations updatePhoto(String existingURL, String photoURL, Uri uri) {
    	mValues.clear();
        if (!TextUtils.equals(existingURL, photoURL)) {
        	if(photoURL != null) {  	
        		byte[] b = photoURLtoByte(photoURL);
        		mValues.put(Photo.PHOTO, b);
        		mValues.put(Photo.DATA6, photoURL);
        		
        		addUpdateOp(uri);
        		Log.i("ContactOperations", "wijzigen foto; "+ photoURL);
        	}
    	}
    	
    	return this;
    }
        
    public ContactOperations updateAddress(String existingStreet, String existingZIP, String existingCity, String existingLand, String street, String ZIP, String city, String land, int type, Uri uri) {
        mValues.clear();
        if (!TextUtils.equals(existingStreet, street)) {
            mValues.put(StructuredPostal.STREET, street);
            Log.i("ContactOperations", "straat : |" + existingStreet +"|" + street +"|");
        }
        if (!TextUtils.equals(existingZIP, ZIP)) {
            mValues.put(StructuredPostal.POSTCODE, ZIP);
            Log.i("ContactOperations", "PC : |" + existingZIP +"|" + ZIP +"|");
        }
        
        if (!TextUtils.equals(existingCity, city)) {
            mValues.put(StructuredPostal.CITY, city);
            Log.i("ContactOperations", "Plaats : |" + existingCity +"|" + city +"|");
        }
        
        if (!TextUtils.equals(existingLand, land)) {
            mValues.put(StructuredPostal.COUNTRY, land);
            Log.i("ContactOperations", "Land : |" + existingLand +"|" + land +"|");
        }
        
        if (mValues.size() > 0) {
        	mValues.put(StructuredPostal.TYPE, type);
        	//mValues.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
        	
            addUpdateOp(uri);
            Log.i("ContactOperations", "wijzigen adres (" + type + ") "+ street +";"+ ZIP +";"+ city +";"+ land);
        }
        return this;
    }

    /**
     * Updates contact's phone
     * 
     * @param existingNumber phone number stored in contacts provider
     * @param phone new phone number for the contact
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updatePhone(String existingNumber, String phone, int phoneType, Uri uri) {
        if (!TextUtils.equals(phone, existingNumber)) {
            mValues.clear();
            mValues.put(Phone.NUMBER, phone);
            mValues.put(Phone.TYPE, phoneType);
            addUpdateOp(uri);
            Log.i("ContactOperations", "wijzigen telefoon (" + phoneType + ") "+ phone);
        }
        return this;
    }
    
	public ContactOperations updateEvent(String existingEvent, String event, int type, Uri uri) {
		if (!TextUtils.equals(event, existingEvent)) {
			mValues.clear();
            mValues.put(Event.START_DATE, event);
            mValues.put(Event.TYPE, type);
            addUpdateOp(uri);
            Log.i("ContactOperations", "wijzigen event (" + type + ") "+ event);
		}
		
		return this;
	}
	
	public ContactOperations updateOrganization(String existingInstituut, String existingAfdeling, String instituut, String afdeling, int type, Uri uri) {
		mValues.clear();
		if (!TextUtils.equals(existingInstituut, instituut)) {
            mValues.put(Organization.COMPANY, instituut);
		}
		
		if (!TextUtils.equals(afdeling, existingAfdeling)) {
            mValues.put(Organization.DEPARTMENT, afdeling);
		}
		
		if (mValues.size() > 0) {
            mValues.put(Organization.TYPE, type);
            mValues.put(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
            addUpdateOp(uri);
            Log.i("ContactOperations", "wijzigen instituut "+ instituut);
        }
        return this;		
	}
	
	public ContactOperations updateGroup(int existingGroupID, int groupID, Uri uri) {
		if(groupID == 0) {
			mValues.clear();
			mValues.put(GroupMembership.GROUP_ROW_ID, "");
			addUpdateOp(uri);
			Log.i("ContactOperations", "verwijderen uit groep");
		} else if (groupID != existingGroupID) {
			mValues.clear();
        	mValues.put(GroupMembership.GROUP_ROW_ID, groupID);
            addUpdateOp(uri);
            Log.i("ContactOperations", "verplaatsen naar groep "+ groupID);
		}		
		
		return this;
	}
 
	public ContactOperations updateAdditionalNames(String existingName, String name, int type, Uri uri) {
		if (!TextUtils.equals(name, existingName)) {
			mValues.clear();
            mValues.put(Nickname.NAME, name);
            mValues.put(Nickname.TYPE, type);
            addUpdateOp(uri);
            Log.i("ContactOperations", "wijzigen naam (" + type + ") "+ name);
		}
		return this;
	}
	
    /**
     * Updates contact's profile action
     * 
     * @param userId sample SyncAdapter user id
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
//    public ContactOperations updateProfileAction(Integer userId, Uri uri) {
//        mValues.clear();
//        mValues.put(SampleSyncAdapterColumns.DATA_PID, userId);
//        addUpdateOp(uri);
//        return this;
//    }

    /**
     * Adds an insert operation into the batch
     */
    private void addInsertOp() {
        if (!mIsNewContact) {
            mValues.put(Phone.RAW_CONTACT_ID, mRawContactId);
        }
        mBuilder = newInsertCpo(addCallerIsSyncAdapterParameter(Data.CONTENT_URI),mYield);
        mBuilder.withValues(mValues);
        if (mIsNewContact) {
            mBuilder.withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
        }
        mYield = false;
        mBatchOperation.add(mBuilder.build());
    }

    /**
     * Adds an update operation into the batch
     */
    private void addUpdateOp(Uri uri) {
        mBuilder = newUpdateCpo(uri, mYield).withValues(mValues);
        mYield = false;
        mBatchOperation.add(mBuilder.build());
    }

    public static ContentProviderOperation.Builder newInsertCpo(Uri uri, boolean yield) {
        return ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
    }

    public static ContentProviderOperation.Builder newUpdateCpo(Uri uri, boolean yield) {
        return ContentProviderOperation.newUpdate(addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
    }

    public static ContentProviderOperation.Builder newDeleteCpo(Uri uri,
        boolean yield) {
        return ContentProviderOperation.newDelete(
            addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);

    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
            ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }
}
