package com.cherokeelessons.bp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;

public class ShowChallenges extends ChildScreen implements Screen {

	private final BuildDeck buildDeck;
	private boolean dataReady=false;	
	private final Map<String, Card> cards=new HashMap<String, Card>();
	
	private Runnable done = new Runnable() {
		@Override
		public void run() {
			dataReady=true;
		}
	};
	private boolean viewReady;
	private final Json json;
	public ShowChallenges(BoundPronouns game, Screen caller) {
		super(game, caller);
		buildDeck=new BuildDeck(game, getSlot(), done);
		buildDeck.setSkipBareForms(true);
		Gdx.app.postRunnable(buildDeck);
		json=new Json();
		json.setIgnoreUnknownFields(true);
		json.setOutputType(OutputType.json);
	}
	
	private FileHandle getSlot(){
		FileHandle p0;
		String path0 = "BoundPronouns/slots/x";
		switch (Gdx.app.getType()) {
		case Android:
			p0 = Gdx.files.local(path0);
			break;
		case Applet:
			p0 = Gdx.files.external(path0);
			break;
		case Desktop:
			p0 = Gdx.files.external(path0);
			break;
		case HeadlessDesktop:
			p0 = Gdx.files.external(path0);
			break;
		case WebGL:
			p0 = Gdx.files.external(path0);
			break;
		case iOS:
			p0 = Gdx.files.local(path0);
			break;
		default:
			p0 = Gdx.files.external(path0);
		}
		if (!p0.exists()) {
			p0.mkdirs();
		}
		return p0;
	}
	private Skin skin;	
	@Override
	public void show() {
		super.show();
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		Table t = new Table(skin);
		t.setFillParent(true);
		Label msg = new Label("Loading Deck ...", skin);
		msg.getStyle().font=f54();
		t.add(msg).fill().expand().center();
		TiledDrawable background = getBackground();
		t.setBackground(background);
		stage.addActor(t);
	}

	public BitmapFont f54() {
		return game.manager.get("sans54.ttf", BitmapFont.class);
	}

	public TiledDrawable getBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		return background;
	}
	
	@Override
	public void render(float delta) {
		if (!dataReady) {
			super.render(delta);
			return;
		}
		if (!viewReady) {
			viewReady=true;
			readyView();
			return;
		}
		super.render(delta);		
	}

	private void readyView() {
		stage.clear();
		TiledDrawable background = getBackground();
		Dialog chooseGroup = new Dialog("Please Select a Pronoun Group", skin) {
			@Override
			protected void result(Object object) {
				game.click();
				game.setScreen(caller);
				ShowChallenges.this.dispose();
			}
		};
		background.setMinHeight(0);
		background.setMinWidth(0);
		background.setTopHeight(f54().getCapHeight()+20);
		chooseGroup.getStyle().background=background;
		chooseGroup.setStyle(chooseGroup.getStyle());
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font=f54();
		back.setStyle(back.getStyle());
		chooseGroup.button(back);
		chooseGroup.setModal(true);
		chooseGroup.setFillParent(true);
		Deck deck = json.fromJson(Deck.class, getSlot().child("deck.json"));
		cards.clear();
		for (Card card: deck.cards) {
			cards.put(card.pgroup, card);
		}
		deck.cards.clear();
		List<String> groups = new ArrayList<>();
		groups.addAll(cards.keySet());
		chooseGroup.show(stage);
	}

}
