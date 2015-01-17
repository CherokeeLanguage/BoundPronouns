package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public class GoodByeScreen extends ChildScreen {

	public GoodByeScreen(BoundPronouns game, Screen caller) {
		super(game, caller);
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(null);		
	}
	
	@Override
	public void render(float delta) {
		Gdx.app.exit();
	}
}
