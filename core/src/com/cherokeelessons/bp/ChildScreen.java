package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ChildScreen implements Screen, InputProcessor {
	
	protected final BoundPronouns game;
	protected final Screen caller;
	protected final Stage stage;
	protected final InputMultiplexer multi;
	
	protected final ClickListener exit = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			doExit.run();
			return true;
		}
	};
	
	protected final Runnable doExit = new Runnable(){
		@Override
		public void run() {
			game.click();
			game.setScreen(caller);
			dispose();
		}
	};
	
	public ChildScreen(BoundPronouns game, Screen caller) {
		Gdx.app.log("Screen: ", this.getClass().getSimpleName());
		this.game=game;
		this.caller=caller;
		this.multi=new InputMultiplexer();
		stage = new Stage();
		stage.setViewport(BoundPronouns.getFitViewport(stage.getCamera()));
	}
	
	@Override
	public void show() {		
		multi.addProcessor(this);
		multi.addProcessor(stage);
		Gdx.input.setInputProcessor(multi);
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
		stage.setViewport(BoundPronouns.getFitViewport(stage.getCamera()));
		stage.getViewport().update(width, height);
	}

	@Override
	public void pause() {
		//do nothing
	}

	@Override
	public void resume() {
		//do nothing
	}

	@Override
	public void hide() {
		Gdx.input.setInputProcessor(null);
	}

	@Override
	public void dispose() {
		stage.dispose();
		Gdx.app.log(this.getClass().getName(), "dispose()");
	}

	@Override
	public boolean keyDown(int keycode) {
		switch (keycode) {
		case Keys.BACK:
		case Keys.ESCAPE:
			Gdx.app.log(this.getClass().getName(), "<<BACK>>");
			if (doExit!=null) {
				doExit.run();
				return true;
			}
			break;
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
