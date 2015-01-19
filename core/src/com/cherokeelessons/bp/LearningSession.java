package com.cherokeelessons.bp;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;

public class LearningSession extends ChildScreen implements Screen {

	private final long maxTime = 20l * 60l * 60l * 1000l;// 20 minutes max per
															// session ?

	private static final int RT = 5;
	private static final int MaxTotalShows = 100;

	private int totalshows = 0;

	private Table container;

	private Deck deck;
	private ActiveDeck activeDeckFromDisk;
	private final Json json;
	private ActiveDeck current_active = new ActiveDeck();
	private ActiveDeck current_seen = new ActiveDeck();
	private final Set<String> nodupes = new HashSet<>();

	private Runnable loadDeck = new Runnable() {
		@Override
		public void run() {
			game.log(this, "Loading Deck...");
			stage.addAction(Actions.run(loadStats));
			deck = json.fromJson(Deck.class, slot.child("deck.json"));
		}
	};

	protected Comparator<ActiveCard> sortByNextShow = new Comparator<ActiveCard>() {
		@Override
		public int compare(ActiveCard o1, ActiveCard o2) {
			if (o1.show_again_ms != o2.show_again_ms) {
				return o1.show_again_ms > o2.show_again_ms ? 1 : -1;
			}
			return o1.box - o2.box;
		}
	};

	private Runnable loadStats = new Runnable() {
		@Override
		public void run() {
			game.log(this, "Loading Stats...");
			stage.addAction(Actions.run(initSet0));
			if (!slot.child("stats.json").exists()) {
				activeDeckFromDisk = new ActiveDeck();
				json.toJson(activeDeckFromDisk, slot.child("stats.json"));
				return;
			}
			activeDeckFromDisk = json.fromJson(ActiveDeck.class,
					slot.child("stats.json"));
			Collections.sort(activeDeckFromDisk.stats, sortByNextShow);
		}
	};

	private Runnable initSet0 = new Runnable() {
		@Override
		public void run() {
			// stage.addAction(Actions.run(saveStats));
			nodupes.clear();
			game.log(this, "Loading Set 0...");

			int needed = 10;

			recordAlreadySeen(activeDeckFromDisk);

			// time-shift all cards by time since last recorded run
			updateTime(activeDeckFromDisk);

			// add cards if more cards are needed
			addCards(needed, current_active);

			stage.addAction(Actions.run(showACard));
		}

	};
	
	private ActiveCard getNextCard(){
		if (current_active.stats.size()==0) {
			current_active.stats.addAll(current_seen.stats);
			current_seen.stats.clear();
			if (current_active.stats.size()==0) {
				return null;
			}
		}
		ActiveCard card = current_active.stats.get(0);
		current_seen.stats.add(card);
		current_active.stats.remove(0);
		return card;
	}

	private Runnable showACard = new Runnable() {
		@Override
		public void run() {
			ActiveCard card = getNextCard();
			if (card.newCard) {
				newCardDialog.showCard(deck, current_active, current_seen, card);
			} else {
				prevCardDialog.showCard(deck, current_active, current_seen, card);
			}			
		}
	};

	private Runnable saveStats = new Runnable() {
		@Override
		public void run() {
			ActiveDeck tosave = new ActiveDeck();
			tosave.stats.addAll(current_active.stats);
			tosave.stats.addAll(current_seen.stats);
			tosave.lastrun = System.currentTimeMillis();
			FileHandle tmp = FileHandle.tempFile("stats.json");
			tmp.writeString(json.prettyPrint(tosave), false, "UTF-8");
			tmp.moveTo(slot.child("stats.json"));
			tmp.delete();
		}
	};

	/**
	 * record all cards currently "in-play" so that when cards are retrieved
	 * from the master deck they are new cards
	 * 
	 * @param activeDeck
	 */
	public void recordAlreadySeen(ActiveDeck activeDeck) {
		Iterator<ActiveCard> istat = activeDeck.stats.iterator();
		while (istat.hasNext()) {
			ActiveCard next = istat.next();
			String unique_id = next.pgroup + "+" + next.vgroup;
			nodupes.add(unique_id);
		}
	}

	/**
	 * add this many cards to the Stat set first from the current stats set then
	 * from the master Deck set
	 * 
	 * @param needed
	 * @param set
	 */
	public void addCards(int needed, ActiveDeck set) {
		/**
		 * look for previous cards to load first, if their delay time is up
		 */
		Iterator<ActiveCard> istat = activeDeckFromDisk.stats.iterator();
		while (needed > 0 && istat.hasNext()) {
			ActiveCard next = istat.next();
			if (next.show_again_ms > 0) {
				continue;
			}
			next.correct_in_a_row = 0;
			next.tries_remaining = RT * 2;
			set.stats.add(next);
			needed--;
		}

		/**
		 * add new never seen cards second
		 */
		Iterator<Card> ideck = deck.cards.iterator();
		while (needed > 0 && ideck.hasNext()) {
			Card next = ideck.next();
			String unique_id = next.pgroup + "+" + next.vgroup;
			if (nodupes.contains(unique_id)) {
				continue;
			}
			ActiveCard stat = new ActiveCard();
			stat.box = 0;
			stat.correct_in_a_row = 0;
			stat.id = next.id;
			stat.newCard = true;
			stat.pgroup = next.pgroup;
			stat.show_again_ms = 0;
			stat.tries_remaining = RT * 2;
			stat.vgroup = next.vgroup;
			set.stats.add(stat);
			needed--;
			nodupes.add(unique_id);
		}
	}

	private final Skin skin;
	private final FileHandle slot;

	public LearningSession(BoundPronouns game, Screen caller, FileHandle slot) {
		super(game, caller);
		this.slot = slot;
		slot.mkdirs();
		if (!slot.child("deck.json").exists()) {
			BuildDeck.getDeckSlot().child("deck.json")
					.copyTo(slot.child("deck.json"));
		}

		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		container = new Table(skin);
		container.setBackground(d);
		container.setFillParent(true);
		stage.addActor(container);
		stage.addAction(Actions.delay(.01f, Actions.run(loadDeck)));
		json = new Json();
		json.setOutputType(OutputType.json);
		json.setTypeName(null);
		
		newCardDialog = new NewCardDialog(game, "", skin);
		prevCardDialog = new PreviousCardDialog(game, "", skin);
	}
	
	private final NewCardDialog newCardDialog;
	private final PreviousCardDialog prevCardDialog;

	/**
	 * time-shift all cards by time since last recorded run
	 * 
	 * @param activeDeck
	 */
	public void updateTime(ActiveDeck activeDeck) {

		long since = activeDeck.lastrun > 0 ? System.currentTimeMillis()
				- activeDeck.lastrun : System.currentTimeMillis();
		/*
		 * clamp to 24 hours max as the time passed
		 */
		if (since < 0l || since > (24l * 60l * 60l * 1000l)) {
			since = (24l * 60l * 60l * 1000l);
		}
		Iterator<ActiveCard> istat = activeDeck.stats.iterator();
		while (istat.hasNext()) {
			ActiveCard next = istat.next();
			next.show_again_ms -= since;
		}
	}

}
