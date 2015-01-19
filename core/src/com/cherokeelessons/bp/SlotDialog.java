package com.cherokeelessons.bp;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public class SlotDialog extends Dialog {

	public SlotDialog(String title, Skin skin, BoundPronouns game, BitmapFont tfont) {
		super(title, skin);
		
		WindowStyle ws = new WindowStyle(getStyle());
		BitmapFont f54 = tfont;
		
		Texture background = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(background);
		TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);		
		tiled.setTopHeight(f54.getCapHeight()+20);
		
		ws.titleFont = f54;
		ws.background=tiled;
		
		setStyle(ws);
	}	
}
