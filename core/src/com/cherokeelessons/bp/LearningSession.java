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

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.Answer;
import com.cherokeelessons.cards.Answer.AnswerList;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.cards.SlotInfo;

public class LearningSession extends ChildScreen implements Screen {

	private static final String INFO_JSON = BoundPronouns.INFO_JSON;

	private static final long ONE_MINUTE_ms = 60l * 1000l;

	private static final long ONE_DAY_ms = 24l * 60l * ONE_MINUTE_ms;

	private static final long ONE_HOUR_ms = 60l * ONE_MINUTE_ms;

	private static final String ActiveDeckJson = "ActiveDeck.json";

	private static final int maxAnswers = 6;

	private static final int maxCorrect = 2;

	private static final float MaxTimePerCard_sec = 15f;

	private static final int SendToNextSessionThreshold = 4;

	protected static final float MinSessionTime = 60f * 10f;

	protected static final int InitialDeckSize = 7;

	protected static final int IncrementDeckBySize = 3;

	private static final int FULLY_LEARNED_BOX = 10;

	protected static final int PROFICIENT_BOX = 3;

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
	private final Map<String, Card> cards_by_id = new HashMap<>();
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
	private final ActiveDeck current_pending = new ActiveDeck();
	/**
	 * holding area for cards that should not be shown any more this session
	 */
	private final ActiveDeck current_done = new ActiveDeck();

	private Deck deck;

	private Sound ding;

	private Runnable initSet0 = new Runnable() {
		@Override
		public void run() {
			nodupes.clear();
			game.log(this, "Loading Set 0...");

			int needed = InitialDeckSize;

			/*
			 * time-shift all cards by exactly one day
			 */
			current_pending.lastrun = System.currentTimeMillis() - ONE_DAY_ms;
			updateTime(current_pending);
			/*
			 * Make sure we don't have active cards pointing to no longer existing master deck cards
			 */
			Iterator<ActiveCard> ipending = current_pending.deck.iterator();
			while (ipending.hasNext()) {
				ActiveCard active = ipending.next();
				if (cards_by_id.containsKey(active.getId())){
					continue;
				}				
				ipending.remove();
				game.log(this, "Removed no longer valid entry: "+active.getId());
			}
			/*
			 * ALWAYS force reset ALL correct in a row counts on load!
			 */
			resetCorrectInARow(current_pending);
			/*
			 * RESET tries max count
			 */
			resetRetriesCount(current_pending);
			/*
			 * ALWAYS start off as being eligible for "bump"
			 */
			markAllNoErrors(current_pending);

			/*
			 * Make sure no boxes out of range
			 */
			clampBoxValues(current_pending);

			/*
			 * time-shift all cards by an additional seven days to pull in more
			 * cards if this is an extra practice session
			 */

			if (isExtraPractice) {
				for (int days = 0; days < 7; days++) {
					updateTime(current_pending);
				}
			}

			// mark cards already in the active deck
			recordAlreadySeen(current_pending);

			// move cards due tomorrow a later into the already done pile!
			retireNotYetCards(current_pending);

			// initial randomize and resort of pending deck
			Collections.shuffle(current_pending.deck);
			Collections.sort(current_pending.deck, byShowTimeChunks);
			
			// add cards to the active deck
			addCards(needed, current_active);
			

			stage.addAction(Actions.run(showACard));
		}

		private void retireNotYetCards(ActiveDeck current_pending) {
			Iterator<ActiveCard> icard = current_pending.deck.iterator();
			while (icard.hasNext()) {
				ActiveCard card = icard.next();
				if (card.show_again_ms < ONE_HOUR_ms
						&& card.box < PROFICIENT_BOX) {
					continue;
				}
				current_done.deck.add(card);
				icard.remove();
			}
			game.log(
					this,
					"Moved "
							+ current_done.deck.size()
							+ " future pending or proficient cards into the 'done' deck.");
		}

		private void clampBoxValues(ActiveDeck deck) {
			for (ActiveCard card : deck.deck) {
				if (card.box < 0) {
					card.box = 0;
					continue;
				}
				if (card.box > FULLY_LEARNED_BOX) {
					card.box = FULLY_LEARNED_BOX;
				}
			}

		}

		private void markAllNoErrors(ActiveDeck deck) {
			for (ActiveCard card : deck.deck) {
				card.noErrors = true;
			}
		}

	};

