package com.cherokeelessons.bp;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;

public class NewCardDialog extends Dialog {
	
	private final BoundPronouns game;

	public NewCardDialog(BoundPronouns game, String title, Skin skin) {
		super(title, skin);
		this.game=game;
		
		getStyle().titleFont=f54();
		getStyle().background=getDialogBackground();
		setStyle(getStyle());
		
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, skin);
		back.getStyle().font=f54();
		back.setStyle(back.getStyle());
		button(back);
		setModal(true);
		setFillParent(true);
		
		setTitle("Please Study this new Card");
		
		challenge=new Label("", skin);		
		answer=new Label("", skin);
		
		LabelStyle s54 = new LabelStyle(challenge.getStyle());		
		s54.font=s54();
		
		LabelStyle s36 = new LabelStyle(answer.getStyle());		
		s36.font=s36();
		
		challenge.setStyle(s54);		
		answer.setStyle(s36);
	}

	private final Label challenge;
	
	private final Label answer;
	
	public void showCard(Deck deck, ActiveDeck current_active,
			ActiveDeck current_seen, ActiveCard card) {
		Card the;
		for (Card c: deck.cards) {
			if (c.vgroup.equals(card.vgroup) && c.pgroup.equals(card.pgroup)){
				the=c;
				break;
			}
		}
		card.show_again_ms=Deck.intervals.get(card.box);
		
	}

	private TiledDrawable getDialogBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		background.setMinHeight(0);
		background.setMinWidth(0);
		background.setTopHeight(f54().getCapHeight()+20);
		return background;
	}
	
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
}
