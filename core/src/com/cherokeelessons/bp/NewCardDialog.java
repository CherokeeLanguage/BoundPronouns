package com.cherokeelessons.bp;

import java.util.Iterator;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.SlotInfo;

public abstract class NewCardDialog extends Dialog {
	
	public SlotInfo.Settings settings=new SlotInfo.Settings();
	
	private final BoundPronouns game;

	private final TextButton challenge_top;

	private String title="";
	
	public NewCardDialog(BoundPronouns game, Skin skin) {
		super("New Vocabulary Card", skin);
		this.game=game;
		
		getStyle().titleFont=sans54();
		getStyle().background=getDialogBackground();
		setStyle(getStyle());
		
		setModal(true);
		setFillParent(true);
		
		title="New Vocabulary Card";
		setTitle(title);
		
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
		
		Cell<Table> tcell = getCell(getContentTable());
		tcell.expand();
		tcell.fill();
		
		Cell<Table> bcell = getCell(getButtonTable());
		bcell.expandX();
		bcell.fillX().bottom();
		
		row();
		add(appNavBar=new Table(skin)).left().expandX().bottom();
		appNavBar.defaults().space(6);
		
		TextButtonStyle navStyle = new TextButtonStyle(skin.get(TextButtonStyle.class));
		navStyle.font=sans36();
		TextButton main = new TextButton("Main Menu", navStyle);
		appNavBar.row();		
		appNavBar.add(main).left().expandX();
		main.addListener(new ClickListener(){
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				doNav();
				return true;
			}
		});
		
		Table btable = getButtonTable();
		btable.clearChildren();
		btable.row();
		TextButtonStyle tbs_check = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		tbs_check.font=sans36();
		TextButton a = new TextButton("READY!", tbs_check);
		btable.add(a).fill().expandX().bottom();
		setObject(a, null);
	}
	
	@Override
	public void act(float delta) {
		super.act(delta);
	}
	
	protected abstract void  doNav();

	private Table appNavBar;
	
	private final Label challenge_bottom;
	
	private final TextButton answer;
	
	final private StringBuilder showCardSb = new StringBuilder();
	public void setCard(Card the_card) {
		Iterator<String> i = the_card.challenge.iterator();
		if (settings.display.equals(SlotInfo.DisplayMode.Latin)){
			//skip the Syllabary entry
			i.next();
		}
		challenge_top.setText(i.next());
		showCardSb.setLength(0);
		while (i.hasNext()) {
			showCardSb.append(i.next());
			showCardSb.append("\n");
		}
		if (settings.display.equals(SlotInfo.DisplayMode.Syllabary)){
			//dont' add the latin
			showCardSb.setLength(0);
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

	public void setCounter(int cardcount) {
		setTitle(title+" ["+cardcount+"]");
	}

}
