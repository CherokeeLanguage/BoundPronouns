package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.util.SlotFolder;

public class BuildDeckScreen extends ChildScreen {

	private final BuildDeck buildDeck;
	private boolean dataReady = false;

	private final Runnable done = new Runnable() {
		@Override
		public void run() {
			dataReady = true;
		}
	};
	private boolean ready;

	private Skin skin;

	private Label msg;

	public BuildDeckScreen(final BoundPronouns game, final Screen caller) {
		super(game, caller);
		buildDeck = new BuildDeck(game, SlotFolder.getDeckSlot(), done);
		Gdx.app.postRunnable(buildDeck);
	}

	private TiledDrawable getBackground() {
		final Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TextureRegion region = new TextureRegion(texture);
		final TiledDrawable background = new TiledDrawable(region);
		return background;
	}

	@Override
	public void render(final float delta) {
		if (ready) {
			return;
		}
		if (dataReady) {
			ready = true;
			game.setScreen(new MainScreen(game));
			BuildDeckScreen.this.dispose();
			return;
		}
		msg.setText(buildDeck.getStatus());
		super.render(delta);
	}

	@Override
	public void show() {
		super.show();
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		final Table t = new Table(skin);
		t.setFillParent(true);
		msg = new Label("Building Deck ...", skin);
		msg.getStyle().font = game.getFont(Font.SerifXLarge);
		msg.setStyle(msg.getStyle());
		t.add(msg).fill().expand().center();
		final TiledDrawable background = getBackground();
		t.setBackground(background);
		stage.addActor(t);
	}

}
