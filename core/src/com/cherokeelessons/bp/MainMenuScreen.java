package com.cherokeelessons.bp;

import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MainMenuScreen implements Screen {

	private final BoundPronouns game;
	private final FitViewport viewport;
	private final Stage stage;
	private ClickListener viewPronounsList = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			FileHandle list = Gdx.files.internal("csv/pronouns-list.csv");
			List<CSVRecord> records;
			try (CSVParser parse = CSVParser.parse(list.readString(), CSVFormat.RFC4180)){
				records = parse.getRecords();
			} catch (IOException e) {
				game.err(this, e.getMessage(), e);
				return false;
			}
			game.log(this, "Loaded "+records.size()+" records.");
			game.setScreen(new ShowList(game, MainMenuScreen.this, records));
			return true;
		}
	};
	private ClickListener viewSettings = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.log(this, "Event: " + event.getClass().getName());
			return false;
		}
	};
	private ClickListener viewAbout = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.log(this, "Event: " + event.getClass().getName());
			return false;
		}
	};
	private ClickListener viewPractice = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.log(this, "Event: " + event.getClass().getName());
			return false;
		}
	};
	private ClickListener viewQuit = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {			 
			return false;
		}
	};

	public MainMenuScreen(BoundPronouns boundPronouns) {
		this.game = boundPronouns;
		stage = new Stage();
		viewport = new FitViewport(1280, 720, stage.getCamera());
		viewport.update(1280, 720, true);
		stage.setViewport(viewport);

		VerticalGroup vg = new VerticalGroup();
		stage.addActor(vg);
		vg.setFillParent(true);
		vg.setDebug(true, true);

		Label label;

		LabelStyle lstyle24 = new LabelStyle(null, Color.RED);
		lstyle24.font = game.manager.get("font24.ttf", BitmapFont.class);

		LabelStyle lstyle54 = new LabelStyle(null, Color.RED);
		lstyle54.font = game.manager.get("font54.ttf", BitmapFont.class);
		LabelStyle lstyle72 = new LabelStyle(null, Color.RED);
		lstyle72.font = game.manager.get("font72.ttf", BitmapFont.class);

		label = new Label("Cherokee Language Bound Pronouns Practice", lstyle54);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(label);

		TextButtonStyle tbstyle = new TextButtonStyle();
		tbstyle.fontColor = Color.BLUE;
		tbstyle.font = game.manager.get("font54.ttf", BitmapFont.class);

		TextButton button = new TextButton("Do A Practice", tbstyle);
		button.addListener(viewPractice);
		button.setTouchable(Touchable.enabled);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);

		button = new TextButton("View Pronouns List", tbstyle);
		button.addListener(viewPronounsList);
		button.setTouchable(Touchable.enabled);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);

		button = new TextButton("Settings", tbstyle);
		button.addListener(viewSettings);
		button.setTouchable(Touchable.enabled);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);

		button = new TextButton("About", tbstyle);
		button.addListener(viewAbout);
		button.setTouchable(Touchable.enabled);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);

		button = new TextButton("Quit", tbstyle);
		button.addListener(viewQuit);
		button.setTouchable(Touchable.enabled);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void render(float delta) {
		stage.act();
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

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
