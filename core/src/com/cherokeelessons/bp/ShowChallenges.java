package com.cherokeelessons.bp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.cherokeelessons.bp.BuildDeck.DataSet;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;


public class ShowChallenges extends ChildScreen implements Screen {

	private final Map<String, Deck> cards=new HashMap<>();
	
	private boolean viewReady;
	private final Json json;
	public ShowChallenges(BoundPronouns game, Screen caller) {
		super(game, caller);
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

	private BitmapFont f54() {
		return game.manager.get("sans54.ttf", BitmapFont.class);
	}
	
	private BitmapFont s54() {
		return game.manager.get("serif54.ttf", BitmapFont.class);
	}
	
	private BitmapFont f36() {
		return game.manager.get("sans36.ttf", BitmapFont.class);
	}
	
	private BitmapFont s36() {
		return game.manager.get("serif36.ttf", BitmapFont.class);
	}

	private TiledDrawable getBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		return background;
	}
	
	@Override
	public void render(float delta) {
		if (!viewReady) {
			msg.setText("Building view...");
			viewReady=true;
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					readyView();
				}
			});
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
		chooseGroup.getStyle().titleFont=f54();
		chooseGroup.getStyle().background=background;
		chooseGroup.setStyle(chooseGroup.getStyle());
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font=f54();
		back.setStyle(back.getStyle());
		chooseGroup.button(back);
		chooseGroup.setModal(true);
		chooseGroup.setFillParent(true);
		Table groupsTable = new Table(skin);
		final ScrollPane groupsPane = new ScrollPane(groupsTable, skin);
		groupsPane.setFadeScrollBars(false);
		groupsPane.setColor(Color.DARK_GRAY);
		chooseGroup.getContentTable().add(groupsPane).expand().fill();
		
		Deck deck = json.fromJson(Deck.class, BuildDeck.getDeckSlot().child("deck.json"));
		cards.clear();
		for (Card card: deck.cards) {
			Deck groupdeck = cards.get(card.pgroup);
			if (groupdeck==null) {
				groupdeck=new Deck();
				cards.put(card.pgroup, groupdeck);
			}
			groupdeck.cards.add(card);
		}
		deck.cards.clear();
		deck=null;
		
		Map<String, String> lookup_details = new HashMap<String, String>();
		List<DataSet> plist = BoundPronouns.loadPronounRecords();
		for (DataSet data: plist) {
			String prev = lookup_details.get(data.chr);
			lookup_details.put(data.chr, (prev!=null?prev+"\n":"") + data.def);			
		}
		plist.clear();
		plist=null;
		
		List<String> groups = new ArrayList<>();
		groups.addAll(cards.keySet());
		Collections.sort(groups);
		boolean nextRow=true;
		for(final String group: groups) {
			String group_name=group+"\n"+lookup_details.get(group);
			TextButton button = new TextButton(group_name, skin);
			button.getStyle().font=f36();
			button.setStyle(button.getStyle());
			button.addListener(new ClickListener(){
				@Override
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					ShowChallenges.this.showDialogFor(group);
					return true;
				}
			});
			if (nextRow) {
				groupsTable.row();
			}
			groupsTable.add(button).fill();
			nextRow=!nextRow;
		}
		RunnableAction focus = Actions.run(new Runnable() {			
			@Override
			public void run() {
				stage.setScrollFocus(groupsPane);
				stage.setKeyboardFocus(groupsPane);
			}
		});
		chooseGroup.show(stage).addAction(focus);
	}

	private void showDialogFor(String group) {
		TiledDrawable background = getBackground();
		Dialog theGroup = new Dialog(group, skin);
		background.setMinHeight(0);
		background.setMinWidth(0);
		background.setTopHeight(f54().getCapHeight()+20);
		theGroup.getStyle().background=background;
		theGroup.getStyle().titleFont=f54();
		theGroup.setStyle(theGroup.getStyle());
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font=f54();
		back.setStyle(back.getStyle());
		theGroup.button(back);
		theGroup.setModal(true);
		theGroup.setFillParent(true);
		Table challenges = new Table(skin);
		final ScrollPane challengesPane = new ScrollPane(challenges, skin);
		challengesPane.setFadeScrollBars(false);
		challengesPane.setColor(Color.DARK_GRAY);
		theGroup.getContentTable().add(challengesPane).expand().fill();
		
		Deck deck = cards.get(group);
		if (deck==null) {
			game.log(this, "FATAL EMPTY DECK?");
			return;
		}
		Collections.sort(deck.cards);
		TextButtonStyle tbstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		tbstyle.font=s36();
		for (Card card: deck.cards) {
			StringBuilder sb = new StringBuilder();
			for (String c: card.challenge) {
				sb.append(c);
				sb.append("\n");
			}
			for (String c: card.answer) {
				sb.append(c);
				sb.append("\n");
			}
			TextButton button = new TextButton(sb.toString(), tbstyle);
			button.setDisabled(true);
			challenges.row();
			challenges.add(button).fillX().expand().padBottom(20);
			
		}
		RunnableAction focus = Actions.run(new Runnable() {			
			@Override
			public void run() {
				stage.setScrollFocus(challengesPane);
				stage.setKeyboardFocus(challengesPane);
			}
		});
		theGroup.show(stage).addAction(focus);
	}
}
