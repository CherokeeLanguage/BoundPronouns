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
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;

public class CardSessionInit extends ChildScreen {
	private FileHandle slot;
	private Table container;
	private Runnable buildDeck = new Runnable() {
		private Json json = new Json();
		private List<CSVRecord> pronouns = null;
		private List<CSVRecord> challenges = null;
		private Deck deck = new Deck();
		private Map<String, Card> deckmap = new HashMap<>();
		private String prevLatin = "";
		private String prevChr = "";

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
				if (pronouns.size() == 0) {
					game.log(this, "buildDeck#save");
					FileHandle dest = slot.child("deck.json");
					json.setOutputType(OutputType.json);
					json.setTypeName(null);
					Collections.sort(deck.cards);
					game.log(this, deck.cards.size() + " cards in deck.");
					dest.writeString(json.prettyPrint(deck), false);
					CardSessionInit.this.done();
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
					String latin = record.get(2);
					String defin = record.get(3) + " + " + record.get(4);
					if (StringUtils.isBlank(record.get(3))) {
						String tmp = record.get(4);
						passive: {
							if (tmp.equalsIgnoreCase("he")) {
								defin = tmp + " (was being)";
								break passive;
							}
							if (tmp.equalsIgnoreCase("i")) {
								defin = tmp + " (was being)";
								break passive;
							}
							defin = tmp + " (were being)";
							break passive;
						}
					}
					if (StringUtils.isBlank(latin)) {
						latin = prevLatin;
					}
					if (StringUtils.isBlank(chr)) {
						chr = prevChr;
					}

					Card c = deckmap.get(chr);
					if (c == null) {
						c = new Card();
						c.challenge.add(chr);
						c.challenge.add(latin);
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
			Iterator<CSVRecord> irec = challenges.iterator();
			while (irec.hasNext()) {
				CSVRecord challenge = irec.next();
				irec.remove();
				if (StringUtils.isBlank(challenge.get(0))) {
					continue;
				}
				if (challenge.get(0).startsWith("#")) {
					continue;
				}
				Set<String> vtypes=new HashSet<>();
				vtypes.addAll(Arrays.asList(challenge.get(0).split(",\\s*")));
				
				Iterator<CSVRecord> ipro = pronouns.iterator();
				boolean vSetB = challenge.get(3).startsWith("u")
						|| challenge.get(3).startsWith("ụ");
				String vroot = challenge.get(4);
				String vroot_chr = challenge.get(2);
				String vdef_active = challenge.get(5);
				String vdef_passive = challenge.get(6);
				String vdef_objects = challenge.get(7);

				boolean aStem = vroot.matches("[ạaẠA].*");
				boolean eStem = vroot.matches("[ẹeịiọoụuẸEỊIỌOỤU].*");
				boolean vStem = vroot.matches("[ṿvṾV].*");
				boolean cStem = !(aStem | eStem | vStem);
				if (!cStem) {
					continue;
				}
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
					Set<String> ptypes=new HashSet<>();
					ptypes.addAll(Arrays.asList(pronoun.get(0).split(",\\s*")));
					if (Collections.disjoint(vtypes,ptypes)) {
						continue;
					}
					if (cStem) {
						String chr = StringUtils.substringBefore(
								pronoun.get(1), ",");
						chr = StringUtils.substringBefore(chr, "-");
						chr = StringUtils.strip(chr);
						chr += vroot_chr;
						String latin = StringUtils.substringBefore(
								pronoun.get(2), ",");
						latin = StringUtils.substringBefore(latin, "-");
						latin = StringUtils.strip(latin);
						latin += vroot;
						latin = latin.toLowerCase();
						String subj = pronoun.get(3);
						String obj = pronoun.get(4);

						if (vtypes.contains("de")) {
							game.log(this, "de form: "+vroot+" "+subj);
							boolean plural = subj.contains(" and");
							plural |= subj.startsWith("they");
							plural |= subj.startsWith("They");
							plural |= subj.contains(" two");
							plural |= subj.contains(" all");
							if (plural) {
								if (latin.matches("[ạa].*")) {
									latin = "d" + latin;
									chr = "Ꮣ"+chr.substring(1);
								}
								if (latin.matches("[ẹe].*")) {
									latin = "d" + latin;
									chr = "Ꮥ"+chr.substring(1);
								}
								if (latin.matches("[ọo].*")) {
									latin = "d" + latin;
									chr = "Ꮩ"+chr.substring(1);
								}
								if (latin.matches("[ụu].*")) {
									latin = "d" + latin;
									chr = "Ꮪ"+chr.substring(1);
								}
								if (latin.matches("[ṿv].*")) {
									latin = "d" + latin;
									chr = "Ꮫ"+chr.substring(1);
								}
								if (latin.matches("[ịi].*")) {
									latin = "de³"+latin.substring(2);
									chr = "Ꮥ³"+chr.substring(2);
								}
								if (!latin.matches("d.*")){
									latin = "de²"+latin;
									chr = "Ꮥ²" + chr;
								}
							}
						}
						
						if (vtypes.contains("di")) {
							boolean plural = subj.contains(" and");
							plural |= subj.startsWith("they");
							plural |= subj.startsWith("They");
							plural |= subj.contains(" two");
							plural |= subj.contains(" all");
							if (plural) {
								if (latin.matches("[ạa].*")) {
									latin = "dị" + latin.substring(2);
									chr = "Ꮧ"+chr.substring(1);
								}
								if (latin.matches("[ẹe].*")) {
									latin = "j" + latin;
									chr = "Ꮴ"+chr.substring(1);
								}
								if (latin.matches("[ọo].*")) {
									latin = "j" + latin;
									chr = "Ꮶ"+chr.substring(1);
								}
								if (latin.matches("[ụu].*")) {
									latin = "j" + latin;
									chr = "Ꮷ"+chr.substring(1);
								}
								if (latin.matches("[ṿv].*")) {
									latin = "j" + latin;
									chr = "Ꮸ"+chr.substring(1);
								}
								if (latin.matches("[ịi].*")) {
									latin = "d"+latin;
									chr = "Ꮧ"+chr.substring(1);
								}
								if (!latin.matches("[jd].*")){
									latin = "dị²"+latin;
									chr = "Ꮧ̣²" + chr;
								}
							}
						}

						String def;
						if (!StringUtils.isEmpty(subj)) {
							def = vdef_active;
							if (def.startsWith("he ") || def.startsWith("He ")) {
								def = def.replaceFirst("^[hH]e ",
										pronoun.get(3) + " ");
							}
							if (def.startsWith("for him ")
									|| def.startsWith("For him ")) {
								if (!subj.startsWith("I")) {
									subj = StringUtils.left(subj, 1)
											.toLowerCase()
											+ StringUtils.substring(subj, 1);
								}
								def = def.replaceFirst("^[Ff]or him ", "For "
										+ subj + " ");
							}
							if (!StringUtils.isBlank(vdef_objects)) {
								String[] o = vdef_objects.split(",\\s*");
								String vobj = o[0];
								if (o.length > 1 && obj.contains("them")) {
									vobj = o[1];
								}
								def = def.replaceAll("\\bx\\b", vobj);
							} else {
								def = def.replaceAll("\\bx\\b", obj);
							}

						} else {
							def = vdef_passive;
							if (def.startsWith("he ") || def.startsWith("He ")) {
								def = def.replaceFirst("^[hH]e ", obj + " ");
							}
							if (def.startsWith("for him ")
									|| def.startsWith("For him ")) {
								def = def.replaceFirst("^[Ff]or him ", "For "
										+ obj + " ");
							}
						}
						Card c = deckmap.get(chr);
						if (c == null) {
							c = new Card();
							deckmap.put(chr, c);
							deck.cards.add(c);
							c.challenge.add(chr);
							c.challenge.add(latin);
						}
						def = StringUtils.left(def, 1).toUpperCase()
								+ StringUtils.substring(def, 1);
						def = def.replace("and I is", "and I are");
						def = def.replace("I is", "I am");
						def = def.replace("You one is", "You one are");
						def = def.replace("You two is", "You two are");
						def = def.replace("You all is", "You all are");
						def = def.replace("They is", "They are");

						def = def.replace("and I has", "and I have");
						def = def.replace("I has", "I have");
						def = def.replace("You one has", "You one have");
						def = def.replace("You two has", "You two have");
						def = def.replace("You all has", "You all have");
						def = def.replace("They has", "They have");

						if (def.startsWith("For")) {
							def = def.replace("For he ", "For him ");
							def = def.replace("For He ", "For him ");
							def = def.replace("For I ", "For me ");
							def = def.replace("For they ", "For them ");
							def = def.replace("For They ", "For them ");
							def = def.replace("and I ", "and me ");
							def = def.replace("For You ", "For you ");
						}

						if (c.answer.contains(def)) {
							game.log(this, "WARNING! DUPLICATE DEFINITION: "
									+ chr + ", " + def);
						} else {
							c.answer.add(def);
						}
					}
				}
			}

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
	};

	public CardSessionInit(BoundPronouns game, Screen caller, FileHandle slot) {
		super(game, caller);
		this.slot = slot;
	}

	protected void done() {
		game.setScreen(new CardSession(game, caller, slot));
		dispose();
	}

	@Override
	public void show() {
		super.show();
		if (slot.child("session.json").exists()) {
			done();
			return;
		}
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		Skin skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		container = new Table(skin);
		stage.addActor(container);
		container.setBackground(d);
		container.setFillParent(true);
		Label lbl = new Label("One Moment ...", game.manager.get(
				BoundPronouns.SKIN, Skin.class));
		LabelStyle ls = lbl.getStyle();
		ls.font = game.manager.get("sans54.ttf", BitmapFont.class);
		lbl.setStyle(ls);
		container.add(lbl);
		Gdx.app.postRunnable(buildDeck);
	}

	@Override
	public void render(float delta) {
		super.render(delta);
	}
}
