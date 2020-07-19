package com.cherokeelessons.bp.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.cherokeelessons.bp.BoundPronouns;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.util.JsonConverter;

public class BuildDeck {

	public static final int MIN_VSTEM_COUNT = 4;
	public static final int MIN_PFORM_VSTEM_COMBO_COUNT = 2;

	public static final int DECK_VERSION = 100;

	public static void main(final String[] args) throws FileNotFoundException, IOException {
		System.out.println("BUILD DECK");
		try {
			new BuildDeck(new File("../android/assets")).execute();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private final JsonConverter json = new JsonConverter();
	private List<String[]> pronouns = null;
	private final List<String[]> challenges = new ArrayList<>();
	private Deck deck;
	private String prevLatin = "";
	private String prevChr = "";

	private String status = "";

	private final File deckFile;
	private final File forEspeak;
	private final File checkSheet;

	public BuildDeck(final File assetsFolder) {
		deckFile = new File(assetsFolder, "deck.json");
		forEspeak = new File(assetsFolder, "espeak.tsv");
		checkSheet = new File(assetsFolder, "review-sheet.tsv");
	}

	private void addConjugatedChallengesToDeck() {
		final DataSet d = new DataSet();
		final Set<String> vtypes = new HashSet<>();

		final Iterator<String[]> ichallenge = challenges.iterator();
		while (ichallenge.hasNext()) {
			final String[] challenge = ichallenge.next();
			vtypes.clear();
			vtypes.addAll(Arrays.asList(challenge[0].split(",\\s*")));

			if (vtypes.contains("n")) {
				final String latinCitationEntry = challenge[3];
				final String chrCitationEntry = challenge[2];
				final String cardLookupKey = latinCitationEntry;
				setStatus("Please wait, adding term: " + chrCitationEntry);
				Card c = getCardByLatinChallenge(cardLookupKey, deck);
				if (c == null) {
					c = new Card();
					deck.cards.add(c);
					c.vgroup = chrCitationEntry;
					c.pgroup = "";
				}
				// chr
				c.challenge.add(chrCitationEntry);
				// latin
				c.challenge.add(latinCitationEntry);
				for (final String def : challenge[5].split(";")) {
					c.answer.add(StringUtils.strip(def));
				}
				continue;
			}

			String latinStemEntry = challenge[3];

			boolean v_g3rd = false;
			if (vtypes.contains("g")) {
				v_g3rd = true;
				vtypes.remove("g");
			}
			if (vtypes.contains("xde") && vtypes.contains("xwi")) {
				vtypes.remove("xde");
				vtypes.add("xdi");
			}
			final boolean vSetB = latinStemEntry.startsWith("u") || latinStemEntry.startsWith("ụ")
					|| latinStemEntry.startsWith("j");
			final String vroot_set = challenge[4];
			final String vroot_chr_set = challenge[2];
			final String defActiveVoice = challenge[5];
			final String defPassiveVoice = challenge[6];
			final String inanimateObjects = challenge[7];
			final String animateObjects = challenge[8];
			String vroot_aspirated = StringUtils.substringBefore(vroot_set, ",");
			String vroot_aspirated_chr = StringUtils.substringBefore(vroot_chr_set, ",");
			String vroot_deaspirated = StringUtils.substringAfter(vroot_set, ",");
			String vroot_deaspirated_chr = StringUtils.substringAfter(vroot_chr_set, ",");
			if (StringUtils.isBlank(vroot_deaspirated)) {
				vroot_deaspirated = vroot_aspirated;
			}
			if (StringUtils.isBlank(vroot_deaspirated_chr)) {
				vroot_deaspirated_chr = vroot_aspirated_chr;
			}
			vroot_aspirated = StringUtils.strip(vroot_aspirated);
			vroot_deaspirated = StringUtils.strip(vroot_deaspirated);
			vroot_aspirated_chr = StringUtils.strip(vroot_aspirated_chr);
			vroot_deaspirated_chr = StringUtils.strip(vroot_deaspirated_chr);

			boolean adjective = vtypes.contains("adj");

			boolean imperative;
			boolean infinitive;
			if (StringUtils.isBlank(defActiveVoice)) {
				imperative = defPassiveVoice.toLowerCase().startsWith("let") || vtypes.contains("imp");
				infinitive = defPassiveVoice.toLowerCase().startsWith("for") || vtypes.contains("inf");
			} else {
				imperative = defActiveVoice.toLowerCase().startsWith("let") || vtypes.contains("imp");
				infinitive = defActiveVoice.toLowerCase().startsWith("for") || vtypes.contains("inf");
			}

			final boolean useDiPrefixedForms = adjective || imperative || infinitive;

			final boolean gStem = vroot_aspirated.startsWith("ɂ");
			if (gStem) {
				vroot_aspirated = vroot_aspirated.substring(1);
				vroot_deaspirated = vroot_deaspirated.substring(1);
				vroot_aspirated_chr = vroot_aspirated_chr.substring(1);
				vroot_deaspirated_chr = vroot_deaspirated_chr.substring(1);
			}

			final boolean aStem = vroot_aspirated.matches("[ạaẠA].*");
			final boolean eStem = vroot_aspirated.matches("[ẹeẸE].*");
			final boolean iStem = vroot_aspirated.matches("[ịiỊI].*");
			final boolean oStem = vroot_aspirated.matches("[ọoỌO].*");
			final boolean uStem = vroot_aspirated.matches("[ụuỤU].*");
			final boolean vStem = vroot_aspirated.matches("[ṿvṾV].*");
			final boolean cStem = !(aStem | eStem | iStem | oStem | uStem | vStem);

			setStatus("Please wait, conjugating: " + vroot_aspirated_chr);
			final Iterator<String[]> ipro = pronouns.iterator();
			while (ipro.hasNext()) {
				final String[] pronoun = ipro.next();
				String pSetIndicator = pronoun[5];
				final boolean pSetA = pSetIndicator.equalsIgnoreCase("a");
				final boolean pSetB = pSetIndicator.equalsIgnoreCase("b");
				if (pSetB && !vSetB) {
					continue;
				}
				if (pSetA && vSetB) {
					continue;
				}

				final String pTypeSet = pronoun[0];
				final String pSyllabarySet = pronoun[1];
				final String pLatinSet = pronoun[2];
				final String pSyllabaryDiPrefixedSet = pronoun[6];
				final String pLatinDiPrefixedSet = pronoun[7];

				final Set<String> ptypes = new HashSet<>();
				ptypes.addAll(Arrays.asList(pTypeSet.split(",\\s*")));

				boolean deaspirate = ptypes.contains("alt") && !vroot_aspirated.equals(vroot_deaspirated);

				boolean p_g3rd = false;
				if (ptypes.contains("g")) {
					p_g3rd = true;
					ptypes.remove("g");
				}

				if (Collections.disjoint(vtypes, ptypes)) {
					continue;
				}
				final String vgroup;

				final StringBuilder vrootSb = new StringBuilder();
				final StringBuilder vrootChrSb = new StringBuilder();

				if (deaspirate) {
					vrootSb.append(vroot_deaspirated);
					vrootChrSb.append(vroot_deaspirated_chr);
				} else {
					vrootSb.append(vroot_aspirated);
					vrootChrSb.append(vroot_aspirated_chr);
				}

				vgroup = (gStem ? "ɂ" : "") + vrootChrSb.toString() + (deaspirate ? "*" : "");

				d.chr = pSyllabarySet;
				d.latin = pLatinSet;
				d.def = "";

				if (useDiPrefixedForms) {
					if (!StringUtils.isBlank(pSyllabaryDiPrefixedSet)) {
						d.chr = pSyllabaryDiPrefixedSet;
						d.latin = pLatinDiPrefixedSet;
					}
				}

				final String pgroup;
				if (d.chr.lastIndexOf(",") != d.chr.indexOf(",")) {
					pgroup = StringUtils.substringBeforeLast(d.chr, ",").trim();
				} else {
					pgroup = d.chr;
				}

				selectPronounForm(d, cStem, gStem, p_g3rd, v_g3rd);

				if ((imperative || infinitive) && aStem) {
					if (d.chr.equals("Ꮨ̣²")) {
						// game.log(this, "ti -> t");
						d.chr = "Ꮤ͓";
						d.latin = "t";
					}
					if (d.chr.equals("Ꮧ̣²")) {
						// game.log(this, "di -> d");
						d.chr = "Ꮣ͓";
						d.latin = "d";
					}
				}

				/*
				 * pronoun munge for vowel verb stems where we selected single use case
				 * consonent stem
				 */
				if (!cStem) {
					d.chr = d.chr.replaceAll(BoundPronouns.UNDERDOT + "?[¹²³⁴]$", BoundPronouns.UNDERX);
					d.latin = d.latin.replaceAll("[ẠAạaẸEẹeỊIịiỌOọoỤUụuṾVṿv][¹²³⁴]$", "");
				}

				/*
				 * "[d]" reflexive form fixups
				 */
				if (cStem) {
					d.chr = d.chr.replaceAll("\\[Ꮣ͓\\]$", "");
					d.latin = d.latin.replaceAll("\\[d\\]$", "");
				}
				if (aStem) {
					d.chr = d.chr.replaceAll("Ꮣ[¹²³⁴]\\[Ꮣ͓\\]$", "Ꮣ͓");
					d.latin = d.latin.replaceAll("da[¹²³⁴]\\[d\\]$", "d");
				}
				if (eStem || iStem || oStem || uStem) {
					d.chr = d.chr.replaceAll("\\[Ꮣ͓\\]$", "Ꮣ͓");
					d.chr = d.chr.replace("Ꮣ͓Ꭱ", "Ꮥ");
					d.chr = d.chr.replace("Ꮣ͓Ꭲ", "Ꮧ");
					d.chr = d.chr.replace("Ꮣ͓Ꭳ", "Ꮩ");
					d.chr = d.chr.replace("Ꮣ͓Ꭴ", "Ꮪ");

					d.latin = d.latin.replaceAll("\\[d\\]$", "d");
				}
				if (vStem) {
					d.chr = d.chr.replaceAll("\\[Ꮣ͓\\]$", "Ꮣ͓");
					d.chr = d.chr.replace("Ꮣ͓Ꭵ", "Ꮫ");
					d.latin = d.latin.replaceAll("\\[d\\]$", "d");
				}

				/*
				 * combine stem and pronouns together
				 */

				if (cStem) {

					if (vrootSb.toString().matches("[TtDdSs].*")) {
						if (d.latin.equalsIgnoreCase("A¹gị²")) {
							d.latin = "a¹k";
							d.chr = "Ꭰ¹Ꭹ͓";
						}
						if (d.latin.equalsIgnoreCase("Jạ²")) {
							d.latin = "ts";
							d.chr = "Ꮳ͓";
						}
					}
					// TODO: which is more correct? "ijch-" or "its-" ?
					if (vrootSb.toString().matches("[Tt].*")) {
						if (d.latin.equalsIgnoreCase("I¹ji²")) {
							d.latin = "i¹ts";
							d.chr = "Ꭲ¹Ꮵ͓";
						}
					}

					if (vrootSb.length() > 1) {
						String vrootLatin = vrootSb.toString();
						boolean tonedStem = vrootLatin.matches("[¹²³⁴].*");
						if (tonedStem) {
							d.chr = d.chr.replaceAll("[¹²³⁴]+$", "");
							d.latin = d.latin.replaceAll("[¹²³⁴]+$", "");
							if (vrootLatin.matches("(¹|²³|³²|⁴).*") && d.latin.matches("(?i).*[ạẹịọụṿ]")) {
								vrootLatin = vrootLatin.replaceAll("(¹|²³).*", "²");
								vrootLatin = vrootLatin.replaceAll("(³²|⁴).*", "³");
							}
						}
					}

					d.chr += vrootChrSb;
					// d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");

					d.latin += vrootSb;
					// d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
				}

				if (aStem) {
					u_check: {
						if (d.chr.equals("Ꭴ¹Ꮹ͓")) {
							d.chr = "Ꭴ" + vrootChrSb.substring(1);
							d.latin = "u" + vrootSb.substring(1);
							break u_check;
						}
						if (d.chr.equals("Ꮷ²Ꮹ͓")) {
							d.chr = "Ꮷ" + vrootChrSb.substring(1);
							d.latin = "ju" + vrootSb.substring(1);
							break u_check;
						}
						if (d.chr.equals("Ꮪ²Ꮹ͓")) {
							d.chr = "Ꮪ" + vrootChrSb.substring(1);
							d.latin = "du" + vrootSb.substring(1);
							break u_check;
						}
						d.chr += vrootChrSb;
						d.latin += vrootSb;
					}
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
				}

				if (eStem || iStem || oStem || uStem) {
					d.chr += vrootChrSb;
					// d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");

					d.latin += vrootSb;
					// d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
				}

				if (vStem) {
					u_check: {
						if (d.chr.equals("Ꭴ¹Ꮹ͓")) {
							d.chr = "Ꭴ̣²Ꮹ" + vrootChrSb.substring(1);
							d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
							d.latin = "ụ²wa" + vrootSb.substring(1);
							d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
							break u_check;
						}
						if (d.chr.equals("Ꮷ²Ꮹ͓")) {
							d.chr = "Ꮷ̣²Ꮹ" + vrootChrSb.substring(1);
							d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
							d.latin = "jụ²wa" + vrootSb.substring(1);
							d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
							break u_check;
						}
						if (d.chr.equals("Ꮪ²Ꮹ͓")) {
							d.chr = "Ꮪ̣²Ꮹ" + vrootChrSb.substring(1);
							d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
							d.latin = "dụ²wa" + vrootSb.substring(1);
							d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
							break u_check;
						}
						d.chr += vrootChrSb;
						d.latin += vrootSb;
					}
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "");
				}

				doSyllabaryConsonentVowelFixes(d);

				d.latin = d.latin.toLowerCase();
				String subj = pronoun[3];
				String pronounObject = pronoun[4];

				if (!ptypes.contains("de")) {
					if (!StringUtils.isBlank(subj) && isPluralSubj(subj)) {
						if (vtypes.contains("xde")) {
							addDePrefix(d);
						}
						if (vtypes.contains("xdi")) {
							addDiPrefix(d, aStem);
						}
					} else if (isPluralSubj(pronounObject)) {
						if (vtypes.contains("xde")) {
							addDePrefix(d);
						}
						if (vtypes.contains("xdi")) {
							addDiPrefix(d, aStem);
						}
					}
				}

				if (vtypes.contains("xwi")) {
					addWiPrefix(d);
				}

				if (imperative && !vtypes.contains("xwi")) {
					if (!isIncludesYou(subj, pronounObject)) {
						addWiPrefix(d);
					}
				}

				d.def = null;
				if (!StringUtils.isEmpty(subj)) {
					d.def = defActiveVoice;
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ", pronoun[3] + " ");
					}
					if (d.def.contains("self")) {
						d.def = d.def.replaceFirst("^[Hh]im", pronoun[3] + "-");
						d.def = d.def.replace("I-self", "Myself");
						d.def = d.def.replace("You one-self", "Your one self");
						d.def = d.def.replace("He-self", "Himself");
						d.def = d.def.replace("We-self", "Ourselves");
						d.def = d.def.replace("we-self", "ourselves");
						d.def = d.def.replace("You two-self", "Your two selves");
						d.def = d.def.replace("You all-self", "Your all selves");
						d.def = d.def.replace("They-self", "Themselves");
						d.def = d.def.replace("our-self", "ourselves");
					}
					if (d.def.matches("^His\\b.*")) {
						final String replaceFirst = d.def.replaceFirst("^His\\b", "");
						d.def = pronoun[8] + replaceFirst;
					}
					if (subj.contains("I")) {
						d.def = d.def.replace("[s]", "");
					}
					if (subj.contains("You one")) {
						d.def = d.def.replace("[s]", "");
					}
					if (isPluralSubj(subj)) {
						d.def = d.def.replace("[s]", "");
					} else {
						d.def = d.def.replace("[s]", "s");
					}
					if (d.def.startsWith("for him ") || d.def.startsWith("For him ")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase() + StringUtils.substring(subj, 1);
						}
						d.def = d.def.replaceFirst("^[Ff]or him ", "For " + subj + " ");
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase() + StringUtils.substring(subj, 1);
						}
						d.def = d.def.replaceFirst("^[Ll]et him ", "Let " + subj + " ");
					}

					if (!StringUtils.isBlank(inanimateObjects)) {
						final String[] o = inanimateObjects.split(",\\s*");
						if (pronounObject.equalsIgnoreCase("them (inanimate)") && o.length > 1) {
							String verbObject = o[1];
							d.def = d.def.replaceAll("\\bx\\b", verbObject);
						} else if (pronounObject.equalsIgnoreCase("it")) {
							String verbObject = o[0];
							d.def = d.def.replaceAll("\\bx\\b", verbObject);
						} else {
							d.def = d.def.replaceAll("\\bx\\b", pronounObject);
						}
					} else if (!StringUtils.isBlank(animateObjects)) {
						final String[] o = animateObjects.split(",\\s*");
						if (pronounObject.equalsIgnoreCase("them (animate)") && o.length > 1) {
							String verbObject = o[1];
							d.def = d.def.replaceAll("\\bx\\b", verbObject);
						} else if (pronounObject.equalsIgnoreCase("him")) {
							String verbObject = o[0];
							d.def = d.def.replaceAll("\\bx\\b", verbObject);
						} else {
							d.def = d.def.replaceAll("\\bx\\b", pronounObject);
						}
					} else {
						d.def = d.def.replaceAll("\\bx\\b", pronounObject);
					}

				} else {
					d.def = defPassiveVoice;
					if (d.def.toLowerCase().startsWith("someone") || d.def.toLowerCase().startsWith("for someone")) {
						if (d.def.contains(" him")) {
							d.def = d.def.replaceFirst(" him", " " + pronounObject);
						} else if (d.def.contains(" me")) {
							d.def = d.def.replaceFirst(" me", " " + pronounObject);
						}
					}
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ", pronounObject + " ");
					}
					if (pronounObject.contains("I")) {
						d.def = d.def.replace("[s]", "");
					}
					if (isPluralSubj(pronounObject)) {
						d.def = d.def.replace("[s]", "");
					} else {
						d.def = d.def.replace("[s]", "s");
					}
					if (d.def.startsWith("for him ") || d.def.startsWith("For him ")) {
						d.def = d.def.replaceFirst("^[Ff]or him ", "For " + pronounObject + " ");
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!pronounObject.startsWith("I")) {
							pronounObject = (StringUtils.left(pronounObject, 1).toLowerCase()
									+ StringUtils.substring(pronounObject, 1));
						}
						d.def = d.def.replaceFirst("^[Ll]et him ", "Let " + pronounObject + " ");
					}
				}
				Card c = getCardByLatinChallenge(d.latin, deck);
				if (c == null) {
					c = new Card();
					deck.cards.add(c);
					c.vgroup = vgroup;
					c.pgroup = pgroup;
					c.challenge.add(d.chr);
					c.challenge.add(d.latin);
				}

				definitionEnglishFixer(d);

				if (c.answer.contains(d.def)) {
					System.err.println("BUILD DECK WARNING! DUPLICATE DEFINITION: " + d.chr + ", " + d.def);
				} else {
					c.answer.add(d.def);
				}
				mergeSelectedDefinitions(c.answer);
			}
		}
		setStatus("Finished conjugating ...");
	}

	private void mergeSelectedDefinitions(List<String> answers) {
		Set<String> forMerging = new HashSet<>();
		for (String answer: answers) {
			if (answer.contains(" (animate)")) {
				String tmp = answer.replace(" (animate)", " (inanimate)");
				if (answers.contains(tmp)) {
					forMerging.add(answer);
					continue;
				}
			}
			if (answer.contains(" (inanimate)")) {
				String tmp = answer.replace(" (inanimate)", " (animate)");
				if (answers.contains(tmp)) {
					forMerging.add(answer);
					continue;
				}
			}
		}
		if (forMerging.size()>1) {
			answers.removeAll(forMerging);
			String tmp = forMerging.iterator().next();
			tmp = tmp.replace(" (animate)", "").trim();
			tmp = tmp.replace(" (inanimate)", "").trim();
			answers.add(tmp);
		}
	}

	private void selectPronounForm(DataSet d, boolean cStem, boolean gStem, boolean p_g3rd, boolean v_g3rd) {

		/*
		 * a vs ga select
		 */
		if (v_g3rd && p_g3rd) {
			d.chr = StringUtils.substringAfter(d.chr, ",");
			d.chr = StringUtils.strip(d.chr, " -");
			d.latin = StringUtils.substringAfter(d.latin, ",");
			d.latin = StringUtils.strip(d.latin, " -");
			return;
		}

		/*
		 * a vs ga select
		 */
		if (!v_g3rd && p_g3rd) {
			d.chr = StringUtils.substringBefore(d.chr, ",");
			d.chr = StringUtils.strip(d.chr, " -");
			d.latin = StringUtils.substringBefore(d.latin, ",");
			d.latin = StringUtils.strip(d.latin, " -");
			return;
		}

		final String syllabary = d.chr;
		final String latin = d.latin;

		if (syllabary.contains(",")) {
			String tmpSyllabary = syllabary;
			String pSylConsonant = StringUtils.substringBefore(tmpSyllabary, ",").trim();
			tmpSyllabary = StringUtils.substringAfter(tmpSyllabary, ",").trim();
			String pSylVowel = StringUtils.substringBefore(tmpSyllabary, ",").trim();
			tmpSyllabary = StringUtils.substringAfter(tmpSyllabary, ",").trim();
			String pSylGlottal = tmpSyllabary.trim();
			if (pSylVowel.isEmpty()) {
				pSylVowel = pSylConsonant;
			}
			if (pSylGlottal.isEmpty()) {
				if (pSylVowel.endsWith("Ꮿ" + BoundPronouns.UNDERX + "-")) {
					pSylGlottal = pSylVowel.toLowerCase().replace("Ꮿ" + BoundPronouns.UNDERX + "-", "Ꮿ²-");
				} else if (pSylVowel.endsWith("Ꮹ" + BoundPronouns.UNDERX + "-")) {
					pSylGlottal = pSylVowel.toLowerCase().replace("Ꮹ" + BoundPronouns.UNDERX + "-", "Ꮹ²-");
				} else {
					pSylGlottal = pSylConsonant;
				}
			}
			if (gStem) {
				d.chr = pSylGlottal;
			} else if (cStem) {
				d.chr = pSylConsonant;
			} else {
				d.chr = pSylVowel;
			}
		} else {
			d.chr = syllabary;
		}
		if (latin.contains(",")) {
			String tmpLatin = latin;
			String pLatConsonant = StringUtils.substringBefore(tmpLatin, ",").trim();
			tmpLatin = StringUtils.substringAfter(tmpLatin, ",").trim();
			String pLatVowel = StringUtils.substringBefore(tmpLatin, ",").trim();
			tmpLatin = StringUtils.substringAfter(tmpLatin, ",").trim();
			String pLatGlottal = tmpLatin.trim();
			if (pLatVowel.isEmpty()) {
				pLatVowel = pLatConsonant;
			}
			if (pLatGlottal.isEmpty()) {
				if (pLatVowel.endsWith("y-")) {
					pLatGlottal = pLatVowel.toLowerCase().replace("y-", "ya²-");
				} else if (pLatVowel.endsWith("w-") && !pLatVowel.endsWith("gw-") && !pLatVowel.endsWith("kw-")) {
					pLatGlottal = pLatVowel.toLowerCase().replace("w-", "wa²-");
				} else {
					pLatGlottal = pLatConsonant;
				}
			}
			if (gStem) {
				d.latin = pLatGlottal;
			} else if (cStem) {
				d.latin = pLatConsonant;
			} else {
				d.latin = pLatVowel;
			}
		} else {
			d.latin = latin;
		}
		d.chr = StringUtils.strip(d.chr, " -");
		d.latin = StringUtils.strip(d.latin, " -");
	}

	private void addDePrefix(final DataSet d) {
		if (d.latin.matches("[d].*")) {
			return;
		}
		if (d.latin.matches("[ạa].*")) {
			d.latin = ("d" + d.latin);
			d.chr = ("Ꮣ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = ("d" + d.latin);
			d.chr = ("Ꮥ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = ("d" + d.latin);
			d.chr = ("Ꮩ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = ("d" + d.latin);
			d.chr = ("Ꮪ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = ("d" + d.latin);
			d.chr = ("Ꮫ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = ("de³" + d.latin.substring(2));
			d.chr = ("Ꮥ³" + d.chr.substring(2));
			return;
		}

		d.latin = ("de²" + d.latin);
		d.chr = ("Ꮥ²" + d.chr);

	}

	private void addDiPrefix(final DataSet d, final boolean aStem) {
		if (d.latin.matches("[d].*")) {
			return;
		}
		if (d.latin.matches("[ạ].*") && aStem) {
			d.latin = ("dạ" + d.latin.substring(1));
			d.chr = ("Ꮣ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[a].*") && aStem) {
			d.latin = ("da" + d.latin.substring(1));
			d.chr = ("Ꮣ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ạa].*")) {
			d.latin = ("dị" + d.latin.substring(1));
			d.chr = ("Ꮧ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = ("j" + d.latin);
			d.chr = ("Ꮴ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = ("j" + d.latin);
			d.chr = ("Ꮶ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = ("j" + d.latin);
			d.chr = ("Ꮷ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = ("j" + d.latin);
			d.chr = ("Ꮸ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = ("d" + d.latin);
			d.chr = ("Ꮧ" + d.chr.substring(1));
			return;
		}

		d.latin = ("dị²" + d.latin);
		d.chr = ("Ꮧ̣²" + d.chr);

	}

	public void addWiPrefix(final DataSet d) {
		if (d.latin.matches("[ạa].*")) {
			d.latin = ("w" + d.latin);
			d.chr = ("Ꮹ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = ("w" + d.latin);
			d.chr = ("Ꮺ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = ("w" + d.latin);
			d.chr = ("Ꮼ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = ("w" + d.latin);
			d.chr = ("Ꮽ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = ("w" + d.latin);
			d.chr = ("Ꮾ" + d.chr.substring(1));
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = ("w" + d.latin);
			d.chr = ("Ꮻ" + d.chr.substring(1));
			return;
		}
		d.latin = ("wị²" + d.latin);
		d.chr = ("Ꮻ̣²" + d.chr);
	}

	private void appendText(final File tmp, final String text) throws FileNotFoundException, IOException {
		try (FileOutputStream fos = new FileOutputStream(tmp, true)) {
			fos.write(text.getBytes(Charset.forName("UTF-8")));
			fos.close();
		}
	}

	private String asFilename(String challenge) {
		challenge = challenge.replace("ɂ", "-");

		challenge = challenge.replace("¹", "1");
		challenge = challenge.replace("²", "2");
		challenge = challenge.replace("³", "3");
		challenge = challenge.replace("⁴", "4");

		challenge = challenge.replace("a", "aa");
		challenge = challenge.replace("e", "ee");
		challenge = challenge.replace("i", "ii");
		challenge = challenge.replace("o", "oo");
		challenge = challenge.replace("u", "uu");
		challenge = challenge.replace("v", "vv");

		challenge = challenge.replace("ạ", "a");
		challenge = challenge.replace("ẹ", "e");
		challenge = challenge.replace("ị", "i");
		challenge = challenge.replace("ọ", "o");
		challenge = challenge.replace("ụ", "u");
		challenge = challenge.replace("ṿ", "v");

		challenge = challenge.replaceAll("(?i)[^a-z1234\\-]", "");

		challenge = challenge.replaceAll("([cdghjklmnstwy])([aeiouv])([aeiouv])([cdghjklmnstwy\\-])", "$1$2$4");
		challenge = challenge.replaceAll("^(.*)([aeiouv])([aeiouv])([1234]+)?$", "$1$2$4");

		return challenge;
	}

	private String asPlainSyllabary(String syllabary) {
		syllabary = syllabary.replace(BoundPronouns.UNDERDOT, "");
		syllabary = syllabary.replace(BoundPronouns.UNDERX, "");
		syllabary = syllabary.replaceAll("[¹²³⁴]", "");
		return syllabary;
	}

	public void definitionEnglishFixer(final DataSet d) {
		d.def = StringUtils.left(d.def, 1).toUpperCase() + StringUtils.substring(d.def, 1);

		d.def = d.def.replaceAll("\\b([Uu]s)(, .*?)( recently)", "$1$3$2");

		d.def = d.def.replaceAll("([Ww]e)( .*? | )is ", "$1$2are ");
		d.def = d.def.replaceAll("([Ww]e)( .*? | )was ", "$1$2were ");
		d.def = d.def.replaceAll("([Ww]e)( .*? | )has ", "$1$2have ");
		
		d.def = d.def.replaceAll("([Yy]ou)( .*? | )is ", "$1$2are ");
		d.def = d.def.replaceAll("([Yy]ou)( .*? | )was ", "$1$2were ");
		d.def = d.def.replaceAll("([Yy]ou)( .*? | )has ", "$1$2have ");

		d.def = d.def.replaceAll(" all has ", " all have ");
		d.def = d.def.replaceAll(" two has ", " two have ");
		d.def = d.def.replaceAll(" both has ", " both have ");

		d.def = d.def.replace("and I is", "and I are");
		d.def = d.def.replace("and I usually is", "and I usually am");
		d.def = d.def.replace("I is", "I am");
		d.def = d.def.replace("You one is", "You one are");
		d.def = d.def.replace("You two is", "You two are");
		d.def = d.def.replace("You all is", "You all are");
		d.def = d.def.replace("They is", "They are");

		d.def = d.def.replace("and I was", "and I were");
		d.def = d.def.replace("You one was", "You one were");
		d.def = d.def.replace("You two was", "You two were");
		d.def = d.def.replace("You all was", "You all were");
		d.def = d.def.replace("They was", "They were");

		d.def = d.def.replace("and I often is", "and I often are");
		d.def = d.def.replace("I often is", "I often am");
		d.def = d.def.replace("You one often is", "You one often are");
		d.def = d.def.replace("You two often is", "You two often are");
		d.def = d.def.replace("You all often is", "You all often are");
		d.def = d.def.replace("They often is", "They often are");
		
		d.def = d.def.replace("and I usually is", "and I usually are");
		d.def = d.def.replace("I usually is", "I usually am");
		d.def = d.def.replace("You one usually is", "You one usually are");
		d.def = d.def.replace("You two usually is", "You two usually are");
		d.def = d.def.replace("You all usually is", "You all usually are");
		d.def = d.def.replace("They usually is", "They usually are");

		d.def = d.def.replace("and I has", "and I have");
		d.def = d.def.replace("I has", "I have");
		d.def = d.def.replace("You one has", "You one have");
		d.def = d.def.replace("You two has", "You two have");
		d.def = d.def.replace("You all has", "You all have");
		d.def = d.def.replace("They has", "They have");

		d.def = d.def.replace("and I often has", "and I often have");
		d.def = d.def.replace("I often has", "I often have");
		d.def = d.def.replace("You one often has", "You one often have");
		d.def = d.def.replace("You two often has", "You two often have");
		d.def = d.def.replace("You all often has", "You all often have");
		d.def = d.def.replace("They often has", "They often have");
		
		d.def = d.def.replace("and I usually has", "and I usually have");
		d.def = d.def.replace("I usually has", "I usually have");
		d.def = d.def.replace("You one usually has", "You one usually have");
		d.def = d.def.replace("You two usually has", "You two usually have");
		d.def = d.def.replace("You all usually has", "You all usually have");
		d.def = d.def.replace("They usually has", "They usually have");

		if (d.def.startsWith("Someone")) {
			d.def = d.def.replaceAll("\\b[Hh]e\\b", "him");
			d.def = d.def.replaceAll("\\b[Tt]hey\\b", "them");
			d.def = d.def.replaceAll("\\bI\\b", "me");
			d.def = d.def.replaceAll("\\bYou\\b", "you");
			// d.def = d.def.replaceAll("For (.*?), [Ww]e\\b", "For $1");
			d.def = d.def.replaceAll(", [Ww]e$", "");
		}
		if (d.def.startsWith("For someone")) {
			d.def = d.def.replaceAll("\\b[Hh]e\\b", "him");
			d.def = d.def.replaceAll("\\b[Tt]hey\\b", "them");
			d.def = d.def.replaceAll("\\bI\\b", "me");
			d.def = d.def.replaceAll("\\bYou\\b", "you");
			// d.def = d.def.replaceAll("For (.*?), [Ww]e\\b", "For $1");
			d.def = d.def.replaceAll(", [Ww]e$", "");
		}
		if (d.def.startsWith("For")) {
			d.def = d.def.replaceAll("\\b[Hh]e\\b", "him");
			d.def = d.def.replaceAll("\\b[Tt]hey\\b", "them");
			d.def = d.def.replaceAll("\\bI\\b", "me");
			d.def = d.def.replaceAll("\\bYou\\b", "you");
			d.def = d.def.replaceAll("For (.*?), [Ww]e\\b", "For us, $1,");
		}

		if (d.def.startsWith("Let")) {
			d.def = d.def.replaceAll("Let [Hh]e\\b", "Let him");
			d.def = d.def.replaceAll("Let [Tt]hey\\b", "Let them");
			d.def = d.def.replaceAll("Let You\\b", "Let you");
			d.def = d.def.replaceAll("Let I\\b", "Let me");
			d.def = d.def.replaceAll("and I\\b", "and me");
			d.def = d.def.replaceAll("Let You\\b", "Let you");
			d.def = d.def.replaceAll("Let (.*?), we\\b", "Let us, $1,");
		}

		/**
		 * Idiom fixup: (recognize => acquainted)
		 */
		if (!d.def.contains("often")) {
			if (d.def.startsWith("Let")) {
				d.def = d.def.replace("recognize each other", "be acquainted");
			}
			if (d.def.startsWith("For")) {
				d.def = d.def.replace("recognize each other", "be acquainted");
			}
			d.def = d.def.replace("recognized each other recently", "were acquainted recently");
			d.def = d.def.replace("recognized each other a while ago", "were acquainted a while ago");
			d.def = d.def.replace("recognize each other", "are acquainted");
		}

		/**
		 * Final replacements on the replacements for more succinct English.
		 */
		d.def = d.def.replace("You all and I, we", "All of us");
		d.def = d.def.replace("You one and I, we", "You one and I");
		d.def = d.def.replace("He and I, we", "He and I");

		d.def = d.def.replace("They and I, we", "They and I");

		d.def = d.def.replace("us, you all and me,", "all of us");
		d.def = d.def.replace("us, you all and me", "all of us");

		d.def = d.def.replace("us, them and me,", "them and me");
		d.def = d.def.replace("us, them and me", "them and me");

		d.def = d.def.replace("us, you one and me,", "you one and me");
		d.def = d.def.replace("us, you one and me", "you one and me");

		d.def = d.def.replace("us, him and me,", "him and me");
		d.def = d.def.replace("us, him and me", "him and me");

		/**
		 * English grammar fixes.
		 */
		d.def = d.def.replace("Let we", "Let us");
		d.def = d.def.replace("let we", "let us");

		/**
		 * Final replacements.
		 */
		d.def = d.def.replace("you one or you two", "you one or both");
		d.def = d.def.replace("You one or you two", "You one or both");

		d.def = d.def.replace("you one or two", "you one or both");
		d.def = d.def.replace("You one or two", "You one or both");

		d.def = d.def.replace("you one", "you (1)");
		d.def = d.def.replace("You one", "You (1)");

		d.def = d.def.replace("you two", "you both");
		d.def = d.def.replace("You two", "You both");
	}

	private void doSyllabaryConsonentVowelFixes(final DataSet d) {
		final boolean debug = false;// d.chr.endsWith("Ꭱ³Ꭶ");
		if (debug) {
			System.out.println("Build Deck: " + d.chr);
		}
		final String x = BoundPronouns.UNDERX;
		if (!d.chr.contains(x)) {
			return;
		}
		if (!d.chr.matches(".*" + x + "[ᎠᎡᎢᎣᎤᎥ].*")) {
			return;
		}
		// special case for u + v => uwa
		d.chr = d.chr.replace("Ꭴ¹Ꮹ͓Ꭵ", "Ꭴ̣²Ꮹ");

		String set;
		set = "[Ꭰ-Ꭵ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭰ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭱ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭲ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꭳ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꭴ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꭵ");
		if (debug) {
			System.out.println("Build Deck: " + d.chr);
		}

		set = "[Ꭷ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭷ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭸ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭹ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꭺ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꭻ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꭼ");

		set = "[ᎦᎨᎩᎪᎫᎬ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭶ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭸ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭹ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꭺ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꭻ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꭼ");

		set = "[ᎭᎮᎯᎰᎱᎲ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭽ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭾ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭿ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮀ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮁ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮂ");

		set = "[ᎾᏁᏂᏃᏄᏅ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮎ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮑ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮒ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮓ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮔ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮕ");

		set = "[Ꮏ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮏ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮑ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮒ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮓ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮔ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮕ");

		set = "[ᏣᏤᏥᏦᏧᏨ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮳ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮴ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮵ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮶ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮷ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮸ");

		set = "[ᏆᏇᏈᏉᏊᏋ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮖ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮗ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮘ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮙ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮚ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮛ");

		set = "[ᏓᏕᏗᏙᏚᏛ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮣ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮥ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮧ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮩ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮪ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮫ");

		set = "[ᏔᏖᏘ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮤ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮦ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮨ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮩ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮪ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮫ");

		set = "[ᏩᏪᏫᏬᏭᏮ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮹ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮺ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮻ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮼ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮽ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮾ");

		set = "[ᏯᏰᏱᏲᏳᏴ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮿ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ᏸ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ᏹ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ᏺ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ᏻ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ᏼ");
		if (debug) {
			System.out.println("Build Deck: " + d.chr);
		}
	}

	private Deck oldDeck = null;

	public void execute() throws FileNotFoundException, IOException {
		if (deckFile.exists()) {
			try {
				oldDeck = json.fromJson(deckFile, Deck.class);
			} catch (final Exception e) {
				// ignore
			}
		}

		deck = new Deck();

		loadPronouns();

		/*
		 * Add bare pronoun cards to deck.
		 */
		final Iterator<String[]> ipronoun = pronouns.iterator();
		while (ipronoun.hasNext()) {
			final String[] pronounRecord = ipronoun.next();
			String chr = pronounRecord[1];
			if (chr.lastIndexOf(",") != chr.indexOf(",")) {
				chr = StringUtils.substringBeforeLast(chr, ",").trim();
			}
			String pgroup = chr;
			String latin = pronounRecord[2];
			if (latin.lastIndexOf(",") != latin.indexOf(",")) {
				latin = StringUtils.substringBeforeLast(latin, ",").trim();
			}
			/*
			 * Strip out "[" and "]" that are in the reflexive forms for pronoun card
			 * challenges ...
			 */
			chr = chr.replace("[", "").replace("]", "");
			latin = latin.replace("[", "").replace("]", "");
			setStatus("Create pronoun card for " + chr);
			String defin = pronounRecord[3] + " + " + pronounRecord[4];
			if (StringUtils.isBlank(pronounRecord[3])) {
				final String tmp = pronounRecord[4];
				passive: {
					defin = tmp;
					if (tmp.equalsIgnoreCase("he")) {
						defin += " (being)";
						break passive;
					}
					if (tmp.equalsIgnoreCase("i")) {
						defin += " (being)";
						break passive;
					}
					defin += " (being)";
					break passive;
				}
			}
			if (StringUtils.isBlank(latin)) {
				latin = prevLatin;
			}
			if (StringUtils.isBlank(chr)) {
				chr = prevChr;
			}

			Card c = getCardByLatinChallenge(chr.toString(), deck);
			if (c == null) {
				c = new Card();
				c.pgroup = pgroup;
				c.vgroup = "";
				c.challenge.add(chr.toString());
				c.challenge.add(latin.toString());
				deck.cards.add(c);
			}
			c.answer.add(defin);
			prevChr = chr;
			prevLatin = latin;
		}

		addConjugatedChallengesToDeck();
		
		mergeSelvesAndEachOther();

		setStatus("Saving ...");

		sortThenSaveDeck();
	}

	private void mergeSelvesAndEachOther() {
		for (Card card: deck.cards) {
			if (card.answer.size()<2) {
				continue;
			}
			boolean hasEachOther=false;
			for (String a: card.answer) {
				if (a.toLowerCase().contains("each other")) {
					hasEachOther=true;
					break;
				}
			}
			if (!hasEachOther) {
				continue;
			}
			Iterator<String> iter = card.answer.iterator();
			while (iter.hasNext()) {
				final String next = iter.next();
				if (next.toLowerCase().contains("selves")) {
					iter.remove();
					continue;
				}
			}
		}
	}

	private Card getCardByLatinChallenge(final String latin, @SuppressWarnings("hiding") final Deck deck) {
		for (final Card card : deck.cards) {
			if (card.challenge.get(1).equalsIgnoreCase(latin)) {
				return card;
			}
		}
		return null;
	}

	public String getStatus() {
		return status;
	}

	private boolean isIncludesYou(String subj, final String obj) {
		if (StringUtils.isBlank(subj)) {
			subj = obj;
		}
		subj = subj.toLowerCase();
		return subj.matches(".*\\byou\\b.*");
	}

	public boolean isPluralSubj(final String subj) {
		boolean pluralSubj = subj.contains(" and");
		pluralSubj |= subj.startsWith("they");
		pluralSubj |= subj.startsWith("They");
		pluralSubj |= subj.contains(" two");
		pluralSubj |= subj.contains(" both");
		pluralSubj |= subj.contains(" all");
		return pluralSubj;
	}

	private void loadPronouns() throws IOException {
		pronouns = new ArrayList<>();
		FileInputStream tsv = new FileInputStream("../android/assets/tsv/pronouns-list-tab.tsv");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(tsv, Charset.forName("UTF-8")))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				final String[] copyOf = Arrays.copyOf(line.split("\t"), 9);
				for (int i = 0; i < copyOf.length; i++) {
					copyOf[i] = copyOf[i] == null ? "" : copyOf[i];
				}
				pronouns.add(copyOf);
			}
		}
		final Iterator<String[]> ipro = pronouns.iterator();
		while (ipro.hasNext()) {
			final String[] pronoun = ipro.next();
			final String vtmode = StringUtils.strip(pronoun[0]);
			final String syllabary = StringUtils.strip(pronoun[1]);
			if (StringUtils.isBlank(vtmode)) {
				// game.log(this, "Skipping: "+vtmode+" - "+syllabary);
				ipro.remove();
				continue;
			}
			if (vtmode.startsWith("#")) {
				// game.log(this, "Skipping: "+vtmode+" - "+syllabary);
				ipro.remove();
				continue;
			}
			if (syllabary.startsWith("#")) {
				// game.log(this, "Skipping: "+vtmode+" - "+syllabary);
				ipro.remove();
				continue;
			}
		}
		tsv = new FileInputStream("../android/assets/tsv/challenges-tab.tsv");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(tsv, Charset.forName("UTF-8")))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				final String[] copyOf = Arrays.copyOf(line.split("\t"), 9);
				for (int i = 0; i < copyOf.length; i++) {
					copyOf[i] = copyOf[i] == null ? "" : copyOf[i];
				}
				challenges.add(copyOf);
			}
		}

		final Iterator<String[]> ichallenge = challenges.iterator();
		while (ichallenge.hasNext()) {
			final String[] challenge = ichallenge.next();
			final String vtmode = StringUtils.strip(challenge[0]);
			if (StringUtils.isBlank(vtmode)) {
				// game.log(this, "Skipping: "+vtmode);
				ichallenge.remove();
				continue;
			}
			if (vtmode.startsWith("#")) {
				// game.log(this, "Skipping: "+vtmode);
				ichallenge.remove();
				continue;
			}
		}
	}

	private void sortThenSaveDeck() throws FileNotFoundException, IOException {
		/*
		 * Presort deck by challenge length ascending, then challenge value ascending
		 */
		Collections.sort(deck.cards, new Comparator<Card>() {
			@Override
			public int compare(final Card a, final Card b) {
				String v1 = a.vgroup.trim();
				String v2 = b.vgroup.trim();
				if (v1.isEmpty() != v2.isEmpty()) {
					if (v1.isEmpty()) {
						return -1;
					}
					return 1;
				}
				String c1 = a.challenge.get(0);
				String c2 = b.challenge.get(0);
				int s1 = c1.replaceAll("[¹²³⁴]", "").length();
				int s2 = c2.replaceAll("[¹²³⁴]", "").length();
				if (s1 != s2) {
					return Integer.compare(s1, s2);
				}
				return c1.compareToIgnoreCase(c2);
			}
		});
		/*
		 * assign psets based on order and pronoun unique counts
		 */
		final Map<String, AtomicInteger> pronounCounts = new HashMap<>();
		for (final Card card : deck.cards) {
			final String pgroup = card.pgroup;
			if (!pronounCounts.containsKey(pgroup)) {
				pronounCounts.put(pgroup, new AtomicInteger(1));
			}
			if (!StringUtils.isBlank(card.vgroup)) {
				card.setPset(pronounCounts.get(pgroup).incrementAndGet());
			}
		}
		/*
		 * resort deck by assigned pset, vset length, and challenge length
		 */
		Collections.sort(deck.cards, new Comparator<Card>() {
			@Override
			public int compare(final Card a, final Card b) {
				String c1 = a.challenge.get(0);
				int p1 = a.getPset();
				String v1 = a.vgroup;
				String c2 = b.challenge.get(0);
				int p2 = b.getPset();
				String v2 = b.vgroup;
				if (p1 != p2) {
					return p1 - p2;
				}
				if (v1.length()!=v2.length()) {
					return Integer.compare(v1.length(), v2.length());
				}
				if (c1.length() != c2.length()) {
					return Integer.compare(c1.length(), c2.length());
				}
				return v1.compareToIgnoreCase(v2);
			}
		});
		/*
		 * Assign vsets based on order and verb unique counts
		 */
		final Map<String, AtomicInteger> verbStemCounts = new HashMap<>();
		for (final Card card : deck.cards) {
			final String vset = card.vgroup;
			if (!vset.isEmpty()) {
				if (!verbStemCounts.containsKey(vset)) {
					verbStemCounts.put(vset, new AtomicInteger());
				}
				card.setVset(verbStemCounts.get(vset).incrementAndGet());
			}
		}
		// resort deck by "p group" then "v group"
		Collections.sort(deck.cards, new Comparator<Card>() {
			@Override
			public int compare(Card a, Card b) {
				if (a.getPset() != b.getPset()) {
					return a.getPset() - b.getPset();
				}
				return a.getVset() - b.getVset();
			}
		});
		// assign ids based on card positions in the deck
		for (int i = 0; i < deck.cards.size(); i++) {
			deck.cards.get(i).id = i + 1;
		}

		reduceDeckSize(deck);
		/*
		 * Assign new IDs on the reduced set of cards
		 */
		System.out.println("Assigning final IDs");
		int id=0;
		for (Card card: deck.cards) {
			card.id=++id;
		}
		System.out.println("Final id: "+id);

		deck.version = DECK_VERSION;
		deck.size = deck.cards.size();

		/*
		 * Only rewrite deck file if the master deck has changed.
		 */
		if (!deck.equals(oldDeck)) {
			System.out.println(deck.cards.size() + " cards in deck to save.");
			json.toJson(deckFile, deck);
		}

		if (forEspeak.exists()) {
			forEspeak.delete();
		}

		if (checkSheet.exists()) {
			checkSheet.delete();
		}

		appendText(checkSheet, "ID\tPSET\tVSET\tPRONOUN\tVERB\tCHALLENGE\t\tANSWER\n");

		final Set<String> already = new HashSet<>();
		final StringBuilder espeak = new StringBuilder();
		final StringBuilder check = new StringBuilder();
		int maxAnswers = 0;
		for (final Card card : deck.cards) {
			maxAnswers = Math.max(maxAnswers, card.answer.size());
		}
		for (final Card card : deck.cards) {
			final String syllabary;
			if (card.challenge.size() > 0) {
				syllabary = asPlainSyllabary(StringUtils.defaultString(card.challenge.get(0)));
			} else {
				syllabary = "";
			}
			final String challenge;
			if (card.challenge.size() > 1) {
				challenge = StringUtils.defaultString(card.challenge.get(1));
			} else {
				challenge = "";
			}
			if (challenge.trim().endsWith("-")) {
				continue;
			}
			espeak.append(syllabary);
			espeak.append("\t");
			espeak.append(challenge);
			espeak.append("\t");
			final String asFilename;
			if (!challenge.isEmpty() && !challenge.endsWith("-")) {
				asFilename = asFilename(challenge);
				espeak.append(asFilename);
			} else {
				asFilename = "";
			}
			espeak.append("\n");

			appendText(forEspeak, espeak.toString());

			espeak.setLength(0);

			check.append(card.id);
			check.append("\t");
			check.append(card.getPset());
			check.append("\t");
			check.append(card.getVset());
			check.append("\t");
			check.append(card.pgroup);
			check.append("\t");
			check.append(card.vgroup);
			check.append("\t");
			check.append(syllabary);
			check.append("\t");
			check.append(challenge);

			for (int ix = 0; ix < maxAnswers; ix++) {
				check.append("\t");
				if (card.answer.size() > ix) {
					check.append(card.answer.get(ix));
				}
			}
			check.append("\n");
			appendText(checkSheet, check.toString());
			check.setLength(0);

			if (!challenge.isEmpty() && already.contains(challenge)) {
				throw new RuntimeException("DUPLICATE CHALLENGE: " + challenge);
			}
			already.add(challenge);
			if (!asFilename.isEmpty() && already.contains(asFilename)) {
				throw new RuntimeException("DUPLICATE FILENAME: " + asFilename);
			}
			already.add(asFilename);
		}
	}

	/**
	 * Reduce deck size while keeping a mandatory set of bound pronouns and
	 * spreading the usage of the remainder pronouns as much as possible.
	 * 
	 * @param deckToFilter
	 */
	private void reduceDeckSize(Deck deckToFilter) {
		Set<String> alwaysKeep = new HashSet<>();
		alwaysKeep.addAll(Arrays.asList(//
				"", //
				"Ꮵ²-, Ꮵ²Ꮿ͓-", //
				"Ꮵ̣²-, Ꭶ͓-", //
				"Ꭰ¹Ꭹ̣²-, Ꭰ¹Ꮖ͓-", //
				"Ꭿ²-, Ꭿ²Ꮿ͓-", //
				"Ꭿ̣²-", "Ꮳ̣²-", //
				"Ꭰ̣²-, Ꭶ̣²-", //
				"Ꭴ¹-, Ꭴ¹Ꮹ͓-"));

		/*
		 * split out into buckets based on bound pronoun and first letter of vstem and
		 * whether the stem is deaspirated
		 */
		List<Card> tmpDeck = new ArrayList<>(deckToFilter.cards);
		Map<String, List<Card>> bucketsByPronoun = new HashMap<>();
		Iterator<Card> iter = tmpDeck.iterator();
		while (iter.hasNext()) {
			Card card = iter.next();
			String pgroup = card.pgroup;// pgroupBucket(card);
			String asPlainSyllabary = asPlainSyllabary(card.vgroup);
			if (!asPlainSyllabary.isEmpty()) {
				pgroup += " " + asPlainSyllabary.substring(0, 1);
			}
			if (card.vgroup.endsWith("*")) {
				pgroup+="*";
			}
			if (!bucketsByPronoun.containsKey(pgroup)) {
				bucketsByPronoun.put(pgroup, new ArrayList<Card>());
			}
			/*
			 * Certain cards are in the always keep set.
			 */
			if (!alwaysKeep.contains(card.pgroup)) {
				bucketsByPronoun.get(pgroup).add(card);
			}
		}

		/*
		 * Sort each bucket by vset desc, pset desc to weight for removal the most
		 * common occurrences first
		 */
		for (List<Card> bucket : bucketsByPronoun.values()) {
			Collections.sort(bucket, new Comparator<Card>() {
				@Override
				public int compare(Card a, Card b) {
					if (a.getVset() == b.getVset()) {
						return b.getPset() - a.getPset();
					}
					return b.getVset() - a.getVset();
				}
			});
		}

		System.out.println("Reducing final deck size");

		tmpDeck.clear();
		/**
		 * Work from pronoun sets with largest to smallest for removals. <br>
		 * Work to keep at least a set minimum of each pronoun's pronunciation
		 * variations, as well as to keep at least a set minimum of each verb stem with
		 * non-core conjugations.
		 */

		while (!bucketsByPronoun.isEmpty()) {
			final Map<String, AtomicInteger> stemCounts = countsPerVerbStem(bucketsByPronoun.values());
			for (List<Card> bucket : bucketsByPronoun.values()) {
				bucket.removeIf(new Predicate<Card>() {
					@Override
					public boolean test(Card card) {
						return stemCounts.get(card.vgroup).get() <= MIN_VSTEM_COUNT;
					}
				});
			}
			bucketsByPronoun.values().removeIf(new Predicate<List<Card>>() {
				@Override
				public boolean test(List<Card> bucket) {
					return bucket.size() <= MIN_PFORM_VSTEM_COMBO_COUNT;
				}
			});
			List<List<Card>> sorted = new ArrayList<>(bucketsByPronoun.values());
			Collections.sort(sorted, new Comparator<List<Card>>() {
				@Override
				public int compare(List<Card> a, List<Card> b) {
					return b.size() - a.size();
				}
			});
			if (!sorted.isEmpty()) {
				List<Card> biggestBucket = sorted.get(0);
				tmpDeck.add(biggestBucket.remove(0));
			}
		}

		/*
		 * single shot removal
		 */
		System.out.println("Removing: " + tmpDeck.size());
		deckToFilter.cards.removeAll(tmpDeck);

		/*
		 * rebuild buckets with cards for use to get a basic report of counts by bound
		 * pronoun set
		 */
		bucketsByPronoun.clear();
		tmpDeck = new ArrayList<>(deckToFilter.cards);
		iter = tmpDeck.iterator();
		while (iter.hasNext()) {
			Card card = iter.next();
			String pgroup = card.pgroup;
			int ib = pgroup.indexOf("[");
			if (ib != -1) {
				pgroup = pgroup.replaceAll("\\[.*\\]", "");
			}
			int ix = pgroup.indexOf("-");
			if (ix > 0) {
				pgroup = card.challenge.get(0).substring(0, ix - 1) + "|" + pgroup;
			}
			if (!bucketsByPronoun.containsKey(pgroup)) {
				bucketsByPronoun.put(pgroup, new ArrayList<Card>());
			}
			bucketsByPronoun.get(pgroup).add(0, card);
		}

		System.out.println("Final deck size: " + (deckToFilter.cards.size()));

		/*
		 * Dump a review report showing the counts by prefix set.
		 */
		StringBuilder sb = new StringBuilder();
		for (String bucket : new TreeSet<>(bucketsByPronoun.keySet())) {
			if (bucket.isEmpty()) {
				sb.append("FIXED WORDS: ");
			} else {
				sb.append(bucket + ": ");
			}
			sb.append(NumberFormat.getInstance().format(bucketsByPronoun.get(bucket).size()));
			sb.append("\n");
		}
	}

	private String pgroupBucket(Card card) {
		String pgroup = card.pgroup;
		pgroup = pgroup.replaceAll("\\[.*\\]", "");
		int ix = pgroup.indexOf("-");
		if (ix != -1 && card.challenge.get(0).length() > ix) {
			pgroup = card.challenge.get(0).substring(0, ix - 1) + "|" + pgroup;
		} else {
			System.out.println("pgroup: " + pgroup + " card: " + card.challenge.get(0));
		}
		return pgroup;
	}

	private Map<String, AtomicInteger> countsPerVerbStem(Collection<List<Card>> buckets) {
		/*
		 * calculate counts per verb stem across all buckets
		 */
		final Map<String, AtomicInteger> stemCounts = new HashMap<>();
		buckets.forEach(new Consumer<List<Card>>() {
			@Override
			public void accept(List<Card> list) {
				list.forEach(new Consumer<Card>() {
					@Override
					public void accept(Card card) {
						if (!stemCounts.containsKey(card.vgroup)) {
							stemCounts.put(card.vgroup, new AtomicInteger());
						}
						stemCounts.get(card.vgroup).incrementAndGet();
					}
				});
			}
		});
		return stemCounts;
	}

	private void setStatus(final String string) {
		System.out.println(" - " + string);
		this.status = string;
	}
}