	private void resetCorrectInARow(ActiveDeck current_pending) {
		for (ActiveCard card : current_pending.deck) {
			resetCorrectInARow(card);
		}
	}

	protected void resetRetriesCount(ActiveDeck deck) {
		for (ActiveCard card : deck.deck) {
			Card dcard = cards_by_id.get(card.getId());
			card.tries_remaining = SendToNextSessionThreshold
					* dcard.answer.size();
		}
	}

	public void resetCorrectInARow(ActiveCard card) {
		Card dcard = cards_by_id.get(card.getId());
		if (dcard==null) {
			card.resetCorrectInARow(new ArrayList<String>());
			return;
		}
		card.resetCorrectInARow(dcard.answer);
	}

	private final Json json;
	private Runnable loadDeck = new Runnable() {
		@Override
		public void run() {
			game.log(this, "Loading Master Deck...");
			stage.addAction(Actions.run(loadStats));
			deck = json.fromJson(Deck.class,
					BuildDeck.getDeckSlot().child("deck.json"));
			cards_by_id.clear();
			for (Card c : deck.cards) {
				cards_by_id.put(c.getId(), c);
			}
		}
	};

	private Runnable setName = new Runnable() {
		@Override
		public void run() {
			Dialog setNameDialog = new Dialog("Please name this session", skin) {
				final TextField tf;
				{
					TextFieldStyle tfs = skin.get(TextFieldStyle.class);
					tfs.font = game.manager.get("sans36.ttf", BitmapFont.class);
					tf = new TextField("", tfs);
					tf.setMessageText("Please enter a description.");
					tf.setAlignment(Align.center);
					getContentTable().row();
					getContentTable().add(tf).fillX().expandX();

					TextButtonStyle tbs = new TextButtonStyle(
							skin.get(TextButtonStyle.class));
					tbs.font = game.manager.get("sans36.ttf", BitmapFont.class);
					TextButton tb;
					tb = new TextButton("OK", tbs);
					button(tb, tf);
					setFillParent(true);
				}

				final String fallback = "ᏐᏈᎵ ᏂᏧᏙᎥᎾ";
				final String[] prefixes = { "ᎢᎬᏱᎢ", "ᏔᎵᏁᎢ", "ᏦᎢᏁᎢ", "ᏅᎩᏁᎢ",
						"ᎯᏍᎩᏁᎢ", "ᏑᏓᎵᏁᎢ", "ᎦᎵᏉᎩᏁᎢ" };

				protected void result(Object object) {
					SlotInfo info = new SlotInfo();
					info.name = tf.getText();
					int islot = -1;
					try {
						islot = Integer.valueOf(slot.nameWithoutExtension());
					} catch (NumberFormatException e) {
					}
					if (info.name.length() == 0) {
						info.name = fallback;
						if (islot >= 0 && islot < prefixes.length) {
							info.name = prefixes[islot] + " " + info.name;
						} else {
							info.name = info.name + " "
									+ slot.nameWithoutExtension();
						}
					}
					json.toJson(info, slot.child(INFO_JSON));
					stage.addAction(Actions.run(initSet0));
				};

				public Dialog show(final Stage stage) {
					super.show(stage);
					RunnableAction focus = Actions.run(new Runnable() {
						@Override
						public void run() {
							stage.setScrollFocus(tf);
							stage.setKeyboardFocus(tf);
						}
					});
					stage.addAction(focus);
					return this;
				};
			};
			setNameDialog.show(stage);
		}
	};

