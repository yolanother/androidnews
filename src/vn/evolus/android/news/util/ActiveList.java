package vn.evolus.android.news.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ActiveList<T> extends ArrayList<T> implements Serializable {
	private static final long serialVersionUID = -2676879815793179223L;
	
	transient private List<ActiveListListener<T>> listeners;
	
	public interface ActiveListListener<T> {
		void onAdd(T item);
		void onInsert(int location, T item);
		void onClear();
	}		
	
	public void clear() {
		super.clear();
		fireChangedEvent();
	}
	
	public boolean add(T item) {
		boolean success = super.add(item);
		if (success) {
			fireAddEvent(item);
		}
		return success;
	}
	
	public void add(int location, T item) {
		super.add(location, item);		
		fireInsertEvent(location, item);		
	}
	
	public void addListener(ActiveListListener<T> listener) {
		if (this.listeners == null) {
			listeners = new ArrayList<ActiveListListener<T>>();
		}
		this.listeners.add(listener);
	}	
	public void removeListener(ActiveListListener<T> listener) {
		if (this.listeners != null) {
			this.listeners.remove(listener);
		}
	}
	
	private void fireChangedEvent() {
		if (this.listeners == null) return;
		for (ActiveListListener<T> listener : listeners) {
			listener.onClear();
		}
	}
	
	private void fireInsertEvent(int location, T item) {
		if (this.listeners == null) return;		
		for (ActiveListListener<T> listener : listeners) {
			listener.onInsert(location, item);
		}
	}
	
	private void fireAddEvent(T item) {
		if (this.listeners == null) return;		
		for (ActiveListListener<T> listener : listeners) {
			listener.onAdd(item);
		}
	}
}
