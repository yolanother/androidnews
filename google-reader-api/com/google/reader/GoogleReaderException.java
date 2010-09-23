package com.google.reader;

public class GoogleReaderException extends Exception {	
	private static final long serialVersionUID = 8167163168097638617L;
	
	public int code;
		
	public GoogleReaderException(int code, String detailMessage) {
		super(String.valueOf(code) + ": " + detailMessage);
		this.code = code;
	}

	public GoogleReaderException() {
		super();
	}

	public GoogleReaderException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public GoogleReaderException(String detailMessage) {
		super(detailMessage);
	}

	public GoogleReaderException(Throwable throwable) {
		super(throwable);
	}	
}
