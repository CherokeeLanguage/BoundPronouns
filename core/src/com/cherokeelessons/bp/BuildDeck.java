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
	

	private boolean skipBareForms = false;

	private Json json = new Json();
	private List<CSVRecord> pronouns = null;
	private List<CSVRecord> challenges = null;
	private Deck deck = new Deck();
	private Map<String, Card> deckmap = new HashMap<>();
	private String prevLatin = "";
	private String prevChr = "";
	private String status="";
	public String getStatus() {
		return status;
	}

	private final BoundPronouns game;
	private final FileHandle slot;
	private final Runnable done;

	public BuildDeck(BoundPronouns game, FileHandle slot, Runnable done) {
		this.game = game;
		this.slot = slot;
		this.done = done;
	}

	public Runnable save=new Runnable() {
		@Override
		public void run() {
			game.log(this, "buildDeck#save");
			FileHandle dest = slot.child("deck.json");
			json.setOutputType(OutputType.json);
			json.setTypeName(null);
			Collections.sort(deck.cards);
			game.log(this, deck.cards.size() + " cards in deck.");
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
				setStatus("Storing ...");
				Gdx.app.postRunnable(save);
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
				StringBuilder chr = new StringBuilder(record.get(1));
				if (chr.toString().startsWith("#")) {
					continue;
				}
				setStatus("Create pronoun card for "+chr);
				StringBuilder latin = new StringBuilder(record.get(2));
				StringBuilder defin = new StringBuilder(record.get(3) + " + " + record.get(4));
				if (StringUtils.isBlank(record.get(3))) {
					String tmp = record.get(4);
					passive: {
						defin.setLength(0);
						defin.append(tmp);
						if (tmp.equalsIgnoreCase("he")) {
							defin.append(" (was being)");
							break passive;
						}
						if (tmp.equalsIgnoreCase("i")) {
							defin.append(" (was being)");
							break passive;
						}
						defin.append(" (were being)");
						break passive;
					}
				}
				if (StringUtils.isBlank(latin)) {
					latin.setLength(0);
					latin.append(prevLatin);
				}
				if (StringUtils.isBlank(chr)) {
					chr.setLength(0);
					chr.append(prevChr);
				}

				Card c = deckmap.get(chr);
				if (c == null) {
					c = new Card();
					c.pgroup = chr.toString();
					c.vgroup = "";
					c.challenge.add(chr.toString());
					c.challenge.add(latin.toString());
					deck.cards.add(c);
					deckmap.put(chr.toString(), c);
				}
				c.answer.add(defin.toString());
				prevChr = chr.toString();
				prevLatin = latin.toString();
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
		StringBuilder vroot=new StringBuilder();
		StringBuilder vroot_chr=new StringBuilder();
		
		long tick=System.currentTimeMillis();
		Iterator<CSVRecord> irec = challenges.iterator();
		while (irec.hasNext()) {
			/*
			 * Breathe...
			 */
			if (System.currentTimeMillis()-tick>100) {
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
			Set<String> vtypes = new HashSet<>();
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
			String vroot_h=StringUtils.substringBefore(vroot_set, ",");
			String vroot_h_chr=StringUtils.substringBefore(vroot_chr_set, ",");
			String vroot_alt=StringUtils.substringAfter(vroot_set, ",");
			String vroot_alt_chr=StringUtils.substringAfter(vroot_chr_set, ",");
			if (StringUtils.isBlank(vroot_alt)) {
				vroot_alt=vroot_h;
			}
			if (StringUtils.isBlank(vroot_alt_chr)) {
				vroot_alt_chr=vroot_h_chr;
			}
			vroot_h=StringUtils.strip(vroot_h);
			vroot_alt=StringUtils.strip(vroot_alt);
			vroot_h_chr=StringUtils.strip(vroot_h_chr);
			vroot_alt_chr=StringUtils.strip(vroot_alt_chr);

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

			setStatus("Conjugating: "+vroot_h);
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
				Set<String> ptypes = new HashSet<>();
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
				d.def="";

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
					d.chr = StringUtils.substringAfter(d.chr, ",");
					d.chr = StringUtils.strip(d.chr);
					d.latin = StringUtils.substringAfter(d.latin, ",");
					d.latin = StringUtils.strip(d.latin);
				}

				/*
				 * a vs ga select
				 */
				if (!v_g3rd && p_g3rd) {
					d.chr = StringUtils.substringBefore(d.chr, ",");
					d.chr = StringUtils.strip(d.chr);
					d.latin = StringUtils.substringBefore(d.latin, ",");
					d.latin = StringUtils.strip(d.latin);
				}

				if (!cStem && d.chr.contains(",")) {
					/*
					 * select vowel stem pronoun
					 */
					d.chr = StringUtils.substringAfter(d.chr, ",");
					d.chr = StringUtils.substringBefore(d.chr, "-");
					d.chr = StringUtils.strip(d.chr);

					d.latin = StringUtils.substringAfter(d.latin, ",");
					d.latin = StringUtils.substringBefore(d.latin, "-");
					d.latin = StringUtils.strip(d.latin);

				} else {
					/*
					 * select consonent stem pronoun
					 */
					d.chr = StringUtils.substringBefore(d.chr, ",");
					d.chr = StringUtils.substringBefore(d.chr, "-");
					d.chr = StringUtils.strip(d.chr);
					d.latin = StringUtils.substringBefore(d.latin, ",");
					d.latin = StringUtils.substringBefore(d.latin, "-");
					d.latin = StringUtils.strip(d.latin);
				}

				/*
				 * pronoun munge for vowel verb stems where we selected single
				 * use case consonent stem
				 */
				if (!cStem) {
					d.chr = d.chr.replaceAll(BoundPronouns.UNDERDOT
							+ "?[¹²³⁴]$", BoundPronouns.UNDERX);
					d.latin = d.latin.replaceAll(
							"[ẠAạaẸEẹeỊIịiỌOọoỤUụuṾVṿv][¹²³⁴]$", "");
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
				if (eStem) {
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
					
					if (vroot.toString().matches("[TtDdSs].*")){
						if (d.latin.equalsIgnoreCase("A¹gị²")){
							d.latin="a¹k";
							d.chr="Ꭰ¹Ꭹ͓";
						}
						if (d.latin.equalsIgnoreCase("Jạ²")){
							d.latin="ts";
							d.chr="Ꮳ͓";
						}
					}
					if (vroot.toString().matches("[Tt].*")){
						if (d.latin.equalsIgnoreCase("I¹ji²")){
							d.latin="i¹jch";
							d.chr="Ꭲ¹Ꮵ͓";
						}
					}
					
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
					
					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
				}

				if (aStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
				}

				if (eStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
				}

				if (vStem) {
					d.chr += vroot_chr;
					d.chr = d.chr.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");

					d.latin += vroot;
					d.latin = d.latin.replaceAll("[¹²³⁴](?=ɂ?[¹²³⁴])", "");
				}

				doSyllabaryConsonentVowelFixes(d);

				d.latin = d.latin.toLowerCase();
				String subj = pronoun.get(3);
				String obj = pronoun.get(4);

				if (!StringUtils.isBlank(subj) && isPluralSubj(subj)) {
					if (vtypes.contains("xde")) {
						addDePrefix(d);
					}
					if (vtypes.contains("xdi")) {
						addDiPrefix(d);
					}
				} else if (isPluralSubj(obj)) {
					if (vtypes.contains("xde")) {
						addDePrefix(d);
					}
					if (vtypes.contains("xdi")) {
						addDiPrefix(d);
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
						d.def = d.def.replace("[s]", "");
					}
					if (isPluralSubj(subj)) {
						d.def = d.def.replace("[s]", "");
					} else {
						d.def = d.def.replace("[s]", "s");
					}
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ", pronoun.get(3)
								+ " ");
					}
					if (d.def.startsWith("for him ")
							|| d.def.startsWith("For him ")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase()
									+ StringUtils.substring(subj, 1);
						}
						d.def = d.def.replaceFirst("^[Ff]or him ", "For "
								+ subj + " ");
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!subj.startsWith("I")) {
							subj = StringUtils.left(subj, 1).toLowerCase()
									+ StringUtils.substring(subj, 1);
						}
						d.def = d.def.replaceFirst("^[Ll]et him ", "Let "
								+ subj + " ");
					}
					if (!StringUtils.isBlank(vdef_objects)) {
						String[] o = vdef_objects.split(",\\s*");
						String vobj = o[0];
						if (o.length > 1 && obj.contains("them")) {
							vobj = o[1];
						}
						d.def = d.def.replaceAll("\\bx\\b", vobj);
					} else {
						d.def = d.def.replaceAll("\\bx\\b", obj);
					}

				} else {
					d.def = vdef_passive;
					if (obj.contains("I")) {
						d.def = d.def.replace("[s]", "");
					}
					if (isPluralSubj(obj)) {
						d.def = d.def.replace("[s]", "");
					} else {
						d.def = d.def.replace("[s]", "s");
					}
					if (d.def.startsWith("he ") || d.def.startsWith("He ")) {
						d.def = d.def.replaceFirst("^[hH]e ", obj + " ");
					}
					if (d.def.startsWith("for him ")
							|| d.def.startsWith("For him ")) {
						d.def = d.def.replaceFirst("^[Ff]or him ", "For " + obj
								+ " ");
					}
					if (d.def.matches("[Ll]et him.*")) {
						if (!obj.startsWith("I")) {
							obj = StringUtils.left(obj, 1).toLowerCase()
									+ StringUtils.substring(obj, 1);
						}
						d.def = d.def.replaceFirst("^[Ll]et him ", "Let " + obj
								+ " ");
					}
				}
				Card c = deckmap.get(d.chr);
				if (c == null) {
					c = new Card();
					deckmap.put(d.chr, c);
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
		this.status=string;		
	}

	private void doSyllabaryConsonentVowelFixes(DataSet d) {
		boolean debug = false;// d.chr.endsWith("Ꭱ³Ꭶ");
		if (debug) {
			game.log(this, d.chr);
		}
		final String x = BoundPronouns.UNDERX;
		if (!d.chr.contains(x)){
			return;
		}
		if (!d.chr.matches(".*"+x+"[ᎠᎡᎢᎣᎤᎥ].*")){
			return;
		}
		String set;
		set = "[Ꭰ-Ꭵ]";
		d.chr = d.chr.replaceAll(set + x + "Ꭰ", "Ꭰ");
		d.chr = d.chr.replaceAll(set + x + "Ꭱ", "Ꭱ");
		d.chr = d.chr.replaceAll(set + x + "Ꭲ", "Ꭲ");
		d.chr = d.chr.replaceAll(set + x + "Ꭳ", "Ꭳ");
		d.chr = d.chr.replaceAll(set + x + "Ꭴ", "Ꭴ");
		d.chr = d.chr.replaceAll(set + x + "Ꭵ", "Ꭵ");
		if (debug) {
			game.log(this, d.chr);
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
		d.def = d.def.replace("and I is", "and I are");
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

		if (d.def.startsWith("For")) {
			d.def = d.def.replace("For he ", "For him ");
			d.def = d.def.replace("For He ", "For him ");
			d.def = d.def.replace("For I ", "For me ");
			d.def = d.def.replace("For they ", "For them ");
			d.def = d.def.replace("For They ", "For them ");
			d.def = d.def.replace("and I ", "and me ");
			d.def = d.def.replace("For You ", "For you ");
		}

		if (d.def.startsWith("Let")) {
			d.def = d.def.replace("Let he ", "Let him ");
			d.def = d.def.replace("Let He ", "Let him ");
			d.def = d.def.replace("Let I ", "Let me ");
			d.def = d.def.replace("Let they ", "Let them ");
			d.def = d.def.replace("Let They ", "Let them ");
			d.def = d.def.replace("and I ", "and me ");
			d.def = d.def.replace("Let You ", "Let you ");
		}
	}

	private void addDiPrefix(DataSet d) {
		if (d.latin.matches("[ạa].*")) {
			d.latin = "dị" + d.latin.substring(1);
			d.chr = "Ꮧ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = "j" + d.latin;
			d.chr = "Ꮴ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = "j" + d.latin;
			d.chr = "Ꮶ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = "j" + d.latin;
			d.chr = "Ꮷ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = "j" + d.latin;
			d.chr = "Ꮸ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = "d" + d.latin;
			d.chr = "Ꮧ" + d.chr.substring(1);
			return;
		}

		d.latin = "dị²" + d.latin;
		d.chr = "Ꮧ̣²" + d.chr;

	}

	public void addWiPrefix(DataSet d) {
		if (d.latin.matches("[ạa].*")) {
			d.latin = "w" + d.latin;
			d.chr = "Ꮹ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = "w" + d.latin;
			d.chr = "Ꮺ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = "w" + d.latin;
			d.chr = "Ꮼ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = "w" + d.latin;
			d.chr = "Ꮽ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = "w" + d.latin;
			d.chr = "Ꮾ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = "w" + d.latin;
			d.chr = "Ꮻ" + d.chr.substring(1);
			return;
		}
		d.latin = "wị²" + d.latin;
		d.chr = "Ꮻ̣²" + d.chr;
	}

	private void addDePrefix(DataSet d) {
		if (d.latin.matches("[ạa].*")) {
			d.latin = "d" + d.latin;
			d.chr = "Ꮣ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ẹe].*")) {
			d.latin = "d" + d.latin;
			d.chr = "Ꮥ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ọo].*")) {
			d.latin = "d" + d.latin;
			d.chr = "Ꮩ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ụu].*")) {
			d.latin = "d" + d.latin;
			d.chr = "Ꮪ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ṿv].*")) {
			d.latin = "d" + d.latin;
			d.chr = "Ꮫ" + d.chr.substring(1);
			return;
		}
		if (d.latin.matches("[ịi].*")) {
			d.latin = "de³" + d.latin.substring(2);
			d.chr = "Ꮥ³" + d.chr.substring(2);
			return;
		}

		d.latin = "de²" + d.latin;
		d.chr = "Ꮥ²" + d.chr;

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
	
	public static FileHandle getDeckSlot(){
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
