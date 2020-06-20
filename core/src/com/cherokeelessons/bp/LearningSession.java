package com.cherokeelessons.bp;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ColorAction;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.bp.LearningSession.SaveActiveDeckWithDialog.SaveParams;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.Answer;
import com.cherokeelessons.cards.Answer.AnswerList;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.cards.SlotInfo;
import com.cherokeelessons.cards.SlotInfo.DeckMode;
import com.cherokeelessons.cards.SlotInfo.SessionLength;
import com.cherokeelessons.cards.SlotInfo.TimeLimit;
import com.cherokeelessons.util.GameUtils;
import com.cherokeelessons.util.JsonConverter;
import com.cherokeelessons.util.Log;
import com.cherokeelessons.util.RandomName;

public class LearningSession extends ChildScreen {

	private class ActiveDeckLoader implements Runnable {
		@Override
		public void run() {
			log.info("Loading Active Deck ...");
			if (!slot.child(ACTIVE_DECK_JSON).exists()) {
				json.toJson(new ActiveDeck(), slot.child(ACTIVE_DECK_JSON));
			}
			
			try {
				final ActiveDeck tmp = json.fromJson(ActiveDeck.class, slot.child(ACTIVE_DECK_JSON));
				current_due.deck = tmp.deck;
				current_due.lastrun = tmp.lastrun;
				Collections.sort(current_due.deck, byShowTime);
			} catch (Exception e) {
				current_due.deck=new ActiveDeck().deck;
				current_due.lastrun=0;
			}

			stage.addAction(Actions.run(processActiveCards));
		}
	}

	private class LoadMasterDeck implements Runnable {
		@Override
		public void run() {
			log.info("Loading Master Deck...");
			stage.addAction(Actions.run(activeDeckLoader));
			log.info("DeckMode=" + info.settings.deck.name() + ", Card count=" + game.deck.cards.size());
		}
	}

	private class ProcessActiveCards implements Runnable {
		private void markAllNoErrors(final ActiveDeck deck) {
			for (final ActiveCard card : deck.deck) {
				card.noErrors = true;
			}
		}

		private void onlyPositiveBoxValues(final ActiveDeck deck) {
			for (final ActiveCard card : deck.deck) {
				if (card.box < 0) {
					card.box = 0;
					continue;
				}
			}
		}

		private void resetScoring(final ActiveDeck deck) {
			for (final ActiveCard card : deck.deck) {
				card.showCount = 0;
				card.showTime = 0f;
			}
		}

		private void retireNotYetCards(final ActiveDeck current_pending) {
			final Iterator<ActiveCard> icard = current_pending.deck.iterator();
			while (icard.hasNext()) {
				final ActiveCard card = icard.next();
				if (card.show_again_ms < ONE_HOUR_ms) {
					continue;
				}
				current_done.deck.add(card);
				icard.remove();
			}
			game.log(this, "Moved " + current_done.deck.size()
					+ " future pending or fully learned cards into the 'done' deck.");
		}

		@Override
		public void run() {
			nodupes.clear();
			log.info("Processing Active Cards ...");

			final int needed = InitialDeckSize;

			/*
			 * time-shift all cards by exactly one day + one extra hour for safety
			 */
			updateTime(current_due, ONE_DAY_ms + ONE_HOUR_ms);
			int due = 0;
			for (final ActiveCard card : current_due.deck) {
				if (card.show_again_ms < 0) {
					due++;
				}
			}
			log.info(due + " cards previous cards are due.");

			/*
			 * Make sure we don't have active cards pointing to no longer existing master
			 * deck cards
			 */
			final Iterator<ActiveCard> ipending = current_due.deck.iterator();
			while (ipending.hasNext()) {
				final ActiveCard active = ipending.next();
				if (getCardById(active.pgroup, active.vgroup) != null) {
					continue;
				}
				ipending.remove();
				log.info("Removed no longer valid entry: " + active.pgroup + " - " + active.vgroup);
			}

			/*
			 * Reset as new cards that are at box 0 and were wrong alot
			 */
			resetAsNew(current_due);

			/*
			 * Reset 'scoring' related values for all cards
			 */
			resetScoring(current_due);

			/*
			 * ALWAYS force reset ALL correct in a row counts on load!
			 */
			resetCorrectInARow(current_due);

			/*
			 * RESET tries max count
			 */
			resetRetriesCount(current_due);

			/*
			 * ALWAYS start off as being eligible for "bump"
			 */
			markAllNoErrors(current_due);

			/*
			 * Make sure no boxes out of range
			 */
			onlyPositiveBoxValues(current_due);

			/*
			 * Reset tries count after box clamping done
			 */
			for (final ActiveCard card : current_due.deck) {
				card.resetTriesRemaining();
			}

			/*
			 * time-shift all cards by an additional seven days to pull in more cards if
			 * this is an extra practice session
			 */
			// if (isExtraPractice) {
			// updateTime(current_due, ONE_DAY_ms * 7l);
			// }

			/*
			 * mark cards already in the active deck
			 */
			recordAlreadySeen(current_due);

			/*
			 * move cards due tomorrow or later into the already done pile!
			 */
			retireNotYetCards(current_due);

			/*
			 * truncate card timings to minute (enables semi-shuffled ordering)
			 */
			truncateToNearestMinute(current_due.deck);

			/*
			 * initial shuffle
			 */
			Collections.shuffle(current_due.deck);

			/*
			 * resort deck, any cards with the same truncated show time stay in their local
			 * shuffled order
			 */
			Collections.sort(current_due.deck, byShowTimeChunks);

			/*
			 * add cards to the active deck
			 */
			addCards(needed, current_active);

			/*
			 * go!
			 */
			stage.addAction(Actions.run(showACard));

			log.info("Elapsed :" + elapsed);
		}

