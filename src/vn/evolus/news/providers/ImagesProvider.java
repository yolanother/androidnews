package vn.evolus.news.providers;

import java.io.File;
import java.io.FileNotFoundException;

import vn.evolus.news.model.Image;
import vn.evolus.news.util.ImageCache;
import vn.evolus.news.util.ImageLoader;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class ImagesProvider extends ContentProvider {
	private static final String URI_PREFIX = "content://vn.evolus.news.images";
	
	public static String constructUri(String url) {
		Uri uri = Uri.parse(url);
		return uri.isAbsolute() ? url : URI_PREFIX + url;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)		
			throws FileNotFoundException {		
		File file = new File(uri.getPath());
		if (!file.exists()) {
			long id = ContentUris.parseId(uri);
			Image image = Image.load(id, getContext().getContentResolver());
			if (image != null) {
				Log.d("DEBUG", "Image not found. Start downloading " + image.getUrl());
				try {
					//ImageCache.downloadImage(image.getUrl());					
					ImageLoader.start(image.getUrl(), null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, 
				ParcelFileDescriptor.MODE_READ_ONLY);		
		return parcel;		
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException(
				"Not supported by this provider");
	}

	@Override
	public String getType(Uri uri) {
		throw new UnsupportedOperationException(
				"Not supported by this provider");
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException(
				"Not supported by this provider");
	}

	@Override
	public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
		throw new UnsupportedOperationException(
				"Not supported by this provider");
	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s,
			String[] as) {
		throw new UnsupportedOperationException(
				"Not supported by this provider");
	}
}
