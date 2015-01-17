package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class ChildScreen implements Screen {
	
	protected final BoundPronouns game;
	protected final Screen caller;
	protected final FitViewport viewport;
	protected final Stage stage;
	
	protected final ClickListener exit = new ClickListener() {
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			game.setScreen(caller);
			dispose();
			return true;
		};
	};
	
	public ChildScreen(BoundPronouns game, Screen caller) {
		this.game=game;
		this.caller=caller;
		stage = new Stage();
		viewport = new FitViewport(1280, 720, stage.getCamera());
		viewport.update(1280, 720, true);
		stage.setViewport(viewport);
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