	private Runnable tooSoon = new Runnable() {

		@Override
		public void run() {
//			game.log(this, "Not long enough!");
			Dialog whichMode = new Dialog(
					"It's too soon for a regular session.", skin) {
				{
					text("Please select an option:\n\n"
							+ "Would you like to practice your existing challenges?\n\n"
							+ "Would you like to jump forward by a full day?\n\n"
							+ "Would you like to cancel and go back to main menu?");
					TextButtonStyle tbs = new TextButtonStyle(
							skin.get(TextButtonStyle.class));
					tbs.font = game.manager.get("sans36.ttf", BitmapFont.class);
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
					stage.addAction(Actions.run(initSet0));
				};
			};
			whichMode.show(stage);
		}
	};

	private Runnable loadStats = new Runnable() {
		@Override
		public void run() {
			game.log(this, "Loading Active Deck ...");

			if (!slot.child(ActiveDeckJson).exists()) {
				json.toJson(new ActiveDeck(), slot.child(ActiveDeckJson));
			}
			ActiveDeck tmp = json.fromJson(ActiveDeck.class,
					slot.child(ActiveDeckJson));
			current_pending.deck = tmp.deck;
			current_pending.lastrun = tmp.lastrun;
			Collections.sort(current_pending.deck, byShowTime);

			if (System.currentTimeMillis() - current_pending.lastrun < 16 * ONE_HOUR_ms) {
				Gdx.app.postRunnable(tooSoon);
				return;
			}
			if (!slot.child(INFO_JSON).exists()) {
				Gdx.app.postRunnable(setName);
				return;
			}
			stage.addAction(Actions.run(initSet0));
		}
	};

	private boolean isExtraPractice = false;

	private final NewCardDialog newCardDialog;

	private final Set<String> nodupes = new HashSet<>();
	private final Random rand = new Random();
	private final long sessionStart;

	private Runnable saveActiveDeck = new Runnable() {
		@Override
		public void run() {
			if (isExtraPractice) {
				game.log(this, "Extra Practice Session - NOT SAVING!");
				return;
			}
			ActiveDeck tosave = new ActiveDeck();
			tosave.deck.addAll(current_active.deck);
			tosave.deck.addAll(current_pending.deck);
			tosave.deck.addAll(current_done.deck);
			tosave.lastrun = sessionStart;
			Collections.sort(tosave.deck, byShowTime);

			SlotInfo info;
			FileHandle infoFile = slot.child(INFO_JSON);
			if (!infoFile.exists()) {
				info = new SlotInfo();
				info.name = "ᎤᏲᏒ ᎣᎦᎾ!";
			} else {
				info = json.fromJson(SlotInfo.class, infoFile);
			}
			calculateStats(tosave, info);

			FileHandle tmp = slot.child(ActiveDeckJson + ".tmp");
			tmp.writeString(json.prettyPrint(tosave), false, "UTF-8");
			tmp.moveTo(slot.child(ActiveDeckJson));
			tmp.delete();

			tmp = slot.child(INFO_JSON + ".tmp");
			tmp.writeString(json.prettyPrint(info), false, "UTF-8");
			tmp.moveTo(slot.child(INFO_JSON));
			tmp.delete();
		}

	};

	public void calculateStats(ActiveDeck activeDeck, SlotInfo info) {
		/*
		 * How many are "fully learned" out of the full deck?
		 */
		float decksize = deck.cards.size();
		float full = 0f;
		for (ActiveCard card : activeDeck.deck) {
			if (card.box >= FULLY_LEARNED_BOX) {
				full++;
			}
		}
		info.learned = full / decksize;

		/*
		 * How many are "well known" out of the active deck? (excluding full
		 * learned ones)
		 */
		decksize = 0f;
		full = 0f;
		for (ActiveCard card : activeDeck.deck) {
			if (card.box >= FULLY_LEARNED_BOX) {
				continue;
			}
			if (card.box > PROFICIENT_BOX) {
				full++;
			}
			decksize++;
		}
		info.proficiency = full / decksize;
		info.activeCards = (int) decksize;
	}

