package com.cherokeelessons.bp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;

public class BuildDeck implements Runnable {
	
	/**
	 * If enabled will add reversed cards, this causes this deck size to jump from 1,939
	 * to 4,378 cards! Additionally the UI is net setup to handle these reversed cards.
	 */
	private static final boolean addReversed = false;

	private static final boolean forceRebuild = false;

	public static int version = 17;

	private boolean skipBareForms = false;

	private Json json = new Json();
	private List<CSVRecord> pronouns = null;
	private List<CSVRecord> challenges = null;
	private Deck deck = new Deck();
	private Deck inverted = new Deck();
	private Map<String, Card> deckmap = new HashMap<>();
	private String prevLatin = "";
	private String prevChr = "";
	private String status = "";

	public String getStatus() {
		return status;
	}

	private final BoundPronouns game;
	private final Runnable done;
	private final FileHandle dest;

	public BuildDeck(BoundPronouns game, FileHandle slot, Runnable done) {
		this.game = game;
		this.done = done;
		dest = slot.child("deck.json");
	}

	public Runnable save = new Runnable() {
		@Override
		public void run() {
			game.log(this, "buildDeck#save");
			for (int i = 0; i < deck.cards.size(); i++) {
				deck.cards.get(i).id = i + 1;
			}
			deck.version = version;
			json.setOutputType(OutputType.json);
			json.setTypeName(null);
			game.log(this, deck.cards.size() + " cards in deck.");
			deck.size = deck.cards.size();
			dest.writeString(json.prettyPrint(deck), false, "UTF-8");
			if (done != null) {
				Gdx.app.postRunnable(done);
			}
		}
	};

	public Runnable addReversedCards = new Runnable() {
		@Override
		public void run() {
			Collections.sort(deck.cards);
			if (addReversed) {
				game.log(this, "buildDeck#addReversedCards");
				_run();
			}
			Gdx.app.postRunnable(save);
		}

		private void _run() {
			Collections.reverse(deck.cards);
			Iterator<Card> rcard = inverted.cards.iterator();
			while (rcard.hasNext()) {
				Card reversed = rcard.next();
				insert_lookup: for (int i = 0; i < deck.cards.size(); i++) {
					Card card = deck.cards.get(i);
					if (card.id == reversed.id) {
						deck.cards.add(i, reversed);
						break insert_lookup;
					}
				}
			}
			Collections.reverse(deck.cards);
		}
	};

