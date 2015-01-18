package com.cherokeelessons.bp;

import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
	private ClickListener viewPronounsList = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			FileHandle list = Gdx.files.internal("csv/pronouns-list.csv");
			List<CSVRecord> records;
			try (CSVParser parse = CSVParser.parse(list.readString(),
					CSVFormat.RFC4180)) {
				records = parse.getRecords();
			} catch (IOException e) {
				game.err(this, e.getMessage(), e);
				return false;
			}
			// game.log(this, "Loaded "+records.size()+" records.");
			game.setScreen(new ShowList(game, MainScreen.this, records));
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
			SlotDialog chooseSlot = new SlotDialog("Select Slot", skin, game, f54){
				@Override
				protected void result(Object object) {
					if (object==null) {
						return;
					}
					if (object instanceof FileHandle) {						
						game.log(this, ((FileHandle)object).path());
						game.setScreen(new CardSessionInit(game, MainScreen.this, (FileHandle)object));
					}
				}				
			};
			chooseSlot.setKeepWithinStage(true);
			chooseSlot.setModal(true);
			chooseSlot.setFillParent(true);
			
			for (int ix = 0; ix < 5; ix++) {
				FileHandle p0, p1;
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
				Table t = new Table(skin);
				TextButton textb = new TextButton(txt, skin);
				TextButtonStyle tbs = new TextButtonStyle(textb.getStyle());
				tbs.font=f36;
				textb.setStyle(tbs);
				t.add(textb).pad(0).expand().fill();
				chooseSlot.text(textb, p0);
			}
			
			TextButtonStyle backstyle = new TextButtonStyle(skin.get("default",
					TextButtonStyle.class));
			backstyle.font = f54;
			TextButton tb;
			tb = new TextButton(BoundPronouns.BACK_ARROW, backstyle);
			chooseSlot.button(tb);
			chooseSlot.show(stage);
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
		int padBottom = 20;
		container.add(button).padBottom(padBottom);

		bstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bstyle.font = f54;
		button = new TextButton("Do A Practice", bstyle);
		button.addListener(viewPractice);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(button).padBottom(padBottom);

		button = new TextButton("View Pronouns List", bstyle);
		button.addListener(viewPronounsList);
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