		private void truncateToNearestMinute(final List<ActiveCard> deck) {
			for (final ActiveCard card : deck) {
				card.show_again_ms = 60l * 1000l * (card.show_again_ms / (1000l * 60l));
			}
		}
	}

	public static class SaveActiveDeckWithDialog implements Runnable {
		public static class SaveParams {
			public Screen caller;
			public ActiveDeck deck;
			public float elapsed_secs;
			public BoundPronouns game;
			// public boolean isExtraPractice;
			public Skin skin;
			public FileHandle slot;
			public Stage stage;
		}

		private final SaveParams params;

		public SaveActiveDeckWithDialog(final SaveParams params) {
			this.params = params;
		}

		@Override
		public void run() {
			final JsonConverter json = new JsonConverter();

			params.deck.lastrun = System.currentTimeMillis() - (long) params.elapsed_secs * 1000l;
			Collections.sort(params.deck.deck, byShowTime);

			final SlotInfo info;
			final FileHandle infoFile = params.slot.child(INFO_JSON);
			if (!infoFile.exists()) {
				info = new SlotInfo();
				info.settings.name = RandomName.getRandomName();
			} else {
				info = json.fromJson(SlotInfo.class, infoFile);
				if (StringUtils.isBlank(info.settings.name)) {
					info.settings.name = RandomName.getRandomName();
				}
				infoFile.copyTo(params.slot.child(INFO_JSON + ".bak"));
			}
			SlotInfo.calculateStats(info, params.deck);
			SlotInfo.calculateTotalCardCount(info, params.game.deck.cards);

			final TextButtonStyle tbs = new TextButtonStyle(params.skin.get(TextButtonStyle.class));
			tbs.font = params.game.getFont(Font.SerifMedium);

			final TextButton btn_ok = new TextButton("OK", tbs);

//			Texture img_sync = params.game.manager.get(BoundPronouns.IMG_SYNC, Texture.class);
//			TextureRegionDrawable draw_sync = new TextureRegionDrawable(new TextureRegion(img_sync));
//			final ImageButton syncb = new ImageButton(draw_sync);
//			syncb.setTransform(true);
//			syncb.getImage().setScaling(Scaling.fit);
//			syncb.getImage().setColor(Color.DARK_GRAY);

			// String dtitle = params.isExtraPractice ? "Extra Practice Results"
			// : "Practice Results";
			final String dtitle = "Practice Results";
			final WindowStyle dws = new WindowStyle(params.skin.get(WindowStyle.class));
			dws.titleFont = params.game.getFont(Font.SerifLarge);
			final Dialog bye = new Dialog(dtitle, dws) {
				@SuppressWarnings("hiding")
				final Dialog bye = this;
				{
					final NumberFormat nf = NumberFormat.getInstance();
					getTitleLabel().setAlignment(Align.center);
					final Texture background = params.game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
					final TextureRegion region = new TextureRegion(background);
					final TiledDrawable tiled = new TiledDrawable(region);
					tiled.setMinHeight(0);
					tiled.setMinWidth(0);
					tiled.setTopHeight(params.game.getFont(Font.SerifLarge).getCapHeight() + 20);
					bye.background(tiled);

					final LabelStyle lstyle = new LabelStyle(params.skin.get(LabelStyle.class));
					lstyle.font = params.game.getFont(Font.SerifMedium);

					final StringBuilder sb = new StringBuilder();
					sb.append("Level: ");
					sb.append(info.level);
					sb.append("\n");
					sb.append("Score: ");
					sb.append(nf.format(info.lastScore));
					sb.append("\n");
					sb.append(nf.format(info.activeCards) + " cards");
					if (info.getTotalCards() > 0) {
						final int pct = info.activeCards * 100 / info.getTotalCards();
						sb.append(" out of ");
						sb.append(nf.format(info.getTotalCards()));
						sb.append(" (");
						sb.append(pct);
						sb.append("%)");
					}
					sb.append("\n");
					sb.append("You currently have a " + info.proficiency + "% proficiency level");
					sb.append("\n");
					final int minutes = (int) (params.elapsed_secs / 60f);
					final int seconds = (int) (params.elapsed_secs - minutes * 60f);
					sb.append("Total actual challenge time: " + minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
					final Label label = new Label(sb.toString(), lstyle);
					text(label);
					button(btn_ok, btn_ok);
				}

				@Override
				protected void result(final Object object) {
//					if (syncb.equals(object)) {
//						cancel();
//						return;
//					}
					final Screen current = params.game.getScreen();
					if (params.caller != null && !params.caller.equals(current)) {
						params.game.setScreen(params.caller);
						current.dispose();
					}
				}
			};
			bye.show(params.stage);
			bye.setModal(true);
			bye.setFillParent(true);

			FileHandle tmp = params.slot.child(ACTIVE_DECK_JSON + ".tmp");
			json.toJson(params.deck, tmp);
			tmp.moveTo(params.slot.child(ACTIVE_DECK_JSON));
			tmp.delete();
			tmp = params.slot.child(INFO_JSON + ".tmp");
			json.toJson(info, tmp);
			tmp.moveTo(params.slot.child(INFO_JSON));
			tmp.delete();
		}
	}

