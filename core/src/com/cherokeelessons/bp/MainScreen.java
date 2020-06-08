package com.cherokeelessons.bp;

import java.text.NumberFormat;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.SlotInfo;
import com.cherokeelessons.cards.SlotInfo.DeckMode;
import com.cherokeelessons.cards.SlotInfo.DisplayMode;
import com.cherokeelessons.cards.SlotInfo.SessionLength;
import com.cherokeelessons.cards.SlotInfo.TimeLimit;
import com.cherokeelessons.util.JsonConverter;
import com.cherokeelessons.util.RandomName;
import com.cherokeelessons.util.SlotFolder;

public class MainScreen implements Screen, InputProcessor {

	public class DialogX extends Dialog {
		public DialogX(final String title, final Skin skin) {
			super(title, skin);
			final Texture background = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
			final TextureRegion region = new TextureRegion(background);
			final TiledDrawable tiled = new TiledDrawable(region);
			tiled.setMinHeight(0);
			tiled.setMinWidth(0);
			tiled.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);
			getTitleLabel().setAlignment(Align.center);
		}
	}

	public static FileHandle getFolder(final int ix) {
		return SlotFolder.getFolder(ix + "");
	}

	private final BoundPronouns game;
	private final Stage stage;
	private final Skin skin;

	private final ClickListener viewPronouns = new ClickListener() {
		@Override
		public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
				final int button) {
			game.click();
			game.setScreen(new ShowPronouns(game, MainScreen.this));
			return true;
		}
	};

	private final ClickListener viewInfo = new ClickListener() {
		@Override
		public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
				final int button) {
			game.click();
			game.setScreen(new ShowInfo(game, MainScreen.this));
			return true;
		}
	};

	private final ClickListener viewAbout = new ClickListener() {
		@Override
		public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
				final int button) {
			game.click();
			game.setScreen(new ShowAbout(game, MainScreen.this));
			return true;
		}
	};

	private final JsonConverter json;

	private final ClickListener viewPractice = new ClickListener() {
		@Override
		public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
				final int button) {
			doSlotsDialog();
			return true;
		}
	};

	private final ClickListener viewQuit = new ClickListener() {
		@Override
		public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
				final int button) {
			game.click();
			game.setScreen(new GoodByeScreen(game, MainScreen.this));
			dispose();
			return true;
		}
	};

	private final Table container;

	private final InputMultiplexer multi;

	public MainScreen(final BoundPronouns boundPronouns) {
		game = boundPronouns;
		game.loadEspeakMap();
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		multi = new InputMultiplexer();
		stage = new Stage();
		stage.setViewport(BoundPronouns.getFitViewport(stage.getCamera()));

		json = new JsonConverter();

		container = new Table();
		container.setFillParent(true);
		stage.addActor(container);

		final Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		container.setBackground(d);

		TextButton button;
		TextButtonStyle bstyle;

		bstyle = new TextButtonStyle(skin.get(TextButtonStyle.class));
		bstyle.font = game.getFont(Font.SerifXLarge);

		button = new TextButton("Cherokee Language\nBound Pronouns Practice", bstyle);
		button.setDisabled(true);
		button.setTouchable(Touchable.disabled);

		int column = 0;
		final int padBottom = 12;
		if (++column % 2 == 0) {
			container.row();
			column = 0;
		}
		container.add(button).padBottom(padBottom).colspan(2).expand().fill();

		bstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bstyle.font = game.getFont(Font.SerifXLarge);
		button = new TextButton("Practice", bstyle);
		button.addListener(viewPractice);
		button.setTouchable(Touchable.enabled);
		if (++column % 2 == 0) {
			container.row();
			column = 0;
		}
		container.add(button).padBottom(padBottom).expand().fill().width(Value.percentWidth(.5f, container));

		button = new TextButton("Information", bstyle);
		button.addListener(viewInfo);
		button.setTouchable(Touchable.enabled);
		if (++column % 2 == 0) {
			container.row();
			column = 0;
		}
		container.add(button).padBottom(padBottom).expand().fill().width(Value.percentWidth(.5f, container));

		button = new TextButton("View Pronouns", bstyle);
		button.addListener(viewPronouns);
		button.setTouchable(Touchable.enabled);
		if (++column % 2 == 0) {
			container.row();
			column = 0;
		}
		container.add(button).padBottom(padBottom).expand().fill().width(Value.percentWidth(.5f, container));

		button = new TextButton("About", bstyle);
		button.addListener(viewAbout);
		button.setTouchable(Touchable.enabled);
		if (++column % 2 == 0) {
			container.row();
			column = 0;
		}
		container.add(button).padBottom(padBottom).expand().fill().width(Value.percentWidth(.5f, container));

		button = new TextButton("Quit", bstyle);
		button.addListener(viewQuit);
		button.setTouchable(Touchable.enabled);
		if (++column % 2 == 0) {
			container.row();
			column = 0;
		}
		container.add(button).padBottom(padBottom).colspan(2).expand().fill();
	}

	@Override
	public void dispose() {
		stage.dispose();
	}

	public void doSlotsDialog() {
		final SlotDialog chooseSlot = new SlotDialog("Select Session", skin, game, game.getFont(Font.SerifLarge));
		chooseSlot.setKeepWithinStage(true);
		chooseSlot.setModal(true);
		chooseSlot.setFillParent(true);

		final Texture background = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TextureRegion region = new TextureRegion(background);
		final TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);
		tiled.setMinWidth(0);
		tiled.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);

		final Table slots = new Table(skin);
		final ScrollPane slotsPane = new ScrollPane(slots, skin);
		slotsPane.setFadeScrollBars(false);
		slotsPane.setColor(Color.DARK_GRAY);
		chooseSlot.getContentTable().add(slotsPane).expand().fill();

		for (int ix = 0; ix < 4; ix++) {
			final FileHandle p0, infoFile;
			p0 = getFolder(ix);

			boolean blank = true;
			SlotInfo info = null;
			infoFile = p0.child(BoundPronouns.INFO_JSON);
			if (infoFile.exists()) {
				info = json.fromJson(SlotInfo.class, infoFile);
				blank = info == null;
				if (info == null) {
					info = new SlotInfo();
					info.settings.sessionLength = SessionLength.Brief;
					info.settings.timeLimit = TimeLimit.Novice;
					info.settings.deck = DeckMode.Conjugations;
				}
				if (StringUtils.isBlank(info.settings.name)) {
					info.settings.name = RandomName.getRandomName();
					json.toJson(info, infoFile);
				}
				if (!info.updatedVersion()) {
					final FileHandle activeDeckFile = p0.child(LearningSession.ActiveDeckJson);
					ActiveDeck activeDeck = null;
					if (activeDeckFile.exists()) {
						activeDeck = json.fromJson(ActiveDeck.class, activeDeckFile);
					}
					if (activeDeck == null) {
						activeDeck = new ActiveDeck();
					}
					SlotInfo.calculateStats(info, activeDeck);
					json.toJson(info, infoFile);
					// String name = StringUtils.isBlank(info.settings.name) ?
					// RandomName.getRandomName() : info.settings.name;
					// String tag = info.level.getEnglish() + "!!!" + name;
				}
			}
			if (info == null || blank) {
				info = new SlotInfo();
				info.settings.name = "*** NEW SESSION ***";
				info.settings.sessionLength = SessionLength.Brief;
				info.settings.timeLimit = TimeLimit.Novice;
				info.settings.deck = DeckMode.Conjugations;
			}
			info.validate();
			final SlotInfo.Settings settings = info.settings;

			SlotInfo.calculateTotalCardCount(info, game.deck.cards);
			final NumberFormat nf = NumberFormat.getInstance();
			String txt = "";
			txt += info.level;
			txt += " ";
			txt += StringUtils.isBlank(settings.name) ? RandomName.getRandomName() : settings.name;
			txt += " - ";
			txt += "Score: " + info.lastScore;
			txt += "\n";
			txt += nf.format(info.activeCards) + " of " + nf.format(info.getTotalCards()) + " cards";
			txt += " with a " + info.proficiency + "% proficiency";

			TextButtonStyle tbs = new TextButtonStyle(skin.get(TextButtonStyle.class));
			tbs.font = game.getFont(Font.SerifSmall);
			final TextButton textb = new TextButton(txt, tbs);

			slots.row();
			final Image levelImage = new Image(
					game.manager.get(BoundPronouns.levelImg(info.level.getLevel()), Texture.class));
			slots.add(levelImage).pad(5).left();
			slots.add(textb).pad(0).expand().fill().left();
			final boolean isNewSession = blank;
			textb.addListener(new ClickListener() {
				@Override
				public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
						final int button) {
					final Runnable startSession = new Runnable() {
						@Override
						public void run() {
							chooseSlot.hide(null);
							game.log(this, p0.path());
							game.setScreen(new LearningSession(game, MainScreen.this, p0));
						}
					};
					if (isNewSession) {
						final DialogX d = getEditDialogFor(infoFile, isNewSession, startSession);
						d.show(stage);
						return true;
					}
					Gdx.app.postRunnable(startSession);
					return true;
				}
			});
			tbs = new TextButtonStyle(textb.getStyle());
			tbs.font = game.getFont(Font.SerifSmall);
			final Table editControls = new Table();
			editControls.center();
			editControls.defaults().pad(10);

			final Texture img_edit = game.manager.get(BoundPronouns.IMG_SETTINGS, Texture.class);
			final TextureRegionDrawable draw_edit = new TextureRegionDrawable(new TextureRegion(img_edit));
			final ImageButton editb = new ImageButton(draw_edit);
			editb.setTransform(true);
			editb.getImage().setScaling(Scaling.fit);
			editb.getImage().setColor(Color.DARK_GRAY);
			editControls.add(editb).center();

			final Texture img_delete = game.manager.get(BoundPronouns.IMG_ERASE, Texture.class);
			final TextureRegionDrawable draw_delete = new TextureRegionDrawable(new TextureRegion(img_delete));
			final ImageButton deleteb = new ImageButton(draw_delete);
			deleteb.setTransform(true);
			deleteb.getImage().setScaling(Scaling.fit);
			deleteb.getImage().setColor(Color.DARK_GRAY);
			editControls.add(deleteb).center();

