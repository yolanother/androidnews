/* Copyright (c) 2009 Matthias Käppler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vn.evolus.droidreader.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import vn.evolus.droidreader.Constants;
import vn.evolus.droidreader.Settings;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Image;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.util.Log;

import com.google.common.collect.MapMaker;

/**
 * <p>
 * A simple 2-level cache for bitmap images consisting of a small and fast
 * in-memory cache (1st level cache) and a slower but bigger disk cache (2nd
 * level cache). For second level caching, the application's cache directory
 * will be used. Please note that Android may at any point decide to wipe that
 * directory.
 * </p>
 * <p>
 * When pulling from the cache, it will first attempt to load the image from
 * memory. If that fails, it will try to load it from disk. If that succeeds,
 * the image will be put in the 1st level cache and returned. Otherwise it's a
 * cache miss, and the caller is responsible for loading the image from
 * elsewhere (probably the Internet).
 * </p>
 * <p>
 * Pushes to the cache are always write-through (i.e., the image will be stored
 * both on disk and in memory).
 * </p>
 * 
 * @author Matthias Kaeppler
 */
public class ImageCache implements Map<String, Bitmap> {
	private static ImageCache instance;
	
    private int cachedImageQuality = 100;    
    private String secondLevelCacheDir;
    private Map<String, Bitmap> cache;
    private CompressFormat compressedImageFormat = CompressFormat.PNG;
    
    private ImageCache(Context context, int initialCapacity, int concurrencyLevel) {
        this.cache = new MapMaker().initialCapacity(initialCapacity).concurrencyLevel(
            concurrencyLevel).weakValues().makeMap();
        this.secondLevelCacheDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/droidreader/imagecache";
        new File(secondLevelCacheDir).mkdirs();                
    }
    
    public static synchronized ImageCache createInstance(Context context, int initialCapacity, int concurrencyLevel) {
    	instance = new ImageCache(context, initialCapacity, concurrencyLevel);
    	return instance;
    }
    
    public static synchronized ImageCache getInstance() {
    	return instance;
    }
    
    /**
     * @param cachedImageQuality
     *        the quality of images being compressed and written to disk (2nd
     *        level cache) as a number in [0..100]
     */
    public void setCachedImageQuality(int cachedImageQuality) {
        this.cachedImageQuality = cachedImageQuality;
    }

    public int getCachedImageQuality() {
        return cachedImageQuality;
    }

    public synchronized Bitmap get(Object key) {
        String imageUrl = (String) key;
        Bitmap bitmap = cache.get(imageUrl);

        if (bitmap != null) {
            // 1st level cache hit (memory)
            return bitmap;
        }

        File imageFile = getImageFile(imageUrl);
        if (imageFile.exists()) {
            // 2nd level cache hit (disk)
        	try {
        		bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        	} catch (Throwable e) {}
        	
            if (bitmap == null) {
                // treat decoding errors as a cache miss
                return null;
            }
            cache.put(imageUrl, bitmap);
            return bitmap;
        }

        // cache miss
        return null;
    }

    public Bitmap put(String imageUrl, Bitmap image) {
        File imageFile = getImageFile(imageUrl);
        try {
            imageFile.createNewFile();
            FileOutputStream ostream = new FileOutputStream(imageFile);
            image.compress(compressedImageFormat, cachedImageQuality, ostream);
            ostream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cache.put(imageUrl, image);
    }

    public void putAll(Map<? extends String, ? extends Bitmap> t) {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return cache.containsValue(value);
    }

    public Bitmap remove(Object key) {
        return cache.remove(key);
    }

    public Set<String> keySet() {
        return cache.keySet();
    }

    public Set<java.util.Map.Entry<String, Bitmap>> entrySet() {
        return cache.entrySet();
    }

    public int size() {
        return cache.size();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public void clear() {
        cache.clear();
    }

    public Collection<Bitmap> values() {
        return cache.values();
    }

    private File getImageFile(String imageUrl) {
        return new File(getCacheFileName(imageUrl));
    }
    
    public static void downloadImage(String imageUrl) throws IOException {    	
    	HttpClient client = new DefaultHttpClient();
    	HttpParams params = client.getParams();
    	HttpConnectionParams.setConnectionTimeout(params, ImageLoader.CONNECT_TIMEOUT);
    	HttpConnectionParams.setSoTimeout(params, ImageLoader.READ_TIMEOUT);
    	HttpGet httpGet = new HttpGet(imageUrl);    	
    	HttpResponse response = client.execute(httpGet);    	
        InputStream is = response.getEntity().getContent();
        
        // save to cache folder
        if (Constants.DEBUG_MODE) Log.d(Constants.LOG_TAG, "Save image to " + getCacheFileName(imageUrl));
    	File imageFile = new File(getCacheFileName(imageUrl));    	
        imageFile.createNewFile();
        FileOutputStream os = null;
        try {
        	os = new FileOutputStream(imageFile);	                
        	StreamUtils.writeStream(is, os);
        } finally {
        	if (os != null) {
        		os.close();
        	}
        }
    }
    
    public static String getCacheFileName(String imageUrl) {
    	long imageId = 0;
    	int pos = imageUrl.lastIndexOf('#');
		if (pos > 0) {
			try {
				imageId = Integer.parseInt(imageUrl.substring(pos + 1));
			} catch (NumberFormatException nfe) {				
			}
		}
		if (imageId == 0) {
			Image image = ContentManager.loadImage(imageUrl);
	    	if (image == null) return "";
	    	imageId = image.id;
		}
    	return getCacheFileName(imageId);
    }
    
    public static String getCacheFileName(long imageId) {    	
    	return instance.secondLevelCacheDir + "/" + imageId;
    }
    
    public static boolean isCached(String imageUrl) {    	
    	File file = new File(getCacheFileName(imageUrl));
    	return file.exists();
    }
    
    public static void clearCacheIfNecessary() {
    	List<Integer> oldestImageIds = ContentManager.loadOldestImageIds(Settings.getKeepMaxImages());    	
    	for (int imageId : oldestImageIds) {
    		try {
	    		File imageFile = new File(getCacheFileName(imageId));
	    		imageFile.delete();
	    		ContentManager.deleteImage(imageId);
    		} catch (RuntimeException e) {    			
    		}
    	}
	}
    
    public static void clearCacheFolder() {
    	File cacheDir = new File(instance.secondLevelCacheDir);
    	if (!cacheDir.exists()) return;
    	
    	File[] files = cacheDir.listFiles();
    	if (files != null) {
    		for (File file : files) {
    			try {
    				file.delete();
    			} catch (Throwable t) {    				
    			}
    		}    		
    	}
	}
}
