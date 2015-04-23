package org.topbraid.mauiserver;

@SuppressWarnings("serial")
public class MauiServerException extends RuntimeException {

	public MauiServerException(String message) {
		super(message);
	}
	
	public MauiServerException(Throwable cause) {
		super(cause);
	}
	
	public MauiServerException(String message, Throwable cause) {
		super(message, cause);
	}
}
