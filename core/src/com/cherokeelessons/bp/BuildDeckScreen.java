package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;


public class BuildDeckScreen extends ChildScreen {
	
	private final BuildDeck buildDeck;
	private boolean dataReady=false;
	
	private Runnable done = new Runnable() {
		@Override
		public void run() {
			dataReady=true;
		}
	};
	private boolean ready;
	private final Json json;

	public BuildDeckScreen(BoundPronouns game, Screen caller) {
		super(game, caller);
		buildDeck=new BuildDeck(game, BuildDeck.getDeckSlot(), done);	
		
		Gdx.app.postRunnable(buildDeck);
		json=new Json();
		json.setIgnoreUnknownFields(true);
		json.setOutputType(OutputType.json);
	}
	
	private Skin skin;

	@Override
	public void show() {
		super.show();
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		Table t = new Table(skin);
		t.setFillParent(true);
		msg = new Label("Building Deck ...", skin);
		msg.getStyle().font=s54();
		msg.setStyle(msg.getStyle());
		t.add(msg).fill().expand().center();
		TiledDrawable background = getBackground();
		t.setBackground(background);
		stage.addActor(t);
	}
	
	private Label msg;

	private BitmapFont s54() {
		return game.manager.get("serif54.ttf", BitmapFont.class);
	}
	
	private TiledDrawable getBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		return background;
	}
	
	@Override
	public void render(float delta) {
		if (ready) {
			return;
		}
		if (dataReady) {
			ready=true;
			game.setScreen(new MainScreen(game));
			BuildDeckScreen.this.dispose();
			return;
		}
		msg.setText(buildDeck.getStatus());
		super.render(delta);
	}

}