	private class ShowACard implements Runnable {

		private ActiveCard previousCard;

		Random r = new Random();

		/*
		 * genderizer
		 */
		private void randomizeSexes(final AnswerList answers) {
			for (final Answer answer : answers.list) {
				if (r.nextBoolean() && answer.answer.contains("himself")) {
					answer.answer = answer.answer.replace("He ", "She ");
					answer.answer = answer.answer.replace(" him", " her");
				}
				if (r.nextBoolean() && answer.answer.contains("Himself")) {
					answer.answer = answer.answer.replace("He ", "She ");
					answer.answer = answer.answer.replace("Him", "Her");
				}
				if (r.nextBoolean() && answer.answer.matches(".*\\b[Hh]is\\b.*")) {
					answer.answer = answer.answer.replaceFirst("\\b([Hh])is\\b", "$1er");
				}
				if (r.nextBoolean() && !answer.answer.contains("himself")) {
					answer.answer = answer.answer.replace("He ", "She ");
				}
				if (r.nextBoolean() && !answer.answer.contains("himself")) {
					answer.answer = answer.answer.replace(" him", " her");
				}
			}
		}

		@Override
		public void run() {
			if (elapsed > info.settings.sessionLength.getSeconds()) {
				elapsed_tick_on = false;
				log.info("No time remaining...");
				/**
				 * scan current discards for cards that need to get "bumped"
				 */
				for (final ActiveCard tmp : current_discards.deck) {
					if (tmp.noErrors && tmp.tries_remaining < 1) {
						tmp.box++;
						tmp.show_again_ms = tmp.show_again_ms + Deck.getNextSessionInterval(tmp.box);
						log.info("Bumped Card: " + tmp.pgroup + " " + tmp.vgroup);
					}
				}
				/**
				 * scan current discards for cards that need to get "downgraded"
				 */
				for (final ActiveCard tmp : current_discards.deck) {
					if (!tmp.noErrors && tmp.tries_remaining < 1) {
						tmp.box--;
						tmp.show_again_ms = tmp.show_again_ms + Deck.getNextSessionInterval(tmp.box);
						log.info("Retired Card: " + tmp.pgroup + " " + tmp.vgroup);
					}
				}

				final SaveParams params = new SaveParams();
				params.caller = LearningSession.this.caller;
				params.deck = new ActiveDeck();
				params.deck.deck.addAll(current_active.deck);
				params.deck.deck.addAll(current_due.deck);
				params.deck.deck.addAll(current_discards.deck);
				params.deck.deck.addAll(current_done.deck);
				params.elapsed_secs = elapsed;
				params.game = game;
				params.skin = skin;
				params.slot = slot;
				params.stage = stage;
				saveActiveDeckWithDialog = new SaveActiveDeckWithDialog(params);
				stage.addAction(Actions.run(saveActiveDeckWithDialog));
				return;
			}
			ActiveCard activeCard;
			do {
				activeCard = getNextCard();
				if (activeCard == null) {
					break;
				}
				if (!activeCard.equals(previousCard)) {
					break;
				}
				if (current_discards.deck.size() == 0 && current_active.deck.size() == 0) {
					break;
				}
				activeCard.show_again_ms = Deck.getNextInterval(activeCard.getMinCorrectInARow() + 1);
				previousCard = null;
			} while (true);

			if (activeCard == null) {
				if (elapsed < info.settings.sessionLength.getSeconds()) {
					if (current_discards.deck.size() < IncrementDeckBySize) {
						log.info("not enough discards remaining...");
						addCards(IncrementDeckBySize, current_active);
						Gdx.app.postRunnable(showACard);
						return;
					}
					log.info("session time is not up");
					final long shift_by_ms = getMinShiftTimeOf(current_discards);
					log.info("shifting discards to zero point: " + shift_by_ms / ONE_SECOND_ms);
					if (shift_by_ms >= 15l * ONE_SECOND_ms) {
						addCards(IncrementDeckBySize, current_active);
					}
					updateTime(current_discards, shift_by_ms);
					Gdx.app.postRunnable(showACard);
					return;
				}
				/*
				 * Session time is up, force time shift cards into active show range...
				 */
				if (elapsed > info.settings.sessionLength.getSeconds() && current_discards.deck.size() > 0) {
					final long shift_by_ms = getMinShiftTimeOf(current_discards);
					log.info("shifting discards to zero point: " + shift_by_ms / ONE_SECOND_ms);
					updateTime(current_discards, shift_by_ms);
					Gdx.app.postRunnable(showACard);
					return;
				}
				/*
				 * Fallback behavior.
				 */
				log.info("Forcing discards to be time shifted.");
				final long shift_by_ms = getMinShiftTimeOf(current_discards);
				addCards(IncrementDeckBySize, current_active);
				updateTime(current_discards, shift_by_ms);
				Gdx.app.postRunnable(showACard);
				return;
			}
			previousCard = activeCard;
			final Card deckCard = new Card(getCardById(activeCard.pgroup, activeCard.vgroup));
			final float sessionSeconds = info.settings.sessionLength.getSeconds();
			if (activeCard.newCard) {
				elapsed_tick_on = false;
				ticktock.stop(ticktock_id);
				if (elapsed < sessionSeconds) {
					newCardDialog.setTimeRemaining(sessionSeconds - elapsed);
				} else {
					newCardDialog.setTimeRemaining(0f);
				}
				newCardDialog.setCard(deckCard);
				newCardDialog.show(stage);
				activeCard.box = 0;
				activeCard.newCard = false;
				activeCard.show_again_ms = Deck.getNextInterval(0);
				reInsertCard(activeCard);
			} else {
				challenge_elapsed = 0f;
				elapsed_tick_on = true;
				ticktock_id = ticktock.loop(0f);
				if (elapsed < sessionSeconds) {
					challengeCardDialog.setTimeRemaining(sessionSeconds - elapsed);
				} else {
					challengeCardDialog.setTimeRemaining(0f);
				}
				challengeCardDialog.setCard(activeCard, deckCard);
				challengeCardDialog.show(stage);

				AnswerList tracked_answers;
				tracked_answers = GameUtils.getAnswerSetsBySimilarChallenges(activeCard, deckCard, game.deck);
				final AnswerList displayed_answers = new AnswerList(tracked_answers);
				randomizeSexes(displayed_answers);
				activeCard.tries_remaining--;
				challengeCardDialog.setAnswers(tracked_answers, displayed_answers);
				float duration = info.settings.timeLimit.getSeconds() - activeCard.box
						- activeCard.getMinCorrectInARow();
				if (duration < 4) {
					duration = 4f;
				}
				challengeCardDialog.addAction(Actions.delay(duration, Actions.run(new Runnable() {
					@Override
					public void run() {
						challengeCardDialog.result(null);
					}
				})));
				for (float x = duration; x >= 0; x -= .25f) {
					final float timer = duration - x;
					final float volume = x / duration;
					final DelayAction updater = Actions.delay(x - .05f, Actions.run(new Runnable() {
						@Override
						public void run() {
							if (challengeCardDialog.settings.muted) {
								ticktock.setVolume(ticktock_id, 0f);
							} else {
								ticktock.setVolume(ticktock_id, volume);
							}
							challengeCardDialog.setTimer(timer);
						}
					}));
					challengeCardDialog.addAction(updater);
				}
			}
		}
	}

