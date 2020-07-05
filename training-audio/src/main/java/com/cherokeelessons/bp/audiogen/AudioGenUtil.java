package com.cherokeelessons.bp.audiogen;

public class AudioGenUtil {
	private AudioGenUtil() {}
	
	public static String asFilename(String challenge) {
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
}
