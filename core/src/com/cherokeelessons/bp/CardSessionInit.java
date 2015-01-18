package com.cherokeelessons.bp;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
		private FileHandle list = null;
		private List<CSVRecord> records = null;
		private Deck deck = new Deck();
		private Map<String, Card> deckmap = new HashMap<>();
		private String prevLatin = "";
		private String prevChr = "";
		@Override
		public void run() {
			long tick = System.currentTimeMillis();
			work: {
				if (list == null) {
					game.log(this, "buildDeck#init");
					init();
					break work;
				}
				if (records.size()==0) {
					game.log(this, "buildDeck#save");
					FileHandle dest = slot.child("deck.json");
					json.setOutputType(OutputType.json);
					json.setTypeName(null);
					Collections.sort(deck.cards);
					game.log(this, deck.cards.size()+" cards in deck.");
					dest.writeString(json.prettyPrint(deck), false);					
					CardSessionInit.this.done();
					return;
				}
				game.log(this, "buildDeck#run");
				Iterator<CSVRecord> irec = records.iterator();
				while (irec.hasNext()) {
					CSVRecord record = irec.next();
					irec.remove();
					String vtmode=record.get(0);
					if (StringUtils.isBlank(vtmode)){
						continue;
					}
					String chr = record.get(1);
					if (chr.startsWith("#")) {
						continue;
					}
					String latin = record.get(2);
					String defin = record.get(3)+" + "+record.get(4);
					if (StringUtils.isBlank(record.get(3))){
						String tmp = record.get(4);
						passive:{
							if (tmp.equalsIgnoreCase("he")){
								defin = tmp+" (was being)";
								break passive;
							}
							if (tmp.equalsIgnoreCase("i")){
								defin = tmp+" (was being)";
								break passive;
							}
							defin = tmp+" (were being)";
							break passive;
						}				
					}
					if (StringUtils.isBlank(latin)) {
						latin = prevLatin;
					}
					if (StringUtils.isBlank(chr)) {
						chr = prevChr;
					}
					
					Card c=deckmap.get(chr);
					if (c==null) {
						c=new Card();
						c.challenge.add(chr);
						c.challenge.add(latin);
						deck.cards.add(c);
						deckmap.put(chr, c);
					}
					c.answer.add(defin);
					prevChr=chr;
					prevLatin=latin;
					if (System.currentTimeMillis()-tick>100){
						game.log(this, "buildDeck#breathe");
						break work;
					}
				}
			}
			Gdx.app.postRunnable(this);
		}

		private void init() {
			list = Gdx.files.internal("csv/pronouns-list.csv");
			try (CSVParser parse = CSVParser.parse(list.readString(),
					CSVFormat.RFC4180)) {
				records = parse.getRecords();
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
