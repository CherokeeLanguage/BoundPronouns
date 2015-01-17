package com.cherokeelessons.bp;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public class ShowSettings extends ChildScreen {
	
	private final Skin skin;
	private final Table container;
	private final Table table;
	
	public ShowSettings(BoundPronouns game, MainScreen mainScreen) {
		super(game, mainScreen);

		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		table = new Table();
		container = new Table(skin);
		stage.addActor(container);
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		container.setBackground(d);
		container.setFillParent(true);
		
		BitmapFont f36 = game.manager.get("font36.ttf", BitmapFont.class);
		
		TextButtonStyle bstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bstyle.font=f36;

		container.row();
		TextButtonStyle bls=new TextButtonStyle(bstyle);
		bls.fontColor=Color.BLUE;
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, bls);
		container.add(back).center().fill().width(BoundPronouns.BACK_WIDTH);
		back.addListener(exit);		
	}

	@Override
	public void dispose() {
		super.dispose();
	}

}
