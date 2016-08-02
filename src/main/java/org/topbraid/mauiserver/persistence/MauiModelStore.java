package org.topbraid.mauiserver.persistence;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.util.DataLoader;

public class MauiModelStore extends FileStore<MauiFilter> {
	private static final Logger log = LoggerFactory.getLogger(MauiModelStore.class);

	public MauiModelStore(String taggerId, File file) {
		super(taggerId, file);
	}

	@Override
	protected MauiFilter read() throws IOException {
		try {
			return DataLoader.loadModel(getFile().getAbsolutePath());
		} catch (Exception ex) {
			return throwException("Error reading", ex);
		}
	}

	@Override
	protected void write(MauiFilter mauiModel) throws IOException {
		// TODO: This should write the new model to a temporary location and replace the old one only on success
		log.info("Writing Maui model: " + getFile());
		MauiModelBuilder modelBuilder = new MauiModelBuilder();
		modelBuilder.modelName = getFile().getAbsolutePath();
		try {
			modelBuilder.saveModel(mauiModel);
		} catch (Exception ex) {
			throwException("Error saving", ex);
		}
	}
}
