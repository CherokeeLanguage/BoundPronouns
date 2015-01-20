package com.cherokeelessons.bp;

import java.util.Iterator;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.cards.Card;

public class NewCardDialog extends Dialog {
	
	private final BoundPronouns game;

	private final TextButton challenge_top;

	private String title="";

	public NewCardDialog(BoundPronouns game, String title, Skin skin) {
		super(title, skin);
		this.game=game;
		this.title=title;
		
		getStyle().titleFont=sans54();
		getStyle().background=getDialogBackground();
		setStyle(getStyle());
		
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font=sans54();
		back.setStyle(back.getStyle());
		button(back);
		setModal(true);
		setFillParent(true);
		
		setTitle("New Vocabulary Card");
		
		challenge_top=new TextButton("", skin);
		challenge_top.setDisabled(true);
		challenge_bottom=new Label("", skin);
		answer=new TextButton("", skin);
		answer.setDisabled(true);
		
		TextButtonStyle chr_san_large = new TextButtonStyle(challenge_top.getStyle());		
		chr_san_large.font=sans_large();
		challenge_top.setStyle(chr_san_large);
		
		LabelStyle pronounce_large = new LabelStyle(challenge_bottom.getStyle());
		pronounce_large.font=serif54();
		challenge_bottom.setStyle(pronounce_large);		
		
		TextButtonStyle answerStyle = new TextButtonStyle(answer.getStyle());		
		answerStyle.font=serif36();
		answer.setStyle(answerStyle);
		
		challenge_top.add(challenge_bottom).pad(0).top();
		
		Table ctable = getContentTable();
		ctable.row();
		ctable.add(challenge_top).fill().expand();
		ctable.row();
		ctable.add(answer).fill().expand();
	}

	private final Label challenge_bottom;
	
	private final TextButton answer;
	
	final private StringBuilder showCardSb = new StringBuilder();
	public void setCard(Card the_card) {
		Iterator<String> i = the_card.challenge.iterator();
		challenge_top.setText(i.next());
		showCardSb.setLength(0);
		while (i.hasNext()) {
			showCardSb.append(i.next());
			showCardSb.append("\n");
		}
		challenge_bottom.setText(showCardSb.toString());
		showCardSb.setLength(0);
		for (String a: the_card.answer) {
			showCardSb.append(a);
			showCardSb.append("\n");
		}
		answer.setText(showCardSb.toString());		
	}

	private TiledDrawable getDialogBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		background.setMinHeight(0);
		background.setMinWidth(0);
		background.setTopHeight(sans54().getCapHeight()+20);
		return background;
	}
	
	private BitmapFont sans_large() {
		return game.manager.get("sans72.ttf", BitmapFont.class);
	}
	
	private BitmapFont sans54() {
		return game.manager.get("sans54.ttf", BitmapFont.class);
	}
	
	private BitmapFont serif54() {
		return game.manager.get("serif54.ttf", BitmapFont.class);
	}
	
	private BitmapFont f36() {
		return game.manager.get("sans36.ttf", BitmapFont.class);
	}
	
	private BitmapFont serif36() {
		return game.manager.get("serif36.ttf", BitmapFont.class);
	}

	private int counter=0;
	public void setCounter(int cardcount) {
		counter=cardcount;
		setTitle(title+" ["+cardcount+"]");
	}
}
