package com.cherokeelessons.bp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ColorAction;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.Answer;
import com.cherokeelessons.cards.Answer.AnswerList;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;

public class LearningSession extends ChildScreen implements Screen {

	private static final int maxAnswers = 4;

	private static final int maxCorrect = 2;

	private static final float MaxTimePerCard_sec = 15f;
	private static final int MaxTotalShows = 100;

	private static final int RT = 5;

	private ActiveDeck activeDeckFromDisk;

	private Sound buzzer;
	/**
	 * Sort answers by edit distance so the list can be trimmed to size easily.
	 * The sort only considers edit distance and does not factor in actual
	 * String values - this is intentional.
	 */
	private Comparator<Answer> byDistance = new Comparator<Answer>() {
		@Override
		public int compare(Answer o1, Answer o2) {
			if (o1.correct != o2.correct && o1.correct) {
				return -1;
			}
			if (o2.correct) {
				return 1;
			}
			return Integer.compare(o1.distance, o2.distance);
		}
	};
	private int cardcount = 0;
	private final Map<String, Card> cards_by_id = new HashMap<>();
	private final ChallengeCardDialog challengeCardDialog;
	private Table container;
	private Sound cow;

	private ActiveDeck current_active = new ActiveDeck();

	private ActiveDeck current_seen = new ActiveDeck();

	private Deck deck;

	private Sound ding;

	private float elapsed_sec;

