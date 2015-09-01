# Maui Server HTTP API
This document describes the HTTP API of [Maui Server](https://github.com/TopQuadrant/MauiServer). Maui Server is a RESTful wrapper around the [Maui content indexer](https://github.com/zelandiya/maui). Version 1.0 of Maui Server only exposes the subject indexing functionality of Maui, that is, it detects the main topics of documents, where the list of possible topics are provided as a [SKOS](http://www.w3.org/2004/02/skos/) taxonomy.

## Concepts
A Maui Server is a collection of *taggers*. Each tagger is an auto-tagging service that provides tag recommendations for a particular *vocabulary* (a.k.a. *taxonomy*) and has been trained with a particular training set of documents. By supporting multiple taggers on a single Maui Server, we can do auto-tagging for multiple separate vocabularies.

Each tagger comes with several associated resources, such as a configuration resource, a training resource, and the actual tag suggestion service.

## Typical flow of API calls

**Preparing the tagger:**

1. Create a tagger (`POST` to home resource).
2. Configure the tagger (optional, `PUT` or `POST` to tagger configuration resource).
3. Upload the SKOS vocabulary (`PUT` to tagger vocabulary resource).
4. Train the tagger by uploading a training corpus (`POST` to tagger training resource).

**Using the tagger:**

5. Call the tag suggestion service to provide tag recommendations for a document (`POST` to tagger suggestion resource).
6. Check the log (`GET` to tagger log resource—not yet implemented!).

## Overview

| Resource | URL pattern | GET | PUT | POST | DELETE |
| --- | --- | --- | --- | --- | --- |
| **[Home](#resource-service)** | `/` | List taggers (JSON) | | Create tagger (formencoded) | |
| **[Tagger](#resource-tagger)** | `/{tagger-id}` | Tagger status (JSON) | | | Delete tagger |
| **[Tagger configuration](#resource-tagger-configuration)** | `/{tagger-id}/config` | Show config (JSON) | Replace config (JSON) | Update config (JSON, formencoded) | Reset config |
| **[Tagger vocabulary](#resource-tagger-vocabulary)** | `/{tagger-id}/vocab` | Return vocabulary (SKOS) | Replace vocabulary (SKOS) | | Clear vocabulary |
| **[Tagger training](#resource-tagger-training)** | `/{tagger-id}/train` | Training status (JSON) | | Send training documents (JSON) | Clear model |
| **[Tagger suggestions](#resource-tagger-suggestions)** | `/{tagger-id}/suggest` | Service description (JSON) | | Provide recommendations for document (formencoded) | |
| **[Tagger log](#resource-tagger-log)** (not yet implemented!) | `/{tagger-id}/log` | Show log (Plain text) | | | Clear log |

## Resource: Service
URL pattern: `/`

### `GET`: Get list of taggers
Returns a list of the taggers available on the server, in JSON format, as well as some general configuration information about the service.

#### Example request

`curl http://localhost:8080/`

#### Example response

    {
      "title": "Maui Server",
      "data_dir": "/usr/local/MauiServer/data",
      "default_lang": "en",
      "taggers": [
        {
          "id": "demo",
          "href": "/demo",
          "title": "Demo Tagger"
        }
      ]
    }


### `POST`: Create new tagger
Creates a new tagger. The tagger's ID must be enclosed with the POST message as form-encoded key-value pairs. Additional configuration parameters for the tagger may be enclosed as well. See the *Tagger Configuration* resource for documentation on the configuration parameters.

The response is the same as for `GET`.

#### Example request
`curl -d id=demo http://localhost:8080/`

## Resource: Tagger
URL pattern: `/{tagger-id}`

### `GET`: Get tagger status
Returns general information about the status of the tagger in JSON format, such as:

- Is it trained? On how many documents?
- How many concepts in the vocabulary? How deep is the hierarchy?
- Links to all sub-resources (config, vocab, log, etc.)

#### Example request
`curl http://localhost:8080/demo`

### Example response
    {
      "title": "Demo Tagger",
      "id": "demo",
      "is_trained": true,
      "has_vocabulary": true,
      "vocab_stats": {
        "num_concepts": 150621,
        "num_altlabels": 126736,
        "num_concepts_with_relationships": 6696
      },
      "links": {
        "home": "/",
        "tagger": "/demo",
        "config": "/demo/config",
        "vocab": "/demo/vocab",
        "train": "/demo/train",
        "suggest": "/demo/suggest"
      }
    }

### `DELETE`: Delete tagger
This removes the tagger and all its sub-resources from the server. On success, the response is `204 No Content` and an empty response body.

#### Example request
`curl -X DELETE http://localhost:8080/demo`

## Resource: Tagger Configuration
URL pattern: `/{tagger-id}/config`

### `GET`: Show configuration
Returns the tagger's configuration in JSON format.

| Key | Format | Description | 
| --- | --- | --- |
| title | String | Human-readable title for this tagger |
| description | String | Human-readable description for this tagger |
| lang | One of `en`, `fr`, `de`, `es` | Language for this tagger, or `null` for the server default |
| stemmer_class | Qualified Java class name | Custom stemmer impementation; overrides `lang` |
| stopword_class | Qualified Java class name | Custom stopword implementation; overrides `lang` |

#### Example request
`curl http://localhost:8080/demo/config`

#### Example response
    {
      "title": "Demo Tagger",
      "description": null,
      "lang": "en",
      "stemmer_class": null,
      "stopwords_class": null
    }

### `PUT`: Replace configuration
Updates all configuration settings based on the enclosed JSON document. See `GET` for supported configuration settings. The response format is the same as for `GET`.

#### Example request
    curl -X PUT --data-binary @- http://localhost:8080/demo/config
    {
      "title": "Demo Tagger",
      "description": null,
      "lang": "en",
      "stemmer_class": null,
      "stopwords_class": null
    }

### `POST`: Update configuration settings
Updates individual configuration settings based on the enclosed JSON documents. Any settings that are not present in the JSON document will be left as is. Alternatively, the settings to be updated can be provided in form-encoded format. See `GET` for supported configuration settings. The response format is the same as for `GET`.

#### Example request
`curl -d 'lang=en' http://localhost:8080/demo/config`

## Resource: Tagger Vocabulary
URL pattern: `/{tagger-id}/vocab`

### `GET`: Show vocabulary
Returns the vocabulary used by this tagger in SKOS format, using Turtle syntax.

#### Example request
`curl http://localhost:8080/demo/vocab`

### `PUT`: Replace vocabulary
Replaces the vocabulary with the enclosed SKOS document. If the `Content-Type` header is `application/rdf+xml`, `text/xml` or `application/xml`, the document is assumed to be in RDF/XML syntax. Otherwise, it is assumed to be in Turtle syntax. The response is the same as for `GET`.

#### Example request
`curl -X PUT --data-binary @my-taxonomy.ttl http://localhost:8080/demo/vocab`

### `DELETE`: Delete vocabulary
Deletes the vocabulary. The response is `204 No Content`.

#### Example request
`curl -X DELETE http://localhost:8080/demo/vocab`

## Resource: Tagger Training
URL pattern: `/{tagger-id}/train`

### `GET`: Training status
Returns a JSON document indicating training status: Not trained, Trained, Training in progress.

### `POST`: Train tagger with training data
Enclosed with the POST request there must be a collection of documents to be used as training data. The entire collection must be formatted as a single JSON array, where each element is a JSON object representing one document. The fields of the object will be used or ignored based on the tagger's configuration.

#### Example request
`curl --data-binary @training-corpus.jsonl http://localhost:8080/test/train`

### `DELETE`: Reset model
Removes the Maui model for this tagger, freeing up memory. The tagger itself will remain on the server, but must be re-trained before it can be used again for suggestions.

## Resource: Tagger Suggestions
URL pattern: `/{tagger-id}/suggest`

### `GET`: Service description
A simple JSON document stating whether the service is operational (that is, a SKOS vocabulary is present and the tagger has been trained).

### `POST`: Perform tag recommendation
This is the key function of the entire server! A document, either plain text or JSON fields, must be enclosed with the request. Returned is a list of recommended concepts from the SKOS vocabulary in JSON. For each concept, the preferred label, URI, and probability is included.

## Resource: Tagger Log
URL pattern: `/{tagger-id}/log`

*Not implemented yet!*
