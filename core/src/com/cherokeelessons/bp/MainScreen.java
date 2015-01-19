package com.cherokeelessons.bp;

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
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

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
	private ClickListener viewSettings = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			game.setScreen(new ShowSettings(game, MainScreen.this));
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

	private ClickListener viewPractice = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			BitmapFont f54 = game.manager.get("sans54.ttf", BitmapFont.class);
			BitmapFont f36 = game.manager.get("sans36.ttf", BitmapFont.class);
			final SlotDialog chooseSlot = new SlotDialog("Select Slot", skin, game, f54);
			chooseSlot.setKeepWithinStage(true);
			chooseSlot.setModal(true);
			chooseSlot.setFillParent(true);
			
			Table slots = new Table(skin);
			final ScrollPane slotsPane = new ScrollPane(slots, skin);
			slotsPane.setFadeScrollBars(false);
			slotsPane.setColor(Color.DARK_GRAY);
			chooseSlot.getContentTable().add(slotsPane).expand().fill();
			
			for (int ix = 0; ix < 10; ix++) {
				final FileHandle p0, p1;
				String path0 = "BoundPronouns/slots/" + ix + "/";
				String path1 = "BoundPronouns/slots/" + ix + "/info.json";
				switch (Gdx.app.getType()) {
				case Android:
					p0 = Gdx.files.local(path0);
					p1 = Gdx.files.local(path1);
					break;
				case Applet:
					p0 = Gdx.files.external(path0);
					p1 = Gdx.files.external(path1);
					break;
				case Desktop:
					p0 = Gdx.files.external(path0);
					p1 = Gdx.files.external(path1);
					break;
				case HeadlessDesktop:
					p0 = Gdx.files.external(path0);
					p1 = Gdx.files.external(path1);
					break;
				case WebGL:
					p0 = Gdx.files.external(path0);
					p1 = Gdx.files.external(path1);
					break;
				case iOS:
					p0 = Gdx.files.local(path0);
					p1 = Gdx.files.local(path1);
					break;
				default:
					continue;
				}
				if (!p0.exists()) {
					p0.mkdirs();
				}
				String txt = "*** EMPTY ***";
				if (p1.exists()) {
					txt = p1.readString("UTF-8");
				}
				TextButton textb = new TextButton(txt, skin);
				TextButtonStyle tbs = new TextButtonStyle(textb.getStyle());
				tbs.font=f36;
				textb.setStyle(tbs);
				slots.row();
				slots.add(textb).pad(0).expand().fill();
				textb.addListener(new ClickListener(){
					public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
						game.log(this, p0.path());
						game.setScreen(new LearningSession(game, MainScreen.this, p0));
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
			back.getStyle().font=f54;
			back.setStyle(back.getStyle());
			chooseSlot.button(back);
			chooseSlot.show(stage).addAction(focus);
			game.click();
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
		int padBottom = 0;
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

		button = new TextButton("Settings", bstyle);
		button.addListener(viewSettings);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom);

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
