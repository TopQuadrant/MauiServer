package org.topbraid.mauiserver.lang.pt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.entopix.maui.stopwords.Stopwords;

/**
 * Portuguese stopwords filter, based on stopwords loaded from a
 * file we found on the internet.
 * 
 * @see https://gist.github.com/alopes/5358189
 */
public class StopwordsPortuguese extends Stopwords {

	private static final long serialVersionUID = 1L;

	public StopwordsPortuguese() {
		super(readStopwordsFile());
	}

	private static List<String> readStopwordsFile() {
		ClassLoader cl = StopwordsPortuguese.class.getClassLoader();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(cl.getResourceAsStream("/stopwords-pt.txt")))) {
			List<String> result = new ArrayList<>();
			String line = null;
			while ((line = br.readLine()) != null) {
				result.add(line.trim());
			}
			return result;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
