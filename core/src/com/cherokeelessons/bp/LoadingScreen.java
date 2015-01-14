package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class LoadingScreen implements Screen {

	private final BoundPronouns game;
	private final FitViewport viewport;
	private final Stage stage;
	private Image loadingBar=null;
	private final VerticalGroup vg;
	public LoadingScreen(BoundPronouns boundPronouns) {
		this.game=boundPronouns;
		stage = new Stage();
		viewport = new FitViewport(1280, 720, stage.getCamera());
	    viewport.update(1280, 720, true);
	    stage.setViewport(viewport);				
		vg = new VerticalGroup();
		stage.addActor(vg);
		vg.setFillParent(true);		
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub

	}

	private Music howl;
	
	@Override
	public void render(float delta) {
		if (game.manager.update()) {
			if (!Gdx.input.isTouched() && howl!=null && howl.isPlaying()) {
				return;
			}
			game.setScreen(new MainMenuScreen(game));
			dispose();
		}
		if (howl==null && game.manager.isLoaded(BoundPronouns.SND_HOWL)){
			howl=game.manager.get(BoundPronouns.SND_HOWL, Music.class);
			howl.setLooping(false);
			howl.setVolume(1f);
			howl.play();
		}
		if (!game.manager.isLoaded(BoundPronouns.IMG_LOADINGBAR)){
			return;
		}		
		if (loadingBar==null) {
			Gdx.app.log(this.getClass().getName(), "Loading Bar");
			Texture texture = game.manager.get(BoundPronouns.IMG_LOADINGBAR, Texture.class);
			if (texture==null) {
				Gdx.app.log(this.getClass().getName(), "Failed loading 'Loading Bar' image!");
			}
			loadingBar=new Image(texture); 
			vg.addActor(loadingBar);
			float w = loadingBar.getWidth();
			float h = loadingBar.getHeight();
			loadingBar.setOrigin(w/2, h/2);
			loadingBar.invalidateHierarchy();
			loadingBar.setColor(Color.RED);			
			vg.pack();
		}
		stage.act();
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
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		stage.dispose();
		if (howl!=null) {
			howl.stop();
		}
		game.manager.unload(BoundPronouns.SND_HOWL);
		game.manager.unload(BoundPronouns.IMG_LOADINGBAR);
	}

}
