/**
 * 
 */
package vn.evolus.droidreader.adapter;

import java.lang.ref.WeakReference;

import vn.evolus.droidreader.util.ImageLoader;
import vn.evolus.droidreader.util.ImageLoaderHandler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;

public class ItemImageLoaderHandler extends ImageLoaderHandler {
	private WeakReference<ImageView> imageRef;
	private String imageUrl;
	
	public ItemImageLoaderHandler(ImageView imageView, String imageUrl) {		
		this.imageRef = new WeakReference<ImageView>(imageView);
		this.imageUrl = imageUrl;
	}
	public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.what == ImageLoader.BITMAP_DOWNLOADED_SUCCESS) {
        	ImageView imageView = imageRef.get();
        	if (imageView == null) return;
        	
	        if (imageUrl.equals((String)imageView.getTag())) {
	        	imageView.setVisibility(View.VISIBLE);
	        	imageView.setImageBitmap(super.getImage());
	        }
        }
    }
}