package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class LoadingScreen implements Screen {

	private static final boolean doHowl = true;
	private final BoundPronouns game;
	private final FitViewport viewport;
	private final Stage stage;
	private Image loadingBar = null;
	private final Table table;

	public LoadingScreen(BoundPronouns boundPronouns) {
		this.game = boundPronouns;
		stage = new Stage();
		viewport = BoundPronouns.getFitViewport(stage.getCamera());
		stage.setViewport(viewport);
		table = new Table();
		stage.addActor(table);
		table.setFillParent(true);
	}

	@Override
	public void show() {
	}

	private Music howl;

	@Override
	public void render(float delta) {
		stage.act();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
		if (game.manager.update()) 
			nextscreen: {
				if (howl != null && !Gdx.input.isTouched() && howl.isPlaying()) {
					break nextscreen;
				}				
				game.setScreen(new BuildDeckScreen(game, null));
				dispose();
				return;
			}
		if (doHowl) {
			if (howl == null && game.manager.isLoaded(BoundPronouns.SND_COYOTE)) {
				howl = game.manager.get(BoundPronouns.SND_COYOTE, Music.class);
				howl.setLooping(false);
				howl.setVolume(1f);
				howl.play();
			}
		}
		if (!game.manager.isLoaded(BoundPronouns.IMG_LOADING)) {
			return;
		}
		if (loadingBar == null) {
			Gdx.app.log(this.getClass().getName(), "Loading Bar");
			Texture texture = game.manager.get(BoundPronouns.IMG_LOADING,
					Texture.class);
			if (texture == null) {
				Gdx.app.log(this.getClass().getName(),
						"Failed loading 'Loading Bar' image!");
			}
			loadingBar = new Image(texture);
			table.add(loadingBar).align(Align.center).expand();
			loadingBar.setColor(Color.RED);
		}
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
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		stage.dispose();
		if (howl != null) {
			howl.stop();
		}
		game.manager.unload(BoundPronouns.SND_COYOTE);
		game.manager.unload(BoundPronouns.IMG_LOADING);
	}

}
