package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MainMenuScreen implements Screen {

	private final BoundPronouns game;
//	private final PerspectiveCamera camera;
	private final FitViewport viewport;
	private final Stage stage;
	
	public MainMenuScreen(BoundPronouns boundPronouns) {
		this.game=boundPronouns;
		stage = new Stage();
	    viewport = new FitViewport(1280, 720, stage.getCamera());
	    viewport.update(1280, 720, true);
	    stage.setViewport(viewport);
	    LabelStyle lstyle=new LabelStyle(game.font, Color.RED);
		Label label = new Label("Bound Pronouns", lstyle);
		VerticalGroup vg = new VerticalGroup();
		stage.addActor(vg);
		vg.setFillParent(true);
		vg.addActor(label);
		vg.setDebug(true);
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(float delta) {
		stage.act();
		
//		Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.draw();        
        

//        if (Gdx.input.isTouched()) {
//            game.setScreen(new GameScreen(game));
//            dispose();
//        }
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
	}

}
