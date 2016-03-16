package com.cherokeelessons.bp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.util.JsonConverter;

public class BuildDeck implements Runnable {

	private static final boolean forceRebuild = false;

	public static int version = 61;

	private JsonConverter json = new JsonConverter();
	private List<String[]> pronouns = null;
	private final List<String[]> challenges = new ArrayList<>();
	private final Deck deck;
	private String prevLatin = "";
	private String prevChr = "";
	private String status = "";

	private Card getCardByChallenge(String chr, Deck deck) {
		for (Card card : deck.cards) {
			if (card.challenge.get(0).equals(chr)) {
				return card;
			}
		}
		return null;
	}

	public String getStatus() {
		return status;
	}

	private final BoundPronouns game;
	private final Runnable done;
	private final FileHandle dest;

	public BuildDeck(BoundPronouns game, FileHandle slot, Runnable done) {
		this.game = game;
		this.done = done;
		this.deck = game.deck;
		dest = slot.child("deck.json");
	}

	public Runnable save = new Runnable() {
		@Override
		public void run() {
			Collections.sort(deck.cards);
			for (int i = 0; i < deck.cards.size(); i++) {
				deck.cards.get(i).id = i + 1;
			}
			deck.version = version;
			game.log(this, deck.cards.size() + " cards in deck.");
			deck.size = deck.cards.size();
			json.toJson(deck, dest);
			if (done != null) {
				Gdx.app.postRunnable(done);
			}
		}
	};

