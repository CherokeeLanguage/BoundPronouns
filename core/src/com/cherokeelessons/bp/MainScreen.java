package com.cherokeelessons.bp;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.SlotInfo;
import com.cherokeelessons.cards.SlotInfo.DeckMode;
import com.cherokeelessons.cards.SlotInfo.DisplayMode;

public class MainScreen implements Screen {

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

	private final Json json;

	public class DialogX extends Dialog {
		public DialogX(String title, Skin skin) {
			super(title, skin);
			final Texture background = game.manager.get(
					BoundPronouns.IMG_MAYAN, Texture.class);
			final TextureRegion region = new TextureRegion(background);
			final TiledDrawable tiled = new TiledDrawable(region);
			BitmapFont f54 = game.manager.get("sans54.ttf", BitmapFont.class);
			tiled.setMinHeight(0);
			tiled.setMinWidth(0);
			tiled.setTopHeight(f54.getCapHeight() + 20);
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

		final BitmapFont f54 = game.manager.get("sans54.ttf", BitmapFont.class);
		final BitmapFont f36 = game.manager.get("sans36.ttf", BitmapFont.class);

		final Texture background = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		final TextureRegion region = new TextureRegion(background);
		final TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);
		tiled.setMinWidth(0);
		tiled.setTopHeight(f54.getCapHeight() + 20);

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
		TextButtonStyle tbs = skin.get(TextButtonStyle.class);
		tbs.font = f36;
		// Slot display name
		TextFieldStyle tfs = new TextFieldStyle(skin.get(TextFieldStyle.class));
		tfs.font = f36;

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
				if (whichCards.isDisabled()) {
					return false;
				}
				info.settings.deck = DeckMode.getNext(info.settings.deck);
				whichCards.setText(info.settings.deck.toString());
				return true;
			}
		});
		if (!newSession) {
			whichCards.setDisabled(true);
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
		final Table contentTable = edit.getContentTable();
		edit.setBackground(tiled);
		edit.setFillParent(true);
		contentTable.clearChildren();
		contentTable.row();
		contentTable.add("Name: ").left().fillX();
		contentTable.add(name).expand().fillX().left();
		contentTable.row();
		contentTable.add("Display: ").left().fillX();
		contentTable.add(mode).expand().fillX().left();
		contentTable.row();
		contentTable.add("Mute by default: ").left().fillX();
		contentTable.add(muted).expand().fillX().left();
		contentTable.row();
		contentTable.add("Which card set?").left().fillX();
		contentTable.add(whichCards).expand().fillX().left();
		TextButton ok = new TextButton("OK", tbs);
		edit.button(ok);
		return edit;
	};

	public void doSlotsDialog() {

		final BitmapFont f54 = game.manager.get("sans54.ttf", BitmapFont.class);
		final BitmapFont f36 = game.manager.get("sans36.ttf", BitmapFont.class);
		final BitmapFont f30 = game.manager.get("sans30.ttf", BitmapFont.class);
		final SlotDialog chooseSlot = new SlotDialog("Select Slot", skin, game,
				f54);
		chooseSlot.setKeepWithinStage(true);
		chooseSlot.setModal(true);
		chooseSlot.setFillParent(true);

		final Texture background = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		final TextureRegion region = new TextureRegion(background);
		final TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);
		tiled.setMinWidth(0);
		tiled.setTopHeight(f54.getCapHeight() + 20);

		Table slots = new Table(skin);
		final ScrollPane slotsPane = new ScrollPane(slots, skin);
		slotsPane.setFadeScrollBars(false);
		slotsPane.setColor(Color.DARK_GRAY);
		chooseSlot.getContentTable().add(slotsPane).expand().fill();

		for (int ix = 0; ix < 4; ix++) {
			final FileHandle p0, p1;
			String path0 = "BoundPronouns/slots/" + ix + "/";
			switch (Gdx.app.getType()) {
			case Android:
				p0 = Gdx.files.local(path0);
				break;
			case Applet:
				p0 = Gdx.files.external(path0);
				break;
			case Desktop:
				p0 = Gdx.files.external(path0);
				break;
			case HeadlessDesktop:
				p0 = Gdx.files.external(path0);
				break;
			case WebGL:
				p0 = Gdx.files.external(path0);
				break;
			case iOS:
				p0 = Gdx.files.local(path0);
				break;
			default:
				continue;
			}
			if (!p0.exists()) {
				p0.mkdirs();
			}

			boolean blank = true;
			SlotInfo info = null;
			p1 = p0.child(BoundPronouns.INFO_JSON);
			if (p1.exists()) {
				info = json.fromJson(SlotInfo.class, p1);
				blank = false;
				if (info.version != SlotInfo.StatsVersion) {
					if (!p0.child(LearningSession.ActiveDeckJson).exists()) {
						json.toJson(new ActiveDeck(),
								p0.child(LearningSession.ActiveDeckJson));
					}

					ActiveDeck adeck = json.fromJson(ActiveDeck.class,
							p0.child(LearningSession.ActiveDeckJson));
					if (adeck == null) {
						adeck = new ActiveDeck();
					}
					LearningSession.calculateStats(adeck, info);
					adeck = null;
					json.toJson(info, p1);

				}
			}
			if (info == null) {
				info = new SlotInfo();
				info.settings.name = "*** NEW SESSION ***";
			}
			SlotInfo.Settings settings = info.settings;
			String txt = (StringUtils.isBlank(settings.name)) ? "ᎤᏲᏒ ᏥᏍᏕᏥ!"
					: settings.name;
			txt += "\n" + info.activeCards + " cards";
			txt += ", " + ((int) (info.shortTerm * 100)) + "% short term";
			txt += ", " + ((int) (info.mediumTerm * 100)) + "% medium term";
			txt += ", " + ((int) (info.longTerm * 100)) + "% long term";
			TextButton textb = new TextButton(txt, skin);
			TextButtonStyle tbs = new TextButtonStyle(textb.getStyle());
			tbs.font = f36;
			textb.setStyle(tbs);
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
						DialogX d = getEditDialogFor(p1, isNewSession,
								startSession);
						d.show(stage);
						return true;
					}
					Gdx.app.postRunnable(startSession);
					return true;
				};
			});
			tbs = new TextButtonStyle(textb.getStyle());
			tbs.font = f30;
			VerticalGroup editControls = new VerticalGroup();
			editControls.center();
			TextButton editb = new TextButton("SETTINGS", tbs);
			TextButton deleteb = new TextButton("ERASE", tbs);
			editControls.addActor(editb);
			editControls.addActor(deleteb);
			editControls.fill();
			slots.add(editControls);
			if (blank) {
				editb.setDisabled(true);
				deleteb.setDisabled(true);
			}
			final String slotTxt = txt;
			editb.addListener(new ClickListener() {
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					DialogX edit = getEditDialogFor(p1, false, new Runnable() {
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
					ls.font = f36;
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
		back.getStyle().font = f54;
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
		stage = new Stage();
		viewport = new FitViewport(1280, 720, stage.getCamera());
		viewport.update(1280, 720, true);
		stage.setViewport(viewport);

		json = new Json();
		json.setOutputType(OutputType.json);
		json.setTypeName(null);
		json.setIgnoreUnknownFields(true);

		BitmapFont f54 = game.manager.get("sans54.ttf", BitmapFont.class);

		container = new Table();
		container.setFillParent(true);
		stage.addActor(container);

		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		container.setBackground(d);

		TextButton button;
		TextButtonStyle bstyle;

		bstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bstyle.font = f54;

		button = new TextButton("Cherokee Language Bound Pronouns Practice",
				bstyle);
		button.setDisabled(true);

		container.row();
		int padBottom = 12;
		container.add(button).padBottom(padBottom);

		bstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bstyle.font = f54;
		button = new TextButton("Do A Practice", bstyle);
		button.addListener(viewPractice);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom);

		button = new TextButton("View Pronouns", bstyle);
		button.addListener(viewPronouns);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom);

		button = new TextButton("View Challenges", bstyle);
		button.addListener(viewChallenges);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom);

		// button = new TextButton("Settings", bstyle);
		// button.addListener(viewSettings);
		// button.setTouchable(Touchable.enabled);
		// container.row();
		// container.add(button).padBottom(padBottom);

		button = new TextButton("About", bstyle);
		button.addListener(viewAbout);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom);

		button = new TextButton("Quit", bstyle);
		button.addListener(viewQuit);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom);
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void render(float delta) {
		stage.act();
		Gdx.gl.glClearColor(0, 0, 0, 1);
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

}
