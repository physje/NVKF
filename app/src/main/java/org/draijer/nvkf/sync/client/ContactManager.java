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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;

//import com.example.android.samplesync.R;

import java.util.ArrayList;
import java.util.List;

import org.draijer.nvkf.Constants;
import org.draijer.nvkf.model.Contact;

/**
 * Class for managing contacts sync related mOperations
 */
public class ContactManager {
    /**
     * Custom IM protocol used when storing status messages.
     */
    //public static final String CUSTOM_IM_PROTOCOL = "SampleSyncAdapter";
    private static final String TAG = "NVKF ContactManager";

    /**
     * Synchronize raw contacts
     * 
     * @param context The context of Authenticator Activity
     * @param account The username for the account
     * @param users The list of users
     */
	public static synchronized void syncContacts(Context context, String account, List<Contact> contacts) {
		long userId;
        long rawContactId = 0;
        ArrayList<String> WerkterreinArray;
        ArrayList<Integer> IDArray;
        
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        Log.d(TAG, "In SyncContacts");
        for (final Contact contact : contacts) {
        	WerkterreinArray = contact.getContactGroups();
        	userId = contact.getID();
        	
        	// Check to see if the contact-group needs to be inserted or already exist
        	if(WerkterreinArray.size() > 0) {
        		IDArray = lookupGroupIDs(resolver, WerkterreinArray, account);
            	contact.setGroupID(IDArray);
        	}
        	
            // Check to see if the contact needs to be inserted or updated
            rawContactId = lookupRawContact(resolver, userId);
            //Log.d(TAG, userId +" "+ contact.getAchternaam() +" -> "+ rawContactId);
            if (rawContactId != 0) {
                if (!contact.isDeleted()) {
                	// update contact
                	Log.i(TAG, "updateContact : "+ userId);
                	updateContact(context, resolver, account, contact, rawContactId, batchOperation);
                } else {
                    // delete contact
                	Log.d(TAG, "deleteContact : "+ userId);
                    deleteContact(context, rawContactId, batchOperation);
                }
            } else {
                // add new contact
                Log.i(TAG, "addContact : "+ userId);
                if (!contact.isDeleted()) {
                	addContact(context, account, contact, batchOperation);
                }
            }
            // A sync adapter should batch operations on multiple contacts,
            // because it will make a dramatic performance difference.
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        batchOperation.execute();
    }

    /**
     * Adds a single contact to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param accountName the account the contact belongs to
     * @param user the sample SyncAdapter User object
     */
    private static void addContact(Context context, String accountName, Contact contact, BatchOperation batchOperation) {
        // Put the data in the contacts provider
        final ContactOperations contactOp = ContactOperations.createNewContact(context, contact.getID(), accountName, batchOperation);
        contactOp.addName(null, contact.getRoepnaam(), contact.getAchternaam(), contact.getTussenvoegsel());        
        contactOp.addAdditionalNames(contact.getMeisjesNaam(), Nickname.TYPE_MAIDEN_NAME);
        contactOp.addEmail(contact.getWerkEmail(), Email.TYPE_WORK);
        contactOp.addEmail(contact.getPriveEmail(), Email.TYPE_HOME);     
        contactOp.addPhone(contact.getWerkTelefoon(), Phone.TYPE_WORK);
        contactOp.addPhone(contact.getWerkTelefoon_2(), Phone.TYPE_OTHER);
        contactOp.addPhone(contact.getPriveMobiel(), Phone.TYPE_MOBILE);              
        contactOp.addPhone(contact.getPriveTel(), Phone.TYPE_HOME);
        contactOp.addAddress(contact.getPriveAdres(), contact.getPrivePC(), contact.getPrivePlaats(), contact.getPriveLand(), StructuredPostal.TYPE_HOME);
        contactOp.addAddress(contact.getInstituutAdres(), contact.getInstituutPC(), contact.getInstituutPlaats(), null, StructuredPostal.TYPE_WORK);
        contactOp.addEvent(contact.getGeboortedatum(0), Event.TYPE_BIRTHDAY);
        contactOp.addOrganization(contact.getInstituut(), contact.getAfdeling(), Organization.TYPE_WORK);
        
        // Kijk of er uberhaupt een werkterrein is en zo ja, loop dan het hele array af
        // Voor die paar leden die 2 werkterreinen hebben
        if(contact.getGroupID() != null) {
        	ArrayList<Integer> IDs = contact.getGroupID();
        	for (Integer id : IDs) {
        		contactOp.asignGroup(id);
        	}
        }
        contactOp.addPhoto(contact.getPasfoto());
        //contactOp.addProfileAction(contact.getUserId());
    }

    /**
     * Updates a single contact to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param resolver the ContentResolver to use
     * @param accountName the account the contact belongs to
     * @param user the sample SyncAdapter contact object.
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     */
    private static void updateContact(Context context, ContentResolver resolver, String accountName, Contact contact, long rawContactId, BatchOperation batchOperation) {
        Uri uri;        
        String meisjesnaam = null;
        String cellPhone = null;
        String otherPhone = null;
        String workPhone = null;
        String homePhone = null;
        String email = null;
        String priveEmail = null;
        String workStreet = null;
        String workCity = null;
        String workZIP = null;
        String workLand = null;
        String priveStreet = null;
        String priveCity = null;
        String priveLand = null;
        String priveZIP = null;
        String instituut = null;
        String afdeling = null;
        String photoURL = null;
        String birthday = null;
        ArrayList<Integer> IDs = new ArrayList<Integer>();
        IDs = contact.getGroupID();
        int groupCounter = 0;

        final Cursor c = resolver.query(Data.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION, new String[] {String.valueOf(rawContactId)}, null);
        final ContactOperations contactOp = ContactOperations.updateExistingContact(context, rawContactId, batchOperation);

        try {
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);

                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                	final String prefix = c.getString(DataQuery.COLUMN_PREFIX);
                	final String lastName = c.getString(DataQuery.COLUMN_FAMILY_NAME);
                    final String firstName = c.getString(DataQuery.COLUMN_GIVEN_NAME);
                    final String middleName = c.getString(DataQuery.COLUMN_MIDDLE_NAME);
                    
                    contactOp.updateName(prefix, firstName, lastName, middleName, null, contact.getRoepnaam(), contact.getAchternaam(), contact.getTussenvoegsel(), uri);
                } else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    final int type = c.getInt(DataQuery.COLUMN_PHONE_TYPE);

                    if (type == Phone.TYPE_MOBILE) {
                        cellPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updatePhone(contact.getPriveMobiel(), cellPhone, Phone.TYPE_MOBILE, uri);
                    } else if (type == Phone.TYPE_WORK) {
                    	workPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updatePhone(contact.getWerkTelefoon(), workPhone, Phone.TYPE_WORK, uri);
                    } else if (type == Phone.TYPE_OTHER) {
                    	otherPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updatePhone(contact.getWerkTelefoon_2(), otherPhone, Phone.TYPE_OTHER, uri);
                    } else if (type == Phone.TYPE_HOME) {
                        homePhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updatePhone(contact.getPriveTel(), homePhone, Phone.TYPE_HOME, uri);
                    }
                } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                	final int type = c.getInt(DataQuery.COLUMN_EMAIL_TYPE);
                	
