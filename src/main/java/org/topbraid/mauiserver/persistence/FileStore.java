package org.topbraid.mauiserver.persistence;

import java.io.File;
import java.io.IOException;

import org.topbraid.mauiserver.MauiServerException;

public abstract class FileStore<T> implements ObjectStore<T> {
	private final String taggerId;
	private final File file;
	
	public FileStore(String taggerId, File file) {
		this.taggerId = taggerId;
		this.file = file;
	}

	protected String getFileName() {
		return file.getName();
	}
	
	protected File getFile() {
		return file;
	}
	
	protected String getTaggerId() {
		return taggerId;
	}
	
	@Override
	public boolean contains() {
		return file.exists();
	}
	
	@Override
	public final T get() {
		if (!contains()) {
			return null;
		}
		try {
			return read();
		} catch (IOException ex) {
			return throwException("Error reading from", ex);
		}
	}

	protected abstract T read() throws IOException;
	
	@Override
	public final void put(T storable) {
		if (storable == null) {
			file.delete();
			return;
		}
		try {
			write(storable);
		} catch (IOException ex) {
			throwException("Error writing to", ex);
		}
	}
	
	protected abstract void write(T storable) throws IOException;
		
	@Override
	public void delete() {
		put(null);
	}
	
	protected T throwException(String activity) {
		return throwException(activity, null);
	}
	
	protected T throwException(String activity, Throwable cause) {
		if (cause == null || (cause.getCause() == null && cause.getMessage() == null)) {
			throw new MauiServerException(activity + " " + getFileName() + " for tagger " + taggerId);
		} else {
			throw new MauiServerException(activity + " " + getFileName() + " for tagger " + taggerId + ": " + cause.getMessage(), cause);
		}
	}
}