	private Runnable initSet0 = new Runnable() {
		@Override
		public void run() {
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
	private final Json json;
	private Runnable loadDeck = new Runnable() {
		@Override
		public void run() {
			game.log(this, "Loading Deck...");
			stage.addAction(Actions.run(loadStats));
			deck = json.fromJson(Deck.class,
					BuildDeck.getDeckSlot().child("deck.json"));
			cards_by_id.clear();
			for (Card c : deck.cards) {
				cards_by_id.put(c.pgroup + "+" + c.vgroup, c);
			}
		}
	};

	private Runnable loadStats = new Runnable() {
		@Override
		public void run() {
			game.log(this, "Loading Active Deck ...");
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

	private final long maxTime = 20l * 60l * 60l * 1000l;// 20 minutes max per
															// session ?

	private final NewCardDialog newCardDialog;

	private final Set<String> nodupes = new HashSet<>();
	private final Random rand = new Random();

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

	private Runnable showACard = new Runnable() {

		@Override
		public void run() {
			stage.addAction(Actions.run(saveStats));
			final ActiveCard activeCard = getNextCard();
			String card_id = activeCard.pgroup + "+" + activeCard.vgroup;
			final Card deckCard = cards_by_id.get(card_id);
			if (activeCard.newCard) {
				ticktock.stop();
				newCardDialog.setCounter(cardcount++);
				newCardDialog.setCard(deckCard);
				newCardDialog.show(stage);
				activeCard.box = 0;
				activeCard.correct_in_a_row.clear();
				activeCard.newCard = false;
				activeCard.show_again_ms = Deck.intervals.get(0);
				reInsertCard(activeCard);
			} else {
				ticktock_id = ticktock.play(0f);
				ticktock.setLooping(ticktock_id, true);
				challengeCardDialog.setCounter(cardcount++);
				challengeCardDialog.setCard(activeCard, deckCard);
				challengeCardDialog.show(stage);
				challengeCardDialog.addAnswers(getAnswerSetsFor(activeCard,
						deckCard, deck));
				challengeCardDialog.addAction(Actions.delay(MaxTimePerCard_sec,
						Actions.run(new Runnable() {
							@Override
							public void run() {
								challengeCardDialog.result(null);
							}
						})));				
				for (float x = MaxTimePerCard_sec; x >= 0; x-=.1f) {
					final float timer = MaxTimePerCard_sec - x;
					final float volume = x/MaxTimePerCard_sec;
					DelayAction updater = Actions.delay(x - .05f, Actions.run(new Runnable() {
						@Override
						public void run() {
							ticktock.setVolume(ticktock_id, volume);
							challengeCardDialog.setTimer(timer);							
						}
					}));
					challengeCardDialog.addAction(updater);
				}				
				activeCard.tries_remaining--;
			}
			elapsed_sec = 0f;
		}

	};

	private final Skin skin;

	private final FileHandle slot;

	protected Comparator<ActiveCard> sortByNextShow = new Comparator<ActiveCard>() {
		@Override
		public int compare(ActiveCard o1, ActiveCard o2) {
			if (o1.show_again_ms != o2.show_again_ms) {
				return o1.show_again_ms > o2.show_again_ms ? 1 : -1;
			}
			return o1.box - o2.box;
		}
	};

	private Sound ticktock;

	private long ticktock_id;

	private int totalshows = 0;

	public LearningSession(BoundPronouns _game, Screen caller, FileHandle slot) {
		super(_game, caller);
		this.slot = slot;
		slot.mkdirs();
		if (slot.child("deck.json").exists()) {
			slot.child("deck.json").delete();
		}

		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		container = new Table(skin);
		container.setBackground(d);
		container.setFillParent(true);
		stage.addActor(container);
		stage.addAction(Actions.delay(.05f, Actions.run(loadDeck)));
		json = new Json();
		json.setOutputType(OutputType.json);
		json.setTypeName(null);

		newCardDialog = new NewCardDialog(game, skin) {
			@Override
			protected void doNav() {
				game.setScreen(LearningSession.this.caller);
				LearningSession.this.dispose();
			}

			@Override
			protected void result(Object object) {
				this.clearActions();
				stage.addAction(Actions.run(showACard));
			}

			@Override
			public Dialog show(Stage stage) {
				return super.show(stage);
			}
		};
		challengeCardDialog = new ChallengeCardDialog(game, skin) {
			Runnable hideThisCard = new Runnable() {
				@Override
				public void run() {
					hide();
				}
			};

			@Override
			protected void doNav() {
				game.setScreen(LearningSession.this.caller);
				LearningSession.this.dispose();
			}

			@Override
			protected void result(Object object) {
				this.clearActions();
				setTimer(0);
				ticktock.stop();
				cancel();
				/**
				 * set when any wrong
				 */
				boolean doBuzzer = false;
				/**
				 * worst case scenario, all wrong ones marked and no right ones
				 * marked, gets set to false if ANY combination of
				 * checked/unchecked is valid
				 */
				boolean doCow = true;
				for (Actor b : getButtonTable().getChildren()) {
					if (b instanceof Button) {
						((Button) b).setDisabled(true);
					}
					if (b instanceof TextButton) {
						TextButton tb = (TextButton) b;
						if (tb.getUserObject() != null
								&& tb.getUserObject() instanceof Answer) {
							Answer ans = (Answer) tb.getUserObject();

							if (!tb.isChecked() && !ans.correct) {
								tb.addAction(Actions.fadeOut(.2f));
								doCow = false;
							}
							if (tb.isChecked() && !ans.correct) {
								ColorAction toRed = Actions.color(Color.RED,
										.4f);
								tb.addAction(toRed);
								tb.setText(BoundPronouns.HEAVY_BALLOT_X + " "
										+ ans.answer);
								doBuzzer = true;
							}
							if (!tb.isChecked() && ans.correct) {
								ColorAction toGreen = Actions.color(
										Color.GREEN, .4f);
								ColorAction toClear = Actions.color(
										Color.CLEAR, .2f);
								SequenceAction sequence = Actions.sequence(
										toClear, toGreen);
								tb.addAction(Actions.repeat(2, sequence));
								tb.setText(BoundPronouns.RIGHT_ARROW + " "
										+ ans.answer);
								doBuzzer = true;
							}
							if (tb.isChecked() && ans.correct) {
								ColorAction toGreen = Actions.color(
										Color.GREEN, .2f);
								tb.addAction(toGreen);
								doCow = false;
								tb.setText(BoundPronouns.HEAVY_CHECK_MARK + " "
										+ ans.answer);
							}
						}
					}
				}
				if (doCow) {
					cow.play();
				}
				if (doBuzzer && !doCow) {
					buzzer.play();
				}
				if (!doCow && !doBuzzer) {
					ding.play();
				}
				stage.addAction(Actions.delay(doBuzzer ? 5.9f : .9f,
						Actions.run(hideThisCard)));
				stage.addAction(Actions.delay(doBuzzer ? 6f : 1f,
						Actions.run(showACard)));
			}
		};
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
			next.correct_in_a_row.clear();
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
			stat.correct_in_a_row.clear();
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

	@Override
	public void dispose() {
		super.dispose();
		game.manager.unload(BoundPronouns.SND_DING);
		game.manager.unload(BoundPronouns.SND_BUZZ);
		game.manager.unload(BoundPronouns.SND_COW);
		game.manager.unload(BoundPronouns.SND_TICKTOCK);
		buzzer = null;
		cow = null;
		ticktock = null;
	}

	private AnswerList getAnswerSetsFor(final ActiveCard active,
			final Card card, Deck deck) {
		/**
		 * contains copies of used answers, vgroups, and pgroups to prevent
		 * duplicates
		 */
		Set<String> already = new HashSet<String>();
		AnswerList answers = new AnswerList();

		/**
		 * for temporary manipulation of list data so we don't mess with master
		 * copies in cards, etc.
		 */
		List<String> tmp_answers = new ArrayList<String>();
		List<String> tmp_correct = new ArrayList<String>();

		already.add(card.pgroup);
		already.add(card.vgroup);
		already.addAll(card.answer);

		tmp_correct.clear();
		tmp_correct.addAll(card.answer);
		/**
		 * sort answers from least known to most known
		 */
		Collections.sort(tmp_correct, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Integer i1 = active.correct_in_a_row.get(o1);
				Integer i2 = active.correct_in_a_row.get(o2);
				i1 = (i1 == null ? 0 : i1);
				i2 = (i2 == null ? 0 : i2);
				if (i1 != i2) {
					return Integer.compare(i1, i2);
				}
				return o1.compareTo(o2);
			}
		});
		int r = rand.nextInt(tmp_correct.size()) + 1;
		for (int i = 0; i < r && i < maxCorrect; i++) {
			String answer = tmp_correct.get(i);
			answers.list.add(0, new Answer(true, answer, 0));
		}

		/*
		 * look for "similar" looking answers
		 */
		Deck tmp = new Deck();
		tmp.cards.addAll(deck.cards);
		Collections.shuffle(tmp.cards);
		scanDeck: for (int distance = 1; distance < 10; distance++) {
			for (Card deckCard : deck.cards) {
				/*
				 * make sure we have unique pronouns for each wrong answer
				 */
				if (already.contains(deckCard.pgroup)) {
					continue;
				}
				/*
				 * make sure we keep bare pronouns with bare pronouns and
				 * vice-versa
				 */
				if (StringUtils.isBlank(card.vgroup) != StringUtils
						.isBlank(deckCard.vgroup)) {
					continue;
				}
				/*
				 * keep verbs unique as well
				 */
				if (!StringUtils.isBlank(deckCard.vgroup)) {
					if (already.contains(deckCard.vgroup)) {
						continue;
					}
				}
				/**
				 * if edit distance is close enough, add it, then add pgroup,
				 * vgroup and selected answer to already used list
				 */

				tmp_answers.clear();
				tmp_answers.addAll(deckCard.answer);
				Collections.shuffle(tmp_answers);
				addWrongAnswer: for (String t : tmp_answers) {
					if (already.contains(t)) {
						continue;
					}
					tmp_correct.clear();
					tmp_correct.addAll(card.answer);
					Collections.shuffle(tmp_correct);
					for (String s : card.answer) {
						int ldistance = StringUtils.getLevenshteinDistance(s,
								t, distance);
						if (ldistance < 1) {
							continue;
						}
						answers.list.add(new Answer(false, t, ldistance));
						already.add(deckCard.pgroup);
						already.add(deckCard.vgroup);
						already.add(t);
						break addWrongAnswer;
					}
				}
			}
			if (answers.list.size() > maxAnswers) {
				break scanDeck;
			}
		}
		Collections.sort(answers.list, byDistance);
		if (answers.list.size() > maxAnswers) {
			answers.list.subList(maxAnswers, answers.list.size()).clear();
		}
		for (Answer a : answers.list) {
			game.log(this, a.toString());
		}
		Collections.shuffle(answers.list);
		return answers;
	}

	private ActiveCard getNextCard() {
		if (current_active.stats.size() == 0) {
			current_active.stats.addAll(current_seen.stats);
			current_seen.stats.clear();
			if (current_active.stats.size() == 0) {
				return null;
			}
		}
		ActiveCard card = current_active.stats.get(0);
		current_seen.stats.add(card);
		current_active.stats.remove(0);
		return card;
	}

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

	private void reInsertCard(ActiveCard card) {
		if (current_active.stats.size() < 2) {
			current_active.stats.add(card);
		} else {
			current_active.stats.add(1, card);
		}
		current_seen.stats.remove(card);
	}

	@Override
	public void render(float delta) {
		super.render(delta);
	}

	@Override
	public void show() {
		super.show();
		game.manager.load(BoundPronouns.SND_DING, Sound.class);
		game.manager.load(BoundPronouns.SND_BUZZ, Sound.class);
		game.manager.load(BoundPronouns.SND_COW, Sound.class);
		game.manager.load(BoundPronouns.SND_TICKTOCK, Sound.class);
		game.manager.finishLoading();
		ding = game.manager.get(BoundPronouns.SND_DING, Sound.class);
		ding.play();
		buzzer = game.manager.get(BoundPronouns.SND_BUZZ, Sound.class);
		cow = game.manager.get(BoundPronouns.SND_COW, Sound.class);
		ticktock = game.manager.get(BoundPronouns.SND_TICKTOCK, Sound.class);
	}

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