                	if (type == Email.TYPE_WORK) {
                		email = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
                		contactOp.updateEmail(email, contact.getWerkEmail(), Email.TYPE_WORK, uri);
                	 } else if (type == Email.TYPE_HOME) {
                		priveEmail = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
                 		contactOp.updateEmail(priveEmail, contact.getPriveEmail(), Email.TYPE_HOME, uri);
                	 }
                } else if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
                	final int type = c.getInt(DataQuery.COLUMN_ADDRESS_TYPE);
                	
                	if (type == StructuredPostal.TYPE_HOME) {
                		priveStreet = c.getString(DataQuery.COLUMN_ADDRESS_STREET);
                		priveZIP = c.getString(DataQuery.COLUMN_ADDRESS_ZIP);
                		priveCity = c.getString(DataQuery.COLUMN_ADDRESS_CITY);
                		priveLand = c.getString(DataQuery.COLUMN_ADDRESS_COUNTRY);
                		
                		contactOp.updateAddress(priveStreet, priveZIP, priveCity, priveLand, contact.getPriveAdres(), contact.getPrivePC(), contact.getPrivePlaats(), contact.getPriveLand(), type, uri);
                	} else if (type == StructuredPostal.TYPE_WORK) {
                		workStreet = c.getString(DataQuery.COLUMN_ADDRESS_STREET);
                		workZIP = c.getString(DataQuery.COLUMN_ADDRESS_ZIP);
                		workCity = c.getString(DataQuery.COLUMN_ADDRESS_CITY);
                		workLand = c.getString(DataQuery.COLUMN_ADDRESS_COUNTRY);
                		
                		contactOp.updateAddress(workStreet, workZIP, workCity, workLand, contact.getInstituutAdres(), contact.getInstituutPC(), contact.getInstituutPlaats(), null, type, uri);
                	}
                } else if (mimeType.equals(Nickname.CONTENT_ITEM_TYPE)) {
                	final int type = c.getInt(DataQuery.COLUMN_NAME_TYPE);
                	
                	if (type == Nickname.TYPE_MAIDEN_NAME) {
                		meisjesnaam = c.getString(DataQuery.COLUMN_NAME_TYPE);
                		contactOp.updateAdditionalNames(meisjesnaam, contact.getMeisjesNaam(), type, uri);
                	}                	
                } else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE)) {
                	photoURL = c.getString(DataQuery.COLUMN_PHOTO_URL);                	
                	contactOp.updatePhoto(photoURL, contact.getPasfoto(), uri);
                } else if (mimeType.equals(Event.CONTENT_ITEM_TYPE)) {
                	final int type = c.getInt(DataQuery.COLUMN_EVENT_TYPE);
                	
                	if (type == Event.TYPE_BIRTHDAY) {
                		birthday = c.getString(DataQuery.COLUMN_EVENT_DATE);
                		contactOp.updateEvent(birthday, contact.getGeboortedatum(0), type, uri);
                	}
                } else if (mimeType.equals(GroupMembership.CONTENT_ITEM_TYPE)) {
                    final int groupID = c.getInt(DataQuery.COLUMN_GROUP_ID);
                    
                    if(IDs != null && IDs.size() > groupCounter) {
                    	contactOp.updateGroup(groupID, IDs.get(groupCounter), uri);
                        groupCounter++;
                    } else {
                    	contactOp.updateGroup(groupID, 0, uri);
                    }
                } else if (mimeType.equals(Organization.CONTENT_ITEM_TYPE)) {
                    final int type = c.getInt(DataQuery.COLUMN_ORGANIZATION_TYPE);
                    
                    if (type == Organization.TYPE_WORK) {
                    	instituut = c.getString(DataQuery.COLUMN_ORGANIZATION_NAME);
                    	afdeling = c.getString(DataQuery.COLUMN_ORGANIZATION_AFDELING);
                    	contactOp.updateOrganization(instituut, afdeling, contact.getInstituut(), contact.getAfdeling(), type, uri);
                    }
                }
            } // while
            
            // Add the email address, if present and not updated above
            if (email == null) {
                contactOp.addEmail(contact.getWerkEmail(), Email.TYPE_WORK);
            }
            
            // Add the email address, if present and not updated above
            if (priveEmail == null) {
                contactOp.addEmail(contact.getPriveEmail(), Email.TYPE_HOME);
            }
            
            // Add the cell phone, if present and not updated above
            if (cellPhone == null) {
                contactOp.addPhone(contact.getPriveMobiel(), Phone.TYPE_MOBILE);
            }

            // Add the work-phone, if present and not updated above
            if (workPhone == null) {
                contactOp.addPhone(contact.getWerkTelefoon(), Phone.TYPE_WORK);
            }
            
            // Add the other phone, if present and not updated above
            if (otherPhone == null) {
                contactOp.addPhone(contact.getWerkTelefoon_2(), Phone.TYPE_OTHER);
            }      
            
            // Add the home phone, if present and not updated above
            if (homePhone == null) {
                contactOp.addPhone(contact.getPriveTel(), Phone.TYPE_HOME);
            }
            
            if(photoURL == null) {
            	contactOp.addPhoto(contact.getPasfoto());
            }
            
            if(priveStreet == null && priveZIP == null && priveCity == null && priveLand == null) {
            	contactOp.addAddress(contact.getPriveAdres(), contact.getPrivePC(), contact.getPrivePlaats(), contact.getPriveLand(), StructuredPostal.TYPE_HOME);
            }
            
            if(workStreet == null && workZIP == null && workCity == null && workLand == null) {
            	contactOp.addAddress(contact.getInstituutAdres(), contact.getInstituutPC(), contact.getInstituutPlaats(), null, StructuredPostal.TYPE_WORK);
            }
            
            if(birthday == null) {
            	contactOp.addEvent(contact.getGeboortedatum(0), Event.TYPE_BIRTHDAY);
            }
            
            if(meisjesnaam == null) {
            	contactOp.addAdditionalNames(contact.getMeisjesNaam(), Nickname.TYPE_MAIDEN_NAME);
            }
            	            
            if(IDs!= null && IDs.size() > groupCounter) {
            	contactOp.asignGroup(IDs.get(groupCounter));
            	Log.d(TAG, "toevoegen werkterrein");
            }
            
        } finally {
            c.close();            
        }        
    }

    /**
     * Deletes a contact from the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     */
    private static void deleteContact(Context context, long rawContactId, BatchOperation batchOperation) {
        batchOperation.add(ContactOperations.newDeleteCpo(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId), true).build());
    }

    /**
     * Returns the RawContact id for a sample SyncAdapter contact, or 0 if the
     * sample SyncAdapter user isn't found.
     * 
     * @param context the Authenticator Activity context
     * @param userId the sample SyncAdapter user ID to lookup
     * @return the RawContact id, or 0 if not found
     */
    private static long lookupRawContact(ContentResolver resolver, long userId) {
        long authorId = 0;
        final Cursor c = resolver.query(RawContacts.CONTENT_URI, UserIdQuery.PROJECTION, UserIdQuery.SELECTION, new String[] {String.valueOf(userId)}, null);
        try {
            if (c.moveToFirst()) {
                authorId = c.getLong(UserIdQuery.COLUMN_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return authorId;
    }
    
    private static ArrayList<Integer> lookupGroupIDs(ContentResolver resolver, ArrayList<String> groupArray, String accountName) {
    	ArrayList<Integer> IDArray = new ArrayList<Integer>();    	
    	for (String groupName : groupArray) {
    		if (getGroupID(resolver, groupName) == 0) {
    			ArrayList<ContentProviderOperation> opsGroup = new ArrayList<ContentProviderOperation>();
    			opsGroup.add(ContentProviderOperation.newInsert(ContactsContract.Groups.CONTENT_URI)
    					.withValue(Groups.TITLE, groupName)
    					.withValue(Groups.GROUP_VISIBLE, true)
    					.withValue(Groups.ACCOUNT_NAME, accountName)
    					.withValue(Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE)
    					.build());

    			try {
    				resolver.applyBatch(ContactsContract.AUTHORITY, opsGroup);
    				Log.d(TAG, "toevoegen groep '"+ groupName +"'");
    			} catch (RemoteException e) {
    				e.printStackTrace();
    			} catch (OperationApplicationException e) {
    				e.printStackTrace();
    			}
    		}
    		
    		IDArray.add(getGroupID(resolver, groupName));
    	}
        
    	return IDArray;
    }
    
    private static int getGroupID(ContentResolver resolver, String groupName) {
    	int groupId = 0;
    	
    	final Cursor c = resolver.query(ContactsContract.Groups.CONTENT_URI, null, ContactsContract.Groups.TITLE + "='" + groupName +"'", null, null);
    			
    	try {
            if (c.moveToFirst()) {
                groupId = c.getInt(c.getColumnIndex(ContactsContract.Groups._ID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    	
    	return groupId;
    }

    /**
     * Returns the Data id for a sample SyncAdapter contact's profile row, or 0
     * if the sample SyncAdapter user isn't found.
     * 
     * @param resolver a content resolver
     * @param userId the sample SyncAdapter user ID to lookup
     * @return the profile Data row id, or 0 if not found
     */
//    private static long lookupProfile(ContentResolver resolver, long userId) {
//        long profileId = 0;
//        final Cursor c =
//            resolver.query(Data.CONTENT_URI, ProfileQuery.PROJECTION,
//                ProfileQuery.SELECTION, new String[] {String.valueOf(userId)},
//                null);
//        try {
//            if (c != null && c.moveToFirst()) {
//                profileId = c.getLong(ProfileQuery.COLUMN_ID);
//            }
//        } finally {
//            if (c != null) {
//                c.close();
//            }
//        }
//        return profileId;
//    }

    /**
     * Constants for a query to find a contact given a sample SyncAdapter user
     * ID.
     */
//    private interface ProfileQuery {
//        public final static String[] PROJECTION = new String[] {Data._ID};
//
//        public final static int COLUMN_ID = 0;
//
//        public static final String SELECTION =
//            Data.MIMETYPE + "='" + SampleSyncAdapterColumns.MIME_PROFILE
//                + "' AND " + SampleSyncAdapterColumns.DATA_PID + "=?";
//    }
    /**
     * Constants for a query to find a contact given a user ID.
     */
    private interface UserIdQuery {
        public final static String[] PROJECTION = new String[] {RawContacts._ID};

        public final static int COLUMN_ID = 0;

        public static final String SELECTION = RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE + "' AND " + RawContacts.SOURCE_ID + "=?";
    }

    
    /**
     * Constants for a query to get contact data for a given rawContactId
     */
    private interface DataQuery {
        public static final String[] PROJECTION = new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA5, Data.DATA6, Data.DATA7, Data.DATA8, Data.DATA9, Data.DATA10,};
    	//public static final String[] PROJECTION = new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3,};

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_MIMETYPE = 1;
        public static final int COLUMN_DATA1 = 2;
        public static final int COLUMN_DATA2 = 3;
        public static final int COLUMN_DATA3 = 4;
        public static final int COLUMN_DATA4 = 5;
        public static final int COLUMN_DATA5 = 6;
        public static final int COLUMN_DATA6 = 7;
        public static final int COLUMN_DATA7 = 8;
        //public static final int COLUMN_DATA8 = 9;
        public static final int COLUMN_DATA9 = 10;
        public static final int COLUMN_DATA10 = 11;
        
        public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
        public static final int COLUMN_PHONE_TYPE = COLUMN_DATA2;
        
        public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
        public static final int COLUMN_EMAIL_TYPE = COLUMN_DATA2;
                
        public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
        public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;
        public static final int COLUMN_PREFIX = COLUMN_DATA4;
        public static final int COLUMN_MIDDLE_NAME = COLUMN_DATA5;
        public static final int COLUMN_NAME_TYPE = COLUMN_DATA2;

        public static final int COLUMN_PHOTO_URL = COLUMN_DATA6;
        
        public static final int COLUMN_EVENT_DATE = COLUMN_DATA1;
        public static final int COLUMN_EVENT_TYPE = COLUMN_DATA2;
        
        public static final int COLUMN_ADDRESS_TYPE = COLUMN_DATA2;
        public static final int COLUMN_ADDRESS_STREET = COLUMN_DATA4;
        public static final int COLUMN_ADDRESS_ZIP = COLUMN_DATA9;
        public static final int COLUMN_ADDRESS_CITY = COLUMN_DATA7;
        public static final int COLUMN_ADDRESS_COUNTRY = COLUMN_DATA10;
        
        public static final int COLUMN_ORGANIZATION_TYPE = COLUMN_DATA2;
        public static final int COLUMN_ORGANIZATION_NAME = COLUMN_DATA1;
        public static final int COLUMN_ORGANIZATION_AFDELING = COLUMN_DATA5;
        
        public static final int COLUMN_GROUP_ID = COLUMN_DATA1;
        
        
        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }
}