	@Override
	public void run() {
		long tick = System.currentTimeMillis();
		work: {
			if (forceRebuild) {
				if (dest.exists()) {
					dest.delete();
				}
			}
			if (dest.exists()) {
				Deck deck;
				try {
					deck = json.fromJson(Deck.class, dest);
				} catch (Exception e) {
					deck = new Deck();
				}
				if (deck.version == version) {
					game.deck.cards.clear();
					game.deck.cards.addAll(deck.cards);
					deck.cards.clear();
					game.deck.size = deck.size;
					game.deck.version = deck.version;
					game.log(this, game.deck.cards.size() + " cards in deck.");
					if (done != null) {
						Gdx.app.postRunnable(done);
					}
					deck = null;
					return;
				}
				dest.delete();
			}
			if (pronouns==null) {
				try {
					init();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				break work;
			}
			if (challenges.size() != 0) {
				addChallengesToDeck();
				break work;
			}
			if (pronouns.size() == 0) {
				setStatus("Saving ...");
				Gdx.app.postRunnable(save);
				return;
			}
			Iterator<String[]> ipronoun = pronouns.iterator();
			while (ipronoun.hasNext()) {
				String[] pronounRecord = ipronoun.next();
				ipronoun.remove();
				String chr = pronounRecord[1];
				String latin = pronounRecord[2];
				/*
				 * Strip out "[" and "]" that are in the reflexive forms for
				 * pronoun card challenges ...
				 */
				chr = chr.replace("[", "").replace("]", "");
				latin = latin.replace("[", "").replace("]", "");				
				setStatus("Create pronoun card for " + chr);
				String defin = pronounRecord[3] + " + " + pronounRecord[4];
				if (StringUtils.isBlank(pronounRecord[3])) {
					String tmp = pronounRecord[4];
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
				if (System.currentTimeMillis() - tick > 100) {
					break work;
				}
			}
		}
		Gdx.app.postRunnable(this);
	}

	private void addChallengesToDeck() {
		DataSet d = new DataSet();
		StringBuilder vroot = new StringBuilder();
		StringBuilder vroot_chr = new StringBuilder();
		Set<String> vtypes = new HashSet<String>();
		Set<String> ptypes = new HashSet<String>();
		long tick = System.currentTimeMillis();
		final Iterator<String[]> ichallenge = challenges.iterator();
		while (ichallenge.hasNext()) {
			/*
			 * Breathe...
			 */
			if (System.currentTimeMillis() - tick > 25) {
				// game.log(this, "buildDeck#breathe-conjugating");
				return;
			}
			String[] challenge = ichallenge.next();
			ichallenge.remove();
			vtypes.clear();
			vtypes.addAll(Arrays.asList(challenge[0].split(",\\s*")));
			boolean v_g3rd = false;
			if (vtypes.contains("g")) {
				v_g3rd = true;
				vtypes.remove("g");
			}
			if (vtypes.contains("xde") && vtypes.contains("xwi")) {
				vtypes.remove("xde");
				vtypes.add("xdi");
			}
			boolean vSetB = challenge[3].startsWith("u")
					|| challenge[3].startsWith("ụ")
					|| challenge[3].startsWith("j");
			String vroot_set = challenge[4];
			String vroot_chr_set = challenge[2];
			String vdef_active = challenge[5];
			String vdef_passive = challenge[6];
			String vdef_objects = challenge[7];
			String vroot_h = StringUtils.substringBefore(vroot_set, ",")
					.intern();
			String vroot_h_chr = StringUtils
					.substringBefore(vroot_chr_set, ",").intern();
			String vroot_alt = StringUtils.substringAfter(vroot_set, ",")
					.intern();
			String vroot_alt_chr = StringUtils.substringAfter(vroot_chr_set,
					",").intern();
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

			boolean aStem = vroot_h.matches("[ạaẠA].*");
			boolean eStem = vroot_h.matches("[ẹeịiọoụuẸEỊIỌOỤU].*");
			boolean vStem = vroot_h.matches("[ṿvṾV].*");
			boolean cStem = !(aStem | eStem | vStem);

			String vgroup = vroot_h_chr;

			setStatus("Conjugating: " + vroot_h);
			final Iterator<String[]> ipro = pronouns.iterator();
			while (ipro.hasNext()) {
				String[] pronoun = ipro.next();
				boolean pSetB = pronoun[5].equalsIgnoreCase("b");
				boolean pSetA = pronoun[5].equalsIgnoreCase("a");
				if (pSetB && !vSetB) {
					continue;
				}
				if (pSetA && vSetB) {
					continue;
				}
				String vtmode = pronoun[0];
				String syllabary = pronoun[1];
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

				String pgroup = d.chr;

				if (v_imp || v_inf) {
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
					d.latin = StringUtils.substringBefore(d.latin, ",")
							.intern();
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
					d.latin = StringUtils.substringBefore(d.latin, "-")
							.intern();
					d.latin = StringUtils.strip(d.latin).intern();

				} else {
					/*
					 * select consonent stem pronoun
					 */
					d.chr = StringUtils.substringBefore(d.chr, ",").intern();
					d.chr = StringUtils.substringBefore(d.chr, "-").intern();
					d.chr = StringUtils.strip(d.chr).intern();
					d.latin = StringUtils.substringBefore(d.latin, ",")
							.intern();
					d.latin = StringUtils.substringBefore(d.latin, "-")
							.intern();
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
				 * pronoun munge for vowel verb stems where we selected single
				 * use case consonent stem
				 */
				if (!cStem) {
					d.chr = d.chr.replaceAll(
							BoundPronouns.UNDERDOT + "?[¹²³⁴]$",
							BoundPronouns.UNDERX).intern();
					d.latin = d.latin.replaceAll(
							"[ẠAạaẸEẹeỊIịiỌOọoỤUụuṾVṿv][¹²³⁴]$", "").intern();
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
					d.latin = d.latin.replaceAll("da[¹²³⁴]\\[d\\]$", "d")
							.intern();
				}
				if (eStem) {
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

					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "")
							.intern();
				}

				if (aStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "")
							.intern();

				}

				if (eStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "")
							.intern();
				}

				if (vStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "")
							.intern();
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
//					if (!isOnlyYou(subj, obj)) {
//						addWiPrefix(d);
//					}
					if (!isIncludesYou(subj, obj)) {
						addWiPrefix(d);
					}
				}

				d.def = null;
				if (!StringUtils.isEmpty(subj)) {
					d.def = vdef_active;
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
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ",
								pronoun[3] + " ").intern();
					}
					if (d.def.startsWith("for him ")
							|| d.def.startsWith("For him ")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase()
									+ StringUtils.substring(subj, 1).intern();
						}
						d.def = d.def.replaceFirst("^[Ff]or him ",
								"For " + subj + " ").intern();
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase()
									+ StringUtils.substring(subj, 1).intern();
						}
						d.def = d.def.replaceFirst("^[Ll]et him ",
								"Let " + subj + " ").intern();
					}
					if (!StringUtils.isBlank(vdef_objects)) {
						String[] o = vdef_objects.split(",\\s*");
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
					if (obj.contains("I")) {
						d.def = d.def.replace("[s]", "").intern();
					}
					if (isPluralSubj(obj)) {
						d.def = d.def.replace("[s]", "").intern();
					} else {
						d.def = d.def.replace("[s]", "s").intern();
					}
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ", obj + " ")
								.intern();
					}
					if (d.def.startsWith("for him ")
							|| d.def.startsWith("For him ")) {
						d.def = d.def.replaceFirst("^[Ff]or him ",
								"For " + obj + " ").intern();
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!obj.startsWith("I")) {
							obj = (StringUtils.left(obj, 1).toLowerCase() + StringUtils
									.substring(obj, 1)).intern();
						}
						d.def = d.def.replaceFirst("^[Ll]et him ",
								"Let " + obj + " ").intern();
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
					game.log(this, "WARNING! DUPLICATE DEFINITION: " + d.chr
							+ ", " + d.def);
				} else {
					c.answer.add(d.def);
				}

			}
		}
		setStatus("Finished conjugating ...");
	}

	private void setStatus(String string) {
		this.status = string;
	}

	private void doSyllabaryConsonentVowelFixes(DataSet d) {
		boolean debug = false;// d.chr.endsWith("Ꭱ³Ꭶ");
		if (debug) {
			game.log(this, d.chr);
		}
		final String x = BoundPronouns.UNDERX;
		if (!d.chr.contains(x)) {
			return;
		}
		if (!d.chr.matches(".*" + x + "[ᎠᎡᎢᎣᎤᎥ].*")) {
			return;
		}
		String set;
		set = "[Ꭰ-Ꭵ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭰ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭱ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭲ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꭳ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꭴ").intern();
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꭵ").intern();
		if (debug) {
			game.log(this, d.chr);
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
			game.log(this, d.chr);
			game.log("");
		}
	}

	@SuppressWarnings("unused")
	private boolean isOnlyYou(String subj, String obj) {
		if (StringUtils.isBlank(subj)) {
			subj = obj;
		}
		subj = subj.toLowerCase();
		if (subj.equals("you one")) {
			return true;
		}
		if (subj.equals("you two")) {
			return true;
		}
		if (subj.equals("you all")) {
			return true;
		}
		return false;
	}
	
	private boolean isIncludesYou(String subj, String obj) {
		if (StringUtils.isBlank(subj)) {
			subj = obj;
		}
		subj = subj.toLowerCase();
		return subj.matches(".*\\byou\\b.*");
	}

	public boolean isPluralSubj(String subj) {
		boolean pluralSubj = subj.contains(" and");
		pluralSubj |= subj.startsWith("they");
		pluralSubj |= subj.startsWith("They");
		pluralSubj |= subj.contains(" two");
		pluralSubj |= subj.contains(" all");
		return pluralSubj;
	}

	public void definitionEnglishFixer(DataSet d) {
		d.def = StringUtils.left(d.def, 1).toUpperCase()
				+ StringUtils.substring(d.def, 1);
		
		d.def = d.def.replaceAll("(We .*?\\ ) is ", "$1 are ").intern();
		d.def = d.def.replaceAll("(We .*?\\ ) was ", "$1 were ").intern();
		
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
		d.def = d.def.replace("You one often has", "You one often have")
				.intern();
		d.def = d.def.replace("You two often has", "You two often have")
				.intern();
		d.def = d.def.replace("You all often has", "You all often have")
				.intern();
		d.def = d.def.replace("They often has", "They often have").intern();

		if (d.def.startsWith("For")) {
			d.def = d.def.replace("For We ", "For we ").intern();
			
			d.def = d.def.replace("For we (he ", "For us (him ").intern();
			d.def = d.def.replace("For we (they ", "For us (them ").intern();
			d.def = d.def.replace("For we (you ", "For us (you ").intern();
			
			d.def = d.def.replace("For he ", "For him ").intern();
			d.def = d.def.replace("For He ", "For him ").intern();
			d.def = d.def.replace("For they ", "For them ").intern();
			d.def = d.def.replace("For They ", "For them ").intern();
			d.def = d.def.replace("For I ", "For me ").intern();
			d.def = d.def.replace("and I ", "and me ").intern();
			d.def = d.def.replace("For You ", "For you ").intern();
		}

		if (d.def.startsWith("Let")) {
			d.def = d.def.replace("Let We ", "Let we ").intern();
			
			d.def = d.def.replace("Let we (he ", "Let us (him ").intern();
			d.def = d.def.replace("Let we (they ", "Let us (them ").intern();
			d.def = d.def.replace("Let we (you ", "Let us (you ").intern();
			
			d.def = d.def.replace("Let he ", "Let him ").intern();
			d.def = d.def.replace("Let He ", "Let him ").intern();
			d.def = d.def.replace("Let they ", "Let them ").intern();
			d.def = d.def.replace("Let You ", "Let you ").intern();
			d.def = d.def.replace("Let I ", "Let me ").intern();
			d.def = d.def.replace("Let They ", "Let them ").intern();
			d.def = d.def.replace("and I ", "and me ").intern();
			d.def = d.def.replace("Let You ", "Let you ").intern();
		}
		
		d.def = d.def.replaceAll("([Uu]s \\(.*? and) I\\)", "$1 me)").intern();
	}

	private void addDiPrefix(DataSet d, boolean aStem) {
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

	public void addWiPrefix(DataSet d) {
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

	private void addDePrefix(DataSet d) {
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

	private void init() throws IOException {
		pronouns=new ArrayList<>();
		FileHandle csv = Gdx.files.internal("csv/pronouns-list-tab.csv");
		try (BufferedReader reader = csv.reader(16*1024,"UTF-8")) {
			for (String line=reader.readLine(); line!=null; line = reader.readLine()) {
				String[] copyOf = Arrays.copyOf(line.split("\t"), 8);
				for (int i=0; i<copyOf.length; i++) {
					copyOf[i]=(copyOf[i]==null)?"":copyOf[i];
				}
				pronouns.add(copyOf);
			}
		}
		Iterator<String[]> ipro=pronouns.iterator();
		while (ipro.hasNext()) {
			String[] pronoun=ipro.next();
			String vtmode = StringUtils.strip(pronoun[0]);
			String syllabary = StringUtils.strip(pronoun[1]);
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
		csv = Gdx.files.internal("csv/challenges-tab.csv");
		try (BufferedReader reader = csv.reader(16*1024,"UTF-8")) {
			for (String line=reader.readLine(); line!=null; line = reader.readLine()) {
				String[] copyOf = Arrays.copyOf(line.split("\t"),9);
				for (int i=0; i<copyOf.length; i++) {
					copyOf[i]=(copyOf[i]==null)?"":copyOf[i];
				}
				challenges.add(copyOf);
			}
		}
		
		Iterator<String[]> ichallenge = challenges.iterator();
		while (ichallenge.hasNext()) {
			String[] challenge = ichallenge.next();
			String vtmode = StringUtils.strip(challenge[0]);
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

	public static class DataSet {
		public String chr;
		public String latin;
		public String def;
	}

	public static FileHandle getDeckSlot() {
		FileHandle p0;
		String path0 = "BoundPronouns/slots/deck";
		switch (Gdx.app.getType()) {
		case Android:
			p0 = Gdx.files.local(path0);
			break;
		case Applet:
			p0 = Gdx.files.external(path0);
			break;
		case Desktop:
			p0 = Gdx.files.external(path0);
			break;
		case HeadlessDesktop:
			p0 = Gdx.files.external(path0);
			break;
		case WebGL:
			p0 = Gdx.files.external(path0);
			break;
		case iOS:
			p0 = Gdx.files.local(path0);
			break;
		default:
			p0 = Gdx.files.external(path0);
		}
		if (!p0.exists()) {
			p0.mkdirs();
		}
		return p0;
	}
}
