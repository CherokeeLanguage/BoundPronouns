package com.cherokeelessons.bp;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ColorAction;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
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
import com.cherokeelessons.util.DreamLo;
import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores;
import com.cherokeelessons.util.JsonConverter;
import com.cherokeelessons.util.Log;

public class LearningSession extends ChildScreen implements Screen {

	private static final Logger log = Log.getLogger(LearningSession.class.getName());

	private class ActiveDeckLoader implements Runnable {

		@Override
		public void run() {
			log.info("Loading Active Deck ...");
			if (!slot.child(ActiveDeckJson).exists()) {
				json.toJson(new ActiveDeck(), slot.child(ActiveDeckJson));
			}
			ActiveDeck tmp = json.fromJson(ActiveDeck.class, slot.child(ActiveDeckJson));
			current_due.deck = tmp.deck;
			current_due.lastrun = tmp.lastrun;
			Collections.sort(current_due.deck, byShowTime);

			stage.addAction(Actions.run(processActiveCards));
		}
	}

	private class LoadMasterDeck implements Runnable {
		@Override
		public void run() {
			log.info("Loading Master Deck...");
			stage.addAction(Actions.run(activeDeckLoader));
			log.info("Loaded " + info.settings.deck.name() + " " + game.deck.cards.size() + " master cards.");
		}
	}

	private class ProcessActiveCards implements Runnable {
		private void markAllNoErrors(ActiveDeck deck) {
			for (ActiveCard card : deck.deck) {
				card.noErrors = true;
			}
		}

		private void onlyPositiveBoxValues(ActiveDeck deck) {
			for (ActiveCard card : deck.deck) {
				if (card.box < 0) {
					card.box = 0;
					continue;
				}
			}
		}

		private void resetScoring(ActiveDeck deck) {
			for (ActiveCard card : deck.deck) {
				card.showCount = 0;
				card.showTime = 0f;
			}
		}