	private static final Logger log = Log.getLogger(LearningSession.class.getName());

	// private class TooSoonDialog implements Runnable {
	// @Override
	// public void run() {
	// Dialog whichMode = new Dialog(
	// "It's too soon for a regular session.", skin) {
	// {
	// String text = "Please select an option:\n\n"
	// + "Would you like to practice your existing challenges?\n\n"
	// + "Would you like to jump forward by a full day?\n\n"
	// + "Would you like to cancel and go back to main menu?";
	//
	// LabelStyle lstyle = new LabelStyle(
	// skin.get(LabelStyle.class));
	// lstyle.font = game.getFont(Font.SerifMedium);
	// Label label = new Label(text, lstyle);
	// label.setAlignment(Align.left, Align.left);
	// label.setWrap(true);
	// getContentTable().clearChildren();
	// getContentTable().add(label).fill().expand().left();
	// TextButtonStyle tbs = new TextButtonStyle(
	// skin.get(TextButtonStyle.class));
	// tbs.font = game.getFont(Font.SerifMedium);
	// TextButton tb;
	// tb = new TextButton("DO A PRACTICE", tbs);
	// button(tb, "A");
	// tb = new TextButton("JUMP A DAY", tbs);
	// button(tb, "B");
	// tb = new TextButton("CANCEL", tbs);
	// button(tb, "C");
	// setFillParent(true);
	// this.getTitleLabel().setAlignment(Align.center);
	// }
	//
	// protected void result(Object object) {
	// if (object == null) {
	// return;
	// }
	// if (object.toString().equals("A")) {
	// LearningSession.this.isExtraPractice = true;
	// }
	// if (object.toString().equals("B")) {
	// LearningSession.this.isExtraPractice = false;
	// }
	// if (object.toString().equals("C")) {
	// game.setScreen(caller);
	// LearningSession.this.dispose();
	// return;
	// }
	// stage.addAction(Actions.run(processActiveCards));
	// };
	// };
	// whichMode.show(stage);
	// }
	// }

	public static final String ACTIVE_DECK_JSON = "ActiveDeck.json";

	private static Comparator<ActiveCard> byShowTime = new Comparator<ActiveCard>() {
		@Override
		public int compare(final ActiveCard o1, final ActiveCard o2) {
			if (o1.show_again_ms != o2.show_again_ms) {
				return o1.show_again_ms > o2.show_again_ms ? 1 : -1;
			}
			return o1.box - o2.box;
		}
	};

	private static Comparator<ActiveCard> byShowTimeChunks = new Comparator<ActiveCard>() {
		@Override
		public int compare(final ActiveCard o1, final ActiveCard o2) {
			long dif = o1.show_again_ms - o2.show_again_ms;
			if (dif < 0) {
				dif = -dif;
			}
			if (dif < ONE_MINUTE_ms) {
				return 0;
			}
			return o1.show_again_ms > o2.show_again_ms ? 1 : -1;
		}
	};

	private static final int IncrementDeckBySize = 3;

	private static final String INFO_JSON = BoundPronouns.INFO_JSON;

	private static final int InitialDeckSize = 5;

