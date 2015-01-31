package com.cherokeelessons.bp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.bp.BuildDeck.DataSet;
import com.cherokeelessons.cards.Card;


public class ShowChallenges extends ChildScreen implements Screen {

	private final List<String> pgroups;
	
	private boolean viewReady;
	public ShowChallenges(BoundPronouns game, Screen caller) {
		super(game, caller);
		pgroups=new ArrayList<String>();
	}	
	
	private Skin skin;	
	@Override
	public void show() {
		super.show();
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		Table t = new Table(skin);
		t.setFillParent(true);
		msg = new Label("Loading Deck ...", skin);
		msg.getStyle().font=game.getFont(Font.SerifLarge);
		msg.setStyle(msg.getStyle());
		t.add(msg).fill().expand().center();
		TiledDrawable background = getBackground();
		t.setBackground(background);
		stage.addActor(t);
	}
	
	private Label msg;

	private TiledDrawable getBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		return background;
	}
	
	@Override
	public void render(float delta) {
		super.render(delta);		
		if (!viewReady) {
			viewReady=true;
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					readyView();
				}
			});
			return;
		}
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
		background.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight()+20);
		chooseGroup.getStyle().titleFont=game.getFont(Font.SerifLarge);
		chooseGroup.getStyle().background=background;
		chooseGroup.setStyle(chooseGroup.getStyle());
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font=game.getFont(Font.SerifLarge);
		back.setStyle(back.getStyle());
		chooseGroup.button(back);
		chooseGroup.setModal(true);
		chooseGroup.setFillParent(true);
		Table groupsTable = new Table(skin);
		final ScrollPane groupsPane = new ScrollPane(groupsTable, skin);
		groupsPane.setFadeScrollBars(false);
		groupsPane.setColor(Color.DARK_GRAY);
		chooseGroup.getContentTable().add(groupsPane).expand().fill();

		for (Card card: game.deck.cards) {
			if (StringUtils.isBlank(card.vgroup)){
				continue;
			}
			if (pgroups.contains(card.pgroup)){
				continue;
			}
			pgroups.add(card.pgroup);
		}
		
		Map<String, String> lookup_details = new HashMap<String, String>();
		List<DataSet> plist = BoundPronouns.loadPronounRecords();
		for (DataSet data: plist) {
			String prev = lookup_details.get(data.chr);
			lookup_details.put(data.chr, (prev!=null?prev+"\n":"") + data.def);			
		}
		plist.clear();
		plist=null;
		
		Collections.sort(pgroups);
		boolean nextRow=true;
		for(final String group: pgroups) {
			String group_name=group+"\n"+lookup_details.get(group);
			TextButton button = new TextButton(group_name, skin);
			button.getStyle().font=game.getFont(Font.SerifMedium);
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
		background.setTopHeight(game.getFont(Font.SerifXLarge).getCapHeight()+20);
		theGroup.getStyle().background=background;
		theGroup.getStyle().titleFont=game.getFont(Font.SerifXLarge);
		theGroup.setStyle(theGroup.getStyle());
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font=game.getFont(Font.SerifLarge);
		back.setStyle(back.getStyle());
		theGroup.button(back);
		theGroup.setModal(true);
		theGroup.setFillParent(true);
		Table challenges = new Table(skin);
		final ScrollPane challengesPane = new ScrollPane(challenges, skin);
		challengesPane.setFadeScrollBars(false);
		challengesPane.setColor(Color.DARK_GRAY);
		theGroup.getContentTable().add(challengesPane).expand().fill();
		
		List<Card> cards = getCardsFor(group);
		if (cards.size()==0) {
			game.log(this, "FATAL EMPTY PGROUP?");
			return;
		}
		TextButtonStyle tbstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		tbstyle.font=game.getFont(Font.SerifMedium);
		StringBuilder sb = new StringBuilder();
		for (Card card: cards) {
			sb.setLength(0);
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
			button.setTouchable(Touchable.disabled);
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

	private List<Card> getCardsFor(String group) {
		List<Card> list = new ArrayList<>();
		for (Card card: game.deck.cards) {
			if (!card.pgroup.equals(group)){
				continue;
			}
			list.add(card);
		}
		Collections.sort(list);
		return list;
	}
}
