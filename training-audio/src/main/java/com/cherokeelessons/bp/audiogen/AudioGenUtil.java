package com.cherokeelessons.bp.audiogen;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

public class AudioGenUtil {
	public static String alternizeEnglishSexes(final String text) {
		String tmp = StringUtils.normalizeSpace(text);
		if (tmp.toLowerCase().contains("brother")) {
			return tmp;
		}
		if (tmp.toLowerCase().contains("sister")) {
			return tmp;
		}
		if (tmp.contains("himself")) {
			tmp = tmp.replaceAll("(?i)(He )", "$1or she ");
			tmp = tmp.replaceAll("(?i)( himself)", "$1 or herself");
		}
		if (tmp.contains("Himself")) {
			tmp = tmp.replaceAll("(?i)\\b(He )", "$1or she ");
			tmp = tmp.replaceAll("(?i)\\b(Himself)", "$1 or herself");
		}
		if (tmp.matches(".*\\b[Hh]is\\b.*")) {
			tmp = tmp.replaceAll("(?i)\\b(His)", "$1 or her");
		}
		if (!tmp.contains(" or she")) {
			tmp = tmp.replaceAll("(?i)\\b(He )", "$1or she ");
		}
		if (!tmp.contains(" or her")) {
			tmp = tmp.replaceAll("(?i)( him)", "$1 or her");
		}
		return tmp;
	}

	public static String asEnglishFilename(final String challenge) {
		String tmp = challenge.toLowerCase();
		if (tmp.indexOf(".") > 4) {
			tmp = tmp.substring(0, tmp.indexOf("."));
		}
		tmp = tmp.replaceAll("(?i)[^A-Za-z1234\\-]", "-").replaceAll("-+", "-");
		return tmp;
	}

	public static String asPhoneticFilename(final String challenge) {
		String tmp = Normalizer.normalize(challenge.toLowerCase(), Form.NFD);
		
		tmp = tmp.replace("ɂ", "-");
		tmp = tmp.replaceAll("(?i)[^a-z0-9:\\-]", "");
		tmp = tmp.replace("a:", "aa");
		tmp = tmp.replace("e:", "ee");
		tmp = tmp.replace("i:", "ii");
		tmp = tmp.replace("o:", "oo");
		tmp = tmp.replace("u:", "uu");
		tmp = tmp.replace("v:", "vv");

		return tmp;
	}

	public static String randomizeEnglishSexes(final String text) {
		final Random r = new Random();
		String tmp = text;
		if (r.nextBoolean() && tmp.contains("himself")) {
			tmp = tmp.replace("He ", "She ");
			tmp = tmp.replace(" him", " her");
		}
		if (r.nextBoolean() && tmp.contains("Himself")) {
			tmp = tmp.replace("He ", "She ");
			tmp = tmp.replace("Him", "Her");
		}
		if (r.nextBoolean() && tmp.matches(".*\\b[Hh]is\\b.*")) {
			tmp = tmp.replaceFirst("\\b([Hh])is\\b", "$1er");
		}
		if (r.nextBoolean() && !tmp.contains("himself")) {
			tmp = tmp.replace("He ", "She ");
		}
		if (r.nextBoolean() && !tmp.contains("himself")) {
			tmp = tmp.replace(" him", " her");
		}
		return tmp;
	}

	public static String removeEnglishFixedGenderMarks(final String text) {
		String tmp = text;
		tmp = tmp.replace("xHe", "He");
		tmp = tmp.replace("xShe", "She");
		tmp = tmp.replace("xhe", "he");
		tmp = tmp.replace("xshe", "she");
		return tmp;
	}

	private AudioGenUtil() {
	}
}