	private static final long ONE_DAY_ms;
	private static final long ONE_HOUR_ms;
	private static final long ONE_MINUTE_ms;
	private static final long ONE_SECOND_ms;
	static {
		ONE_SECOND_ms = 1000l;
		ONE_MINUTE_ms = 60l * ONE_SECOND_ms;
		ONE_HOUR_ms = 60l * ONE_MINUTE_ms;
		ONE_DAY_ms = 24l * ONE_HOUR_ms;
	}

	private final ActiveDeckLoader activeDeckLoader = new ActiveDeckLoader();

	private Sound buzzer;

	/**
	 * How long this challenge has been displayed.
	 */
	private float challenge_elapsed;

	// private int cardcount = 0;

	private final ChallengeCardDialog challengeCardDialog;

	private Table container;

	private Sound cow;

	/**
	 * currently being looped through for display
	 */
	private final ActiveDeck current_active = new ActiveDeck();

	/**
	 * holding area for cards that have just been displayed or are not scheduled yet
	 * for display
	 */
	private final ActiveDeck current_discards = new ActiveDeck();

	/**
	 * holding area for cards that should not be shown any more this session
	 */
	private final ActiveDeck current_done = new ActiveDeck();

	/**
	 * holding area for cards that are "due" but deck size says don't show yet
	 */
	private final ActiveDeck current_due = new ActiveDeck();

	private Sound ding;

	private Skin dskin = null;

	/**
	 * total challenge time accumulated
	 */
	private float elapsed = 0f;
	/**
	 * Whether elapsed time should be accumulated or not
	 */
	private boolean elapsed_tick_on = false;

	final private SlotInfo info;

	private final JsonConverter json;

	// private boolean isExtraPractice = false;

	private final LoadMasterDeck loadDeck = new LoadMasterDeck();

	private final NewCardDialog newCardDialog;
	private final Set<String> nodupes = new HashSet<>();
	/**
	 * used for logging periodic messages
	 */
	private float notice_elapsed = 0f;
	private float counter_elapsed = 0f;
	private final ProcessActiveCards processActiveCards = new ProcessActiveCards();
	private final Random rand = new Random();

	private SaveActiveDeckWithDialog saveActiveDeckWithDialog;

	private final ShowACard showACard = new ShowACard();

	/**
	 * time since last "shuffle"
	 */
	private float sinceLastNextCard_elapsed = 0f;

	private final Skin skin;

	private final FileHandle slot;

	private Sound ticktock;

	private long ticktock_id;

