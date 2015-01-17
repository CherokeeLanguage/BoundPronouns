package com.cherokeelessons.bp;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

public class ChooseProfileScreen extends ChildScreen {

	public ChooseProfileScreen(BoundPronouns game, Screen mainMenuScreen) {
		super(game, mainMenuScreen);

		Table container = new Table();
		container.setFillParent(true);		
		stage.addActor(container);
		
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		container.setBackground(d);

		Label label;

		LabelStyle lstyle24 = new LabelStyle(null, Color.BLUE);
		lstyle24.font = game.manager.get("font24.ttf", BitmapFont.class);

		LabelStyle lstyle54 = new LabelStyle(null, Color.BLUE);
		lstyle54.font = game.manager.get("font54.ttf", BitmapFont.class);
		LabelStyle lstyle72 = new LabelStyle(null, Color.BLUE);
		lstyle72.font = game.manager.get("font72.ttf", BitmapFont.class);

		TextButtonStyle tbstyle = new TextButtonStyle();
		tbstyle.fontColor = Color.BLUE;
		tbstyle.font = game.manager.get("font54.ttf", BitmapFont.class);

		TextButton button = new TextButton("Existing Profile", tbstyle);
//		button.addListener(viewPractice);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(new Label(" ", lstyle24));
		container.row();
		container.add(button);

		button = new TextButton("New Profile", tbstyle);
//		button.addListener(viewPronounsList);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(new Label(" ", lstyle24));
		container.row();
		container.add(button);

		button = new TextButton("Back", tbstyle);
		button.addListener(exit);
		button.setTouchable(Touchable.enabled);
		container.row();
		container.add(new Label(" ", lstyle24));
		container.row();
		container.add(button);
		
		container.pack();
	}
}
