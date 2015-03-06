package com.cherokeelessons.bp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

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
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.Answer;
import com.cherokeelessons.cards.Answer.AnswerList;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.cards.SlotInfo;
import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.JsonConverter;

public class LearningSession extends ChildScreen implements Screen {

	private static final String INFO_JSON = BoundPronouns.INFO_JSON;

	private static final long ONE_MINUTE_ms = 60l * 1000l;

	private static final long ONE_DAY_ms = 24l * 60l * ONE_MINUTE_ms;

	private static final long ONE_HOUR_ms = 60l * ONE_MINUTE_ms;

	public static final String ActiveDeckJson = "ActiveDeck.json";

	private static final int maxAnswers = 6;

	private static final int maxCorrect = 6;

	private static final int SendToNextSessionThreshold = 4;

	private static final int InitialDeckSize = 5;

	private static final int IncrementDeckBySize = 3;

	private static final long ONE_SECOND_ms = 1000l;

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
			if (o1.distance < o2.distance) {
				return -1;
			}
			if (o1.distance > o2.distance) {
				return 1;
			}
			return 0;
		}
	};
	private int cardcount = 0;
	private final ChallengeCardDialog challengeCardDialog;
	private Table container;
	private Sound cow;

	/**
	 * currently being looped through for display
	 */
	private final ActiveDeck current_active = new ActiveDeck();
	/**
	 * holding area for cards that have just been displayed or are not scheduled
	 * yet for display
	 */
	private final ActiveDeck current_discards = new ActiveDeck();
	/**
	 * holding area for cards that are "due" but deck size says don't show yet
	 */
	private final ActiveDeck current_due = new ActiveDeck();
	/**
	 * holding area for cards that should not be shown any more this session
	 */
	private final ActiveDeck current_done = new ActiveDeck();

	private Sound ding;

	private class ProcessActiveCards implements Runnable {
		@Override
		public void run() {
			nodupes.clear();
			game.log(this, "Processing Active Cards ...");

			int needed = InitialDeckSize;

			/*
			 * time-shift all cards by exactly one day + one extra hour for
			 * safety
			 */
			updateTime(current_due, ONE_DAY_ms + ONE_HOUR_ms);
			int due = 0;
			for (ActiveCard card : current_due.deck) {
				if (card.show_again_ms < 0) {
					due++;
				}
			}
			game.log(this, due + " cards previous cards are due.");

			/*
			 * Make sure we don't have active cards pointing to no longer
			 * existing master deck cards
			 */
			Iterator<ActiveCard> ipending = current_due.deck.iterator();
			while (ipending.hasNext()) {
				ActiveCard active = ipending.next();
				if (getCardById(active.pgroup, active.vgroup) != null) {
					continue;
				}
				ipending.remove();
				game.log(this, "Removed no longer valid entry: "
						+ active.pgroup + " - " + active.vgroup);
			}
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
			 * time-shift all cards by an additional seven days to pull in more
			 * cards if this is an extra practice session
			 */
			if (isExtraPractice) {
				updateTime(current_due, ONE_DAY_ms * 7l);
			}

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
			 * resort deck, any cards with the same truncated show time stay in
			 * their local shuffled order
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
		}

		private void resetScoring(ActiveDeck deck) {
			for (ActiveCard card : deck.deck) {
				card.showCount = 0;
				card.showTime = 0f;
			}
		}

		private void truncateToNearestMinute(List<ActiveCard> deck) {
			for (ActiveCard card : deck) {
				card.show_again_ms = (60l * 1000l)
						* (card.show_again_ms / (1000l * 60l));
			}
		}

		private void retireNotYetCards(ActiveDeck current_pending) {
			Iterator<ActiveCard> icard = current_pending.deck.iterator();
			while (icard.hasNext()) {
				ActiveCard card = icard.next();
				if (card.show_again_ms < ONE_HOUR_ms
						&& card.box < SlotInfo.PROFICIENT_BOX) {
					continue;
				}
				current_done.deck.add(card);
				icard.remove();
			}
			game.log(
					this,
					"Moved "
							+ current_done.deck.size()
							+ " future pending or fully learned cards into the 'done' deck.");
		}

		private void onlyPositiveBoxValues(ActiveDeck deck) {
			for (ActiveCard card : deck.deck) {
				if (card.box < 0) {
					card.box = 0;
					continue;
				}
			}

		}

		private void markAllNoErrors(ActiveDeck deck) {
			for (ActiveCard card : deck.deck) {
				card.noErrors = true;
			}
		}
	}

	private ProcessActiveCards processActiveCards = new ProcessActiveCards() {
	};

	private void resetCorrectInARow(ActiveDeck current_pending) {
		for (ActiveCard card : current_pending.deck) {
			resetCorrectInARow(card);
		}
	}

	protected void resetRetriesCount(ActiveDeck deck) {
		for (ActiveCard card : deck.deck) {
			Card dcard = getCardById(card.pgroup, card.vgroup);
			card.tries_remaining = SendToNextSessionThreshold
					* dcard.answer.size();
		}
	}

	public void resetCorrectInARow(ActiveCard card) {
		Card dcard = getCardById(card.pgroup, card.vgroup);
		if (dcard == null) {
			card.resetCorrectInARow(new ArrayList<String>());
			return;
		}
		card.resetCorrectInARow(dcard.answer);
	}

	private final JsonConverter json;

	private class LoadMasterDeck implements Runnable {
		@Override
		public void run() {
			game.log(this, "Loading Master Deck...");
			stage.addAction(Actions.run(activeDeckLoader));

			game.log(this, "Loaded " + info.settings.deck.name() + " "
					+ game.deck.cards.size() + " master cards.");
		}
	}

	private LoadMasterDeck loadDeck = new LoadMasterDeck() {
	};

	private class TooSoonDialog implements Runnable {
		@Override
		public void run() {
			Dialog whichMode = new Dialog(
					"It's too soon for a regular session.", skin) {
				{
					String text = "Please select an option:\n\n"
							+ "Would you like to practice your existing challenges?\n\n"
							+ "Would you like to jump forward by a full day?\n\n"
							+ "Would you like to cancel and go back to main menu?";

					LabelStyle lstyle = skin.get(LabelStyle.class);
					// lstyle.font = game.getFont(Font.SansMedium);
					lstyle.font = game.getFont(Font.SerifMedium);
					Label label = new Label(text, lstyle);
					label.setAlignment(Align.left, Align.left);
					label.setWrap(true);
					getContentTable().clearChildren();
					getContentTable().add(label).fill().expand().left();
					TextButtonStyle tbs = new TextButtonStyle(
							skin.get(TextButtonStyle.class));
					// tbs.font = game.getFont(Font.SansMedium);
					tbs.font = game.getFont(Font.SerifMedium);
					TextButton tb;
					tb = new TextButton("DO A PRACTICE", tbs);
					button(tb, "A");
					tb = new TextButton("JUMP A DAY", tbs);
					button(tb, "B");
					tb = new TextButton("CANCEL", tbs);
					button(tb, "C");
					setFillParent(true);
				}

				protected void result(Object object) {
					if (object == null) {
						return;
					}
					if (object.toString().equals("A")) {
						LearningSession.this.isExtraPractice = true;
					}
					if (object.toString().equals("B")) {
						LearningSession.this.isExtraPractice = false;
					}
					if (object.toString().equals("C")) {
						game.setScreen(caller);
						LearningSession.this.dispose();
						return;
					}
					stage.addAction(Actions.run(processActiveCards));
				};
			};
			whichMode.show(stage);
		}
	}

	private TooSoonDialog tooSoon = new TooSoonDialog() {
	};

	private class ActiveDeckLoader implements Runnable {
		@Override
		public void run() {
			game.log(this, "Loading Active Deck ...");

			if (!slot.child(ActiveDeckJson).exists()) {
				json.toJson(new ActiveDeck(), slot.child(ActiveDeckJson));
			}
			ActiveDeck tmp = json.fromJson(ActiveDeck.class,
					slot.child(ActiveDeckJson));
			current_due.deck = tmp.deck;
			current_due.lastrun = tmp.lastrun;
			Collections.sort(current_due.deck, byShowTime);

			if (System.currentTimeMillis() - current_due.lastrun < 16 * ONE_HOUR_ms) {
				Gdx.app.postRunnable(tooSoon);
				return;
			}
			stage.addAction(Actions.run(processActiveCards));
		}
	}

	private ActiveDeckLoader activeDeckLoader = new ActiveDeckLoader() {
	};

	private boolean isExtraPractice = false;

	private final NewCardDialog newCardDialog;

	private final Set<String> nodupes = new HashSet<>();
	private final Random rand = new Random();

	private Callback<Void> noop_success = new Callback<Void>() {
		@Override
		public void success(Void result) {
			Gdx.app.log("LearningSession-Score Submit", "success");
		}
	};

	private class SaveActiveDeckWithDialog implements Runnable {
		@Override
		public void run() {
			ActiveDeck tosave = new ActiveDeck();
			tosave.deck.addAll(current_active.deck);
			tosave.deck.addAll(current_due.deck);
			tosave.deck.addAll(current_discards.deck);
			tosave.deck.addAll(current_done.deck);
			tosave.lastrun = System.currentTimeMillis() - ((long) elapsed)
					* 1000l;
			Collections.sort(tosave.deck, byShowTime);

			final SlotInfo info;
			FileHandle infoFile = slot.child(INFO_JSON);
			if (!infoFile.exists()) {
				info = new SlotInfo();
				info.settings.name = "ᎤᏲᏒ ᎣᎦᎾ!";
			} else {
				info = json.fromJson(SlotInfo.class, infoFile);
				infoFile.copyTo(slot.child(INFO_JSON + ".bak"));
			}
			SlotInfo.calculateStats(info, tosave);

			TextButtonStyle tbs = new TextButtonStyle(
					skin.get(TextButtonStyle.class));
			tbs.font = game.getFont(Font.SerifMedium);
			final TextButton fb = new TextButton("SHARE STATS", tbs);
			String dtitle = isExtraPractice ? "Extra Practice Results"
					: "Practice Results";
			Dialog bye = new Dialog(dtitle, skin) {
				{
					LabelStyle lstyle = skin.get(LabelStyle.class);
					lstyle.font = game.getFont(Font.SerifMedium);

					StringBuilder sb = new StringBuilder();
					sb.append("Level: ");
					sb.append(info.level);
					sb.append("\n");
					sb.append("Score: ");
					sb.append(info.lastScore);
					sb.append("\n");
					sb.append(info.activeCards + " active cards");
					sb.append("\n");
					sb.append(info.shortTerm + " short term memorized");
					sb.append("\n");
					sb.append(info.mediumTerm + " medium term memorized");
					sb.append("\n");
					sb.append(info.longTerm + " long term memorized");
					sb.append("\n");
					int minutes = (int) (elapsed / 60f);
					int seconds = (int) (elapsed - minutes * 60f);
					sb.append("Total actual challenge time: " + minutes + ":"
							+ (seconds < 10 ? "0" : "") + seconds);
					Label label = new Label(sb.toString(), lstyle);
					text(label);
					button("OK!");
					google_submit: {
						if (BoundPronouns.services == null) {
							break google_submit;
						}
						if (isExtraPractice) {
							break google_submit;
						}
						if (!BoundPronouns.getPrefs().getBoolean(
								BoundPronouns.GooglePlayLogginIn, false)) {
							break google_submit;
						}
						Callback<Void> submit_scores=new Callback<Void>() {
							@Override
							public void success(Void result) {
								BoundPronouns.services.lb_submit(
										ShowLeaderboards.BoardId, info.lastScore,
										info.level.getEngrish(), noop_success);
								BoundPronouns.services.ach_unlocked(info.level.getId(),noop_success);
								BoundPronouns.services.ach_reveal(info.level.next().getId(),noop_success);
							}
						};
						BoundPronouns.services.login(submit_scores);
					}
				}

				protected void result(Object object) {
					if (fb.equals(object)) {
						cancel();
						return;
					}
					game.setScreen(caller);
					dispose();
				};
			};
			bye.show(stage);
			bye.setModal(true);
			bye.setFillParent(true);

			if (isExtraPractice) {
				game.log(this, "Extra Practice Session - NOT SAVING!");
				return;
			}

			FileHandle tmp = slot.child(ActiveDeckJson + ".tmp");
			json.toJson(tosave, tmp);
			tmp.moveTo(slot.child(ActiveDeckJson));
			tmp.delete();
			tmp = slot.child(INFO_JSON + ".tmp");
			json.toJson(info, tmp);
			tmp.moveTo(slot.child(INFO_JSON));
			tmp.delete();
		}
	}

	private SaveActiveDeckWithDialog saveActiveDeckWithDialog = new SaveActiveDeckWithDialog() {
	};

	/**
	 * Calculates amount of ms needed to shift by to move deck to "0" point.
	 * 
	 * @param current_pending
	 * @return
	 */
	private long getMinShiftTimeOf(ActiveDeck current_pending) {
		if (current_pending.deck.size() == 0) {
			return 0;
		}
		long by = Long.MAX_VALUE;
		Iterator<ActiveCard> icard = current_pending.deck.iterator();
		while (icard.hasNext()) {
			ActiveCard card = icard.next();
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

	/**
	 * used for logging periodic messages
	 */
	private float notice_elapsed = 0f;
	/**
	 * total challenge time accumulated
	 */
	private float elapsed = 0f;
	/**
	 * time since last "shuffle"
	 */
	private float sinceLastNextCard_elapsed = 0f;
	/**
	 * Whether elapsed time should be accumulated or not
	 */
	private boolean elapsed_tick_on = false;
	/**
	 * How long this challenge has been displayed.
	 */
	private float challenge_elapsed;

	private class ShowACard implements Runnable {

		private ActiveCard previousCard;

		@Override
		public void run() {
			ActiveCard activeCard;
			do {
				activeCard = getNextCard();
				if (activeCard == null) {
					break;
				}
				if (!activeCard.equals(previousCard)) {
					break;
				}
				if (current_discards.deck.size() == 0
						&& current_active.deck.size() == 0) {
					break;
				}
				activeCard.show_again_ms = Deck.getNextInterval(activeCard
						.getMinCorrectInARow() + 1);
				previousCard = null;
			} while (true);

			if (activeCard == null) {
				if (elapsed < info.settings.sessionLength.getSeconds()) {
					game.log(this, "session time is not up");
					long shift_by_ms = getMinShiftTimeOf(current_discards);
					game.log(this, "shifting discards to zero point: "
							+ (shift_by_ms / ONE_SECOND_ms));
					if (shift_by_ms >= 15l * ONE_SECOND_ms) {
						addCards(IncrementDeckBySize, current_active);
					}
					updateTime(current_discards, shift_by_ms);
					Gdx.app.postRunnable(showACard);
					return;
				}
				/*
				 * Session time is up, force time shift cards into active show
				 * range...
				 */
				if (elapsed > info.settings.sessionLength.getSeconds()
						&& current_discards.deck.size() > 0) {
					long shift_by_ms = getMinShiftTimeOf(current_discards);
					game.log(this, "shifting discards to zero point: "
							+ (shift_by_ms / ONE_SECOND_ms));
					updateTime(current_discards, shift_by_ms);
					Gdx.app.postRunnable(showACard);
					return;
				}
				if (elapsed > info.settings.sessionLength.getSeconds()) {
					elapsed_tick_on = false;
					game.log(this, "no cards remaining");
					stage.addAction(Actions.run(saveActiveDeckWithDialog));
					return;
				}
			}
			previousCard = activeCard;
			final Card deckCard = new Card(getCardById(activeCard.pgroup,
					activeCard.vgroup));
			if (activeCard.newCard) {
				elapsed_tick_on = false;
				ticktock.stop(ticktock_id);
				newCardDialog.setCounter(cardcount++);
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
				challengeCardDialog.setCounter(cardcount++);
				challengeCardDialog.setCard(activeCard, deckCard);
				challengeCardDialog.show(stage);

				AnswerList tracked_answers;
				if (rand.nextBoolean()) {
					tracked_answers = getAnswerSetsFor(activeCard, deckCard,
							game.deck);
				} else {
					tracked_answers = getAnswerSetsForBySimilarChallenge(
							activeCard, deckCard, game.deck);
				}
				AnswerList displayed_answers = new AnswerList(tracked_answers);
				randomizeSexes(displayed_answers);

				activeCard.tries_remaining -= tracked_answers.correctCount();
				challengeCardDialog.setAnswers(tracked_answers,
						displayed_answers);

				float duration = info.settings.timeLimit.getSeconds()
						- (float) activeCard.box
						- (float) activeCard.getMinCorrectInARow();
				if (duration < 4) {
					duration = 4f;
				}
				challengeCardDialog.addAction(Actions.delay(duration,
						Actions.run(new Runnable() {
							@Override
							public void run() {
								challengeCardDialog.result(null);
							}
						})));
				for (float x = duration; x >= 0; x -= .1f) {
					final float timer = duration - x;
					final float volume = x / duration;
					DelayAction updater = Actions.delay(x - .05f,
							Actions.run(new Runnable() {
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

		Random r = new Random();

		private void randomizeSexes(AnswerList answers) {
			for (Answer answer : answers.list) {
				if (r.nextBoolean() && answer.answer.contains("himself")) {
					answer.answer = answer.answer.replace("He ", "She ");
					answer.answer = answer.answer.replace(" him", " her");
				}
				if (r.nextBoolean() && !answer.answer.contains("himself")) {
					answer.answer = answer.answer.replace("He ", "She ");
				}
				if (r.nextBoolean() && !answer.answer.contains("himself")) {
					answer.answer = answer.answer.replace(" him", " her");
				}
			}
		}
	}

	private ShowACard showACard = new ShowACard() {
	};

	private final Skin skin;

	private final FileHandle slot;

	protected Comparator<ActiveCard> byShowTime = new Comparator<ActiveCard>() {
		@Override
		public int compare(ActiveCard o1, ActiveCard o2) {
			if (o1.show_again_ms != o2.show_again_ms) {
				return o1.show_again_ms > o2.show_again_ms ? 1 : -1;
			}
			return o1.box - o2.box;
		}
	};

	protected Comparator<ActiveCard> byShowTimeChunks = new Comparator<ActiveCard>() {
		@Override
		public int compare(ActiveCard o1, ActiveCard o2) {
			long dif = o1.show_again_ms - o2.show_again_ms;
			if (dif < 0)
				dif = -dif;
			if (dif < ONE_MINUTE_ms) {
				return 0;
			}
			return o1.show_again_ms > o2.show_again_ms ? 1 : -1;
		}
	};

	private Sound ticktock;

	private long ticktock_id;

	final private SlotInfo info;

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
		json = new JsonConverter();
		// json.setOutputType(OutputType.json);
		// json.setTypeName(null);
		// json.setIgnoreUnknownFields(true);

		FileHandle infoFile = slot.child(INFO_JSON);
		if (!infoFile.exists()) {
			info = new SlotInfo();
			info.settings.name = "ᎤᏲᏒ ᎣᎦᎾ!";
		} else {
			info = json.fromJson(SlotInfo.class, infoFile);
		}

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
				/*
				 * bump show count and add in elapsed display time for later
				 * scoring ...
				 */
				_activeCard.showCount++;
				_activeCard.showTime += challenge_elapsed;
				// Card card = cards_by_id.get(_activeCard.getId());
				this.clearActions();
				this.setCheckVisible(false);
				setTimer(0);
				ticktock.setVolume(ticktock_id, 0f);
				ticktock.stop(ticktock_id);
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
						((Button) b).setTouchable(Touchable.disabled);
					}
					if (b instanceof TextButton) {
						TextButton tb = (TextButton) b;
						if (tb.getUserObject() != null
								&& tb.getUserObject() instanceof Answer) {
							Answer tracked_answer = (Answer) tb.getUserObject();
							if (!tb.isChecked() && !tracked_answer.correct) {
								tb.addAction(Actions.fadeOut(.2f));
								doCow = false;
							}
							if (tb.isChecked() && !tracked_answer.correct) {
								ColorAction toRed = Actions.color(Color.RED,
										.4f);
								tb.addAction(toRed);
								tb.setText(BoundPronouns.HEAVY_BALLOT_X + " "
										+ tb.getText());
								doBuzzer = true;
								resetCorrectInARow(_activeCard);
								_activeCard.noErrors = false;
							}
							if (!tb.isChecked() && tracked_answer.correct) {
								ColorAction toGreen = Actions.color(
										Color.GREEN, .4f);
								ColorAction toClear = Actions.color(
										Color.CLEAR, .2f);
								SequenceAction sequence = Actions.sequence(
										toClear, toGreen);
								tb.addAction(Actions.repeat(2, sequence));
								tb.setText(BoundPronouns.RIGHT_ARROW + " "
										+ tb.getText());
								doBuzzer = true;
								resetCorrectInARow(_activeCard);
								_activeCard.noErrors = false;
							}
							if (tb.isChecked() && tracked_answer.correct) {
								ColorAction toGreen = Actions.color(
										Color.GREEN, .2f);
								tb.addAction(toGreen);
								doCow = false;
								tb.setText(BoundPronouns.HEAVY_CHECK_MARK + " "
										+ tb.getText());
								_activeCard.markCorrect(tracked_answer.answer);
							}
						}
					}
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
				_activeCard.show_again_ms = Deck.getNextInterval(_activeCard
						.getMinCorrectInARow());
				stage.addAction(Actions.delay(doBuzzer ? 5.9f : .9f,
						Actions.run(hideThisCard)));
				stage.addAction(Actions.delay(doBuzzer ? 6f : 1f,
						Actions.run(showACard)));
			}
		};

		newCardDialog.settings = info.settings;
		challengeCardDialog.settings = info.settings;
		challengeCardDialog.updateMuteButtonText();
	}

	/**
	 * add this many cards to the Current Active Deck first from the current
	 * Active Deck then from the master Deck set
	 * 
	 * @param needed
	 * @param active
	 */
	public void addCards(int needed, ActiveDeck active) {
		int startingSize = active.deck.size();
		/**
		 * look for previous cards to load first, if their delay time is up
		 */
		Iterator<ActiveCard> ipending = current_due.deck.iterator();
		while (needed > 0 && ipending.hasNext()) {
			ActiveCard next = ipending.next();
			if (next.box >= SlotInfo.FULLY_LEARNED_BOX) {
				continue;
			}
			if (next.show_again_ms > 0) {
				continue;
			}
			active.deck.add(next);
			needed--;
			ipending.remove();
		}
		game.log(this, "Added " + (active.deck.size() - startingSize)
				+ " previous cards.");

		if (needed <= 0) {
			return;
		}

		/**
		 * not enough already seen cards, add new never seen cards
		 */
		startingSize = active.deck.size();
		Iterator<Card> ideck = game.deck.cards.iterator();
		while (needed > 0 && ideck.hasNext()) {
			Card next = ideck.next();
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
			String unique_id = next.pgroup + "+" + next.vgroup;
			if (nodupes.contains(unique_id)) {
				continue;
			}
			ActiveCard activeCard = new ActiveCard();
			activeCard.box = 0;
			activeCard.noErrors = true;
			activeCard.newCard = true;
			activeCard.pgroup = next.pgroup;
			activeCard.show_again_ms = 0;
			activeCard.vgroup = next.vgroup;
			resetCorrectInARow(activeCard);
			activeCard.tries_remaining = SendToNextSessionThreshold
					* next.answer.size();
			active.deck.add(activeCard);
			needed--;
			nodupes.add(unique_id);
		}
		game.log(this, "Added " + (active.deck.size() - startingSize)
				+ " new cards.");

		if (needed <= 0) {
			return;
		}

		/**
		 * yikes! They processed ALL the cards!
		 */

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
	}

	private AnswerList getAnswerSetsForBySimilarChallenge(
			final ActiveCard active, final Card card, Deck deck) {
		AnswerList answers = new AnswerList();
		String challenge = card.challenge.get(0);
		/**
		 * contains copies of used answers, vgroups, and pgroups to prevent
		 * duplicates
		 */
		Set<String> already = new HashSet<String>();
		already.add(card.pgroup);
		already.add(card.vgroup);
		already.addAll(card.answer);
		already.add(challenge);

		/**
		 * for temporary manipulation of list data so we don't mess with master
		 * copies in cards, etc.
		 */
		List<String> tmp_correct = new ArrayList<String>();
		tmp_correct.clear();
		tmp_correct.addAll(card.answer);

		/**
		 * sort answers from least known to most known
		 */
		Collections.sort(tmp_correct, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Integer i1 = active.getCorrectInARowFor(o1);
				Integer i2 = active.getCorrectInARowFor(o2);
				i1 = (i1 == null ? 0 : i1);
				i2 = (i2 == null ? 0 : i2);
				if (i1 < i2) {
					return -1;
				}
				if (i1 > i2) {
					return 1;
				}
				return o1.compareTo(o2);
			}
		});
		/*
		 * Add a random count of correct answers. Least known first.
		 */
		int r = rand.nextInt(tmp_correct.size()) + 1;
		for (int i = 0; i < r && i < maxCorrect; i++) {
			String answer = tmp_correct.get(i);
			answers.list.add(0, new Answer(true, answer, 0));
		}

		/*
		 * look for "similar" looking challenges
		 */
		Deck tmp = new Deck();
		tmp.cards.addAll(deck.cards);
		// String challenge = getOneOf(card.challenge.get(0));
		scanDeck: for (int distance = 1; distance < 100; distance++) {
			Collections.shuffle(tmp.cards);
			for (Card deckCard : tmp.cards) {
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
				String tmp_challenge = deckCard.challenge.get(0);
				if (already.contains(tmp_challenge)) {
					continue;
				}
				int ldistance = StringUtils.getLevenshteinDistance(challenge,
						tmp_challenge, distance);
				if (ldistance < 1) {
					continue;
				}
				/*
				 * select a random wrong answer
				 */
				String wrong_answer = deckCard.answer.get(rand
						.nextInt(deckCard.answer.size()));
				if (already.contains(wrong_answer)) {
					continue;
				}
				already.add(wrong_answer);
				answers.list.add(new Answer(false, wrong_answer, ldistance));
				if (answers.list.size() >= maxAnswers) {
					break scanDeck;
				}
			}
		}
		Collections.sort(answers.list, byDistance);
		if (answers.list.size() > maxAnswers) {
			answers.list.subList(maxAnswers, answers.list.size()).clear();
		}
		Collections.shuffle(answers.list);
		return answers;
	}

	private AnswerList getAnswerSetsFor(final ActiveCard active,
			final Card challengeCard, Deck deck) {
		AnswerList answers = new AnswerList();
		/*
		 * contains copies of used answers, vgroups, and pgroups to prevent
		 * duplicates
		 */
		Set<String> already = new HashSet<String>();
		already.add(challengeCard.pgroup);
		already.add(challengeCard.vgroup);
		/*
		 * make sure all correct answers are in the "black list" for potential
		 * wrong answers
		 */
		already.addAll(challengeCard.answer);

		/*
		 * for temporary manipulation of list data so we don't mess with master
		 * copies in cards, etc.
		 */
		List<String> tmp_correct = new ArrayList<String>();
		tmp_correct.clear();
		tmp_correct.addAll(challengeCard.answer);

		/**
		 * sort answers from least known to most known
		 */
		Collections.sort(tmp_correct, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Integer i1 = active.getCorrectInARowFor(o1);
				Integer i2 = active.getCorrectInARowFor(o2);
				i1 = (i1 == null ? 0 : i1);
				i2 = (i2 == null ? 0 : i2);
				if (i1 < i2) {
					return -1;
				}
				if (i1 > i2) {
					return 1;
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
		 * look for "similar" looking answers by picking one random correct
		 * answer and comparing to one random wrong answer per card in the
		 * master deck
		 */
		Deck tmp = new Deck();
		tmp.cards.addAll(deck.cards);
		// String challenge = getOneOf(card.challenge.get(0));
		scanDeck: for (int distance = 1; distance < 100; distance++) {
			Collections.shuffle(tmp.cards);
			for (Card deckCard : tmp.cards) {
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
				if (StringUtils.isBlank(challengeCard.vgroup) != StringUtils
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
				/*
				 * select a random correct answer
				 */
				String correct_answer = challengeCard.answer.get(rand
						.nextInt(challengeCard.answer.size()));
				/*
				 * select a random wrong answer
				 */
				String wrong_answer = deckCard.answer.get(rand
						.nextInt(deckCard.answer.size()));
				if (already.contains(wrong_answer)) {
					continue;
				}
				/*
				 * if edit distance is close enough, add it, then add pgroup,
				 * vgroup and selected answer to already used list otherwise go
				 * on and check next card
				 */

				int ldistance = StringUtils.getLevenshteinDistance(
						correct_answer, wrong_answer, distance);
				if (ldistance < 1) {
					continue;
				}
				answers.list.add(new Answer(false, wrong_answer, ldistance));
				already.add(deckCard.pgroup);
				already.add(deckCard.vgroup);
				already.add(wrong_answer);
				if (answers.list.size() >= maxAnswers) {
					break scanDeck;
				}
			}
		}
		Collections.sort(answers.list, byDistance);
		if (answers.list.size() > maxAnswers) {
			answers.list.subList(maxAnswers, answers.list.size()).clear();
		}
		Collections.shuffle(answers.list);
		return answers;
	}

	@SuppressWarnings("unused")
	private String getOneOf(String string) {
		if (!string.contains(",")) {
			return string;
		}
		String[] tmp = StringUtils.split(",");
		string = tmp[rand.nextInt(tmp.length)];
		return StringUtils.strip(string);
	}

	private ActiveCard getNextCard() {
		if (current_active.deck.size() == 0) {
			updateTime(current_discards,
					(long) (sinceLastNextCard_elapsed * 1000f));
			sinceLastNextCard_elapsed = 0;
			Iterator<ActiveCard> itmp = current_discards.deck.iterator();
			while (itmp.hasNext()) {
				ActiveCard tmp = itmp.next();
				if (tmp.noErrors
						&& tmp.isAllCorrectInARow(SendToNextSessionThreshold)) {
					tmp.box++;
					current_done.deck.add(tmp);
					tmp.show_again_ms = tmp.show_again_ms
							+ Deck.getNextSessionInterval(tmp.box);
					itmp.remove();
					game.log(this, "Bumped Card: " + tmp.pgroup + " "
							+ tmp.vgroup);
					return getNextCard();
				}
				if (tmp.tries_remaining < 0) {
					tmp.box--;
					current_done.deck.add(tmp);
					tmp.show_again_ms = tmp.show_again_ms
							+ Deck.getNextSessionInterval(tmp.box);
					itmp.remove();
					game.log(this, "Retired Card: " + tmp.pgroup + " "
							+ tmp.vgroup);
					return getNextCard();
				}
				if (tmp.show_again_ms > 0) {
					continue;
				}
				current_active.deck.add(tmp);
				itmp.remove();
			}
			if (current_active.deck.size() == 0) {
				return null;
			}
			Collections.shuffle(current_active.deck);
			Collections.sort(current_active.deck, byShowTimeChunks);
		}
		ActiveCard card = current_active.deck.get(0);
		current_active.deck.remove(0);
		current_discards.deck.add(card);
		return card;
	}

	/**
	 * record all cards currently "in-play" so that when cards are retrieved
	 * from the master deck they are new cards
	 * 
	 * @param activeDeck
	 */
	public void recordAlreadySeen(ActiveDeck activeDeck) {
		Iterator<ActiveCard> istat = activeDeck.deck.iterator();
		while (istat.hasNext()) {
			ActiveCard next = istat.next();
			String unique_id = next.pgroup + "+" + next.vgroup;
			nodupes.add(unique_id);
		}
	}

	private void reInsertCard(ActiveCard card) {
		if (current_active.deck.size() < 2) {
			current_active.deck.add(card);
		} else {
			current_active.deck.add(1, card);
		}
		current_discards.deck.remove(card);
	}

	@Override
	public void render(float delta) {
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
				int mins = (int) (elapsed / 60);
				int secs = (int) (elapsed - mins * 60f);
				game.log(this, mins + ":" + (secs < 10 ? "0" : "") + secs);
			}
		}
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
		buzzer = game.manager.get(BoundPronouns.SND_BUZZ, Sound.class);
		cow = game.manager.get(BoundPronouns.SND_COW, Sound.class);
		ticktock = game.manager.get(BoundPronouns.SND_TICKTOCK, Sound.class);
	}

	/**
	 * time-shift all cards by time since last recorded run.
	 * 
	 * @param currentDeck
	 */
	public void updateTime(ActiveDeck currentDeck, long ms) {
		Iterator<ActiveCard> istat = currentDeck.deck.iterator();
		while (istat.hasNext()) {
			ActiveCard next = istat.next();
			next.show_again_ms -= ms;
		}
	}

	private Card getCardById(String pgroup, String vgroup) {
		for (Card card : game.deck.cards) {
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
}
