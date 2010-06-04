package vn.evolus.news.providers;

import java.util.HashMap;

import vn.evolus.news.rss.Channel.Channels;
import vn.evolus.news.rss.Item.Items;
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
	public static final String AUTHORITY = "vn.evolus.news.contents";
	private static final String DATABASE_NAME = "droidnews.db";
    private static final int DATABASE_VERSION = 2;
    
	public static final String CHANNELS_TABLE_NAME = "channels";
	public static final String ITEMS_TABLE_NAME = "items";
				
	private static final UriMatcher uriMatcher;
    private static final int CHANNELS = 1;
    private static final int ITEMS = 2;
    private static final int ITEMS_UNREAD_COUNT = 3;
    
    public static final String WHERE_ID = "ID=?";
    
    private DatabaseHelper dbHelper;

    private static HashMap<String, String> channelsProjectionMap;
    private static HashMap<String, String> itemsProjectionMap;
		
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

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
    	int matchedUri = uriMatcher.match(uri);
        if (matchedUri != CHANNELS && matchedUri != ITEMS) { throw new IllegalArgumentException("Unknown URI " + uri); }

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
                limit = "20";
                break;
            case ITEMS_UNREAD_COUNT:            	
                qb.setTables(ITEMS_TABLE_NAME);
                //projection = new String[] { Items.CHANNEL_ID, Items.UNREAD_COUNT};
                qb.setProjectionMap(itemsProjectionMap);
                group = Items.CHANNEL_ID;
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
	                    + Channels.IMAGE_URL + " VARCHAR(1000)"
	                    + ");");
        	
        	db.execSQL("CREATE TABLE " + ITEMS_TABLE_NAME + " (" + Items.ID
	                    + " INTEGER PRIMARY KEY AUTOINCREMENT," 
	                    + Items.TITLE + " VARCHAR(255)," 
	                    + Items.DESCRIPTION + " LONGTEXT,"
	                    + Items.PUB_DATE + " INTEGER,"
	                    + Items.LINK + " VARCHAR(1000),"        			                    
	                    + Items.IMAGE_URL + " VARCHAR(1000),"
	                    + Items.READ + " INTEGER,"
	                    + Items.CHANNEL_ID + " INTEGER"
	                    + ");");
        	
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + ITEMS_TABLE_NAME
                    + "LinkIndex ON " +  ITEMS_TABLE_NAME +" (" + Items.LINK + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + ITEMS_TABLE_NAME
                    + "ChannelIdIndex ON " +  ITEMS_TABLE_NAME +" (" + Items.CHANNEL_ID + ");");
        	db.execSQL("CREATE INDEX IF NOT EXISTS " + ITEMS_TABLE_NAME
                    + "ReadIndex ON " +  ITEMS_TABLE_NAME +" (" + Items.READ + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	db.execSQL("DROP INDEX IF EXISTS " + ITEMS_TABLE_NAME + "ReadIndex");
        	db.execSQL("DROP INDEX IF EXISTS " + ITEMS_TABLE_NAME + "ChannelIdIndex");
        	db.execSQL("DROP INDEX IF EXISTS " + ITEMS_TABLE_NAME + "ReadIndex");
        	db.execSQL("DROP TABLE IF EXISTS " + ITEMS_TABLE_NAME);
        	db.execSQL("DROP TABLE IF EXISTS " + CHANNELS_TABLE_NAME);
        	onCreate(db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            //onDatabaseOpened(db);
        }
    }  
	
	static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, CHANNELS_TABLE_NAME, CHANNELS);
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME, ITEMS);
        uriMatcher.addURI(AUTHORITY, ITEMS_TABLE_NAME + "/unread", ITEMS_UNREAD_COUNT);

        channelsProjectionMap = new HashMap<String, String>();
        channelsProjectionMap.put(Channels.ID, Channels.ID);
        channelsProjectionMap.put(Channels.TITLE, Channels.TITLE);
        channelsProjectionMap.put(Channels.URL, Channels.URL);
        channelsProjectionMap.put(Channels.DESCRIPTION, Channels.DESCRIPTION);        
        channelsProjectionMap.put(Channels.LINK, Channels.LINK);
        channelsProjectionMap.put(Channels.IMAGE_URL, Channels.IMAGE_URL);
        channelsProjectionMap.put(Channels.UNREAD, 
        		"(SELECT COUNT(*) FROM " + ITEMS_TABLE_NAME
        		+ " WHERE " + ITEMS_TABLE_NAME + ".CHANNEL_ID = "
        		+ CHANNELS_TABLE_NAME + ".ID AND " + ITEMS_TABLE_NAME + ".READ = 0) AS UNREAD");
        
        itemsProjectionMap = new HashMap<String, String>();
        itemsProjectionMap.put(Items.ID, Items.ID);
        itemsProjectionMap.put(Items.TITLE, Items.TITLE);
        itemsProjectionMap.put(Items.DESCRIPTION, Items.DESCRIPTION);
        itemsProjectionMap.put(Items.PUB_DATE, Items.PUB_DATE);
        itemsProjectionMap.put(Items.LINK, Items.LINK);
        itemsProjectionMap.put(Items.IMAGE_URL, Items.IMAGE_URL);
        itemsProjectionMap.put(Items.READ, Items.READ);
        itemsProjectionMap.put(Items.CHANNEL_ID, Items.CHANNEL_ID);
        itemsProjectionMap.put(Items.UNREAD_COUNT, "COUNT(*) AS UNREAD");
    }
}