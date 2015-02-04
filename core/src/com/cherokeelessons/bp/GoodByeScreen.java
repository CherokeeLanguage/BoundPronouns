package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Keys;

public class GoodByeScreen extends ChildScreen {
	
	@Override
	public boolean keyDown(int keycode) {
		switch (keycode) {
		case Keys.BACK:
		case Keys.ESCAPE:
			return true;
		default:
		}
		return super.keyDown(keycode);
	}

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
