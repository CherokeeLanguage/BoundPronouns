package com.cherokeelessons.bp.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;

public class CherokeeUtils {

	/**
	 * Manual TESTS.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		String[] cedTest = { //
				"U²sgal²sdi ạ²dv¹ne²³li⁴sgi.", //
				"Ụ²wo²³dị³ge⁴ɂi gi²hli a¹ke²³he³²ga na ạ²chu⁴ja.",
				"Ạ²ni²³tạɂ³li ạ²ni²sgạ²ya a¹ni²no²hạ²li²³do³²he, ạ²hwi du¹ni²hyọ²he.", //
				"Sa¹gwu⁴hno ạ²sgạ²ya gạ²lo¹gwe³ ga²ne²he sọ³ɂị³hnv³ hla².",
				"Na³hnv³ gạ²lo¹gwe³ ga²ne⁴hi u²dlv²³kwsạ²ti ge¹se³, ạ²le go²hu⁴sdi yu²³dv³²ne⁴la a¹dlv²³kwsge³.",
				"A¹na³ɂi²sv⁴hnv go²hu⁴sdi wu²³ni³go²he do²jụ²wạ³ɂị²hlv,", //
				"na³hnv³ gạ²lo¹gwe³ ga²ne⁴hi kị²lạ²gwu ị²yv⁴da wị²du²³sdạ³yo²hle³ o²³sdạ²gwu nu²³ksẹ²stạ²nv⁴na ị²yu³sdi da¹sdạ²yo²hị²hv⁴.",
				"U²do²hị²yu⁴hnv³ wu²³yo³hle³ ạ²le u¹ni²go²he³ gạ²nv³gv⁴.",
				"Na³hnv³ gạ²lo¹gwe³ nị²ga²³ne³hv⁴na \"ạ²hwi e¹ni²yo³ɂa!\" u¹dv²hne.",
				"\"Ji²yo³ɂe³²ga\" u¹dv²hne na³ gạ²lo¹gwe³ ga²ne⁴hi, a¹dlv²³kwsgv³.",
				"U¹na³ne²lu²³gi³²se do²jụ²wạ³ɂị²hlv³ di³dla, nạ²ɂv²³hnị³ge⁴hnv wu²³ni³luh²ja u¹ni²go²he³ so²³gwị³li gạɂ³nv⁴.",
				"\"So²³gwị³lị³le³² i¹nạ²da²hị³si\" u¹dv²hne³ na³ u²yo²hlv⁴.", "\"Hạ²da²hị³se³²ga³\" a¹go¹se²³le³." };
//		cedTest = new String[] {"\"Ji²yo³ɂe³²ga\" u¹dv²hne na³ gạ²lo¹gwe³ ga²ne⁴hi, a¹dlv²³kwsgv³."};
		String[] rrdTest = { //
				"adanạnạgị³ɂa", //
				"adạna³wịdiha", //
				"ụnạnẹsạda" };
		for (String a : cedTest) {
			System.out.println("_______________");
			System.out.println();
			System.out.println(a);
			// + "\n ->\n" +
			System.out.println(ced2mco_nfc(a));
		}
		System.out.println("_______________");
		System.out.println();
	}

	/**
	 * Replacement patterns to convert CED numbers to Modified Community Orthography.
	 * <br>
	 * Replacements <b>must</b> be performed long to short!
	 */
	private static String[][] cedtones2mco = { //
			{ "²³", "\u030C" }, { "³²", "\u0302" }, //
			{ "¹", "\u0300" }, { "²", "" }, //
			{ "³", "\u0301" }, { "⁴", "\u030b" }//
	};

	protected CherokeeUtils() {
		//
	}

	/**
	 * Converts CED Orthography phonemics to Modified Community Orthography
	 * phonemics. UTF decomposed form.
	 * 
	 * @param ced
	 * @return
	 */
	public static String ced2mco_nfd(final String cedOrthography) {
		return Normalizer.normalize(ced2mco_nfc(cedOrthography), Form.NFD);
	}
	
	/**
	 * Converts CED Orthography phonemics to Modified Community Orthography
	 * phonemics. UTF composed form.
	 * 
	 * @param ced
	 * @return
	 */
	public static String ced2mco_nfc(final String cedOrthography) {
		/*
		 * Convert text to "decomposed" form so that we can use combining diacritics
		 * freely.
		 */
		String mco = Normalizer.normalize(cedOrthography, Normalizer.Form.NFD);

		/*
		 * first mark any vowels not followed by a tone mark as explicitly short.
		 */
		mco = mco.replaceAll("(?i)([aeiouv])([^¹²³⁴\u0323]+)", "$1\u0323$2");

		/*
		 * look for word final vowels followed by tone that need to be marked short
		 */
		mco = mco.replaceAll("(?i)([aeiouv])([¹²³⁴]+)$", "$1\u0323$2");

		mco = mco.replaceAll("(?i)([aeiouv])([¹²³⁴]+)([^¹²³⁴a-zɂ])", "$1\u0323$2$3");

		/*
		 * move any tone marks to be immediately after their appropriate vowel
		 */
		mco = mco.replaceAll("(?i)([^aeiouv\u0323¹²³⁴]+)([¹²³⁴]+)", "$2$1");
		mco = mco.replaceAll("(?i)([^aeiouv\u0323¹²³⁴]+)([¹²³⁴]+)", "$2$1");
		mco = mco.replaceAll("(?i)([^aeiouv\u0323¹²³⁴]+)([¹²³⁴]+)", "$2$1");

		/*
		 * Convert long vowels into vowel + tone + colon forms.
		 */
		mco = mco.replaceAll("(?i)([aeiouv])([¹²³⁴]+)", "$1$2:");

		/*
		 * Strip out the combining lower dots, leaving the short vowels in correct form
		 */
		mco = mco.replace("\u0323", "");

		/*
		 * Special corner-case divergence from published standard. If the final tone on
		 * an open word vowel is ², mark it with a macron, as the normal unmarked tone
		 * is usually ⁴³.
		 */
		mco = mco.replaceAll("(?i)([aeiouv])²$", "$1\u0304");

		mco = mco.replaceAll("(?i)([aeiouv])²([^a-zɂ¹²³⁴:])", "$1\u0304$2");

		/*
		 * Convert the tones into combining diacritics
		 */
		for (String[] re : cedtones2mco) {
			mco = mco.replace(re[0], re[1]);
		}
		/*
		 * Finally, convert to fully composed form and return the MCO phonemic value.
		 */
		return Normalizer.normalize(mco, Normalizer.Form.NFC);
	}
}
