package vn.evolus.news.model;

import java.util.ArrayList;

import vn.evolus.news.providers.ContentsProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class Image {
	public static final byte MAX_RETRIES = 3;
	
	public static final byte IMAGE_STATUS_PENDING = -1;
	public static final byte IMAGE_STATUS_QUEUED = 0;
	public static final byte IMAGE_STATUS_DOWNLOADING = 1;
	public static final byte IMAGE_STATUS_DOWNLOADED = 2;
	public static final byte IMAGE_STATUS_FAILED = 3;
	
	private long id;
	private String url;
	private byte status;
	private byte retries = 0;
	
	public Image(String url, byte status) {
		this(0, url, status);
	}
	public Image(long id, String url, byte status) {		
		this.id = id;
		this.url = url;
		this.status = status;
	}
	public long getId() {
		return id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public byte getStatus() {
		return status;
	}
	public void setStatus(byte status) {
		this.status = status;
	}
	public byte getRetries() {
		return retries;
	}	
	public void increaseRetries() {
		this.retries += 1;
	}
	
	public static long queue(String url, ContentResolver cr) {
		Image image = load(url, cr);
		if (image != null) return image.id;		
		
		image = new Image(url, IMAGE_STATUS_PENDING);
		image.save(cr);		
		return image.id;
	}
	
	public static ArrayList<Image> loadAllQueuedImages(ContentResolver cr) {
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.ID,
					Images.URL,
					Images.STATUS,
					Images.RETRIES
				},
				Images.STATUS + "=?",
				new String[] { String.valueOf(IMAGE_STATUS_QUEUED) },
				Images.RETRIES + " DESC, " + Images.ID);
		ArrayList<Image> images = new ArrayList<Image>();
		while (cursor.moveToNext()) {			
			Image image = new Image(cursor.getLong(0), cursor.getString(1), (byte)cursor.getInt(2));
			image.retries = (byte)cursor.getInt(3);
			images.add(image);
		}
		cursor.close();
		return images;
	}
	
	public static boolean exists(String url, ContentResolver cr) {		
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.ID
				}, 
				Images.URL + "=?",
				new String[] { url },
				null);
		boolean result = cursor.moveToFirst();
		cursor.close();
		return result;		
	}
	
	public static Image load(long id, ContentResolver cr) {
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.ID,
					Images.URL,
					Images.STATUS
				}, 
				Images.ID + "=?",
				new String[] { String.valueOf(id) },
				null);
		Image image = null;
		if (cursor.moveToFirst()) {			
			image = new Image(cursor.getLong(0), cursor.getString(1), (byte)cursor.getInt(2));
		}
		cursor.close();
		return image;
	}
	
	public static Image load(String url, ContentResolver cr) {
		Cursor cursor = cr.query(Images.CONTENT_URI, 
				new String[] {
					Images.ID,
					Images.URL,
					Images.STATUS
				}, 
				Images.URL + "=?",
				new String[] { url },
				null);
		Image image = null;
		if (cursor.moveToFirst()) {			
			image = new Image(cursor.getLong(0), cursor.getString(1), (byte)cursor.getInt(2));			
		}
		cursor.close();
		return image;
	}		
	
	public void save(ContentResolver cr) {
		ContentValues values = new ContentValues();
		if (this.id == 0) {
			if (exists(this.url, cr)) return;
			
			values.put(Images.URL, this.url);
			values.put(Images.STATUS, this.status);
			values.put(Images.RETRIES, this.retries);
			Uri contentUri = cr.insert(Images.CONTENT_URI, values);
			this.id = ContentUris.parseId(contentUri);
		} else {
			values.put(Images.STATUS, this.status);
			values.put(Images.RETRIES, this.retries);
			cr.update(Images.CONTENT_URI, values, ContentsProvider.WHERE_ID, 
					new String[] { String.valueOf(this.id) });
		}
	}
	
	public void delete(ContentResolver cr) {		
		cr.delete(Images.CONTENT_URI, ContentsProvider.WHERE_ID, new String[] { String.valueOf(this.id) });
		this.id = 0;
	}
	
	public static final class Images implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" 
				+ ContentsProvider.AUTHORITY + "/images");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.evolus.droidnews.images";
		
		public static final String ID = "ID";
		public static final String URL = "URL";
		public static final String STATUS = "STATUS";
		public static final String RETRIES = "RETRIES";
		
		private Images() {
		}
	}
}
