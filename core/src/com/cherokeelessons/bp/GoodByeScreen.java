package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;

public class GoodByeScreen extends ChildScreen {

	public GoodByeScreen(final BoundPronouns game, final Screen caller) {
		super(game, caller);
	}

	@Override
	public boolean keyDown(final int keycode) {
		switch (keycode) {
		case Keys.BACK:
		case Keys.ESCAPE:
			return true;
		default:
		}
		return super.keyDown(keycode);
	}

	@Override
	public void render(final float delta) {
		Gdx.app.exit();
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(null);
	}
}
