package org.topbraid.mauiserver.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;

public class VocabularyStore extends FileStore<Model> {

	public VocabularyStore(String taggerId, File file) {
		super(taggerId, file);
	}

	public String getVocabularyFileName() {
		return getFile().getAbsolutePath();		
	}
	
	@Override
	protected Model read() throws IOException {
		try {
			return FileManager.get().loadModel(getFile().getAbsolutePath());
		} catch (JenaException ex) {
			return throwException("Error processing", ex);
		}
	}

	@Override
	protected void write(Model vocabulary) throws IOException {
		try {
			vocabulary.write(new FileOutputStream(getFile()), "TURTLE");
		} catch (JenaException ex) {
			throwException("Error writing to", ex);
		}
	}
}
