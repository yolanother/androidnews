package vn.evolus.droidreader.content;

import vn.evolus.droidreader.model.Job;

public interface JobExecutor {
	String getExecutableJobType();
	void execute(Job job) throws Exception;
}
