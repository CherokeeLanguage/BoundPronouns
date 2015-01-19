package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public class ShowAbout extends ChildScreen {

	private final Skin skin;
	private ScrollPane scroll;
	
	private Table table;
	private Table container;
	
	public ShowAbout(BoundPronouns game, MainScreen mainScreen) {
		super(game, mainScreen);
		
		
		skin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));
		
		container = new Table(skin);
		stage.addActor(container);
		
		container.setBackground(d());
		container.setFillParent(true);
		container.addAction(Actions.delay(.1f, Actions.run(initView)));
	}
	
	private TiledDrawable d() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		return new TiledDrawable(new TextureRegion(texture));
	}
	
	private BitmapFont f36() {
		return game.manager.get("sans36.ttf", BitmapFont.class);
	}

	private Runnable initView = new Runnable() {
		public void run() {
		LabelStyle ls = new LabelStyle(skin.get("default", LabelStyle.class));
		ls.font=f36();
		ls.background=null;
		
		container.row();
		TextButtonStyle bls=new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bls.font=f36();
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, bls);
		container.add(back).left().fill().width(BoundPronouns.BACK_WIDTH);
		back.addListener(exit);
		
		table = new Table(skin);
		
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
		};
	};

	@Override
	public void dispose() {
		skin.dispose();
		super.dispose();
	}

}