//			Texture img_sync = game.manager.get(BoundPronouns.IMG_SYNC, Texture.class);
//			TextureRegionDrawable draw_sync = new TextureRegionDrawable(new TextureRegion(img_sync));
//			ImageButton syncb = new ImageButton(draw_sync);
//			syncb.setTransform(true);
//			syncb.getImage().setScaling(Scaling.fit);
//			syncb.getImage().setColor(Color.DARK_GRAY);
//			editControls.add(syncb).center();

			slots.add(editControls);
			if (blank) {
				editb.setDisabled(true);
				editb.setTouchable(Touchable.disabled);
				editb.getImage().setColor(Color.CLEAR);
				deleteb.setDisabled(true);
				deleteb.setTouchable(Touchable.disabled);
				deleteb.getImage().setColor(Color.CLEAR);
			}
//			syncb.addListener(new ClickListener() {
//				@Override
//				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
//					Gdx.app.log("MainScreen", p0.name());
//					Runnable whenDone = new Runnable() {
//						@Override
//						public void run() {
//							chooseSlot.hide();
//							doSlotsDialog();
//						}
//					};
//					game.click();
//					return true;
//				}
//			});

			final String slotTxt = txt;
			editb.addListener(new ClickListener() {
				@Override
				public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
						final int button) {
					final DialogX edit = getEditDialogFor(infoFile, false, new Runnable() {
						@Override
						public void run() {
							chooseSlot.hide();
							doSlotsDialog();
						}
					});
					edit.show(stage);
					return true;
				}

			});

			deleteb.addListener(new ClickListener() {
				@Override
				public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
						final int button) {
					final Dialog confirm = new Dialog("Erase?", skin) {
						{
							final WindowStyle ws = new WindowStyle(skin.get(WindowStyle.class));
							ws.titleFont = game.getFont(Font.SerifLarge);
							setStyle(ws);
							getTitleLabel().setAlignment(Align.center);
						}

						@Override
						protected void result(final Object object) {
							if (object == null) {
								return;
							}
							if (object.equals("Y")) {
								p0.deleteDirectory();
							}
							chooseSlot.hide();
							doSlotsDialog();
						}
					};
					confirm.setBackground(tiled);
					final LabelStyle ls = skin.get(LabelStyle.class);
					ls.font = game.getFont(Font.SerifMedium);
					final Label msg = new Label("Erase this slot?\n\n" + slotTxt, ls);
					msg.setWrap(true);
					confirm.getContentTable().clearChildren();
					confirm.getContentTable().add(msg).fill().expand();
					confirm.button("Yes", "Y");
					confirm.button("No");
					confirm.setFillParent(true);
					confirm.show(stage);
					return true;
				}
			});
		}

		final RunnableAction focus = Actions.run(new Runnable() {
			@Override
			public void run() {
				stage.setScrollFocus(slotsPane);
				stage.setKeyboardFocus(slotsPane);
			}
		});

		final TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font = game.getFont(Font.SerifMedium);
		back.setStyle(back.getStyle());
		chooseSlot.button(back);
		chooseSlot.show(stage).addAction(focus);
		game.click();
	}

	public DialogX getEditDialogFor(final FileHandle p1, final boolean newSession) {
		return getEditDialogFor(p1, newSession, new Runnable() {
			@Override
			public void run() {
				// Do nothing
			}
		});
	}

	public DialogX getEditDialogFor(final FileHandle p1, final boolean newSession, final Runnable onResult) {
		final Texture background = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TextureRegion region = new TextureRegion(background);
		final TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);
		tiled.setMinWidth(0);
		tiled.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);

		final SlotInfo info;
		if (p1.exists()) {
			final SlotInfo tmp = json.fromJson(SlotInfo.class, p1);
			if (tmp != null) {
				info = tmp;
			} else {
				info = new SlotInfo();
			}
		} else {
			info = new SlotInfo();
		}
		info.validate();
		final TextButtonStyle tbs = new TextButtonStyle(skin.get(TextButtonStyle.class));
		tbs.font = game.getFont(Font.SerifSmall);
		// Slot display name
		final TextFieldStyle tfs = new TextFieldStyle(skin.get(TextFieldStyle.class));
		tfs.font = game.getFont(Font.SerifSmall);

		if (!newSession) {
			info.settings.name = StringUtils.isBlank(info.settings.name) ? RandomName.getRandomName()
					: info.settings.name;
		}
		final TextField name = new TextField(info.settings.name, tfs);
		name.setDisabled(true);
		name.setTouchable(Touchable.enabled);
		name.addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
					final int button) {
				name.setTouchable(Touchable.disabled);
				final TextInputListener listener = new TextInputListener() {
					@Override
					public void canceled() {
						name.setTouchable(Touchable.enabled);
					}

					@Override
					public void input(final String text) {
						name.setText(text);
						name.setTouchable(Touchable.enabled);
					}
				};
				if (BoundPronouns.pInput == null) {
					Gdx.input.getTextInput(listener, "Profile Name?", name.getText(), "");
				} else {
					BoundPronouns.pInput.getTextInput(listener, "Profile Name?", name.getText(), "");
				}
				return true;
			}
		});

		final TextButton mode = new TextButton(info.settings.display.toString(), tbs);
		mode.addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
					final int button) {
				info.settings.display = DisplayMode.getNext(info.settings.display);
				mode.setText(info.settings.display.toString());
				return true;
			}
		});

		final TextButton muted = new TextButton(info.settings.muted ? "Yes" : "No", tbs);
		muted.addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
					final int button) {
				info.settings.muted = !info.settings.muted;
				muted.setText(info.settings.muted ? "Yes" : "No");
				return true;
			}
		});
		final TextButton whichCards = new TextButton(info.settings.deck.toString(), tbs);
		whichCards.addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
					final int button) {
				info.settings.deck = DeckMode.getNext(info.settings.deck);
				whichCards.setText(info.settings.deck.toString());
				return true;
			}
		});
		if (!newSession) {
			whichCards.setDisabled(true);
			whichCards.setTouchable(Touchable.disabled);
		}

		final TextButton ok = new TextButton("OK", tbs);
		final TextButton cancel = new TextButton("CANCEL", tbs);

		final DialogX edit = new DialogX("Settings", skin) {
			@Override
			protected void result(Object object) {
				if (object == null) {
					object = cancel;
				}
				if (ok.equals(object)) {
					info.settings.name = name.getText();
					if (StringUtils.isBlank(info.settings.name)) {
						info.settings.name = RandomName.getRandomName();
					}
					json.toJson(info, p1);
				}
				if (onResult != null) {
					Gdx.app.postRunnable(onResult);
				}
			}

			@Override
			public Dialog show(@SuppressWarnings("hiding") final Stage stage) {
				super.show(stage);
				stage.setKeyboardFocus(name);
				stage.setScrollFocus(name);
				name.setCursorPosition(name.getText().length());
				return this;
			}
		};
		final LabelStyle ls = new LabelStyle(skin.get(LabelStyle.class));
		ls.font = game.getFont(Font.SerifMedium);
		final Table contentTable = edit.getContentTable();
		edit.setBackground(tiled);
		edit.setFillParent(true);
		contentTable.clearChildren();
		contentTable.row();
		contentTable.add(new Label("Name: ", ls)).left().fillX();
		contentTable.add(name).expand().fillX().left();
		contentTable.row();
		contentTable.add(new Label("Display: ", ls)).left().fillX();
		contentTable.add(mode).expand().fillX().left();

		// contentTable.row();
		// contentTable.add(new Label("Session Length: ", ls)).left().fillX();
		// contentTable.add(sessionLength).expand().fillX().left();

		// contentTable.row();
		// contentTable.add(new Label("Card Time Limit: ", ls)).left().fillX();
		// contentTable.add(timeLimit).expand().fillX().left();

		contentTable.row();
		contentTable.add(new Label("Mute by default: ", ls)).left().fillX();
		contentTable.add(muted).expand().fillX().left();
		contentTable.row();
		contentTable.add(new Label("Which card set?", ls)).left().fillX();
		contentTable.add(whichCards).expand().fillX().left();

		edit.button(ok, ok);
		edit.button(cancel, cancel);

		return edit;
	}

	@Override
	public void hide() {
		Gdx.input.setInputProcessor(null);
	}

	@Override
	public boolean keyDown(final int keycode) {
		switch (keycode) {
		case Keys.BACK:
		case Keys.ESCAPE:
			game.setScreen(new GoodByeScreen(game, this));
			return true;
		default:
		}
		return false;
	}

	@Override
	public boolean keyTyped(final char character) {
		return false;
	}

	@Override
	public boolean keyUp(final int keycode) {
		return false;
	}

	@Override
	public boolean mouseMoved(final int screenX, final int screenY) {
		return false;
	}

	@Override
	public void pause() {
		// Do nothing
	}

	@Override
	public void render(final float delta) {
		stage.act();
		BoundPronouns.glClearColor();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
	}

	@Override
	public void resize(final int width, final int height) {
		stage.setViewport(BoundPronouns.getFitViewport(stage.getCamera()));
		stage.getViewport().update(width, height);
	}

	@Override
	public void resume() {
		// Do nothing
	}

	@Override
	public boolean scrolled(final int amount) {
		return false;
	}

	@Override
	public void show() {
		multi.addProcessor(this);
		multi.addProcessor(stage);
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
		return false;
	}

	@Override
	public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
		return false;
	}

	@Override
	public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
		return false;
	}

}
