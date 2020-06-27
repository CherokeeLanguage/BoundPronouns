package com.cherokeelessons.bp.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.cherokeelessons.bp.BoundPronouns;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.util.JsonConverter;

public class BuildDeck {

	public static final int DECK_VERSION = 100;

	public static void main(final String[] args) throws FileNotFoundException, IOException {
		System.out.println("BUILD DECK");
		new BuildDeck(new File("../android/assets")).execute();
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
		forEspeak = new File(assetsFolder, "espeak.txt");
		checkSheet = new File(assetsFolder, "review-sheet.tsv");
	}

	private void addChallengesToDeck() {
		final DataSet d = new DataSet();
		final StringBuilder vroot = new StringBuilder();
		final StringBuilder vroot_chr = new StringBuilder();
		final Set<String> vtypes = new HashSet<>();
		final Set<String> ptypes = new HashSet<>();
		final Iterator<String[]> ichallenge = challenges.iterator();
		while (ichallenge.hasNext()) {
			final String[] challenge = ichallenge.next();
			vtypes.clear();
			vtypes.addAll(Arrays.asList(challenge[0].split(",\\s*")));

			if (vtypes.contains("n")) {
				final String term = challenge[2];
				setStatus("Please wait, adding term: " + term);
				Card c = getCardByChallenge(term, deck);
				if (c == null) {
					c = new Card();
					deck.cards.add(c);
				}
				c.vgroup = term;
				c.pgroup = "";
				// chr
				c.challenge.add(term);
				// latin
				c.challenge.add(challenge[3]);
				for (final String def : challenge[5].split(";")) {
					c.answer.add(StringUtils.strip(def));
				}
				continue;
			}

			boolean v_g3rd = false;
			if (vtypes.contains("g")) {
				v_g3rd = true;
				vtypes.remove("g");
			}
			if (vtypes.contains("xde") && vtypes.contains("xwi")) {
				vtypes.remove("xde");
				vtypes.add("xdi");
			}
			final boolean vSetB = challenge[3].startsWith("u") || challenge[3].startsWith("ụ")
					|| challenge[3].startsWith("j");
			final String vroot_set = challenge[4];
			final String vroot_chr_set = challenge[2];
			final String vdef_active = challenge[5];
			final String vdef_passive = challenge[6];
			final String vdef_objects = challenge[7];
			String vroot_h = StringUtils.substringBefore(vroot_set, ",").intern();
			String vroot_h_chr = StringUtils.substringBefore(vroot_chr_set, ",").intern();
			String vroot_alt = StringUtils.substringAfter(vroot_set, ",").intern();
			String vroot_alt_chr = StringUtils.substringAfter(vroot_chr_set, ",").intern();
			if (StringUtils.isBlank(vroot_alt)) {
				vroot_alt = vroot_h;
			}
			if (StringUtils.isBlank(vroot_alt_chr)) {
				vroot_alt_chr = vroot_h_chr;
			}
			vroot_h = StringUtils.strip(vroot_h);
			vroot_alt = StringUtils.strip(vroot_alt);
			vroot_h_chr = StringUtils.strip(vroot_h_chr);
			vroot_alt_chr = StringUtils.strip(vroot_alt_chr);

			boolean v_imp = vdef_active.toLowerCase().startsWith("let");
			boolean v_inf = vdef_active.toLowerCase().startsWith("for");
			if (StringUtils.isBlank(vdef_active)) {
				v_imp = vdef_passive.toLowerCase().startsWith("let");
				v_inf = vdef_passive.toLowerCase().startsWith("for");
			}
			final boolean use_di_prefixed_forms = vtypes.contains("adj") || v_imp || v_inf;

			final boolean aStem = vroot_h.matches("[ạaẠA].*");
			final boolean eStem = vroot_h.matches("[ẹeẸE].*");
			final boolean iStem = vroot_h.matches("[ịiỊI].*");
			final boolean oStem = vroot_h.matches("[ọoỌO].*");
			final boolean uStem = vroot_h.matches("[ụuỤU].*");
			final boolean vStem = vroot_h.matches("[ṿvṾV].*");
			final boolean cStem = !(aStem | eStem | iStem | oStem | uStem | vStem);

			final String vgroup = vroot_h_chr;

			setStatus("Please wait, conjugating: " + vroot_h_chr);
			final Iterator<String[]> ipro = pronouns.iterator();
			while (ipro.hasNext()) {
				final String[] pronoun = ipro.next();
				final boolean pSetB = pronoun[5].equalsIgnoreCase("b");
				final boolean pSetA = pronoun[5].equalsIgnoreCase("a");
				if (pSetB && !vSetB) {
					continue;
				}
				if (pSetA && vSetB) {
					continue;
				}
				final String vtmode = pronoun[0];
				final String syllabary = pronoun[1];
				ptypes.clear();
				ptypes.addAll(Arrays.asList(vtmode.split(",\\s*")));

				boolean p_g3rd = false;
				if (ptypes.contains("g")) {
					p_g3rd = true;
					ptypes.remove("g");
				}

				if (Collections.disjoint(vtypes, ptypes)) {
					continue;
				}
				vroot.setLength(0);
				vroot_chr.setLength(0);
				if (ptypes.contains("alt")) {
					vroot.append(vroot_alt);
					vroot_chr.append(vroot_alt_chr);
				} else {
					vroot.append(vroot_h);
					vroot_chr.append(vroot_h_chr);
				}

				d.chr = syllabary;
				d.latin = pronoun[2];
				d.def = "";

				final String pgroup = d.chr;

				if (use_di_prefixed_forms) {
					if (!StringUtils.isBlank(pronoun[6])) {
						d.chr = pronoun[6];
						d.latin = pronoun[7];
					}
				}

				/*
				 * a vs ga select
				 */
				if (v_g3rd && p_g3rd) {
					d.chr = StringUtils.substringAfter(d.chr, ",").intern();
					d.chr = StringUtils.strip(d.chr).intern();
					d.latin = StringUtils.substringAfter(d.latin, ",").intern();
					d.latin = StringUtils.strip(d.latin).intern();
				}

				/*
				 * a vs ga select
				 */
				if (!v_g3rd && p_g3rd) {
					d.chr = StringUtils.substringBefore(d.chr, ",").intern();
					d.chr = StringUtils.strip(d.chr).intern();
					d.latin = StringUtils.substringBefore(d.latin, ",").intern();
					d.latin = StringUtils.strip(d.latin).intern();
				}

				if (!cStem && d.chr.contains(",")) {
					/*
					 * select vowel stem pronoun
					 */
					d.chr = StringUtils.substringAfter(d.chr, ",").intern();
					d.chr = StringUtils.substringBefore(d.chr, "-").intern();
					d.chr = StringUtils.strip(d.chr).intern();

					d.latin = StringUtils.substringAfter(d.latin, ",").intern();
					d.latin = StringUtils.substringBefore(d.latin, "-").intern();
					d.latin = StringUtils.strip(d.latin).intern();

				} else {
					/*
					 * select consonent stem pronoun
					 */
					d.chr = StringUtils.substringBefore(d.chr, ",").intern();
					d.chr = StringUtils.substringBefore(d.chr, "-").intern();
					d.chr = StringUtils.strip(d.chr).intern();
					d.latin = StringUtils.substringBefore(d.latin, ",").intern();
					d.latin = StringUtils.substringBefore(d.latin, "-").intern();
					d.latin = StringUtils.strip(d.latin).intern();
				}

				if ((v_imp || v_inf) && aStem) {
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
					d.chr = d.chr.replaceAll(BoundPronouns.UNDERDOT + "?[¹²³⁴]$", BoundPronouns.UNDERX).intern();
					d.latin = d.latin.replaceAll("[ẠAạaẸEẹeỊIịiỌOọoỤUụuṾVṿv][¹²³⁴]$", "").intern();
				}

				/*
				 * "[d]" reflexive form fixups
				 */
				if (cStem) {
					d.chr = d.chr.replaceAll("\\[Ꮣ͓\\]$", "").intern();
					d.latin = d.latin.replaceAll("\\[d\\]$", "").intern();
				}
				if (aStem) {
					d.chr = d.chr.replaceAll("Ꮣ[¹²³⁴]\\[Ꮣ͓\\]$", "Ꮣ͓").intern();
					d.latin = d.latin.replaceAll("da[¹²³⁴]\\[d\\]$", "d").intern();
				}
				if (eStem || iStem || oStem || uStem) {
					d.chr = d.chr.replaceAll("\\[Ꮣ͓\\]$", "Ꮣ͓").intern();
					d.chr = d.chr.replace("Ꮣ͓Ꭱ", "Ꮥ").intern();
					d.chr = d.chr.replace("Ꮣ͓Ꭲ", "Ꮧ").intern();
					d.chr = d.chr.replace("Ꮣ͓Ꭳ", "Ꮩ").intern();
					d.chr = d.chr.replace("Ꮣ͓Ꭴ", "Ꮪ").intern();

					d.latin = d.latin.replaceAll("\\[d\\]$", "d").intern();
				}
				if (vStem) {
					d.chr = d.chr.replaceAll("\\[Ꮣ͓\\]$", "Ꮣ͓").intern();
					d.chr = d.chr.replace("Ꮣ͓Ꭵ", "Ꮫ").intern();
					d.latin = d.latin.replaceAll("\\[d\\]$", "d").intern();
				}

				/*
				 * combine stem and pronouns together
				 */

				if (cStem) {

					if (vroot.toString().matches("[TtDdSs].*")) {
						if (d.latin.equalsIgnoreCase("A¹gị²")) {
							d.latin = "a¹k";
							d.chr = "Ꭰ¹Ꭹ͓";
						}
						if (d.latin.equalsIgnoreCase("Jạ²")) {
							d.latin = "ts";
							d.chr = "Ꮳ͓";
						}
					}
					if (vroot.toString().matches("[Tt].*")) {
						if (d.latin.equalsIgnoreCase("I¹ji²")) {
							d.latin = "i¹jch";
							d.chr = "Ꭲ¹Ꮵ͓";
						}
					}

					if (vroot.length() > 1) {
						final char vroot_0 = vroot.charAt(0);
						final char vroot_1 = vroot.charAt(1);
						if (vroot_0 == 'ɂ' && // glottal stop followed by tone marking
								(vroot_1 == '¹' || vroot_1 == '²' || vroot_1 == '³' || vroot_1 == '⁴')) {
							d.chr = d.chr.replaceAll("[¹²³⁴]+$", "");
							if (!d.chr.endsWith(BoundPronouns.UNDERDOT)) {
								d.chr += BoundPronouns.UNDERDOT;
							}
						}
					}

					d.chr += vroot_chr;
					// d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();

					d.latin += vroot;
					// d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
				}

				if (aStem) {
					u_check: {
						if (d.chr.equals("Ꭴ¹Ꮹ͓")) {
							d.chr = "Ꭴ" + vroot_chr.substring(1);
							d.latin = "u" + vroot.substring(1);
							break u_check;
						}
						if (d.chr.equals("Ꮷ²Ꮹ͓")) {
							d.chr = "Ꮷ" + vroot_chr.substring(1);
							d.latin = "ju" + vroot.substring(1);
							break u_check;
						}
						if (d.chr.equals("Ꮪ²Ꮹ͓")) {
							d.chr = "Ꮪ" + vroot_chr.substring(1);
							d.latin = "du" + vroot.substring(1);
							break u_check;
						}
						d.chr += vroot_chr;
						d.latin += vroot;
					}
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
				}

				if (eStem || iStem || oStem || uStem) {
					d.chr += vroot_chr;
					// d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();

					d.latin += vroot;
					// d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
				}

				if (vStem) {
					u_check: {
						if (d.chr.equals("Ꭴ¹Ꮹ͓")) {
							d.chr = "Ꭴ̣²Ꮹ" + vroot_chr.substring(1);
							d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
							d.latin = "ụ²wa" + vroot.substring(1);
							d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
							break u_check;
						}
						if (d.chr.equals("Ꮷ²Ꮹ͓")) {
							d.chr = "Ꮷ̣²Ꮹ" + vroot_chr.substring(1);
							d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
							d.latin = "jụ²wa" + vroot.substring(1);
							d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
							break u_check;
						}
						if (d.chr.equals("Ꮪ²Ꮹ͓")) {
							d.chr = "Ꮪ̣²Ꮹ" + vroot_chr.substring(1);
							d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
							d.latin = "dụ²wa" + vroot.substring(1);
							d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
							break u_check;
						}
						d.chr += vroot_chr;
						d.latin += vroot;
					}
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ[¹²³⁴])", "").intern();
				}

				doSyllabaryConsonentVowelFixes(d);

				d.latin = d.latin.toLowerCase().intern();
				String subj = pronoun[3];
				String obj = pronoun[4];

				if (!StringUtils.isBlank(subj) && isPluralSubj(subj)) {
					if (vtypes.contains("xde")) {
						addDePrefix(d);
					}
					if (vtypes.contains("xdi")) {
						addDiPrefix(d, aStem);
					}
				} else if (isPluralSubj(obj)) {
					if (vtypes.contains("xde")) {
						addDePrefix(d);
					}
					if (vtypes.contains("xdi")) {
						addDiPrefix(d, aStem);
					}
				}

				if (vtypes.contains("xwi")) {
					addWiPrefix(d);
				}

				if (v_imp && !vtypes.contains("xwi")) {
					if (!isIncludesYou(subj, obj)) {
						addWiPrefix(d);
					}
				}

				d.def = null;
				if (!StringUtils.isEmpty(subj)) {
					d.def = vdef_active;
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ", pronoun[3] + " ").intern();
					}
					if (d.def.contains("self")) {
						d.def = d.def.replaceFirst("^[Hh]im", pronoun[3] + "-").intern();
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
						d.def = d.def.replace("[s]", "").intern();
					}
					if (subj.contains("You one")) {
						d.def = d.def.replace("[s]", "").intern();
					}
					if (isPluralSubj(subj)) {
						d.def = d.def.replace("[s]", "").intern();
					} else {
						d.def = d.def.replace("[s]", "s").intern();
					}
					if (d.def.startsWith("for him ") || d.def.startsWith("For him ")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase() + StringUtils.substring(subj, 1).intern();
						}
						d.def = d.def.replaceFirst("^[Ff]or him ", "For " + subj + " ").intern();
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase() + StringUtils.substring(subj, 1).intern();
						}
						d.def = d.def.replaceFirst("^[Ll]et him ", "Let " + subj + " ").intern();
					}
					if (!StringUtils.isBlank(vdef_objects)) {
						final String[] o = vdef_objects.split(",\\s*");
						String vobj = o[0];
						if (o.length > 1 && obj.contains("them")) {
							vobj = o[1];
						}
						d.def = d.def.replaceAll("\\bx\\b", vobj).intern();
					} else {
						d.def = d.def.replaceAll("\\bx\\b", obj).intern();
					}

				} else {
					d.def = vdef_passive;
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ", obj + " ").intern();
					}
					if (obj.contains("I")) {
						d.def = d.def.replace("[s]", "").intern();
					}
					if (isPluralSubj(obj)) {
						d.def = d.def.replace("[s]", "").intern();
					} else {
						d.def = d.def.replace("[s]", "s").intern();
					}
					if (d.def.startsWith("for him ") || d.def.startsWith("For him ")) {
						d.def = d.def.replaceFirst("^[Ff]or him ", "For " + obj + " ").intern();
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!obj.startsWith("I")) {
							obj = (StringUtils.left(obj, 1).toLowerCase() + StringUtils.substring(obj, 1)).intern();
						}
						d.def = d.def.replaceFirst("^[Ll]et him ", "Let " + obj + " ").intern();
					}
				}
				Card c = getCardByChallenge(d.chr, deck);
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

			}
		}
		setStatus("Finished conjugating ...");
	}

	private void addDePrefix(final DataSet d) {
		if (d.latin.matches("[ạa].*")) {
			d.latin = ("d" + d.latin).intern();
			d.chr = ("Ꮣ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = ("d" + d.latin).intern();
			d.chr = ("Ꮥ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = ("d" + d.latin).intern();
			d.chr = ("Ꮩ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = ("d" + d.latin).intern();
			d.chr = ("Ꮪ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = ("d" + d.latin).intern();
			d.chr = ("Ꮫ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = ("de³" + d.latin.substring(2)).intern();
			d.chr = ("Ꮥ³" + d.chr.substring(2)).intern();
			return;
		}

		d.latin = ("de²" + d.latin).intern();
		d.chr = ("Ꮥ²" + d.chr).intern();

	}

	private void addDiPrefix(final DataSet d, final boolean aStem) {
		if (d.latin.matches("[ạ].*") && aStem) {
			d.latin = ("dạ" + d.latin.substring(1)).intern();
			d.chr = ("Ꮣ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[a].*") && aStem) {
			d.latin = ("da" + d.latin.substring(1)).intern();
			d.chr = ("Ꮣ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ạa].*")) {
			d.latin = ("dị" + d.latin.substring(1)).intern();
			d.chr = ("Ꮧ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = ("j" + d.latin).intern();
			d.chr = ("Ꮴ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = ("j" + d.latin).intern();
			d.chr = ("Ꮶ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = ("j" + d.latin).intern();
			d.chr = ("Ꮷ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = ("j" + d.latin).intern();
			d.chr = ("Ꮸ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = ("d" + d.latin).intern();
			d.chr = ("Ꮧ" + d.chr.substring(1)).intern();
			return;
		}

		d.latin = ("dị²" + d.latin).intern();
		d.chr = ("Ꮧ̣²" + d.chr).intern();

	}

	public void addWiPrefix(final DataSet d) {
		if (d.latin.matches("[ạa].*")) {
			d.latin = ("w" + d.latin).intern();
			d.chr = ("Ꮹ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = ("w" + d.latin).intern();
			d.chr = ("Ꮺ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = ("w" + d.latin).intern();
			d.chr = ("Ꮼ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = ("w" + d.latin).intern();
			d.chr = ("Ꮽ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = ("w" + d.latin).intern();
			d.chr = ("Ꮾ" + d.chr.substring(1)).intern();
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = ("w" + d.latin).intern();
			d.chr = ("Ꮻ" + d.chr.substring(1)).intern();
			return;
		}
		d.latin = ("wị²" + d.latin).intern();
		d.chr = ("Ꮻ̣²" + d.chr).intern();
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

		d.def = d.def.replaceAll("([Ww]e)( .*? | )is ", "$1$2are ").intern();
		d.def = d.def.replaceAll("([Ww]e)( .*? | )was ", "$1$2were ").intern();
		d.def = d.def.replaceAll("([Ww]e)( .*? | )has ", "$1$2have ").intern();

		d.def = d.def.replace("and I is", "and I are").intern();
		d.def = d.def.replace("I is", "I am").intern();
		d.def = d.def.replace("You one is", "You one are").intern();
		d.def = d.def.replace("You two is", "You two are").intern();
		d.def = d.def.replace("You all is", "You all are").intern();
		d.def = d.def.replace("They is", "They are").intern();

		d.def = d.def.replace("and I was", "and I were").intern();
		d.def = d.def.replace("You one was", "You one were").intern();
		d.def = d.def.replace("You two was", "You two were").intern();
		d.def = d.def.replace("You all was", "You all were").intern();
		d.def = d.def.replace("They was", "They were").intern();

		d.def = d.def.replace("and I often is", "and I often are").intern();
		d.def = d.def.replace("I often is", "I often am").intern();
		d.def = d.def.replace("You one often is", "You one often are").intern();
		d.def = d.def.replace("You two often is", "You two often are").intern();
		d.def = d.def.replace("You all often is", "You all often are").intern();
		d.def = d.def.replace("They often is", "They often are").intern();

		d.def = d.def.replace("and I has", "and I have").intern();
		d.def = d.def.replace("I has", "I have").intern();
		d.def = d.def.replace("You one has", "You one have").intern();
		d.def = d.def.replace("You two has", "You two have").intern();
		d.def = d.def.replace("You all has", "You all have").intern();
		d.def = d.def.replace("They has", "They have").intern();

		d.def = d.def.replace("and I often has", "and I often have").intern();
		d.def = d.def.replace("I often has", "I often have").intern();
		d.def = d.def.replace("You one often has", "You one often have").intern();
		d.def = d.def.replace("You two often has", "You two often have").intern();
		d.def = d.def.replace("You all often has", "You all often have").intern();
		d.def = d.def.replace("They often has", "They often have").intern();

		if (d.def.startsWith("For")) {
			d.def = d.def.replaceAll("\\b[Hh]e\\b", "him");
			d.def = d.def.replaceAll("\\b[Tt]hey\\b", "them");
			d.def = d.def.replaceAll("\\bI\\b", "me");
			d.def = d.def.replaceAll("\\bYou\\b", "you");
			d.def = d.def.replaceAll("For (.*?), [Ww]e\\b", "For us, $1,").intern();
		}

		if (d.def.startsWith("Let")) {
			d.def = d.def.replaceAll("Let [Hh]e\\b", "Let him").intern();
			d.def = d.def.replaceAll("Let [Tt]hey\\b", "Let them").intern();
			d.def = d.def.replaceAll("Let You\\b", "Let you").intern();
			d.def = d.def.replaceAll("Let I\\b", "Let me").intern();
			d.def = d.def.replaceAll("and I\\b", "and me").intern();
			d.def = d.def.replaceAll("Let You\\b", "Let you").intern();
			d.def = d.def.replaceAll("Let (.*?), we\\b", "Let us, $1,").intern();
		}

		/**
		 * Idiom fixup: (recognize => acquainted)
		 */
		if (!d.def.contains("often")) {
			if (d.def.startsWith("Let")) {
				d.def = d.def.replace("recognize each other", "become acquainted");
			}
			if (d.def.startsWith("For")) {
				d.def = d.def.replace("recognize each other", "become acquainted");
			}
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
		d.chr = d.chr.replace("Ꭴ¹Ꮹ͓Ꭵ", "Ꭴ̣²Ꮹ").intern();

		String set;
		set = "[Ꭰ-Ꭵ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭰ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭱ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭲ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꭳ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꭴ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꭵ").intern();
		if (debug) {
			System.out.println("Build Deck: " + d.chr);
		}

		set = "[Ꭷ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭷ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭸ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭹ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꭺ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꭻ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꭼ").intern();

		set = "[ᎦᎨᎩᎪᎫᎬ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭶ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭸ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭹ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꭺ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꭻ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꭼ").intern();

		set = "[ᎭᎮᎯᎰᎱᎲ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭽ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭾ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭿ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮀ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮁ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮂ").intern();

		set = "[ᎾᏁᏂᏃᏄᏅ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮎ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮑ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮒ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮓ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮔ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮕ").intern();

		set = "[Ꮏ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮏ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮑ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮒ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮓ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮔ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮕ").intern();

		set = "[ᏣᏤᏥᏦᏧᏨ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮳ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮴ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮵ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮶ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮷ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮸ").intern();

		set = "[ᏆᏇᏈᏉᏊᏋ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮖ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮗ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮘ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮙ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮚ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮛ").intern();

		set = "[ᏓᏕᏗᏙᏚᏛ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮣ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮥ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮧ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮩ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮪ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮫ").intern();

		set = "[ᏔᏖᏘ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮤ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮦ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮨ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮩ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮪ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮫ").intern();

		set = "[ᏩᏪᏫᏬᏭᏮ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮹ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꮺ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꮻ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꮼ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꮽ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꮾ").intern();

		set = "[ᏯᏰᏱᏲᏳᏴ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꮿ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ᏸ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ᏹ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ᏺ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ᏻ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ᏼ").intern();
		if (debug) {
			System.out.println("Build Deck: " + d.chr);
		}
	}

	private Deck oldDeck=null;
	
	public void execute() throws FileNotFoundException, IOException {
		if (deckFile.exists()) {
			try {
				oldDeck = json.fromJson(deckFile, Deck.class);
			} catch (final Exception e) {
				//ignore
			}
		}
		
		deck = new Deck();
		
		loadPronouns();

		final Iterator<String[]> ipronoun = pronouns.iterator();
		while (ipronoun.hasNext()) {
			final String[] pronounRecord = ipronoun.next();
			String chr = pronounRecord[1];
			String latin = pronounRecord[2];
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

			Card c = getCardByChallenge(chr.toString(), deck);
			if (c == null) {
				c = new Card();
				c.pgroup = chr;
				c.vgroup = "";
				c.challenge.add(chr.toString());
				c.challenge.add(latin.toString());
				deck.cards.add(c);
			}
			c.answer.add(defin);
			prevChr = chr;
			prevLatin = latin;
		}

		addChallengesToDeck();

		setStatus("Saving ...");

		save();
	}

	private Card getCardByChallenge(final String chr, @SuppressWarnings("hiding") final Deck deck) {
		for (final Card card : deck.cards) {
			if (card.challenge.get(0).equalsIgnoreCase(chr)) {
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
		pluralSubj |= subj.contains(" all");
		return pluralSubj;
	}

	private void loadPronouns() throws IOException {
		pronouns = new ArrayList<>();
		FileInputStream csv = new FileInputStream("../android/assets/csv/pronouns-list-tab.tsv");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, Charset.forName("UTF-8")))) {
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
		csv = new FileInputStream("../android/assets/csv/challenges-tab.tsv");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, Charset.forName("UTF-8")))) {
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

	private void save() throws FileNotFoundException, IOException {
		// presort deck by syllabary length, ascending
		Collections.sort(deck.cards, new Comparator<Card>() {
			@Override
			public int compare(final Card a, final Card b) {
				final String c1 = a.challenge.get(0);
				final String c2 = b.challenge.get(0);
				if (c1.length() != c2.length()) {
					return Integer.compare(c1.length(), c2.length());
				}
				return c1.compareToIgnoreCase(c2);
			}
		});
		// assign sets based on order and pronoun + verb set combination
		final Map<String, AtomicInteger> counts = new HashMap<>();
		for (final Card card : deck.cards) {
			final String pset = card.pgroup;
			final String vset = card.vgroup;
			if (!counts.containsKey(pset)) {
				counts.put(pset, new AtomicInteger());
			}
			if (!counts.containsKey(vset)) {
				counts.put(vset, new AtomicInteger());
			}
			card.setPset(counts.get(pset).incrementAndGet());
			card.setVset(counts.get(vset).incrementAndGet());
		}
		// resort deck
		Collections.sort(deck.cards);
		// assign ids based on card positions in the deck
		for (int i = 0; i < deck.cards.size(); i++) {
			deck.cards.get(i).id = i + 1;
		}
		
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
		
		final Set<String> already = new HashSet<>();
		final StringBuilder sb = new StringBuilder();
		final StringBuilder check = new StringBuilder();
		int maxAnswers = 0;
		for (final Card card : deck.cards) {
			maxAnswers = Math.max(maxAnswers, card.answer.size());
		}
		for (final Card card : deck.cards) {
			if (card.challenge.size() < 2) {
				continue;
			}
			final String syllabary = asPlainSyllabary(StringUtils.defaultString(card.challenge.get(0)));
			final String challenge = StringUtils.defaultString(card.challenge.get(1));
			if (challenge.trim().endsWith("-")) {
				continue;
			}
			sb.append(syllabary);
			sb.append("\t");
			sb.append(challenge);
			sb.append("\t");
			final String asFilename = asFilename(challenge);
			sb.append(asFilename);
			sb.append("\n");

			appendText(forEspeak, sb.toString());

			sb.setLength(0);

			check.append(card.getPset());
			check.append("\t");
			check.append(card.getVset());
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

			if (already.contains(challenge)) {
				throw new RuntimeException("DUPLICATE CHALLENGE: " + challenge);
			}
			already.add(challenge);
			if (already.contains(asFilename)) {
				throw new RuntimeException("DUPLICATE FILENAME: " + asFilename);
			}
			already.add(asFilename);
		}
	}

	private void setStatus(final String string) {
		System.out.println(" - " + string);
		this.status = string;
	}
}