	public LearningSession(final BoundPronouns _game, final Screen caller, final FileHandle slot) {
		super(_game, caller);
		final int totalCards = game.deck.cards.size();
		current_active.deck = new ArrayList<>(totalCards);
		current_discards.deck = new ArrayList<>(totalCards);
		current_done.deck = new ArrayList<>(totalCards);
		current_due.deck = new ArrayList<>(totalCards);

		this.slot = slot;
		slot.mkdirs();
		final Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		container = new Table(skin);
		container.setBackground(d);
		container.setFillParent(true);
		stage.addActor(container);
		stage.addAction(Actions.delay(.05f, Actions.run(loadDeck)));
		json = new JsonConverter();

		final FileHandle infoFile = slot.child(INFO_JSON);
		if (!infoFile.exists()) {
			info = new SlotInfo();
			info.settings.name = RandomName.getRandomName();
			info.settings.sessionLength = SessionLength.Brief;
			info.settings.timeLimit = TimeLimit.Novice;
			info.settings.deck = DeckMode.Conjugations;
		} else {
			info = json.fromJson(SlotInfo.class, infoFile);
			info.settings.sessionLength = SessionLength.Brief;
			info.settings.timeLimit = TimeLimit.Novice;
			if (StringUtils.isBlank(info.settings.name)) {
				info.settings.name = RandomName.getRandomName();
			}
		}

		newCardDialog = new NewCardDialog(game, skin) {
			@Override
			protected void result(final Object object) {
				clearActions();
				stage.addAction(Actions.run(showACard));
			}

			@Override
			public Dialog show(@SuppressWarnings("hiding") final Stage stage) {
				return super.show(stage);
			}

			@Override
			protected void showMainMenu() {
				final Runnable yes = new Runnable() {
					@Override
					public void run() {
						game.setScreen(LearningSession.this.caller);
						LearningSession.this.dispose();
					}
				};
				final Runnable no = new Runnable() {
					@Override
					public void run() {
						// Do nothing
					}
				};
				final Dialog dialog = dialogYN("Please Confirm Exit",
						"Do you want to discard your session?\n(All of your work will be lost if you say yes.)", yes,
						no);
				dialog.getTitleLabel().setAlignment(Align.center);
				dialog.show(stage);
			}
		};

		challengeCardDialog = new ChallengeCardDialog(game, skin) {

			private static final String CONTINUE = "CONTINUE";
			private static final String CHECK = "CHECK!";

			Runnable hideThisCard = new Runnable() {
				@Override
				public void run() {
					check.setVisible(false);
					check.setText(CHECK);
					hide();
				}
			};

			DelayAction delayHideThisCard;
			DelayAction delayShowNextCard;

			@Override
			protected void result(final Object object) {
				navEnable(false);
				if (CONTINUE.equals(object)) {
					check.setTouchable(Touchable.disabled);
					check.setDisabled(true);
					stage.getRoot().removeAction(delayHideThisCard);
					stage.getRoot().removeAction(delayShowNextCard);
					stage.addAction(Actions.delay(.1f, Actions.run(hideThisCard)));
					stage.addAction(Actions.delay(.2f, Actions.run(showACard)));
					cancel();
					return;
				}
				/*
				 * bump show count and add in elapsed display time for later scoring ...
				 */
				_activeCard.showCount++;
				_activeCard.showTime += challenge_elapsed;
				clearActions();
				setCheckVisible(false);
				setTimer(0);
				ticktock.setVolume(ticktock_id, 0f);
				ticktock.stop(ticktock_id);
				cancel();
				/**
				 * set when any wrong
				 */
				boolean doBuzzer = false;
				/**
				 * worst case scenario, all wrong ones marked and no right ones marked, gets set
				 * to false if ANY combination of checked/unchecked is valid
				 */
				boolean doCow = true;
				int wrong = 0;
				for (final Actor b : getButtonTable().getChildren()) {
					if (b instanceof Button) {
						((Button) b).setDisabled(true);
						((Button) b).setTouchable(Touchable.disabled);
					}
					if (b instanceof TextButton) {
						final TextButton tb = (TextButton) b;
						final Object userObject = tb.getUserObject();
						if (userObject != null && userObject instanceof Answer) {
							final Answer tracked_answer = (Answer) userObject;
							if (!tb.isChecked() && !tracked_answer.correct) {
								tb.addAction(Actions.fadeOut(.2f));
								doCow = false;
							}
							if (tb.isChecked() && !tracked_answer.correct) {
								final ColorAction toRed = Actions.color(Color.RED, .4f);
								tb.addAction(toRed);
								tb.setText(BoundPronouns.HEAVY_BALLOT_X + " " + tb.getText());
								doBuzzer = true;
								resetCorrectInARow(_activeCard);
								_activeCard.noErrors = false;
								wrong++;
							}
							if (!tb.isChecked() && tracked_answer.correct) {
								final ColorAction toGreen = Actions.color(Color.GREEN, .4f);
								final ColorAction toClear = Actions.color(Color.CLEAR, .2f);
								final SequenceAction sequence = Actions.sequence(toClear, toGreen);
								tb.addAction(Actions.repeat(2, sequence));
								tb.setText(BoundPronouns.RIGHT_ARROW + " " + tb.getText());
								doBuzzer = true;
								resetCorrectInARow(_activeCard);
								if (_activeCard.noErrors && _activeCard.tries_remaining < 1) {
									/*
									 * set for at leat one more show if the first time a card is incorrectly
									 * answered there are no shows remaining
									 */
									_activeCard.tries_remaining++;
								}
								_activeCard.noErrors = false;
							}
							if (tb.isChecked() && tracked_answer.correct) {
								final ColorAction toGreen = Actions.color(Color.GREEN, .2f);
								tb.addAction(toGreen);
								doCow = false;
								tb.setText(BoundPronouns.HEAVY_CHECK_MARK + " " + tb.getText());
								_activeCard.markCorrect(tracked_answer.answer);
							}
						}
					}
				}
				if (wrong > 2) {
					doCow = true;
				}
				if (doCow) {
					if (!challengeCardDialog.settings.muted) {
						cow.play();
					}
				}
				if (doBuzzer && !doCow) {
					if (!challengeCardDialog.settings.muted) {
						buzzer.play();
					}
				}
				if (!doCow && !doBuzzer) {
					if (!challengeCardDialog.settings.muted) {
						ding.play();
					}
				}
				_activeCard.show_again_ms = Deck.getNextInterval(_activeCard.getMinCorrectInARow());
				delayHideThisCard = Actions.delay(doBuzzer ? 5.9f : .9f, Actions.run(hideThisCard));
				delayShowNextCard = Actions.delay(doBuzzer ? 6f : 1f, Actions.run(showACard));
				stage.addAction(delayHideThisCard);
				stage.addAction(delayShowNextCard);

				setObject(check, CONTINUE);
				if (doBuzzer) {
					check.setText(CONTINUE);
					check.setVisible(true);
					check.setDisabled(false);
					check.setTouchable(Touchable.enabled);
				}
			}

			@Override
			protected void showMainMenu() {
				final boolean wasPaused = challengeCardDialog.paused;
				final Runnable yes = new Runnable() {
					@Override
					public void run() {
						game.setScreen(LearningSession.this.caller);
						LearningSession.this.dispose();
					}
				};
				final Runnable no = new Runnable() {
					@Override
					public void run() {
						challengeCardDialog.paused = wasPaused;
					}
				};
				final Dialog dialog = dialogYN("Please Confirm Exit",
						"Do you want to discard your session?\n(All of your work will be lost if you say yes.)", yes,
						no);
				dialog.show(stage);
				challengeCardDialog.paused = true;
			}
		};

		newCardDialog.settings = info.settings;
		challengeCardDialog.settings = info.settings;
		challengeCardDialog.updateMuteButtonText();
	}

