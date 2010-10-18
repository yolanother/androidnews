package vn.evolus.droidreader.content;

public interface SynchronizationListener {
	void onStart();
	void onProgress(String progressText);
	void onFinish(int totalNewItems);
}
