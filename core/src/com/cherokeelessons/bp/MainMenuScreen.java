package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MainMenuScreen implements Screen {

	private final BoundPronouns game;
	private final FitViewport viewport;
	private final Stage stage;
	
	public MainMenuScreen(BoundPronouns boundPronouns) {
		this.game=boundPronouns;
		stage = new Stage();
	    viewport = new FitViewport(1280, 720, stage.getCamera());
	    viewport.update(1280, 720, true);
	    stage.setViewport(viewport);
	    
	    VerticalGroup vg = new VerticalGroup();
	    stage.addActor(vg);
	    vg.setFillParent(true);
	    vg.setDebug(true, true);
	    
	    Label label;
//	    Label spacer;
	    
	    LabelStyle lstyle24=new LabelStyle(null, Color.RED);
	    lstyle24.font = game.manager.get("font24.ttf", BitmapFont.class);
//	    spacer=new Label(" ", lstyle24);
	    
	    LabelStyle lstyle54=new LabelStyle(null, Color.RED);
	    lstyle54.font = game.manager.get("font54.ttf", BitmapFont.class);
	    LabelStyle lstyle72=new LabelStyle(null, Color.RED);
	    lstyle72.font = game.manager.get("font72.ttf", BitmapFont.class);
		
		label = new Label("Cherokee Language Bound Pronouns Practice", lstyle54);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(label);
		
		TextButtonStyle tbstyle=new TextButtonStyle();
		tbstyle.fontColor=Color.BLUE;
		tbstyle.font=game.manager.get("font54.ttf", BitmapFont.class);
		
		TextButton button = new TextButton("Do A Practice", tbstyle);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);
		
		button = new TextButton("View Pronouns List", tbstyle);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);
		
		button = new TextButton("Settings", tbstyle);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);
		
		button = new TextButton("About", tbstyle);
		vg.addActor(new Label(" ", lstyle24));
		vg.addActor(button);
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(float delta) {
		stage.act();
		Gdx.gl.glClearColor(1, 1, 1, 1);
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