	/**
	 * add this many cards to the Current Active Deck first from the current Active
	 * Deck then from the master Deck set
	 *
	 * @param needed
	 * @param active
	 */
	public void addCards(int needed, final ActiveDeck active) {
		int startingSize = active.deck.size();
		/**
		 * look for previous cards to load first, if their delay time is up
		 */
		Iterator<ActiveCard> ipending = current_due.deck.iterator();
		while (needed > 0 && ipending.hasNext()) {
			final ActiveCard next = ipending.next();
			if (next.box >= SlotInfo.FULLY_LEARNED_BOX) {
				continue;
			}
			if (next.show_again_ms > 0) {
				continue;
			}
			next.resetTriesRemaining();
			active.deck.add(next);
			needed--;
			ipending.remove();
		}
		log.info("Added " + (active.deck.size() - startingSize) + " previous cards.");

		if (needed <= 0) {
			return;
		}

		/**
		 * not enough already seen cards, add new never seen cards
		 */
		startingSize = active.deck.size();
		final Iterator<Card> ideck = game.deck.cards.iterator();
		while (needed > 0 && ideck.hasNext()) {
			final Card next = ideck.next();
			switch (info.settings.deck) {
			case Both:
				break;
			case Conjugations:
				if (StringUtils.isBlank(next.vgroup)) {
					continue;
				}
				break;
			case Pronouns:
				if (!StringUtils.isBlank(next.vgroup)) {
					continue;
				}
				break;
			default:
				break;
			}
			final String unique_id = next.pgroup + "+" + next.vgroup;
			if (nodupes.contains(unique_id)) {
				continue;
			}
			final ActiveCard activeCard = new ActiveCard();
			activeCard.box = 0;
			activeCard.noErrors = true;
			activeCard.newCard = true;
			activeCard.pgroup = next.pgroup;
			activeCard.show_again_ms = 0;
			activeCard.vgroup = next.vgroup;
			resetCorrectInARow(activeCard);
			activeCard.resetTriesRemaining();
			active.deck.add(activeCard);
			needed--;
			nodupes.add(unique_id);
		}
		log.info("Added " + (active.deck.size() - startingSize) + " new cards.");

		if (needed <= 0) {
			return;
		}

		/**
		 * yikes! They processed ALL the cards!
		 */
		ipending = current_due.deck.iterator();
		while (needed > 0 && ipending.hasNext()) {
			final ActiveCard next = ipending.next();
			if (next.box < SlotInfo.FULLY_LEARNED_BOX) {
				continue;
			}
			next.resetTriesRemaining();
			active.deck.add(next);
			needed--;
			ipending.remove();
		}
		ipending = current_due.deck.iterator();
		while (needed > 0 && ipending.hasNext()) {
			final ActiveCard next = ipending.next();
			next.resetTriesRemaining();
			active.deck.add(next);
			needed--;
			ipending.remove();
		}

	}

