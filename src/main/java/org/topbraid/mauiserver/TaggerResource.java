package org.topbraid.mauiserver;

import static org.topbraid.mauiserver.JsonUtil.createObjectBuilderThatIgnoresNulls;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.Tagger;
import org.topbraid.mauiserver.tagger.TaggerCollection;

import com.entopix.maui.vocab.VocabularyStore;

public class TaggerResource extends Resource implements Gettable, Deletable {
	private final TaggerCollection taggers;
	private final Tagger tagger;

	public TaggerResource(ServletContext context, TaggerCollection service, Tagger tagger) {
		super(context);
		this.taggers = service;
		this.tagger = tagger;
	}
	
	public String getURL() {
		return getContextPath() + getRelativeTaggerURL(tagger);
	}
	
	@Override
	public Response doGet(Request request) {
		JSONResponse r = request.okJSON();
		r.getRoot()
				.add("title", tagger.getConfiguration().getTitle())
				.add("id", tagger.getId())
				.add("is_trained", tagger.isTrained())
				.add("has_vocabulary", tagger.hasVocabulary())
				.add("links", createObjectBuilderThatIgnoresNulls()
						.add("home", getContextPath() + "/")
						.add("tagger", getContextPath() + TaggerResource.getRelativeTaggerURL(tagger))
						.add("config", getContextPath() + ConfigurationResource.getRelativeConfigurationURL(tagger))
						.add("vocab", getContextPath() + VocabularyResource.getRelativeVocabularyURL(tagger))
						.add("train", getContextPath() + TrainingResource.getRelativeTrainingURL(tagger))
						.add("suggest", getContextPath() + SuggestResource.getRelativeSuggesterURL(tagger))
						.add("xvalidate", getContextPath() + CrossValidationResource.getRelativeCrossValidationURL(tagger)));
		if (tagger.getConfiguration().getDescription() != null) {
			r.getRoot().add("description", tagger.getConfiguration().getDescription());
		}
		if (tagger.hasVocabulary()) {
			VocabularyStore store = tagger.getVocabularyMaui().getVocabularyStore();
			r.getRoot().add("vocab_stats", createObjectBuilderThatIgnoresNulls()
					.add("num_concepts", store.getNumTerms())
					.add("num_altlabels", store.getNumNonDescriptors())
					.add("num_concepts_with_relationships", store.getNumRelatedTerms()));
		}
		return r;
	}

	@Override
	public Response doDelete(Request request) {
		taggers.deleteTagger(tagger.getId());
		return request.noContent();
	}

	public static String getRelativeTaggerURL(Tagger tagger) {
		return "/" + encodeTaggerIdForURL(tagger.getId());
	}

	public static String encodeTaggerIdForURL(String id) {
		try {
			return URLEncoder.encode(id, "utf-8");
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("Can't happen", ex);
		}
	}
	
	public static String decodeTaggerIdFromURL(String id) {
		try {
			return URLDecoder.decode(id, "utf-8"); 
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("Can't happen", ex);
		}
	}
}
