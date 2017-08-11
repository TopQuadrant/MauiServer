package org.topbraid.mauiserver.lang.pt;

import com.entopix.maui.stemmers.SpanishStemmer;
import com.entopix.maui.stemmers.Stemmer;

/**
 * Not a portuguese stemmer. Actually just
 * some code copied from {@link SpanishStemmer}.
 */
public class PortugueseStemmer extends Stemmer {

	private static final long serialVersionUID = 1L;

	public String stem(String word) {
		return stemSpanish(word);
	}
	
    /*  Spanish stemmer tring to remove inflectional suffixes */
    public String stemSpanish(String word) {

        int len = word.length() - 1;

        if (len > 3) {

            word = removeSpanishAccent(word);

            if (word.endsWith("eses")) {
                //  corteses -> cortÈs
                word = word.substring(0, len - 1);
                return word;
            }

            if (word.endsWith("ces")) {
                //  dos veces -> una vez
                word = word.substring(0, len - 2);
                word = word + 'z';
                return word;
            }

            if (word.endsWith("os") || word.endsWith("as") || word.endsWith("es")) {
                //  ending with -os, -as  or -es
                word = word.substring(0, len - 1);
                return word;

            }
            if (word.endsWith("o") || word.endsWith("a") || word.endsWith("e")) {
                //  ending with  -o,  -a, or -e
                word = word.substring(0, len - 1);
                return word;
            }

        }
        return word;
    }

    private String removeSpanishAccent(String word) {
        word = word.replaceAll("‡|·|‚|‰", "a");
        word = word.replaceAll("Ú|Û|Ù|ˆ", "o");
        word = word.replaceAll("Ë|È|Í|Î", "e");
        word = word.replaceAll("˘|˙|˚|¸", "a");
        word = word.replaceAll("Ï|Ì|Ó|Ô", "a");

        return word;
    }

}
