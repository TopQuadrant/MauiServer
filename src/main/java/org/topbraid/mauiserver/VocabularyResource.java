package org.topbraid.mauiserver;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Puttable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.tagger.Tagger;

import com.entopix.maui.vocab.Vocabulary;
import com.entopix.maui.vocab.VocabularyStore;
import org.apache.jena.rdf.model.Model;

public class VocabularyResource extends Resource
		implements Gettable, Puttable, Deletable {
	private final static Logger log = LoggerFactory.getLogger(VocabularyResource.class);
	
	private Tagger tagger;

	public VocabularyResource(ServletContext context, Tagger tagger) {
		super(context);
		this.tagger = tagger;
	}

	@Override
	public String getURL() {
		return getContextPath() + getRelativeVocabularyURL(tagger);
	}
	
	@Override
	public Response doGet(Request request) {
		Model vocabulary = tagger.getVocabularyJena();
		if (vocabulary == null) {
			return request.noContent();
		}
		vocabulary.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		return request.okTurtle(vocabulary);
	}

	@Override
	public Response doPut(Request request) {
		try {
			Model rdf = request.getBodyRDF();
			if (rdf == null) {
				return request.badRequest("Missing or empty request body; expected SKOS vocabulary in Turtle or RDF/XML format");
			}
			Vocabulary vocab = tagger.toMauiVocabulary(rdf);
			VocabularyStore store = vocab.getVocabularyStore();
			log.info("Vocabulary built: " + 
					store.getNumTerms() + " terms, " + 
					store.getNumNonDescriptors() + " non-descriptor terms, " + 
					store.getNumRelatedTerms() + " terms with related terms");
			if (vocab.getVocabularyStore().getNumTerms() == 0) {
				return request.badRequest("No resources of type skos:Concept found in input file");
			}
			tagger.setVocabulary(rdf, vocab);
			return request.okTurtle(tagger.getVocabularyJena());
		} catch (MauiServerException ex) {
			return request.badRequest(ex.getMessage());
		}
	}

	@Override
	public Response doDelete(Request request) {
		tagger.setVocabulary(null, null);
		return request.noContent();
	}

	public static String getRelativeVocabularyURL(Tagger tagger) {
		return TaggerResource.getRelativeTaggerURL(tagger) + "/vocab";
	}
}
