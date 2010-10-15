package vn.evolus.droidreader.content;

public interface SynchronizationListener {
	void onStart();
	void onFinish(int totalNewItems);
}
