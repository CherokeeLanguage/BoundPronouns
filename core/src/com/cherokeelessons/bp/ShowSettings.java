package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public class ShowSettings extends ChildScreen {
	
	private final Skin skin;
	private final Table table;
	private final ScrollPane scroll;
	
	public ShowSettings(BoundPronouns game, MainScreen mainScreen) {
		super(game, mainScreen);
		
		BitmapFont f36 = game.manager.get("font36.ttf", BitmapFont.class);
		skin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));
		
		Texture texture = game.manager.get(BoundPronouns.IMG_PAPER1,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		Table container = new Table(skin);
		stage.addActor(container);
		
		container.setBackground(d);
		container.setFillParent(true);
		
		LabelStyle ls = new LabelStyle(f36, Color.DARK_GRAY);
		
		LabelStyle bls=new LabelStyle(ls);
		bls.fontColor=Color.BLUE;
		Label back = new Label(BoundPronouns.BACK_ARROW, bls);
		back.setAlignment(Align.topLeft);
		ls.fontColor=Color.DARK_GRAY;
		
		container.row();
		container.add(back).left().top().padLeft(30);
		back.addListener(exit);
		
		table = new Table(skin);
		table.setBackground(d);
		
		scroll = new ScrollPane(table, skin);
		scroll.setColor(Color.DARK_GRAY);
		scroll.setFadeScrollBars(false);
		scroll.setSmoothScrolling(true);
		
		
	}

	@Override
	public void dispose() {
		skin.dispose();
		super.dispose();
	}

}
