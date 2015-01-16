package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class ChooseProfileScreen implements Screen {

	private final BoundPronouns game;
	private final FitViewport viewport;
	private final Stage stage;
	private final Screen caller;
	
	private final EventListener goBack=new ClickListener(){
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			game.setScreen(caller);
			dispose();
			return true;
		};
	};
	
	public ChooseProfileScreen(BoundPronouns game, Screen mainMenuScreen) {
		this.game = game;
		stage = new Stage();
		viewport = new FitViewport(1280, 720, stage.getCamera());
		viewport.update(1280, 720, true);
		stage.setViewport(viewport);
		caller=mainMenuScreen;

		Table container = new Table();
		container.setFillParent(true);		
		stage.addActor(container);
		
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		container.setBackground(d);

		Label label;

		LabelStyle lstyle24 = new LabelStyle(null, Color.BLUE);
		lstyle24.font = game.manager.get("font24.ttf", BitmapFont.class);

		LabelStyle lstyle54 = new LabelStyle(null, Color.BLUE);
		lstyle54.font = game.manager.get("font54.ttf", BitmapFont.class);
		LabelStyle lstyle72 = new LabelStyle(null, Color.BLUE);
		lstyle72.font = game.manager.get("font72.ttf", BitmapFont.class);

		TextButtonStyle tbstyle = new TextButtonStyle();
		tbstyle.fontColor = Color.BLUE;
		tbstyle.font = game.manager.get("font54.ttf", BitmapFont.class);

		TextButton button = new TextButton("Existing Profile", tbstyle);
//		button.addListener(viewPractice);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(new Label(" ", lstyle24));
		container.row();
		container.add(button);

		button = new TextButton("New Profile", tbstyle);
//		button.addListener(viewPronounsList);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(new Label(" ", lstyle24));
		container.row();
		container.add(button);

		button = new TextButton("Back", tbstyle);
		button.addListener(goBack);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(new Label(" ", lstyle24));
		container.row();
		container.add(button);
		
		container.pack();
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
