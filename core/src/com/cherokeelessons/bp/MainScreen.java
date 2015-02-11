package com.cherokeelessons.bp;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.SlotInfo;
import com.cherokeelessons.cards.SlotInfo.DeckMode;
import com.cherokeelessons.cards.SlotInfo.DisplayMode;
import com.cherokeelessons.util.JsonConverter;

public class MainScreen implements Screen, InputProcessor {

	private final BoundPronouns game;
	private final FitViewport viewport;
	private final Stage stage;
	private final Skin skin;
	private ClickListener viewPronouns = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			game.setScreen(new ShowList(game, MainScreen.this));
			return true;
		}
	};
	private ClickListener viewChallenges = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			game.setScreen(new ShowChallenges(game, MainScreen.this));
			return true;
		}
	};

	private ClickListener viewAbout = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			game.setScreen(new ShowAbout(game, MainScreen.this));
			return true;
		}
	};

	private final JsonConverter json;

	public class DialogX extends Dialog {
		public DialogX(String title, Skin skin) {
			super(title, skin);
			final Texture background = game.manager.get(
					BoundPronouns.IMG_MAYAN, Texture.class);
			final TextureRegion region = new TextureRegion(background);
			final TiledDrawable tiled = new TiledDrawable(region);
			tiled.setMinHeight(0);
			tiled.setMinWidth(0);
			tiled.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);
		}
	}

	public DialogX getEditDialogFor(final FileHandle p1, boolean newSession) {
		return getEditDialogFor(p1, newSession, new Runnable() {
			@Override
			public void run() {
			}
		});
	}

	public DialogX getEditDialogFor(final FileHandle p1, boolean newSession,
			final Runnable onResult) {
		final Texture background = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		final TextureRegion region = new TextureRegion(background);
		final TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);
		tiled.setMinWidth(0);
		tiled.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);

		final SlotInfo info;
		if (p1.exists()) {
			SlotInfo tmp = json.fromJson(SlotInfo.class, p1);
			if (tmp != null) {
				info = tmp;
			} else {
				info = new SlotInfo();
			}
		} else {
			info = new SlotInfo();
		}
		TextButtonStyle tbs = new TextButtonStyle(
				skin.get(TextButtonStyle.class));
		tbs.font = game.getFont(Font.SerifMedium);
		// Slot display name
		TextFieldStyle tfs = new TextFieldStyle(skin.get(TextFieldStyle.class));
		tfs.font = game.getFont(Font.SerifMedium);

		if (!newSession) {
			info.settings.name = (StringUtils.isBlank(info.settings.name)) ? "ᏐᏈᎵ ᏂᏧᏙᎥᎾ"
					: info.settings.name;
		}

		final TextField name = new TextField(info.settings.name, tfs);
		final TextButton mode = new TextButton(
				info.settings.display.toString(), tbs);
		mode.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				info.settings.display = DisplayMode
						.getNext(info.settings.display);
				mode.setText(info.settings.display.toString());
				return true;
			}
		});
		final TextButton muted = new TextButton(info.settings.muted ? "Yes"
				: "No", tbs);
		muted.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				info.settings.muted = !info.settings.muted;
				muted.setText(info.settings.muted ? "Yes" : "No");
				return true;
			}
		});
		final TextButton whichCards = new TextButton(
				info.settings.deck.toString(), tbs);
		whichCards.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				info.settings.deck = DeckMode.getNext(info.settings.deck);
				whichCards.setText(info.settings.deck.toString());
				return true;
			}
		});
		if (!newSession) {
			whichCards.setDisabled(true);
			whichCards.setTouchable(Touchable.disabled);
		}

		final DialogX edit = new DialogX("Settings", skin) {
			protected void result(Object object) {
				info.settings.name = name.getText();
				json.toJson(info, p1);
				if (onResult != null) {
					Gdx.app.postRunnable(onResult);
				}
			};

			@Override
			public Dialog show(Stage stage) {
				super.show(stage);
				stage.setKeyboardFocus(name);
				stage.setScrollFocus(name);
				name.setCursorPosition(name.getText().length());
				return this;
			}
		};
		LabelStyle ls = new LabelStyle(skin.get(LabelStyle.class));
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
		contentTable.row();
		contentTable.add(new Label("Mute by default: ", ls)).left().fillX();
		contentTable.add(muted).expand().fillX().left();
		contentTable.row();
		contentTable.add(new Label("Which card set?", ls)).left().fillX();
		contentTable.add(whichCards).expand().fillX().left();
		TextButton ok = new TextButton("OK", tbs);
		edit.button(ok);
		return edit;
	};

	public void doSlotsDialog() {
		final SlotDialog chooseSlot = new SlotDialog("Select Slot", skin, game,
				game.getFont(Font.SerifLarge));
		chooseSlot.setKeepWithinStage(true);
		chooseSlot.setModal(true);
		chooseSlot.setFillParent(true);

		final Texture background = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		final TextureRegion region = new TextureRegion(background);
		final TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);
		tiled.setMinWidth(0);
		tiled.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);

		Table slots = new Table(skin);
		final ScrollPane slotsPane = new ScrollPane(slots, skin);
		slotsPane.setFadeScrollBars(false);
		slotsPane.setColor(Color.DARK_GRAY);
		chooseSlot.getContentTable().add(slotsPane).expand().fill();

		for (int ix = 0; ix < 4; ix++) {
			final FileHandle p0, infoFile;
			String path0 = "BoundPronouns/slots/" + ix + "/";
			p0 = Gdx.files.external(path0);
			p0.mkdirs();
			if (Gdx.app.getType().equals(ApplicationType.Android)) {
				FileHandle px = Gdx.files.internal(path0);
				if (px.exists()) {
					game.log(this,
							"Migrating from private storage to public storage...");
					p0.deleteDirectory();
					px.moveTo(p0);
				}
			}

			boolean blank = true;
			SlotInfo info = null;
			infoFile = p0.child(BoundPronouns.INFO_JSON);
			if (infoFile.exists()) {
				info = json.fromJson(SlotInfo.class, infoFile);
				blank = (info == null);
				if (info == null) {
					info = new SlotInfo();
				}
				if (info.version!=SlotInfo.StatsVersion) {
					FileHandle activeDeckFile = p0
							.child(LearningSession.ActiveDeckJson);
					ActiveDeck activeDeck = null;
					if (activeDeckFile.exists()) {
						activeDeck = json.fromJson(ActiveDeck.class,
								activeDeckFile);
					}
					if (activeDeck == null) {
						activeDeck = new ActiveDeck();
					}
					LearningSession.calculateStats(activeDeck, info);
					json.toJson(info, infoFile);
				}
			}
			if (blank) {
				info = new SlotInfo();
				info.settings.name = "*** NEW SESSION ***";
			}
			SlotInfo.Settings settings = info.settings;
			String txt = (StringUtils.isBlank(settings.name)) ? "ᎤᏲᏒ ᏥᏍᏕᏥ!"
					: settings.name;
			txt += " - ";
			txt += info.activeCards + " cards";
			txt += "\n";
			txt += ((int) (info.shortTerm * 100)) + "% short term";
			txt += ", " + ((int) (info.mediumTerm * 100)) + "% medium term";
			txt += ", " + ((int) (info.longTerm * 100)) + "% long term";

			TextButtonStyle tbs = new TextButtonStyle(
					skin.get(TextButtonStyle.class));
			tbs.font = game.getFont(Font.SerifMedium);
			TextButton textb = new TextButton(txt, tbs);

			slots.row();
			slots.add(textb).pad(0).expand().fill().left();
			final boolean isNewSession = blank;
			textb.addListener(new ClickListener() {
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					final Runnable startSession = new Runnable() {
						@Override
						public void run() {
							chooseSlot.hide(null);
							game.log(this, p0.path());
							game.setScreen(new LearningSession(game,
									MainScreen.this, p0));
						}
					};
					if (isNewSession) {
						DialogX d = getEditDialogFor(infoFile, isNewSession,
								startSession);
						d.show(stage);
						return true;
					}
					Gdx.app.postRunnable(startSession);
					return true;
				};
			});
			tbs = new TextButtonStyle(textb.getStyle());
			tbs.font = game.getFont(Font.SerifSmall);
			VerticalGroup editControls = new VerticalGroup();
			editControls.center();
			TextButton editb = new TextButton("SETTINGS", tbs);
			editb.padTop(0);
			editb.padBottom(0);
			TextButton deleteb = new TextButton("ERASE", tbs);
			deleteb.padTop(0);
			deleteb.padBottom(0);
			editControls.addActor(editb);
			editControls.addActor(deleteb);
			editControls.fill();
			slots.add(editControls);
			if (blank) {
				editb.setDisabled(true);
				editb.setTouchable(Touchable.disabled);
				deleteb.setDisabled(true);
				deleteb.setTouchable(Touchable.disabled);
			}
			final String slotTxt = txt;
			editb.addListener(new ClickListener() {
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					DialogX edit = getEditDialogFor(infoFile, false,
							new Runnable() {
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
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					Dialog confirm = new Dialog("Erase?", skin) {
						{
							WindowStyle ws = new WindowStyle(skin
									.get(WindowStyle.class));
							ws.titleFont = game.getFont(Font.SerifLarge);
							setStyle(ws);
						}

						protected void result(Object object) {
							if (object == null) {
								return;
							}
							if (object.equals("Y")) {
								p0.deleteDirectory();
							}
							chooseSlot.hide();
							doSlotsDialog();
						};
					};
					confirm.setBackground(tiled);
					LabelStyle ls = skin.get(LabelStyle.class);
					ls.font = game.getFont(Font.SerifMedium);
					Label msg = new Label("Erase this slot?\n\n" + slotTxt, ls);
					msg.setWrap(true);
					confirm.getContentTable().clearChildren();
					confirm.getContentTable().add(msg).fill().expand();
					confirm.button("Yes", "Y");
					confirm.button("No");
					confirm.setFillParent(true);
					confirm.show(stage);
					return true;
				};
			});
		}

		RunnableAction focus = Actions.run(new Runnable() {
			@Override
			public void run() {
				stage.setScrollFocus(slotsPane);
				stage.setKeyboardFocus(slotsPane);
			}
		});

		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font = game.getFont(Font.SerifLarge);
		back.setStyle(back.getStyle());
		chooseSlot.button(back);
		chooseSlot.show(stage).addAction(focus);
		game.click();
	}

	private ClickListener viewPractice = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			doSlotsDialog();
			return true;
		}
	};

	private ClickListener viewQuit = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			game.setScreen(new GoodByeScreen(game, MainScreen.this));
			dispose();
			return true;
		}
	};

	private final Table container;

	public MainScreen(BoundPronouns boundPronouns) {
		this.game = boundPronouns;
		this.skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		this.multi = new InputMultiplexer();
		stage = new Stage();
		viewport = BoundPronouns.getFitViewport(stage.getCamera());
		stage.setViewport(viewport);

		json = new JsonConverter();

		container = new Table();
		container.setFillParent(true);
		stage.addActor(container);

		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		container.setBackground(d);

		TextButton button;
		TextButtonStyle bstyle;

		bstyle = new TextButtonStyle(skin.get(TextButtonStyle.class));
		bstyle.font = game.getFont(Font.SerifXLarge);

		button = new TextButton("Cherokee Language\nBound Pronouns Practice",
				bstyle);
		button.setDisabled(true);
		button.setTouchable(Touchable.disabled);

		int padBottom = 12;
		container.row();
		container.add(button).padBottom(padBottom).colspan(2).fillX();

		bstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bstyle.font = game.getFont(Font.SerifXLarge);
		button = new TextButton("Do A Practice", bstyle);
		button.addListener(viewPractice);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom).fillX();

		button = new TextButton("View Pronouns", bstyle);
		button.addListener(viewPronouns);
		button.setTouchable(Touchable.enabled);
		// container.row();
		container.add(button).padBottom(padBottom).fillX();

		button = new TextButton("View Challenges", bstyle);
		button.addListener(viewChallenges);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom).fillX();

		button = new TextButton("About", bstyle);
		button.addListener(viewAbout);
		button.setTouchable(Touchable.enabled);
		// container.row();
		container.add(button).padBottom(padBottom).fillX();

		button = new TextButton("Quit", bstyle);
		button.addListener(viewQuit);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom).colspan(2).fillX();
	}

	protected final InputMultiplexer multi;

	@Override
	public void show() {
		multi.addProcessor(this);
		multi.addProcessor(stage);
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void render(float delta) {
		stage.act();
		BoundPronouns.glClearColor();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
		Gdx.input.setInputProcessor(null);
	}

	@Override
	public void dispose() {
		stage.dispose();
	}

	@Override
	public boolean keyDown(int keycode) {
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
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}

}
