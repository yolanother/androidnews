package vn.evolus.droidreader;

import java.util.ArrayList;
import java.util.List;

import com.github.droidfu.DroidFuApplication;

public class Application extends DroidFuApplication {
	private static Application instance;
	private List<OnNewItemsListener> onNewItemListeners;
	
	public Application() {
		instance = this;				
		onNewItemListeners = new ArrayList<OnNewItemsListener>();
	}
			
	public static Application getInstance() {
		return instance;
	}	
	
	public synchronized void registerOnNewItemsListener(OnNewItemsListener listener) {
		if (!onNewItemListeners.contains(listener)) {
			onNewItemListeners.add(listener);
		}
	}
	
	public synchronized void unregisterOnNewItemsListener(OnNewItemsListener listener) {
		if (onNewItemListeners.contains(listener)) {
			onNewItemListeners.remove(listener);
		}
	}
	
	public void notifyNewItemsAvailable() {
		for (OnNewItemsListener listener : onNewItemListeners) {
			listener.onNewItems();
		}
	}		
}
