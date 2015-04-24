package org.topbraid.mauiserver;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.framework.Response.JSONResponse;
import org.topbraid.mauiserver.tagger.TaggerCollection;
import org.topbraid.mauiserver.tagger.Tagger;

import com.entopix.maui.vocab.VocabularyStore;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		r.getRoot().put("title", "Tagger: " + tagger.getConfiguration().getTitle());
		if (tagger.getConfiguration().getDescription() != null) {
			r.getRoot().put("description", tagger.getConfiguration().getDescription());
		}
		r.getRoot().put("id", tagger.getId());
		r.getRoot().put("is_trained", tagger.isTrained());
		r.getRoot().put("has_vocabulary", tagger.hasVocabulary());
		if (tagger.hasVocabulary()) {
			VocabularyStore store = tagger.getVocabularyMaui().getVocabularyStore();
			ObjectNode stats = r.getRoot().objectNode();
			stats.put("num_concepts", store.getNumTerms());
			stats.put("num_altlabels", store.getNumNonDescriptors());
			stats.put("num_concepts_with_relationships", store.getNumRelatedTerms());
			r.getRoot().set("vocab_stats", stats);
		}
		ObjectNode links = r.getRoot().objectNode();
		links.put("service", getContextPath() + "/");
		links.put("tagger", getContextPath() + TaggerResource.getRelativeTaggerURL(tagger));
		links.put("config", getContextPath() + ConfigurationResource.getRelativeConfigurationURL(tagger));
		links.put("vocab", getContextPath() + VocabularyResource.getRelativeVocabularyURL(tagger));
		links.put("train", getContextPath() + TrainingResource.getRelativeTrainingURL(tagger));
		links.put("suggest", getContextPath() + SuggestResource.getRelativeSuggesterURL(tagger));
		links.put("log", getContextPath() + LogResource.getRelativeLogURL(tagger));
		r.getRoot().set("links", links);
		return r;
	}

	@Override
	public Response doDelete(Request request) {
		taggers.deleteTagger(tagger.getId());
		return request.noContent();
	}

	public static String getRelativeTaggerURL(Tagger tagger) {
		return "/" + tagger.getId();
	}
}
