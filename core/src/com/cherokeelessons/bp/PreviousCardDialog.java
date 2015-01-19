package com.cherokeelessons.bp;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.Deck;

public class PreviousCardDialog extends Dialog {

	private final BoundPronouns game;
	
	public PreviousCardDialog(BoundPronouns game, String title, Skin skin) {
		super(title, skin);
		this.game=game;
	}

	public void showCard(Deck deck, ActiveDeck current_active,
			ActiveDeck current_seen, ActiveCard card) {
		// TODO Auto-generated method stub
		
	}

}
