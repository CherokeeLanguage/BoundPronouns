package com.cherokeelessons.bp;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;

public class SlotDialog extends Dialog {

	public SlotDialog(final String title, final Skin skin, final BoundPronouns game, final BitmapFont font) {
		super(title, skin);

		final WindowStyle ws = new WindowStyle(getStyle());

		final Texture background = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TextureRegion region = new TextureRegion(background);
		final TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);
		tiled.setTopHeight(font.getCapHeight() + 20);

		ws.titleFont = font;
		ws.background = tiled;

		setStyle(ws);

		getTitleLabel().setAlignment(Align.center);
	}
}
