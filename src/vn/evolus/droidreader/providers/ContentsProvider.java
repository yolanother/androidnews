package vn.evolus.droidreader.providers;

import java.util.HashMap;

import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Channel.Channels;
import vn.evolus.droidreader.model.Image.Images;
import vn.evolus.droidreader.model.Item.Items;
import vn.evolus.droidreader.model.Job.Jobs;
import vn.evolus.droidreader.model.Tag.Tags;
import vn.evolus.droidreader.model.Tag.TagsOfItems;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class ContentsProvider extends ContentProvider {	
	public static final String AUTHORITY = "vn.evolus.droidreader.contents";
	public static final String DATABASE_NAME = "droidnews.db";
    private static final int DATABASE_VERSION = 3;
    
	public static final String CHANNELS_TABLE_NAME = "channels";
	public static final String ITEMS_TABLE_NAME = "items";	
	public static final String IMAGES_TABLE_NAME = "images";
	public static final String JOBS_TABLE_NAME = "jobs";
	public static final String TAGS_TABLE_NAME = "tags";
	public static final String TAGS_OF_ITEMS_TABLE_NAME = "tagOfItems";
	public static final String ITEMS_BY_TAGS_VIEW_NAME = "itemsByTags";
	public static final String CHANNELS_BY_TAGS_VIEW_NAME = "channelsByTags";
				
	private static final UriMatcher uriMatcher;
    private static final int CHANNELS = 1;
    private static final int ITEMS = 2;
    private static final int IMAGES = 3;
    private static final int ITEMS_UNREAD_COUNT_OF_EACH_CHANNEL = 4;
    private static final int ITEMS_LIMIT = 5;
    private static final int ITEMS_LIMIT_OFFSET = 6;    
    private static final int ITEMS_UNREAD_COUNT_ALL_CHANNELS = 7;
    private static final int IMAGES_LIMIT = 8;        
    private static final int JOBS = 9;
    private static final int JOBS_LIMIT = 10;    
    private static final int TAGS = 11;
    private static final int TAGS_OF_ITEMS = 12;
    private static final int CHANNELS_BY_TAGS = 13;
    private static final int ITEMS_BY_TAGS = 14;
    private static final int COUNT_UNREAD_ITEMS_BY_TAGS = 15;
    
    public static final String WHERE_ID = "ID=?";
    
    private DatabaseHelper dbHelper;

    private static HashMap<String, String> channelsProjectionMap;
    private static HashMap<String, String> itemsProjectionMap;
    private static HashMap<String, String> imagesProjectionMap;
    private static HashMap<String, String> jobsProjectionMap;
    private static HashMap<String, String> tagsProjectionMap;
    private static HashMap<String, String> tagsOfItemsProjectionMap;
		
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
	        case CHANNELS:
	            count = db.delete(CHANNELS_TABLE_NAME, where, whereArgs);
	            break;
            case ITEMS:
                count = db.delete(ITEMS_TABLE_NAME, where, whereArgs);
                break;
            case IMAGES:
                count = db.delete(IMAGES_TABLE_NAME, where, whereArgs);
                break;
            case JOBS:
                count = db.delete(JOBS_TABLE_NAME, where, whereArgs);
                break;
            case TAGS:
                count = db.delete(TAGS_TABLE_NAME, where, whereArgs);
                break;
            case TAGS_OF_ITEMS:
                count = db.delete(TAGS_OF_ITEMS_TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
	        case CHANNELS:
	            return Channels.CONTENT_TYPE;
            case ITEMS:
                return Items.CONTENT_TYPE;
            case IMAGES:
                return Images.CONTENT_TYPE;
            case JOBS:
                return Jobs.CONTENT_TYPE;
            case TAGS:
                return Tags.CONTENT_TYPE;
            case TAGS_OF_ITEMS:
                return TagsOfItems.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
    	int matchedUri = uriMatcher.match(uri);
        if (matchedUri != CHANNELS 
    		&& matchedUri != ITEMS 
    		&& matchedUri != IMAGES
    		&& matchedUri != JOBS
    		&& matchedUri != TAGS
    		&& matchedUri != TAGS_OF_ITEMS) { 
        	throw new IllegalArgumentException("Unknown URI " + uri); 
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = -1;
        Uri contentUri = null;
        switch (matchedUri) {
			case CHANNELS:
				rowId = db.insert(CHANNELS_TABLE_NAME, Channels.DESCRIPTION, values);
				contentUri = Channels.CONTENT_URI;
				break;
			case ITEMS:
				rowId = db.insert(ITEMS_TABLE_NAME, Items.DESCRIPTION, values);
				contentUri = Channels.CONTENT_URI;
				break;
			case IMAGES:
				rowId = db.insert(IMAGES_TABLE_NAME, Images.URL, values);
				contentUri = Images.CONTENT_URI;
				break;
			case JOBS:
				rowId = db.insert(JOBS_TABLE_NAME, Jobs.PARAMS, values);
				contentUri = Jobs.CONTENT_URI;
				break;
			case TAGS:
				rowId = db.insert(TAGS_TABLE_NAME, Tags.SORT_ID, values);
				contentUri = Tags.CONTENT_URI;
				break;
			case TAGS_OF_ITEMS:
				rowId = db.insert(TAGS_OF_ITEMS_TABLE_NAME, TagsOfItems.ITEM_TYPE, values);
				contentUri = Tags.CONTENT_URI;
				break;
			default:
				break;
		}        
        
        if (rowId > 0 && contentUri != null) {
            contentUri = ContentUris.withAppendedId(contentUri, rowId);
            getContext().getContentResolver().notifyChange(contentUri, null);
            return contentUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String group = null, having = null, limit = null;        
        switch (uriMatcher.match(uri)) {
	        case CHANNELS:
	            qb.setTables(CHANNELS_TABLE_NAME);
	            qb.setProjectionMap(channelsProjectionMap);
	            break;
            case ITEMS:
                qb.setTables(ITEMS_TABLE_NAME);
                qb.setProjectionMap(itemsProjectionMap);
                break;
            case ITEMS_LIMIT:
                qb.setTables(ITEMS_TABLE_NAME);
                qb.setProjectionMap(itemsProjectionMap);
                limit = uri.getLastPathSegment();                
                break;
            case ITEMS_LIMIT_OFFSET:
                qb.setTables(ITEMS_TABLE_NAME);
                qb.setProjectionMap(itemsProjectionMap);                
                limit = uri.getLastPathSegment() + ", " + uri.getPathSegments().get(1);                
                break;
            case ITEMS_UNREAD_COUNT_OF_EACH_CHANNEL:
                qb.setTables(ITEMS_TABLE_NAME);
                qb.setProjectionMap(itemsProjectionMap);
                group = Items.CHANNEL_ID;
                break;
            case ITEMS_UNREAD_COUNT_ALL_CHANNELS:
                qb.setTables(ITEMS_TABLE_NAME);
                qb.setProjectionMap(itemsProjectionMap);                
                break;            
            case IMAGES:
                qb.setTables(IMAGES_TABLE_NAME);
                qb.setProjectionMap(imagesProjectionMap);
                limit = "50";
                break;
            case IMAGES_LIMIT:
                qb.setTables(IMAGES_TABLE_NAME);
                qb.setProjectionMap(imagesProjectionMap);
                limit = uri.getLastPathSegment();                
                break;
            case JOBS:
                qb.setTables(JOBS_TABLE_NAME);
                qb.setProjectionMap(jobsProjectionMap);
                break;
            case JOBS_LIMIT:
                qb.setTables(JOBS_TABLE_NAME);
                qb.setProjectionMap(jobsProjectionMap);
                limit = uri.getLastPathSegment();                
                break;
            case TAGS:
                qb.setTables(TAGS_TABLE_NAME);
                qb.setProjectionMap(tagsProjectionMap);
                break;
            case TAGS_OF_ITEMS:
                qb.setTables(TAGS_OF_ITEMS_TABLE_NAME);
                qb.setProjectionMap(tagsOfItemsProjectionMap);                
                break;
            case CHANNELS_BY_TAGS:
                qb.setTables(CHANNELS_BY_TAGS_VIEW_NAME);
                qb.setProjectionMap(channelsProjectionMap);
                limit = uri.getLastPathSegment();
                break;
            case ITEMS_BY_TAGS:
                qb.setTables(ITEMS_BY_TAGS_VIEW_NAME);
                qb.setProjectionMap(itemsProjectionMap);
                limit = uri.getLastPathSegment();
                break;
            case COUNT_UNREAD_ITEMS_BY_TAGS:
                qb.setTables(ITEMS_BY_TAGS_VIEW_NAME);
                qb.setProjectionMap(itemsProjectionMap);                
                group = Items.TAG_ID;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, group, having, sortOrder, limit);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
	        case CHANNELS:
	            count = db.update(CHANNELS_TABLE_NAME, values, where, whereArgs);
	            break;
            case ITEMS:
                count = db.update(ITEMS_TABLE_NAME, values, where, whereArgs);
                break;
            case IMAGES:
                count = db.update(IMAGES_TABLE_NAME, values, where, whereArgs);
                break;
            case JOBS:
                count = db.update(JOBS_TABLE_NAME, values, where, whereArgs);
                break;
            case TAGS:
                count = db.update(TAGS_TABLE_NAME, values, where, whereArgs);
                break;
            case TAGS_OF_ITEMS:
                count = db.update(TAGS_OF_ITEMS_TABLE_NAME, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }	
		
	private class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }        

        @Override
        public void onCreate(SQLiteDatabase db) {        	
        	db.execSQL("CREATE TABLE " + CHANNELS_TABLE_NAME + " (" + Channels.ID
	                    + " INTEGER PRIMARY KEY AUTOINCREMENT," 
	                    + Channels.TITLE + " VARCHAR(255),"
	                    + Channels.URL + " VARCHAR(1000),"
	                    + Channels.DESCRIPTION + " VARCHAR(1000),"
	                    + Channels.LINK + " VARCHAR(1000),"        			                    
	                    + Channels.IMAGE_URL + " VARCHAR(1000),"
	                    + Channels.OPTIONS + " INTEGER"
	                    + ");");        	
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + CHANNELS_TABLE_NAME
                    + "UrlIndex ON " +  CHANNELS_TABLE_NAME +" (" + Channels.URL + ");");
        	
        	db.execSQL("CREATE TABLE " + ITEMS_TABLE_NAME + " (" + Items.ID
	                    + " INTEGER PRIMARY KEY AUTOINCREMENT," 
	                    + Items.TITLE + " VARCHAR(255)," 
	                    + Items.DESCRIPTION + " LONGTEXT,"
	                    + Items.PUB_DATE + " INTEGER,"
	                    + Items.LINK + " VARCHAR(1000),"
	                    + Items.IMAGE_URL + " VARCHAR(1000),"
	                    + Items.READ + " INTEGER,"
	                    + Items.STARRRED + " INTEGER,"
	                    + Items.KEPT + " INTEGER,"
	                    + Items.ORIGINAL_ID + " VARCHAR(50),"
	                    + Items.CHANNEL_ID + " INTEGER,"
	                    + Items.UPDATE_TIME + " INTEGER"
	                    + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + ITEMS_TABLE_NAME
                    + "LinkIndex ON " +  ITEMS_TABLE_NAME +" (" + Items.LINK + ");");        	
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + ITEMS_TABLE_NAME
                    + "ChannelIdIndex ON " +  ITEMS_TABLE_NAME +" (" + Items.CHANNEL_ID + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + ITEMS_TABLE_NAME
                    + "ReadIndex ON " +  ITEMS_TABLE_NAME +" (" + Items.READ + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + ITEMS_TABLE_NAME
                    + "PubDateIndex ON " +  ITEMS_TABLE_NAME +" (" + Items.PUB_DATE + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + ITEMS_TABLE_NAME
                    + "UpdateTimeIndex ON " +  ITEMS_TABLE_NAME +" (" + Items.UPDATE_TIME + ");");
        	
        	db.execSQL("CREATE TABLE " + IMAGES_TABLE_NAME + " (" + Images.ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT," 
                    + Images.URL + " VARCHAR(1000),"
                    + Images.RETRIES + " INTEGER,"
                    + Images.UPDATE_TIME + " INTEGER,"
                    + Images.STATUS + " INTEGER"
                    + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + IMAGES_TABLE_NAME
                    + "UrlIndex ON " +  IMAGES_TABLE_NAME +" (" + Images.URL + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + IMAGES_TABLE_NAME
                    + "UpdateTimeIndex ON " +  IMAGES_TABLE_NAME +" (" + Images.UPDATE_TIME + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + IMAGES_TABLE_NAME
                    + "StatusIndex ON " +  IMAGES_TABLE_NAME +" (" + Images.STATUS + ");");
        	
        	db.execSQL("CREATE TABLE " + TAGS_TABLE_NAME + " (" + Tags.ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Tags.TYPE + " INTEGER,"
                    + Tags.NAME + " VARCHAR(200),"
                    + Tags.SORT_ID + " VARCHAR(200)"
                    + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_TABLE_NAME
                    + "TypeIndex ON " +  TAGS_TABLE_NAME +" (" + Tags.TYPE + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_TABLE_NAME
                    + "NameIndex ON " +  TAGS_TABLE_NAME +" (" + Tags.NAME + ");");
        	
        	db.execSQL("CREATE TABLE " + TAGS_OF_ITEMS_TABLE_NAME + " (" + TagsOfItems.ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"                    
                    + TagsOfItems.ITEM_ID + " INTEGER,"
                    + TagsOfItems.ITEM_TYPE + " INTEGER,"
                    + TagsOfItems.TAG_ID + " INTEGER"
                    + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_OF_ITEMS_TABLE_NAME
                    + "ItemIdIndex ON " + TAGS_OF_ITEMS_TABLE_NAME +" (" + TagsOfItems.ITEM_ID + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_OF_ITEMS_TABLE_NAME
                    + "ItemTypeIndex ON " + TAGS_OF_ITEMS_TABLE_NAME +" (" + TagsOfItems.ITEM_TYPE + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_OF_ITEMS_TABLE_NAME
                    + "TagIdIndex ON " + TAGS_OF_ITEMS_TABLE_NAME +" (" + TagsOfItems.TAG_ID + ");");
        	
        	db.execSQL("CREATE VIEW " + ITEMS_BY_TAGS_VIEW_NAME + " AS SELECT " 
        			+ ITEMS_TABLE_NAME + "." + Items.ID + " AS " + Items.ID + ","
                    + ITEMS_TABLE_NAME + "." + Items.TITLE + " AS " + Items.TITLE + ", " 
                    + ITEMS_TABLE_NAME + "." + Items.DESCRIPTION + " AS " + Items.DESCRIPTION + ", "
                    + ITEMS_TABLE_NAME + "." + Items.PUB_DATE + " AS " + Items.PUB_DATE + ", "
                    + ITEMS_TABLE_NAME + "." + Items.LINK + " AS " + Items.LINK + ", "
                    + ITEMS_TABLE_NAME + "." + Items.IMAGE_URL + " AS " + Items.IMAGE_URL + ", "
                    + ITEMS_TABLE_NAME + "." + Items.READ + " AS " + Items.READ + ", "
                    + ITEMS_TABLE_NAME + "." + Items.STARRRED + " AS " + Items.STARRRED + ", "
                    + ITEMS_TABLE_NAME + "." + Items.KEPT + " AS " + Items.KEPT + ", "
                    + ITEMS_TABLE_NAME + "." + Items.ORIGINAL_ID + " AS " + Items.ORIGINAL_ID + ","
                    + ITEMS_TABLE_NAME + "." + Items.CHANNEL_ID + " AS " + Items.CHANNEL_ID + ", "
                    + ITEMS_TABLE_NAME + "." + Items.UPDATE_TIME + " AS " + Items.UPDATE_TIME + ", "
                    + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.TAG_ID + " AS " + TagsOfItems.TAG_ID
                    + " FROM "  + ITEMS_TABLE_NAME 
                    + " INNER JOIN " + TAGS_OF_ITEMS_TABLE_NAME + " ON "
                    + ITEMS_TABLE_NAME + "." + Items.ID + " = " + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.ITEM_ID
                    + " AND " + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.ITEM_TYPE + "=" + ContentManager.ITEM_TYPE_ITEM);
        	
        	db.execSQL("CREATE VIEW " + CHANNELS_BY_TAGS_VIEW_NAME + " AS SELECT " 
        			+ CHANNELS_TABLE_NAME + "." + Channels.ID + " AS " + Channels.ID + ", "
        			+ CHANNELS_TABLE_NAME + "." + Channels.TITLE + " AS " + Channels.TITLE + ", "
        			+ CHANNELS_TABLE_NAME + "." + Channels.URL + " AS " + Channels.URL + ", "
        			+ CHANNELS_TABLE_NAME + "." + Channels.DESCRIPTION + " AS " + Channels.DESCRIPTION + ", "
        			+ CHANNELS_TABLE_NAME + "." + Channels.LINK + " AS " + Channels.LINK + ", "
        			+ CHANNELS_TABLE_NAME + "." + Channels.IMAGE_URL + " AS " + Channels.IMAGE_URL + ", "
                    + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.TAG_ID + " AS " + TagsOfItems.TAG_ID
                    + " FROM "  + CHANNELS_TABLE_NAME 
                    + " INNER JOIN " + TAGS_OF_ITEMS_TABLE_NAME + " ON "
                    + CHANNELS_TABLE_NAME + "." + Channels.ID + " = " + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.ITEM_ID
                    + " AND " + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.ITEM_TYPE + "=" + ContentManager.ITEM_TYPE_CHANNEL);
        	
        	db.execSQL("CREATE TABLE " + JOBS_TABLE_NAME + " (" + Jobs.ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"                     
                    + Jobs.TYPE + " VARCHAR(50),"
                    + Jobs.PARAMS + " VARCHAR(1000)"
                    + ");");
        	
        	db.execSQL("CREATE TRIGGER delete_channel_tags_trigger "
        			+  " AFTER DELETE ON " + CHANNELS_TABLE_NAME
        			+  " FOR EACH ROW BEGIN "
        			+  " 	DELETE FROM " + TAGS_OF_ITEMS_TABLE_NAME 
        			+  " 	WHERE " + TagsOfItems.ITEM_TYPE + "=" + ContentManager.ITEM_TYPE_CHANNEL
        			+  " 		AND " + TagsOfItems.ITEM_ID + "=old." + Channels.ID + ";"
        			+  " END;");            	
        	db.execSQL("CREATE TRIGGER delete_item_tags_trigger "
        			+  " AFTER DELETE ON " + ITEMS_TABLE_NAME
        			+  " FOR EACH ROW BEGIN "
        			+  " 	DELETE FROM " + TAGS_OF_ITEMS_TABLE_NAME 
        			+  " 	WHERE " + TagsOfItems.ITEM_TYPE + "=" + ContentManager.ITEM_TYPE_ITEM
        			+  " 		AND " + TagsOfItems.ITEM_ID + "=old." + Items.ID + ";"
        			+  " END;");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	if (oldVersion == 1 && newVersion == 2) {
        		db.execSQL("ALTER TABLE " + ITEMS_TABLE_NAME + " ADD COLUMN " + Items.ORIGINAL_ID + " VARCHAR(50)");
        		db.execSQL("ALTER TABLE " + ITEMS_TABLE_NAME + " ADD COLUMN " + Items.STARRRED + " INTEGER");        		
        		db.execSQL("UPDATE " + ITEMS_TABLE_NAME + " SET " + Items.STARRRED + "=0");        		
        		db.execSQL("ALTER TABLE " + ITEMS_TABLE_NAME + " ADD COLUMN " + Items.KEPT + " INTEGER");
        		db.execSQL("UPDATE " + ITEMS_TABLE_NAME + " SET " + Items.KEPT + "=0");
        		
        		db.execSQL("ALTER TABLE " + IMAGES_TABLE_NAME + " ADD COLUMN " + Images.UPDATE_TIME + " INTEGER");        		
        		db.execSQL("CREATE INDEX IF NOT EXISTS " + IMAGES_TABLE_NAME
                        + "UpdateTimeIndex ON " +  IMAGES_TABLE_NAME +" (" + Images.UPDATE_TIME + ");");
        		db.execSQL("UPDATE " + IMAGES_TABLE_NAME + " SET " + Items.UPDATE_TIME + "=0");
        		
        		db.execSQL("CREATE TABLE " + TAGS_TABLE_NAME + " (" + Tags.ID
                        + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + Tags.TYPE + " INTEGER,"
                        + Tags.NAME + " VARCHAR(200),"
                        + Tags.SORT_ID + " VARCHAR(200)"
                        + ");");
        		db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_TABLE_NAME
                        + "TypeIndex ON " +  TAGS_TABLE_NAME +" (" + Tags.TYPE + ");");
            	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_TABLE_NAME
                        + "NameIndex ON " +  TAGS_TABLE_NAME +" (" + Tags.NAME + ");");
            	
            	db.execSQL("CREATE TABLE " + TAGS_OF_ITEMS_TABLE_NAME + " (" + TagsOfItems.ID
                        + " INTEGER PRIMARY KEY AUTOINCREMENT,"                    
                        + TagsOfItems.ITEM_ID + " INTEGER,"
                        + TagsOfItems.ITEM_TYPE + " INTEGER,"
                        + TagsOfItems.TAG_ID + " INTEGER"
                        + ");");
            	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_OF_ITEMS_TABLE_NAME
                        + "ItemIdIndex ON " + TAGS_OF_ITEMS_TABLE_NAME +" (" + TagsOfItems.ITEM_ID + ");");
            	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_OF_ITEMS_TABLE_NAME
                        + "ItemTypeIndex ON " + TAGS_OF_ITEMS_TABLE_NAME +" (" + TagsOfItems.ITEM_TYPE + ");");
            	db.execSQL("CREATE INDEX IF NOT EXISTS " + TAGS_OF_ITEMS_TABLE_NAME
                        + "TagIdIndex ON " + TAGS_OF_ITEMS_TABLE_NAME +" (" + TagsOfItems.TAG_ID + ");");
            	
            	db.execSQL("CREATE VIEW " + ITEMS_BY_TAGS_VIEW_NAME + " AS SELECT " 
            			+ ITEMS_TABLE_NAME + "." + Items.ID + " AS " + Items.ID + ","
                        + ITEMS_TABLE_NAME + "." + Items.TITLE + " AS " + Items.TITLE + "," 
                        + ITEMS_TABLE_NAME + "." + Items.DESCRIPTION + " AS " + Items.DESCRIPTION + ","
                        + ITEMS_TABLE_NAME + "." + Items.PUB_DATE + " AS " + Items.PUB_DATE + ","
                        + ITEMS_TABLE_NAME + "." + Items.LINK + " AS " + Items.LINK + ","
                        + ITEMS_TABLE_NAME + "." + Items.IMAGE_URL + " AS " + Items.IMAGE_URL + ","
                        + ITEMS_TABLE_NAME + "." + Items.READ + " AS " + Items.READ + ","
                        + ITEMS_TABLE_NAME + "." + Items.STARRRED + " AS " + Items.STARRRED + ","
                        + ITEMS_TABLE_NAME + "." + Items.KEPT + " AS " + Items.KEPT + ","
                        + ITEMS_TABLE_NAME + "." + Items.ORIGINAL_ID + " AS " + Items.ORIGINAL_ID + ","
                        + ITEMS_TABLE_NAME + "." + Items.CHANNEL_ID + " AS " + Items.CHANNEL_ID + ","
                        + ITEMS_TABLE_NAME + "." + Items.UPDATE_TIME + " AS " + Items.UPDATE_TIME + ","
                        + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.TAG_ID + " AS " + TagsOfItems.TAG_ID
                        + " FROM "  + ITEMS_TABLE_NAME 
                        + " INNER JOIN " + TAGS_OF_ITEMS_TABLE_NAME + " ON "
                        + ITEMS_TABLE_NAME + "." + Items.ID + " = " + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.ITEM_ID
                        + " AND " + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.ITEM_TYPE + "=" + ContentManager.ITEM_TYPE_ITEM);
            	
            	db.execSQL("CREATE VIEW " + CHANNELS_BY_TAGS_VIEW_NAME + " AS SELECT " 
            			+ CHANNELS_TABLE_NAME + "." + Channels.ID + " AS " + Channels.ID + ","
            			+ CHANNELS_TABLE_NAME + "." + Channels.TITLE + " AS " + Channels.TITLE + ","
            			+ CHANNELS_TABLE_NAME + "." + Channels.URL + " AS " + Channels.URL + ", "
            			+ CHANNELS_TABLE_NAME + "." + Channels.DESCRIPTION + " AS " + Channels.DESCRIPTION + ","
            			+ CHANNELS_TABLE_NAME + "." + Channels.LINK + " AS " + Channels.LINK + ","
            			+ CHANNELS_TABLE_NAME + "." + Channels.IMAGE_URL + " AS " + Channels.IMAGE_URL + ","
                        + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.TAG_ID + " AS " + TagsOfItems.TAG_ID
                        + " FROM "  + CHANNELS_TABLE_NAME 
                        + " INNER JOIN " + TAGS_OF_ITEMS_TABLE_NAME + " ON "
                        + CHANNELS_TABLE_NAME + "." + Channels.ID + " = " + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.ITEM_ID
                        + " AND " + TAGS_OF_ITEMS_TABLE_NAME + "." + TagsOfItems.ITEM_TYPE + "=" + ContentManager.ITEM_TYPE_CHANNEL);
            	
            	db.execSQL("CREATE TABLE " + JOBS_TABLE_NAME + " (" + Jobs.ID
                        + " INTEGER PRIMARY KEY AUTOINCREMENT,"                     
                        + Jobs.TYPE + " VARCHAR(50),"
                        + Jobs.PARAMS + " VARCHAR(1000)"                    
                        + ");");
            	
            	db.execSQL("CREATE TRIGGER delete_channel_tags_trigger "
            			+  " AFTER DELETE ON " + CHANNELS_TABLE_NAME
            			+  " FOR EACH ROW BEGIN "
            			+  " 	DELETE FROM " + TAGS_OF_ITEMS_TABLE_NAME 
            			+  " 	WHERE " + TagsOfItems.ITEM_TYPE + "=" + ContentManager.ITEM_TYPE_CHANNEL
            			+  " 		AND " + TagsOfItems.ITEM_ID + "=old." + Channels.ID + ";"
            			+  " END;");            	
            	db.execSQL("CREATE TRIGGER delete_item_tags_trigger "
            			+  " AFTER DELETE ON " + ITEMS_TABLE_NAME
            			+  " FOR EACH ROW BEGIN "
            			+  " 	DELETE FROM " + TAGS_OF_ITEMS_TABLE_NAME 
            			+  " 	WHERE " + TagsOfItems.ITEM_TYPE + "=" + ContentManager.ITEM_TYPE_ITEM
            			+  " 		AND " + TagsOfItems.ITEM_ID + "=old." + Items.ID + ";"
            			+  " END;");           	
        	}
        	
        	if (oldVersion == 2 && newVersion == 3) {
        		db.execSQL("ALTER TABLE " + CHANNELS_TABLE_NAME + " ADD COLUMN " + Channels.OPTIONS + " INTEGER");
        		db.execSQL("UPDATE " + CHANNELS_TABLE_NAME + " SET " + Channels.OPTIONS + "=0");
        	}
        }

        @Override
        public void onOpen(SQLiteDatabase db) {           
        }
    }
	
	static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, CHANNELS_TABLE_NAME, CHANNELS);
        uriMatcher.addURI(AUTHORITY, CHANNELS_TABLE_NAME + "/tag/#", CHANNELS_BY_TAGS);
        
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME, ITEMS);        
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME + "/#", ITEMS_LIMIT);
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME + "/#/#", ITEMS_LIMIT_OFFSET);
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME + "/unread", ITEMS_UNREAD_COUNT_OF_EACH_CHANNEL);
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME + "/unread/all", ITEMS_UNREAD_COUNT_ALL_CHANNELS);
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME + "/tag/#", ITEMS_BY_TAGS);
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME + "/tag/unread", COUNT_UNREAD_ITEMS_BY_TAGS);
        
        uriMatcher.addURI(AUTHORITY, TAGS_TABLE_NAME, TAGS);
        uriMatcher.addURI(AUTHORITY, TAGS_OF_ITEMS_TABLE_NAME, TAGS_OF_ITEMS);
        
        uriMatcher.addURI(AUTHORITY, IMAGES_TABLE_NAME, IMAGES);
        uriMatcher.addURI(AUTHORITY, IMAGES_TABLE_NAME + "/#", IMAGES_LIMIT);
        
        uriMatcher.addURI(AUTHORITY, JOBS_TABLE_NAME, JOBS);
        uriMatcher.addURI(AUTHORITY, JOBS_TABLE_NAME + "/#", JOBS_LIMIT);

        channelsProjectionMap = new HashMap<String, String>();
        channelsProjectionMap.put(Channels.ID, Channels.ID);
        channelsProjectionMap.put(Channels.TITLE, Channels.TITLE);
        channelsProjectionMap.put(Channels.URL, Channels.URL);
        channelsProjectionMap.put(Channels.DESCRIPTION, Channels.DESCRIPTION);        
        channelsProjectionMap.put(Channels.LINK, Channels.LINK);
        channelsProjectionMap.put(Channels.IMAGE_URL, Channels.IMAGE_URL);
        channelsProjectionMap.put(Channels.OPTIONS, Channels.OPTIONS);
        channelsProjectionMap.put(Channels.UNREAD,
        		"(SELECT COUNT(*) FROM " + ITEMS_TABLE_NAME
        		+ " WHERE " + ITEMS_TABLE_NAME + ".CHANNEL_ID = "
        		+ CHANNELS_TABLE_NAME + ".ID AND " + ITEMS_TABLE_NAME + ".READ = 0) AS UNREAD");
        channelsProjectionMap.put(Channels.LATEST_ITEM_IMAGE_URL,
        		"(SELECT " + ITEMS_TABLE_NAME + ".IMAGE_URL FROM " + ITEMS_TABLE_NAME
        		+ " WHERE " + ITEMS_TABLE_NAME + ".CHANNEL_ID = "
        		+ CHANNELS_TABLE_NAME + ".ID AND " + ITEMS_TABLE_NAME 
        		+ ".IMAGE_URL IS NOT NULL ORDER BY " + ITEMS_TABLE_NAME + ".PUB_DATE DESC LIMIT 1) AS LATEST_ITEM_IMAGE_URL");
        channelsProjectionMap.put(Channels.TAG_ID, Channels.TAG_ID);
        
        itemsProjectionMap = new HashMap<String, String>();
        itemsProjectionMap.put(Items.ID, Items.ID);
        itemsProjectionMap.put(Items.TITLE, Items.TITLE);
        itemsProjectionMap.put(Items.DESCRIPTION, Items.DESCRIPTION);
        itemsProjectionMap.put(Items.PUB_DATE, Items.PUB_DATE);
        itemsProjectionMap.put(Items.LINK, Items.LINK);
        itemsProjectionMap.put(Items.IMAGE_URL, Items.IMAGE_URL);
        itemsProjectionMap.put(Items.READ, Items.READ);
        itemsProjectionMap.put(Items.STARRRED, Items.STARRRED);
        itemsProjectionMap.put(Items.KEPT, Items.KEPT);
        itemsProjectionMap.put(Items.CHANNEL_ID, Items.CHANNEL_ID);
        itemsProjectionMap.put(Items.UPDATE_TIME, Items.UPDATE_TIME);
        itemsProjectionMap.put(Items.ORIGINAL_ID, Items.ORIGINAL_ID);
        itemsProjectionMap.put(Items.UNREAD_COUNT, "COUNT(*) AS UNREAD");
        itemsProjectionMap.put(Items.COUNT, "COUNT(*)");
        itemsProjectionMap.put(Items.TAG_ID, Items.TAG_ID);
        
        imagesProjectionMap = new HashMap<String, String>();
        imagesProjectionMap.put(Images.ID, Images.ID);
        imagesProjectionMap.put(Images.URL, Images.URL);
        imagesProjectionMap.put(Images.STATUS, Images.STATUS);
        imagesProjectionMap.put(Images.UPDATE_TIME, Images.UPDATE_TIME);
        imagesProjectionMap.put(Images.RETRIES, Images.RETRIES);
        imagesProjectionMap.put(Images.COUNT, Images.COUNT);
        
        jobsProjectionMap = new HashMap<String, String>();
        jobsProjectionMap.put(Jobs.ID, Jobs.ID);
        jobsProjectionMap.put(Jobs.TYPE, Jobs.TYPE);
        jobsProjectionMap.put(Jobs.PARAMS, Jobs.PARAMS);
        
        tagsProjectionMap = new HashMap<String, String>();
        tagsProjectionMap.put(Tags.ID, Tags.ID);
        tagsProjectionMap.put(Tags.TYPE, Tags.TYPE);
        tagsProjectionMap.put(Tags.NAME, Tags.NAME);
        tagsProjectionMap.put(Tags.SORT_ID, Tags.SORT_ID);
        
        tagsOfItemsProjectionMap = new HashMap<String, String>();
        tagsOfItemsProjectionMap.put(TagsOfItems.ID, Tags.ID);
        tagsOfItemsProjectionMap.put(TagsOfItems.ITEM_ID, TagsOfItems.ITEM_ID);
        tagsOfItemsProjectionMap.put(TagsOfItems.ITEM_TYPE, TagsOfItems.ITEM_TYPE);
        tagsOfItemsProjectionMap.put(TagsOfItems.TAG_ID, TagsOfItems.TAG_ID);
    }
}