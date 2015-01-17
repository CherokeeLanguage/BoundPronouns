package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public class ShowAbout extends ChildScreen {

	private final Skin skin;
	private final Table table;
	private final ScrollPane scroll;
	
	public ShowAbout(BoundPronouns game, MainScreen mainScreen) {
		super(game, mainScreen);
		
		BitmapFont f36 = game.manager.get("font36.ttf", BitmapFont.class);
		skin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));
		
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		Table container = new Table(skin);
		stage.addActor(container);
		
		container.setBackground(d);
		container.setFillParent(true);
		
		LabelStyle ls = new LabelStyle(skin.get("default", LabelStyle.class));
		ls.font=f36;
		ls.background=null;
		
		container.row();
		TextButtonStyle bls=new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bls.font=f36;
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, bls);
		container.add(back).left().fill().width(BoundPronouns.BACK_WIDTH);
		back.addListener(exit);
		
		table = new Table(skin);
		table.setBackground(d);
		
		scroll = new ScrollPane(table, skin);
		scroll.setColor(Color.DARK_GRAY);
		scroll.setFadeScrollBars(false);
		scroll.setSmoothScrolling(true);
		
		String text = Gdx.files.internal("text/about.txt").readString("UTF-8");
		text+="\n\n";
		text+="libGDX "+com.badlogic.gdx.Version.VERSION;
		text+="\n \n";
		Label label = new Label(text, ls);
		label.setWrap(true);
		
		table.row();
		table.add(label).expand().fill().left().padLeft(20).padRight(20);
		
		container.row();
		container.add(scroll).expand().fill();
		stage.setKeyboardFocus(scroll);
		stage.setScrollFocus(scroll);
	}

	@Override
	public void dispose() {
		skin.dispose();
		super.dispose();
	}

}
