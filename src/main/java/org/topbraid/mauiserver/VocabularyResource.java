package org.topbraid.mauiserver;

import javax.servlet.ServletContext;

import org.topbraid.mauiserver.framework.Request;
import org.topbraid.mauiserver.framework.Resource;
import org.topbraid.mauiserver.framework.Resource.Deletable;
import org.topbraid.mauiserver.framework.Resource.Gettable;
import org.topbraid.mauiserver.framework.Resource.Puttable;
import org.topbraid.mauiserver.framework.Response;
import org.topbraid.mauiserver.tagger.Tagger;

import com.entopix.maui.vocab.Vocabulary;
import com.hp.hpl.jena.rdf.model.Model;

public class VocabularyResource extends Resource
		implements Gettable, Puttable, Deletable {
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