	private float notice_elapsed = 0f;
	private float elapsed = 0f;
	private boolean elapsed_tick_on = false;
	private Runnable showACard = new Runnable() {
		@Override
		public void run() {
			final ActiveCard activeCard = getNextCard();
			if (activeCard == null) {
				if (elapsed < MinSessionTime) {
					game.log(this, "session time is not up, adding new cards");
					addCards(IncrementDeckBySize, current_pending);
					Gdx.app.postRunnable(showACard);
					return;
				}
				/*
				 * Session time is up, force time shift cards into active show
				 * range...
				 */
				if (elapsed > MinSessionTime && current_pending.deck.size() > 0) {
					game.log(this, "session time up, shifting cards");
					current_pending.lastrun = System.currentTimeMillis()
							- ONE_MINUTE_ms;
					updateTime(current_pending);
					Gdx.app.postRunnable(showACard);
					return;
				}
				if (elapsed > MinSessionTime) {
					game.log(this, "no cards remaining");
					stage.addAction(Actions.run(saveActiveDeck));
					Dialog bye = new Dialog("CONGRATULATIONS!", skin) {
						{
							ActiveDeck activeDeck = new ActiveDeck();
							activeDeck.deck.addAll(current_active.deck);
							activeDeck.deck.addAll(current_pending.deck);
							activeDeck.deck.addAll(current_done.deck);

							SlotInfo info = new SlotInfo();
							calculateStats(activeDeck, info);

							StringBuilder sb = new StringBuilder();
							sb.append("You Current Statistics");
							sb.append("\n\n");
							sb.append(info.activeCards + " active cards");
							sb.append("\n");
							sb.append(((int) (info.proficiency * 100))
									+ "% proficiency");
							sb.append("\n");
							sb.append(((int) (info.learned * 100))
									+ "% fully learned");
							sb.append("\n\n");
							int minutes = (int) (elapsed / 60f);
							int seconds = (int) (elapsed - minutes * 60f);
							sb.append("Elapsed time: " + minutes + ":"
									+ (seconds < 10 ? "0" : "") + seconds);
							text(sb.toString());
							button("OK!");
						}

						protected void result(Object object) {
							game.setScreen(caller);
							dispose();
						};
					};
					bye.show(stage);
					bye.setModal(true);
					bye.setFillParent(true);
					return;
				}
			}
			final Card deckCard = cards_by_id.get(activeCard.getId());
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
				elapsed_tick_on = true;
				ticktock_id = ticktock.loop(.01f);
				challengeCardDialog.setCounter(cardcount++);
				challengeCardDialog.setCard(activeCard, deckCard);
				challengeCardDialog.show(stage);
				AnswerList answerSetsFor = getAnswerSetsFor(activeCard,
						deckCard, deck);
				activeCard.tries_remaining -= answerSetsFor.correctCount();				
				challengeCardDialog.setAnswers(answerSetsFor);
				challengeCardDialog.addAction(Actions.delay(MaxTimePerCard_sec,
						Actions.run(new Runnable() {
							@Override
							public void run() {
								challengeCardDialog.result(null);
							}
						})));
				for (float x = MaxTimePerCard_sec; x >= 0; x -= .1f) {
					final float timer = MaxTimePerCard_sec - x;
					final float volume = x / MaxTimePerCard_sec;
					DelayAction updater = Actions.delay(x - .05f,
							Actions.run(new Runnable() {
								@Override
								public void run() {
									ticktock.setVolume(ticktock_id, volume);
									challengeCardDialog.setTimer(timer);
								}
							}));
					challengeCardDialog.addAction(updater);
				}
			}
		}
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
			long dif = o1.show_again_ms-o2.show_again_ms;
			if (dif<0) dif=-dif;
			if (dif<ONE_MINUTE_ms*5) {
				return 0;
			}			
			return o1.show_again_ms > o2.show_again_ms ? 1 : -1;			
		}
	};

	private Sound ticktock;

	private long ticktock_id;

	public LearningSession(BoundPronouns _game, Screen caller, FileHandle slot) {
		super(_game, caller);
		this.slot = slot;
		slot.mkdirs();
		if (slot.child("deck.json").exists()) {
			slot.child("deck.json").delete();
		}
		sessionStart = System.currentTimeMillis();
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
		json.setIgnoreUnknownFields(true);

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
				// Card card = cards_by_id.get(_activeCard.getId());
				this.clearActions();
				this.setCheckVisible(false);
				setTimer(0);
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
								resetCorrectInARow(_activeCard);
								_activeCard.noErrors = false;
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
								resetCorrectInARow(_activeCard);
								_activeCard.noErrors = false;
							}
							if (tb.isChecked() && ans.correct) {
								ColorAction toGreen = Actions.color(
										Color.GREEN, .2f);
								tb.addAction(toGreen);
								doCow = false;
								tb.setText(BoundPronouns.HEAVY_CHECK_MARK + " "
										+ ans.answer);
								_activeCard.markCorrect(ans.answer);
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
				_activeCard.show_again_ms = Deck.getNextInterval(_activeCard
						.getMinCorrectInARow());
				stage.addAction(Actions.delay(doBuzzer ? 5.9f : .9f,
						Actions.run(hideThisCard)));
				stage.addAction(Actions.delay(doBuzzer ? 6f : 1f,
						Actions.run(showACard)));
			}
		};
	}

	/**
	 * add this many cards to the Current Active Deck first from the current
	 * Active Deck then from the master Deck set
	 * 
	 * @param needed
	 * @param active
	 */
	public void addCards(int needed, ActiveDeck active) {
		/**
		 * look for previous cards to load first, if their delay time is up
		 */
		Iterator<ActiveCard> ipending = current_pending.deck.iterator();
		while (needed > 0 && ipending.hasNext()) {
			ActiveCard next = ipending.next();
			if (next.box >= FULLY_LEARNED_BOX) {
				continue;
			}
			if (next.show_again_ms > 0) {
				continue;
			}
			active.deck.add(next);
			needed--;
			ipending.remove();
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
			ActiveCard activeCard = new ActiveCard();
			activeCard.box = 0;
			activeCard.noErrors = true;
			activeCard.newCard = true;
			activeCard.pgroup = next.pgroup;
			activeCard.show_again_ms = 0;
			activeCard.vgroup = next.vgroup;
			resetCorrectInARow(activeCard);
			activeCard.tries_remaining = SendToNextSessionThreshold
					* next.answer.size() + 1;
			active.deck.add(activeCard);
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
		 * look for "similar" looking answers
		 */
		Deck tmp = new Deck();
		tmp.cards.addAll(deck.cards);
		Collections.shuffle(tmp.cards);
		scanDeck: for (int distance = 15; distance < 100; distance++) {
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
		Collections.shuffle(answers.list);
		return answers;
	}

	private ActiveCard getNextCard() {
		if (current_active.deck.size() == 0) {
			/*
			 * prevent scheduling fubars caused by long pauses when a "new card"
			 * is being displayed and the user sets the tablet down and hits
			 * HOME or goes to lunch or something
			 */
			if (System.currentTimeMillis() - current_pending.lastrun > 10 * ONE_MINUTE_ms) {
				current_pending.lastrun = System.currentTimeMillis() - 10
						* ONE_MINUTE_ms;
			}
			updateTime(current_pending);
			current_pending.lastrun = System.currentTimeMillis();
			Iterator<ActiveCard> itmp = current_pending.deck.iterator();
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
		current_pending.deck.add(card);
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
		current_pending.deck.remove(card);
	}

	@Override
	public void render(float delta) {
		if (elapsed_tick_on) {
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
	 * time-shift all cards by time since last recorded run. clamps at 24 hours
	 * for max time adjustment.
	 * 
	 * @param activeDeck
	 */
	public void updateTime(ActiveDeck activeDeck) {
		long since = activeDeck.lastrun > 0 ? System.currentTimeMillis()
				- activeDeck.lastrun : System.currentTimeMillis();
		/*
		 * clamp to 24 hours max as the time passed
		 */
		if (since < 0l || since > ONE_DAY_ms) {
			since = ONE_DAY_ms;
		}
		Iterator<ActiveCard> istat = activeDeck.deck.iterator();
		while (istat.hasNext()) {
			ActiveCard next = istat.next();
			next.show_again_ms -= since;
		}
	}

}