	public Runnable createReversed = new Runnable() {
		@Override
		public void run() {
			if (addReversed) {
				game.log(this, "buildDeck#createReversed");
				_run();
			}
			Gdx.app.postRunnable(addReversedCards);
		}

		private void _run() {
			Map<String, Card> map = new HashMap<>();
			Collections.sort(deck.cards);
			for (int i = 0; i < deck.cards.size(); i++) {
				deck.cards.get(i).id = i + 1;
			}
			for (int ix = 0; ix < deck.cards.size(); ix++) {
				Card card = deck.cards.get(ix);
				for (String answer : card.answer) {
					Card xcard = map.get(answer);
					if (xcard == null) {
						xcard = new Card(card);
						xcard.reversed = true;
						xcard.challenge.clear();
						xcard.answer.clear();
						xcard.vgroup = answer;
						xcard.challenge.add(answer);
						map.put(answer, xcard);
						inverted.cards.add(xcard);
					}
					xcard.id = card.id;
					xcard.answer.add(StringUtils.join(card.challenge, "\n"));
				}
			}
			game.log(this, inverted.cards.size()
					+ " reversed cards created out of main deck.");
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
				Deck deck = json.fromJson(Deck.class, dest);
				if (deck.version == version) {
					game.log(this, deck.cards.size() + " cards in deck.");
					if (done != null) {
						Gdx.app.postRunnable(done);
					}
					return;
				}
				dest.delete();
			}
			if (pronouns == null) {
				game.log(this, "buildDeck#init");
				init();
				break work;
			}
			if (challenges.size() != 0) {
				addChallengesToDeck();
				break work;
			}
			if (isSkipBareForms() || pronouns.size() == 0) {
				setStatus("Saving ...");
				Gdx.app.postRunnable(createReversed);
				return;
			}
			game.log(this, "buildDeck#run");
			Iterator<CSVRecord> irec = pronouns.iterator();
			while (irec.hasNext()) {
				CSVRecord record = irec.next();
				irec.remove();
				String vtmode = record.get(0);
				if (StringUtils.isBlank(vtmode)) {
					continue;
				}
				String chr = record.get(1);
				if (chr.startsWith("#")) {
					continue;
				}
				setStatus("Create pronoun card for " + chr);
				String latin = record.get(2);
				String defin = record.get(3) + " + " + record.get(4);
				if (StringUtils.isBlank(record.get(3))) {
					String tmp = record.get(4);
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

				Card c = deckmap.get(chr.toString());
				if (c == null) {
					c = new Card();
					c.pgroup = chr;
					c.vgroup = "";
					c.challenge.add(chr.toString());
					c.challenge.add(latin.toString());
					deck.cards.add(c);
					deckmap.put(chr, c);
				}
				c.answer.add(defin);
				prevChr = chr;
				prevLatin = latin;
				if (System.currentTimeMillis() - tick > 100) {
					game.log(this, "buildDeck#breathe");
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
		Set<String> vtypes = new HashSet<>();
		Set<String> ptypes = new HashSet<>();
		long tick = System.currentTimeMillis();
		Iterator<CSVRecord> irec = challenges.iterator();
		while (irec.hasNext()) {
			/*
			 * Breathe...
			 */
			if (System.currentTimeMillis() - tick > 50) {
				game.log(this, "buildDeck#breathe-conjugating");
				return;
			}
			CSVRecord challenge = irec.next();
			irec.remove();
			if (StringUtils.isBlank(challenge.get(0))) {
				continue;
			}
			if (challenge.get(0).startsWith("#")) {
				continue;
			}
			vtypes.clear();
			vtypes.addAll(Arrays.asList(challenge.get(0).split(",\\s*")));
			boolean v_g3rd = false;
			if (vtypes.contains("g")) {
				v_g3rd = true;
				vtypes.remove("g");
			}
			if (vtypes.contains("xde") && vtypes.contains("xwi")) {
				vtypes.remove("xde");
				vtypes.add("xdi");
			}
			boolean vSetB = challenge.get(3).startsWith("u")
					|| challenge.get(3).startsWith("ụ")
					|| challenge.get(3).startsWith("j");
			String vroot_set = challenge.get(4);
			String vroot_chr_set = challenge.get(2);
			String vdef_active = challenge.get(5);
			String vdef_passive = challenge.get(6);
			String vdef_objects = challenge.get(7);
			String vroot_h = StringUtils.substringBefore(vroot_set, ",").intern();
			String vroot_h_chr = StringUtils
					.substringBefore(vroot_chr_set, ",").intern();
			String vroot_alt = StringUtils.substringAfter(vroot_set, ",").intern();
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
			Iterator<CSVRecord> ipro = pronouns.iterator();

			while (ipro.hasNext()) {
				CSVRecord pronoun = ipro.next();
				boolean pSetB = pronoun.get(5).equalsIgnoreCase("b");
				boolean pSetA = pronoun.get(5).equalsIgnoreCase("a");
				if (pSetB && !vSetB) {
					continue;
				}
				if (pSetA && vSetB) {
					continue;
				}
				if (StringUtils.isBlank(pronoun.get(0))) {
					continue;
				}
				if (pronoun.get(0).startsWith("#")) {
					continue;
				}
				if (pronoun.get(1).startsWith("#")) {
					continue;
				}
				ptypes.clear();
				ptypes.addAll(Arrays.asList(pronoun.get(0).split(",\\s*")));

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

				d.chr = pronoun.get(1);
				d.latin = pronoun.get(2);
				d.def = "";

				String pgroup = d.chr;

				if (v_imp || v_inf) {
					if (!StringUtils.isBlank(pronoun.get(6))) {
						d.chr = pronoun.get(6);
						d.latin = pronoun.get(7);
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
						game.log(this, "ti -> t");
						d.chr = "Ꮤ͓";
						d.latin = "t";
					}
					if (d.chr.equals("Ꮧ̣²")) {
						game.log(this, "di -> d");
						d.chr = "Ꮣ͓";
						d.latin = "d";
					}
				}

				/*
				 * pronoun munge for vowel verb stems where we selected single
				 * use case consonent stem
				 */
				if (!cStem) {
					d.chr = d.chr.replaceAll(BoundPronouns.UNDERDOT
							+ "?[¹²³⁴]$", BoundPronouns.UNDERX).intern();
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
					d.latin = d.latin.replaceAll("da[¹²³⁴]\\[d\\]$", "d").intern();
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
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();
				}

				if (aStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();

				}

				if (eStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();
				}

				if (vStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "").intern();
				}

				doSyllabaryConsonentVowelFixes(d);

				d.latin = d.latin.toLowerCase().intern();
				String subj = pronoun.get(3);
				String obj = pronoun.get(4);

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
					if (!isOnlyYou(subj, obj)) {
						addWiPrefix(d);
					}
				}

				d.def = null;
				if (!StringUtils.isEmpty(subj)) {
					d.def = vdef_active;
					if (subj.contains("I")) {
						d.def = d.def.replace("[s]", "").intern();
					}
					if (isPluralSubj(subj)) {
						d.def = d.def.replace("[s]", "").intern();
					} else {
						d.def = d.def.replace("[s]", "s").intern();
					}
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ", pronoun.get(3)
								+ " ").intern();
					}
					if (d.def.startsWith("for him ")
							|| d.def.startsWith("For him ")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase()
									+ StringUtils.substring(subj, 1).intern();
						}
						d.def = d.def.replaceFirst("^[Ff]or him ", "For "
								+ subj + " ").intern();
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase()
									+ StringUtils.substring(subj, 1).intern();
						}
						d.def = d.def.replaceFirst("^[Ll]et him ", "Let "
								+ subj + " ").intern();
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
						d.def = d.def.replaceFirst("^[hH]e ", obj + " ").intern();
					}
					if (d.def.startsWith("for him ")
							|| d.def.startsWith("For him ")) {
						d.def = d.def.replaceFirst("^[Ff]or him ", "For " + obj
								+ " ").intern();
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!obj.startsWith("I")) {
							obj = (StringUtils.left(obj, 1).toLowerCase()
									+ StringUtils.substring(obj, 1)).intern();
						}
						d.def = d.def.replaceFirst("^[Ll]et him ", "Let " + obj
								+ " ").intern();
					}
				}
				Card c = deckmap.get(d.chr);
				if (c == null) {
					c = new Card();
					deckmap.put(d.chr.intern(), c);
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
			d.def = d.def.replace("For he ", "For him ").intern();
			d.def = d.def.replace("For He ", "For him ").intern();
			d.def = d.def.replace("For I ", "For me ").intern();
			d.def = d.def.replace("For they ", "For them ").intern();
			d.def = d.def.replace("For They ", "For them ").intern();
			d.def = d.def.replace("and I ", "and me ").intern();
			d.def = d.def.replace("For You ", "For you ").intern();
		}

		if (d.def.startsWith("Let")) {
			d.def = d.def.replace("Let he ", "Let him ").intern();
			d.def = d.def.replace("Let He ", "Let him ").intern();
			d.def = d.def.replace("Let I ", "Let me ").intern();
			d.def = d.def.replace("Let they ", "Let them ").intern();
			d.def = d.def.replace("Let They ", "Let them ").intern();
			d.def = d.def.replace("and I ", "and me ").intern();
			d.def = d.def.replace("Let You ", "Let you ").intern();
		}
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

	private void init() {
		FileHandle csv = Gdx.files.internal("csv/pronouns-list.csv");
		try (CSVParser parse = CSVParser.parse(csv.readString(),
				CSVFormat.RFC4180)) {
			pronouns = parse.getRecords();
		} catch (IOException e) {
			game.err(this, e.getMessage(), e);
			return;
		}
		csv = Gdx.files.internal("csv/challenges.csv");
		try (CSVParser parse = CSVParser.parse(csv.readString(),
				CSVFormat.RFC4180)) {
			challenges = parse.getRecords();
		} catch (IOException e) {
			game.err(this, e.getMessage(), e);
			return;
		}
	}

	public boolean isSkipBareForms() {
		return skipBareForms;
	}

	public void setSkipBareForms(boolean skipBareForms) {
		this.skipBareForms = skipBareForms;
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
