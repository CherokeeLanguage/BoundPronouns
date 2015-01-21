package com.cherokeelessons.bp;

import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.cards.AnswerSet;
import com.cherokeelessons.cards.Card;

public class ChallengeCardDialog extends Dialog {
	
	public void setCounter(int cardcount) {
		setTitle(title+" ["+cardcount+"]");
	}
	
	private String title="";
	
	private final BoundPronouns game;

	private final TextButton challenge_top;

	private Skin skin;
	
	public ChallengeCardDialog(BoundPronouns game, Skin skin) {
		super("Challenge Card", skin);
		this.title="Challenge Card";
		this.game=game;
		this.skin=skin;
		getStyle().titleFont=sans54();
		getStyle().background=getDialogBackground();
		setStyle(getStyle());
		
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font=sans54();
		back.setStyle(back.getStyle());
		button(back);
		setModal(true);
		setFillParent(true);
		
		challenge_top=new TextButton("", skin);
		challenge_top.setDisabled(true);
		challenge_bottom=new Label("", skin);
		answer=new TextButton("", skin);
		answer.setDisabled(true);
		answer.setVisible(false);
		
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
		
		Cell<Table> tcell = getCell(getContentTable());
		tcell.expand();
		tcell.fill();
		
		Cell<Table> bcell = getCell(getButtonTable());
		bcell.expand();
		bcell.fill();
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
	
	private BitmapFont sans36() {
		return game.manager.get("sans36.ttf", BitmapFont.class);
	}
	
	private BitmapFont serif36() {
		return game.manager.get("serif36.ttf", BitmapFont.class);
	}

	public void addAnswers(List<AnswerSet> answerSetsFor) {
		TextButtonStyle tbs = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		tbs.font=serif36();
		 
		Table btable = getButtonTable();
		btable.clearChildren();
		boolean odd=true;
		for (AnswerSet answer: answerSetsFor) {
			if (odd) {
				btable.row();
			}
			final TextButton a = new TextButton(answer.answer, tbs);
			a.addListener(new ClickListener(){
				@Override
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					a.setColor(a.isChecked()?Color.WHITE:Color.GREEN);
					return true;
				}
			});
			a.setColor(Color.WHITE);
			btable.add(a).fill().expandX();
			odd=!odd;
		}
		btable.row();		
		TextButton a = new TextButton("CHECK!", skin);
		setObject(a, null);
		btable.add(a).colspan(2).fill().expandX();
		btable.row();
		LabelStyle ls = new LabelStyle(skin.get("default", LabelStyle.class));
		ls.font=sans36();
		Label label = new Label("Select the correct answer or answers then hit 'CHECK'", ls);
		btable.add(label).colspan(2);
	}

}