		private void retireNotYetCards(ActiveDeck current_pending) {
			Iterator<ActiveCard> icard = current_pending.deck.iterator();
			while (icard.hasNext()) {
				ActiveCard card = icard.next();
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
			log.info(due + " cards previous cards are due.");

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
			for (ActiveCard card : current_due.deck) {
				card.resetTriesRemaining();
			}

			/*
			 * time-shift all cards by an additional seven days to pull in more
			 * cards if this is an extra practice session
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

			log.info("Elapsed :" + elapsed);
		}

		private void truncateToNearestMinute(List<ActiveCard> deck) {
			for (ActiveCard card : deck) {
				card.show_again_ms = (60l * 1000l) * (card.show_again_ms / (1000l * 60l));
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

		public SaveActiveDeckWithDialog(SaveParams params) {
			this.params = params;
		}

		@Override
		public void run() {
			JsonConverter json = new JsonConverter();

			params.deck.lastrun = System.currentTimeMillis() - ((long) params.elapsed_secs) * 1000l;
			Collections.sort(params.deck.deck, byShowTime);

			final SlotInfo info;
			FileHandle infoFile = params.slot.child(INFO_JSON);
			if (!infoFile.exists()) {
				info = new SlotInfo();
				info.settings.name = "ᎤᏲᏒ ᎣᎦᎾ!";
			} else {
				info = json.fromJson(SlotInfo.class, infoFile);
				infoFile.copyTo(params.slot.child(INFO_JSON + ".bak"));
			}
			SlotInfo.calculateStats(info, params.deck);

			TextButtonStyle tbs = new TextButtonStyle(params.skin.get(TextButtonStyle.class));
			tbs.font = params.game.getFont(Font.SerifMedium);

			final TextButton btn_ok = new TextButton("OK", tbs);
			final TextButton btn_scores = new TextButton("Submit Score", tbs);
			btn_scores.setText("View High Scores");

			Texture img_sync = params.game.manager.get(BoundPronouns.IMG_SYNC, Texture.class);
			TextureRegionDrawable draw_sync = new TextureRegionDrawable(new TextureRegion(img_sync));
			final ImageButton syncb = new ImageButton(draw_sync);
			syncb.setTransform(true);
			syncb.getImage().setScaling(Scaling.fit);
			syncb.getImage().setColor(Color.DARK_GRAY);

			// String dtitle = params.isExtraPractice ? "Extra Practice Results"
			// : "Practice Results";
			String dtitle = "Practice Results";
			final WindowStyle dws = new WindowStyle(params.skin.get(WindowStyle.class));
			dws.titleFont = params.game.getFont(Font.SerifLarge);
			Dialog bye = new Dialog(dtitle, dws) {
				final Dialog bye = this;
				{
					this.getTitleLabel().setAlignment(Align.center);
					final Texture background = params.game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
					final TextureRegion region = new TextureRegion(background);
					final TiledDrawable tiled = new TiledDrawable(region);
					tiled.setMinHeight(0);
					tiled.setMinWidth(0);
					tiled.setTopHeight(params.game.getFont(Font.SerifLarge).getCapHeight() + 20);
					bye.background(tiled);

					LabelStyle lstyle = new LabelStyle(params.skin.get(LabelStyle.class));
					lstyle.font = params.game.getFont(Font.SerifMedium);

					StringBuilder sb = new StringBuilder();
					sb.append("Level: ");
					sb.append(info.level);
					sb.append("\n");
					sb.append("Score: ");
					sb.append(info.lastScore);
					sb.append("\n");
					sb.append(info.activeCards + " active cards");
					sb.append("\n");
					sb.append("You currently have a " + info.proficiency + "% proficiency level");
					sb.append("\n");
					int minutes = (int) (params.elapsed_secs / 60f);
					int seconds = (int) (params.elapsed_secs - minutes * 60f);
					sb.append("Total actual challenge time: " + minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
					Label label = new Label(sb.toString(), lstyle);
					text(label);
					button(btn_ok, btn_ok);

					if (lb != null) {
						button(btn_scores, btn_scores);
					}

					if (BoundPronouns.services != null) {
						button(syncb, syncb);
					}

					final GoogleSyncUI gsu = new GoogleSyncUI(params.game, params.stage, params.slot, null);

					final Callback<GameScores> showPublicScores = new Callback<GameScores>() {
						@Override
						public void success(GameScores result) {
							gsu.showScores("Today's Public Scores", result, null);
						}
					};

					final Callback<Void> getPublicScores = new Callback<Void>() {
						@Override
						public void success(Void result) {
							if (lb != null) {
								lb.lb_getScoresFor(null, showPublicScores);
							}
						}
					};

					btn_scores.addListener(new ClickListener() {
						public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
							getPublicScores.success(null);
							return true;
						};
					});

					syncb.addListener(new ClickListener() {
						public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
							if (!BoundPronouns.services.isLoggedIn()) {
								gsu.askToLoginForSync(new Runnable() {
									@Override
									public void run() {
										gsu.upload();
									}
								});
							} else {
								gsu.upload();
							}
							return true;
						};
					});

					if (lb != null) {
						String tag = info.level.getEnglish() + "!!!" + info.settings.name;
						String slot = params.slot.nameWithoutExtension();
						lb.lb_submit(slot != null ? slot : "", info.lastScore, info.activeCards, tag, noop_success);
					}

					if (BoundPronouns.services != null) {
						if (BoundPronouns.services.isLoggedIn()) {
							syncb.setVisible(false);
							gsu.uploadHidden(new Runnable() {
								@Override
								public void run() {
									syncb.setVisible(true);
								}
							});
						} else {
							syncb.setVisible(true);
						}
					}
				}

				protected void result(Object object) {
					if (syncb.equals(object)) {
						cancel();
						return;
					}
					if (btn_scores.equals(object)) {
						cancel();
						return;
					}
					Screen current = params.game.getScreen();
					if (params.caller != null && !params.caller.equals(current)) {
						params.game.setScreen(params.caller);
						current.dispose();
					}
				};
			};
			bye.show(params.stage);
			bye.setModal(true);
			bye.setFillParent(true);

			FileHandle tmp = params.slot.child(ActiveDeckJson + ".tmp");
			json.toJson(params.deck, tmp);
			tmp.moveTo(params.slot.child(ActiveDeckJson));
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
		private void randomizeSexes(AnswerList answers) {
			for (Answer answer : answers.list) {
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
				for (ActiveCard tmp : current_discards.deck) {
					if (tmp.noErrors && tmp.tries_remaining < 1) {
						tmp.box++;
						tmp.show_again_ms = tmp.show_again_ms + Deck.getNextSessionInterval(tmp.box);
						log.info("Bumped Card: " + tmp.pgroup + " " + tmp.vgroup);
					}
				}
				/**
				 * scan current discards for cards that need to get "downgraded"
				 */
				for (ActiveCard tmp : current_discards.deck) {
					if (!tmp.noErrors && tmp.tries_remaining < 1) {
						tmp.box--;
						tmp.show_again_ms = tmp.show_again_ms + Deck.getNextSessionInterval(tmp.box);
						log.info("Retired Card: " + tmp.pgroup + " " + tmp.vgroup);
					}
				}

				SaveParams params = new SaveParams();
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
					long shift_by_ms = getMinShiftTimeOf(current_discards);
					log.info("shifting discards to zero point: " + (shift_by_ms / ONE_SECOND_ms));
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
				if (elapsed > info.settings.sessionLength.getSeconds() && current_discards.deck.size() > 0) {
					long shift_by_ms = getMinShiftTimeOf(current_discards);
					log.info("shifting discards to zero point: " + (shift_by_ms / ONE_SECOND_ms));
					updateTime(current_discards, shift_by_ms);
					Gdx.app.postRunnable(showACard);
					return;
				}
			}
			previousCard = activeCard;
			final Card deckCard = new Card(getCardById(activeCard.pgroup, activeCard.vgroup));
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
					tracked_answers = getAnswerSetsFor(activeCard, deckCard, game.deck);
				} else {
					tracked_answers = getAnswerSetsForBySimilarChallenge(activeCard, deckCard, game.deck);
				}
				AnswerList displayed_answers = new AnswerList(tracked_answers);
				randomizeSexes(displayed_answers);
				activeCard.tries_remaining--;
				challengeCardDialog.setAnswers(tracked_answers, displayed_answers);
				float duration = info.settings.timeLimit.getSeconds() - (float) activeCard.box
						- (float) activeCard.getMinCorrectInARow();
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
					DelayAction updater = Actions.delay(x - .05f, Actions.run(new Runnable() {
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

	public static final String ActiveDeckJson = "ActiveDeck.json";

	private static Comparator<ActiveCard> byShowTime = new Comparator<ActiveCard>() {
		@Override
		public int compare(ActiveCard o1, ActiveCard o2) {
			if (o1.show_again_ms != o2.show_again_ms) {
				return o1.show_again_ms > o2.show_again_ms ? 1 : -1;
			}
			return o1.box - o2.box;
		}
	};

	private static Comparator<ActiveCard> byShowTimeChunks = new Comparator<ActiveCard>() {
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

	private static final int IncrementDeckBySize = 3;

	private static final String INFO_JSON = BoundPronouns.INFO_JSON;

	private static final int InitialDeckSize = 5;

	private static int lv_d[] = new int[1024]; // cost array, horizontally
	/**
	 * <p>
	 * Taken from StringUtils.class and reconfigured to use pre-allocated arrays
	 * to preven GC issues on Android.
	 * </p>
	 * <p>
	 * Find the Levenshtein distance between two Strings if it's less than or
	 * equal to a given threshold.
	 * </p>
	 *
	 * <p>
	 * This is the number of changes needed to change one String into another,
	 * where each change is a single character modification (deletion, insertion
	 * or substitution).
	 * </p>
	 *
	 * <p>
	 * This implementation follows from Algorithms on Strings, Trees and
	 * Sequences by Dan Gusfield and Chas Emerick's implementation of the
	 * Levenshtein distance algorithm from
	 * <a href="http://www.merriampark.com/ld.htm" >http://www.merriampark.com/
	 * ld.htm</a>
	 * </p>
	 *
	 * <pre>
	 * StringUtils.getLevenshteinDistance(null, *, *)             = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance(*, null, *)             = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance(*, *, -1)               = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance("","", 0)               = 0
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 8)       = 7
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 7)       = 7
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 6))      = -1
	 * StringUtils.getLevenshteinDistance("elephant", "hippo", 7) = 7
	 * StringUtils.getLevenshteinDistance("elephant", "hippo", 6) = -1
	 * StringUtils.getLevenshteinDistance("hippo", "elephant", 7) = 7
	 * StringUtils.getLevenshteinDistance("hippo", "elephant", 6) = -1
	 * </pre>
	 *
	 * @param s
	 *            the first String, must not be null
	 * @param t
	 *            the second String, must not be null
	 * @param threshold
	 *            the target threshold, must not be negative
	 * @return result distance, or {@code -1} if the distance would be greater
	 *         than the threshold
	 * @throws IllegalArgumentException
	 *             if either String input {@code null} or negative threshold
	 */
	private static int lv_p[] = new int[1024]; // 'previous' cost array,
												// horizontally
	private static final int maxAnswers = 4;
	private static final int maxCorrect = 4;
	private static Callback<Void> noop_success = new Callback<Void>() {
		@Override
		public void success(Void result) {
			log.info("LearningSession-Score Submit: " + "success");
		}
	};
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

	public static synchronized int getLevenshteinDistanceIgnoreCase(CharSequence s, CharSequence t, int threshold) {
		return getLevenshteinDistanceIgnoreCase(String.valueOf(s), String.valueOf(t), threshold);
	}

	public static synchronized int getLevenshteinDistanceIgnoreCase(String s, String t, int threshold) {
		return getLevenshteinDistance(s != null ? s.toLowerCase() : "", t != null ? t.toLowerCase() : "", threshold);
	}

	public static synchronized int getLevenshteinDistance(CharSequence s, CharSequence t, int threshold) {
		if (s == null || t == null) {
			throw new IllegalArgumentException("Strings must not be null");
		}
		if (threshold < 0) {
			throw new IllegalArgumentException("Threshold must not be negative");
		}

		/*
		 * This implementation only computes the distance if it's less than or
		 * equal to the threshold value, returning -1 if it's greater. The
		 * advantage is performance: unbounded distance is O(nm), but a bound of
		 * k allows us to reduce it to O(km) time by only computing a diagonal
		 * stripe of width 2k + 1 of the cost table. It is also possible to use
		 * this to compute the unbounded Levenshtein distance by starting the
		 * threshold at 1 and doubling each time until the distance is found;
		 * this is O(dm), where d is the distance.
		 * 
		 * One subtlety comes from needing to ignore entries on the border of
		 * our stripe eg. p[] = |#|#|#|* d[] = *|#|#|#| We must ignore the entry
		 * to the left of the leftmost member We must ignore the entry above the
		 * rightmost member
		 * 
		 * Another subtlety comes from our stripe running off the matrix if the
		 * strings aren't of the same size. Since string s is always swapped to
		 * be the shorter of the two, the stripe will always run off to the
		 * upper right instead of the lower left of the matrix.
		 * 
		 * As a concrete example, suppose s is of length 5, t is of length 7,
		 * and our threshold is 1. In this case we're going to walk a stripe of
		 * length 3. The matrix would look like so:
		 * 
		 * 1 2 3 4 5 1 |#|#| | | | 2 |#|#|#| | | 3 | |#|#|#| | 4 | | |#|#|#| 5 |
		 * | | |#|#| 6 | | | | |#| 7 | | | | | |
		 * 
		 * Note how the stripe leads off the table as there is no possible way
		 * to turn a string of length 5 into one of length 7 in edit distance of
		 * 1.
		 * 
		 * Additionally, this implementation decreases memory usage by using two
		 * single-dimensional arrays and swapping them back and forth instead of
		 * allocating an entire n by m matrix. This requires a few minor
		 * changes, such as immediately returning when it's detected that the
		 * stripe has run off the matrix and initially filling the arrays with
		 * large values so that entries we don't compute are ignored.
		 * 
		 * See Algorithms on Strings, Trees and Sequences by Dan Gusfield for
		 * some discussion.
		 */

		int n = s.length(); // length of s
		int m = t.length(); // length of t

		// if one string is empty, the edit distance is necessarily the length
		// of the other
		if (n == 0) {
			return m <= threshold ? m : -1;
		} else if (m == 0) {
			return n <= threshold ? n : -1;
		}

		if (n > m) {
			// swap the two strings to consume less memory
			CharSequence tmp = s;
			s = t;
			t = tmp;
			n = m;
			m = t.length();
		}

		// int lv_p[] = new int[n + 1]; // 'previous' cost array, horizontally
		// int lv_d[] = new int[n + 1]; // cost array, horizontally
		if (lv_p.length < n + 1) {
			lv_p = new int[n + 1];
		}
		if (lv_d.length < n + 1) {
			lv_d = new int[n + 1];
		}
		int _d[]; // placeholder to assist in swapping p and d

		// fill in starting table values
		int boundary = Math.min(n, threshold) + 1;
		for (int i = 0; i < boundary; i++) {
			lv_p[i] = i;
		}
		// these fills ensure that the value above the rightmost entry of our
		// stripe will be ignored in following loop iterations
		Arrays.fill(lv_p, boundary, lv_p.length, Integer.MAX_VALUE);
		Arrays.fill(lv_d, Integer.MAX_VALUE);

		// iterates through t
		for (int j = 1; j <= m; j++) {
			char t_j = t.charAt(j - 1); // jth character of t
			lv_d[0] = j;

			// compute stripe indices, constrain to array size
			int min = Math.max(1, j - threshold);
			int max = Math.min(n, j + threshold);

			// the stripe may lead off of the table if s and t are of different
			// sizes
			if (min > max) {
				return -1;
			}

			// ignore entry left of leftmost
			if (min > 1) {
				lv_d[min - 1] = Integer.MAX_VALUE;
			}

			// iterates through [min, max] in s
			for (int i = min; i <= max; i++) {
				if (s.charAt(i - 1) == t_j) {
					// diagonally left and up
					lv_d[i] = lv_p[i - 1];
				} else {
					// 1 + minimum of cell to the left, to the top, diagonally
					// left and up
					lv_d[i] = 1 + Math.min(Math.min(lv_d[i - 1], lv_p[i]), lv_p[i - 1]);
				}
			}

			// copy current distance counts to 'previous row' distance counts
			_d = lv_p;
			lv_p = lv_d;
			lv_d = _d;
		}

		// if p[n] is greater than the threshold, there's no guarantee on it
		// being the correct
		// distance
		if (lv_p[n] <= threshold) {
			return lv_p[n];
		} else {
			return -1;
		}
	}

	private void resetAsNew(ActiveDeck current_due) {
		for (ActiveCard card : current_due.deck) {
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

	private ActiveDeckLoader activeDeckLoader = new ActiveDeckLoader() {
	};

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

	/**
	 * How long this challenge has been displayed.
	 */
	private float challenge_elapsed;

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

	// private boolean isExtraPractice = false;

	private final JsonConverter json;

	private LoadMasterDeck loadDeck = new LoadMasterDeck() {
	};
	private final NewCardDialog newCardDialog;
	private final Set<String> nodupes = new HashSet<String>();
	/**
	 * used for logging periodic messages
	 */
	private float notice_elapsed = 0f;
	private ProcessActiveCards processActiveCards = new ProcessActiveCards() {
	};

	private final Random rand = new Random();

	private SaveActiveDeckWithDialog saveActiveDeckWithDialog;

	private ShowACard showACard = new ShowACard() {
	};

	/**
	 * time since last "shuffle"
	 */
	private float sinceLastNextCard_elapsed = 0f;

	private final Skin skin;

	private final FileHandle slot;

	private Sound ticktock;

	private long ticktock_id;

	private final static DreamLo lb;

	static {
		lb = new DreamLo(BoundPronouns.getPrefs());
	}

	// private TooSoonDialog tooSoon = new TooSoonDialog() {
	// };

	public LearningSession(BoundPronouns _game, Screen caller, FileHandle slot) {
		super(_game, caller);
		int totalCards = game.deck.cards.size();
		current_active.deck = new ArrayList<ActiveCard>(totalCards);
		current_discards.deck = new ArrayList<ActiveCard>(totalCards);
		current_done.deck = new ArrayList<ActiveCard>(totalCards);
		current_due.deck = new ArrayList<ActiveCard>(totalCards);

		this.slot = slot;
		slot.mkdirs();
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		container = new Table(skin);
		container.setBackground(d);
		container.setFillParent(true);
		stage.addActor(container);
		stage.addAction(Actions.delay(.05f, Actions.run(loadDeck)));
		json = new JsonConverter();

		FileHandle infoFile = slot.child(INFO_JSON);
		if (!infoFile.exists()) {
			info = new SlotInfo();
			info.settings.name = "ᎤᏲᏒ ᎣᎦᎾ!";
			info.settings.sessionLength = SessionLength.Brief;
			info.settings.timeLimit = TimeLimit.Novice;
			info.settings.deck = DeckMode.Conjugations;
		} else {
			info = json.fromJson(SlotInfo.class, infoFile);
			info.settings.sessionLength = SessionLength.Brief;
			info.settings.timeLimit = TimeLimit.Novice;
		}

		newCardDialog = new NewCardDialog(game, skin) {
			@Override
			protected void result(Object object) {
				this.clearActions();
				stage.addAction(Actions.run(showACard));
			}

			@Override
			public Dialog show(Stage stage) {
				return super.show(stage);
			}

			@Override
			protected void showMainMenu() {
				Runnable yes = new Runnable() {
					@Override
					public void run() {
						game.setScreen(LearningSession.this.caller);
						LearningSession.this.dispose();
					}
				};
				Runnable no = new Runnable() {
					@Override
					public void run() {
					}
				};
				Dialog dialog = dialogYN("Please Confirm Exit",
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
			protected void result(Object object) {
				this.navEnable(false);
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
				 * bump show count and add in elapsed display time for later
				 * scoring ...
				 */
				_activeCard.showCount++;
				_activeCard.showTime += challenge_elapsed;
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
				int wrong = 0;
				for (Actor b : getButtonTable().getChildren()) {
					if (b instanceof Button) {
						((Button) b).setDisabled(true);
						((Button) b).setTouchable(Touchable.disabled);
					}
					if (b instanceof TextButton) {
						TextButton tb = (TextButton) b;
						Object userObject = tb.getUserObject();
						if (userObject != null && userObject instanceof Answer) {
							Answer tracked_answer = (Answer) userObject;
							if (!tb.isChecked() && !tracked_answer.correct) {
								tb.addAction(Actions.fadeOut(.2f));
								doCow = false;
							}
							if (tb.isChecked() && !tracked_answer.correct) {
								ColorAction toRed = Actions.color(Color.RED, .4f);
								tb.addAction(toRed);
								tb.setText(BoundPronouns.HEAVY_BALLOT_X + " " + tb.getText());
								doBuzzer = true;
								resetCorrectInARow(_activeCard);
								_activeCard.noErrors = false;
								wrong++;
							}
							if (!tb.isChecked() && tracked_answer.correct) {
								ColorAction toGreen = Actions.color(Color.GREEN, .4f);
								ColorAction toClear = Actions.color(Color.CLEAR, .2f);
								SequenceAction sequence = Actions.sequence(toClear, toGreen);
								tb.addAction(Actions.repeat(2, sequence));
								tb.setText(BoundPronouns.RIGHT_ARROW + " " + tb.getText());
								doBuzzer = true;
								resetCorrectInARow(_activeCard);
								if (_activeCard.noErrors && _activeCard.tries_remaining < 1) {
									/*
									 * set for at leat one more show if the
									 * first time a card is incorrectly answered
									 * there are no shows remaining
									 */
									_activeCard.tries_remaining++;
								}
								_activeCard.noErrors = false;
							}
							if (tb.isChecked() && tracked_answer.correct) {
								ColorAction toGreen = Actions.color(Color.GREEN, .2f);
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
				Runnable yes = new Runnable() {
					@Override
					public void run() {
						game.setScreen(LearningSession.this.caller);
						LearningSession.this.dispose();
					}
				};
				Runnable no = new Runnable() {
					@Override
					public void run() {
						challengeCardDialog.paused = wasPaused;
					}
				};
				Dialog dialog = dialogYN("Please Confirm Exit",
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
			ActiveCard next = ipending.next();
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
			ActiveCard next = ipending.next();
			next.resetTriesRemaining();
			active.deck.add(next);
			needed--;
			ipending.remove();
		}

	}

	private Dialog dialogYN(String title, String message, final Runnable yes, final Runnable no) {
		if (dskin == null) {
			dskin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));
		}
		WindowStyle ws = new WindowStyle(dskin.get(WindowStyle.class));
		ws.titleFont = game.getFont(Font.SerifLLarge);
		LabelStyle ls = new LabelStyle(dskin.get(LabelStyle.class));
		message = WordUtils.wrap(message, 70);
		ls.font = game.getFont(Font.SerifLarge);
		Label msg = new Label(message, ls);
		msg.setAlignment(Align.center);
		TextButtonStyle tbs = new TextButtonStyle(dskin.get(TextButtonStyle.class));
		tbs.font = game.getFont(Font.SerifLarge);
		final TextButton btn_yes = new TextButton("YES", tbs);
		final TextButton btn_no = new TextButton("NO", tbs);
		Dialog dialog = new Dialog(title, ws) {
			@Override
			protected void result(Object object) {
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

	private AnswerList getAnswerSetsFor(final ActiveCard active, final Card challengeCard, Deck deck) {
		Set<String> already = new HashSet<String>(16);
		AnswerList answers = new AnswerList();
		/*
		 * contains copies of used answers, vgroups, and pgroups to prevent
		 * duplicates
		 */
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
		List<String> tmp_correct = new ArrayList<String>(16);
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
		Deck tmp = new Deck(deck);
		scanDeck: for (int distance = 5; distance < 100; distance += 5) {
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
				if (StringUtils.isBlank(challengeCard.vgroup) != StringUtils.isBlank(deckCard.vgroup)) {
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
				String correct_answer = challengeCard.answer.get(rand.nextInt(challengeCard.answer.size())).intern();
				/*
				 * select a random wrong answer
				 */
				String wrong_answer = deckCard.answer.get(rand.nextInt(deckCard.answer.size())).intern();
				if (already.contains(wrong_answer)) {
					continue;
				}
				/*
				 * if edit distance is close enough, add it, then add pgroup,
				 * vgroup and selected answer to already used list otherwise go
				 * on and check next card
				 */

				int ldistance = getLevenshteinDistanceIgnoreCase(correct_answer, wrong_answer, distance);
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
		log.info("getAnswerSetsFor already set size: " + already.size());
		return answers;
	}

	private AnswerList getAnswerSetsForBySimilarChallenge(final ActiveCard active, final Card card, Deck deck) {
		AnswerList answers = new AnswerList();
		String challenge = card.challenge.get(0);
		/**
		 * contains copies of used answers, vgroups, and pgroups to prevent
		 * duplicates
		 */
		Set<String> already = new HashSet<String>(16);
		already.add(card.pgroup);
		already.add(card.vgroup);
		already.addAll(card.answer);
		already.add(challenge);

		/**
		 * for temporary manipulation of list data so we don't mess with master
		 * copies in cards, etc.
		 */
		List<String> tmp_correct = new ArrayList<String>(16);
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
		Deck tmp = new Deck(deck);
		scanDeck: for (int distance = 5; distance < 100; distance += 5) {
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
				if (StringUtils.isBlank(card.vgroup) != StringUtils.isBlank(deckCard.vgroup)) {
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
				int ldistance = getLevenshteinDistanceIgnoreCase(challenge, tmp_challenge, distance);
				if (ldistance < 1) {
					continue;
				}
				/*
				 * select a random wrong answer
				 */
				String wrong_answer = deckCard.answer.get(rand.nextInt(deckCard.answer.size()));
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
				ActiveCard discard = discards.next();
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
				ActiveCard tmp = discards.next();
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
				log.info(mins + ":" + (secs < 10 ? "0" : "") + secs);
			}
		}
		super.render(delta);
	}

	public void resetCorrectInARow(ActiveCard card) {
		Card dcard = getCardById(card.pgroup, card.vgroup);
		if (dcard == null) {
			card.resetCorrectInARow(new ArrayList<String>());
			return;
		}
		card.resetCorrectInARow(dcard.answer);
	}

	private void resetCorrectInARow(ActiveDeck current_pending) {
		for (ActiveCard card : current_pending.deck) {
			resetCorrectInARow(card);
		}
	}

	protected void resetRetriesCount(ActiveDeck deck) {
		for (ActiveCard card : deck.deck) {
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
	 */
	public void updateTime(ActiveDeck currentDeck, long ms) {
		Iterator<ActiveCard> istat = currentDeck.deck.iterator();
		while (istat.hasNext()) {
			ActiveCard next = istat.next();
			next.show_again_ms -= ms;
		}
	}
}
