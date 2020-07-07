package com.cherokeelessons.bp.audiogen;

import java.util.Random;

public class AudioGenUtil {
	private AudioGenUtil() {
	}

	public static String randomizeEnglishSexes(final String text) {
		Random r = new Random();
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

	public static String removeEnglishFixedGenderMarks(String text) {
		String tmp = text;
		tmp = tmp.replace("xHe", "He");
		tmp = tmp.replace("xShe", "She");
		tmp = tmp.replace("xhe", "he");
		tmp = tmp.replace("xshe", "she");
		return tmp;
	}

	public static String asPhoneticFilename(String challenge) {
		String tmp = challenge;
		tmp = tmp.replace("ɂ", "-");

		tmp = tmp.replace("¹", "1");
		tmp = tmp.replace("²", "2");
		tmp = tmp.replace("³", "3");
		tmp = tmp.replace("⁴", "4");

		tmp = tmp.replace("a", "aa");
		tmp = tmp.replace("e", "ee");
		tmp = tmp.replace("i", "ii");
		tmp = tmp.replace("o", "oo");
		tmp = tmp.replace("u", "uu");
		tmp = tmp.replace("v", "vv");

		tmp = tmp.replace("ạ", "a");
		tmp = tmp.replace("ẹ", "e");
		tmp = tmp.replace("ị", "i");
		tmp = tmp.replace("ọ", "o");
		tmp = tmp.replace("ụ", "u");
		tmp = tmp.replace("ṿ", "v");

		tmp = tmp.replaceAll("(?i)[^a-z1234\\-]", "");

		tmp = tmp.replaceAll("([cdghjklmnstwy])([aeiouv])([aeiouv])([cdghjklmnstwy\\-])", "$1$2$4");
		tmp = tmp.replaceAll("^(.*)([aeiouv])([aeiouv])([1234]+)?$", "$1$2$4");

		return tmp;
	}

	public static String asEnglishFilename(String challenge) {
		String tmp = challenge;
		tmp = tmp.replaceAll("(?i)[^A-Za-z1234\\-]", "-").replaceAll("-+", "-");
		return tmp;
	}
}