	private Dialog dialogYN(final String title, String message, final Runnable yes, final Runnable no) {
		if (dskin == null) {
			dskin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));
		}
		final WindowStyle ws = new WindowStyle(dskin.get(WindowStyle.class));
		ws.titleFont = game.getFont(Font.SerifLLarge);
		final LabelStyle ls = new LabelStyle(dskin.get(LabelStyle.class));
		message = WordUtils.wrap(message, 70);
		ls.font = game.getFont(Font.SerifLarge);
		final Label msg = new Label(message, ls);
		msg.setAlignment(Align.center);
		final TextButtonStyle tbs = new TextButtonStyle(dskin.get(TextButtonStyle.class));
		tbs.font = game.getFont(Font.SerifLarge);
		final TextButton btn_yes = new TextButton("YES", tbs);
		final TextButton btn_no = new TextButton("NO", tbs);
		final Dialog dialog = new Dialog(title, ws) {
			@Override
			protected void result(final Object object) {
				super.result(object);
				if (btn_yes.equals(object)) {
					Gdx.app.postRunnable(yes);
				}
				if (btn_no.equals(object)) {
					Gdx.app.postRunnable(no);
				}
			}
		};
		dialog.getTitleLabel().setAlignment(Align.center);
		dialog.text(msg);
		dialog.button(btn_yes, btn_yes);
		dialog.button(btn_no, btn_no);
		return dialog;
	}

	@Override
	public void dispose() {
		super.dispose();
		buzzer.stop();
		cow.stop();
		ding.stop();
		ticktock.stop();
		buzzer = null;
		cow = null;
		ding = null;
		ticktock = null;
		game.manager.unload(BoundPronouns.SND_DING);
		game.manager.unload(BoundPronouns.SND_BUZZ);
		game.manager.unload(BoundPronouns.SND_COW);
		game.manager.unload(BoundPronouns.SND_TICKTOCK);
		if (dskin != null) {
			dskin.dispose();
			dskin = null;
		}
	}

	

	private Card getCardById(final String pgroup, final String vgroup) {
		for (final Card card : game.deck.cards) {
			if (!card.pgroup.equals(pgroup)) {
				continue;
			}
			if (!card.vgroup.equals(vgroup)) {
				continue;
			}
			return card;
		}
		return null;
	}

	/**
	 * Calculates amount of ms needed to shift by to move deck to "0" point.
	 *
	 * @param current_pending
	 * @return
	 */
	private long getMinShiftTimeOf(final ActiveDeck current_pending) {
		if (current_pending.deck.size() == 0) {
			return 0;
		}
		long by = Long.MAX_VALUE;
		final Iterator<ActiveCard> icard = current_pending.deck.iterator();
		while (icard.hasNext()) {
			final ActiveCard card = icard.next();
			if (card.tries_remaining < 1) {
				continue;
			}
			if (by > card.show_again_ms) {
				by = card.show_again_ms;
			}
		}
		if (by == Long.MAX_VALUE) {
			by = ONE_MINUTE_ms;
		}
		return by;
	}

	private ActiveCard getNextCard() {
		if (current_active.deck.size() == 0) {
			updateTime(current_discards, (long) (sinceLastNextCard_elapsed * 1000f));
			sinceLastNextCard_elapsed = 0;
			Iterator<ActiveCard> discards;
			discards = current_discards.deck.iterator();
			/**
			 * remove from active session any cards with no tries left
			 */
			while (discards.hasNext()) {
				final ActiveCard discard = discards.next();
				if (discard.tries_remaining > 0) {
					continue;
				}
				if (discard.noErrors) {
					discard.box++;
					current_done.deck.add(discard);
					discard.show_again_ms = discard.show_again_ms + Deck.getNextSessionInterval(discard.box);
					discards.remove();
					log.info("Bumped Card: " + discard.pgroup + " " + discard.vgroup);
					continue;
				}
				discard.box--;
				current_done.deck.add(discard);
				discard.show_again_ms = discard.show_again_ms + Deck.getNextSessionInterval(discard.box);
				discards.remove();
				log.info("Retired Card: " + discard.pgroup + " " + discard.vgroup);
				return getNextCard();
			}
			discards = current_discards.deck.iterator();
			/**
			 * Find all cards in active session ready for display by time
			 */
			while (discards.hasNext()) {
				final ActiveCard tmp = discards.next();
				if (tmp.show_again_ms > 0) {
					continue;
				}
				current_active.deck.add(tmp);
				discards.remove();
			}
			if (current_active.deck.size() == 0) {
				return null;
			}
			Collections.shuffle(current_active.deck);
			Collections.sort(current_active.deck, byShowTimeChunks);
		}
		final ActiveCard card = current_active.deck.get(0);
		current_active.deck.remove(0);
		current_discards.deck.add(card);
		return card;
	}

	/**
	 * record all cards currently "in-play" so that when cards are retrieved from
	 * the master deck they are new cards
	 *
	 * @param activeDeck
	 */
	public void recordAlreadySeen(final ActiveDeck activeDeck) {
		final Iterator<ActiveCard> istat = activeDeck.deck.iterator();
		while (istat.hasNext()) {
			final ActiveCard next = istat.next();
			final String unique_id = next.pgroup + "+" + next.vgroup;
			nodupes.add(unique_id);
		}
	}

	private void reInsertCard(final ActiveCard card) {
		if (current_active.deck.size() < 2) {
			current_active.deck.add(card);
		} else {
			current_active.deck.add(1, card);
		}
		current_discards.deck.remove(card);
	}

	@Override
	public void render(final float delta) {
		if (challengeCardDialog.paused) {
			BoundPronouns.glClearColor();
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			stage.draw();
			ticktock.setVolume(ticktock_id, 0f);
			return;
		}
		if (elapsed_tick_on) {
			challenge_elapsed += delta;
			sinceLastNextCard_elapsed += delta;
			elapsed += delta;
			notice_elapsed += delta;
			if (notice_elapsed > 60f) {
				notice_elapsed = 0f;
				final int mins = (int) (elapsed / 60);
				final int secs = (int) (elapsed - mins * 60f);
				log.info(mins + ":" + (secs < 10 ? "0" : "") + secs);
			}
			final float sessionSeconds = info.settings.sessionLength.getSeconds();
			if (counter_elapsed + .5f > elapsed) {
				counter_elapsed = elapsed;
				if (elapsed < sessionSeconds) {
					challengeCardDialog.setTimeRemaining(sessionSeconds - elapsed);
				} else {
					challengeCardDialog.setTimeRemaining(0f);
				}
			}
		}
		super.render(delta);
	}

	private void resetAsNew(@SuppressWarnings("hiding") final ActiveDeck current_due) {
		for (final ActiveCard card : current_due.deck) {
			if (card.noErrors) {
				continue;
			}
			if (card.box > 0) {
				continue;
			}
			if (card.getMinCorrectInARow() > 2) {
				continue;
			}
			card.newCard = true;
			log.info("Resetting as new: " + getCardById(card.pgroup, card.vgroup).challenge.toString());
		}
	}

	public void resetCorrectInARow(final ActiveCard card) {
		final Card dcard = getCardById(card.pgroup, card.vgroup);
		if (dcard == null) {
			card.resetCorrectInARow(new ArrayList<String>());
			return;
		}
		card.resetCorrectInARow(dcard.answer);
	}

	private void resetCorrectInARow(final ActiveDeck current_pending) {
		for (final ActiveCard card : current_pending.deck) {
			resetCorrectInARow(card);
		}
	}

	protected void resetRetriesCount(final ActiveDeck deck) {
		for (final ActiveCard card : deck.deck) {
			card.resetTriesRemaining();
		}
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
		buzzer = game.manager.get(BoundPronouns.SND_BUZZ, Sound.class);
		cow = game.manager.get(BoundPronouns.SND_COW, Sound.class);
		ticktock = game.manager.get(BoundPronouns.SND_TICKTOCK, Sound.class);
	}

	/**
	 * time-shift all cards by time since last recorded run.
	 *
	 * @param currentDeck
	 * @param ms
	 */
	public void updateTime(final ActiveDeck currentDeck, final long ms) {
		final Iterator<ActiveCard> istat = currentDeck.deck.iterator();
		while (istat.hasNext()) {
			final ActiveCard next = istat.next();
			next.show_again_ms -= ms;
		}
	}
}
