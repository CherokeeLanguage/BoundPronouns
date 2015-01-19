package com.cherokeelessons.bp;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public class CardSession extends ChildScreen implements Screen {

	public CardSession(BoundPronouns game, Screen caller, FileHandle slot) {
		super(game, caller);
		if (!slot.child("session.json").exists()) {
			slot.mkdirs();
			BuildDeck.getDeckSlot().child("deck.json").copyTo(slot.child("deck.json"));
		}
//		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
//				Texture.class);
//		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
//		Skin skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
//		container = new Table(skin);
//		stage.addActor(container);
//		container.setBackground(d);
//		container.setFillParent(true);
//		Label lbl = new Label("One Moment ...", game.manager.get(
//				BoundPronouns.SKIN, Skin.class));
//		LabelStyle ls = lbl.getStyle();
//		ls.font = game.manager.get("sans54.ttf", BitmapFont.class);
//		lbl.setStyle(ls);
//		container.add(lbl);
	}

}
