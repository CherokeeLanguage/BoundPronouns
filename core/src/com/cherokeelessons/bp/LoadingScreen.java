package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.util.JsonConverter;
import com.cherokeelessons.util.SlotFolder;

public class LoadingScreen implements Screen {

	private static final boolean HOWL = true;
	private final BoundPronouns game;
	private final Stage stage;
	private Image loadingBar = null;
	private final Table table;

	private Music howl;

	public LoadingScreen(final BoundPronouns boundPronouns) {
		Gdx.app.log("Screen: ", this.getClass().getSimpleName());
		this.game = boundPronouns;
		stage = new Stage();
		stage.setViewport(BoundPronouns.getFitViewport(stage.getCamera()));
		table = new Table();
		stage.addActor(table);
		table.setFillParent(true);
	}

	@Override
	public void dispose() {
		Gdx.app.postRunnable(new Runnable() {
			Runnable _self = this;
			int count = 10;

			@Override
			public void run() {
				if (howl != null) {
					howl.stop();
					howl = null;
				}
				if (count-- > 0) {
					Gdx.app.postRunnable(_self);
					return;
				}
				stage.dispose();
				game.manager.unload(BoundPronouns.SND_COYOTE);
				game.manager.unload(BoundPronouns.IMG_LOADING);
			}
		});
	}

	@Override
	public void hide() {
		// Do nothing
	}

	@Override
	public void pause() {
		// Do nothing
	}
	
	private final JsonConverter json = new JsonConverter();

	@Override
	public void render(final float delta) {
		stage.act();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
		if (game.manager.update()) {
			nextscreen: {
				if (howl != null && !Gdx.input.isTouched() && howl.isPlaying()) {
					break nextscreen;
				}
				Deck tmpDeck = json.fromJson(Deck.class, Gdx.files.internal("deck.json"));
				game.deck.cards.clear();
				game.deck.cards.addAll(tmpDeck.cards);
				game.deck.size = tmpDeck.size;
				game.deck.version = tmpDeck.version;
				game.setScreen(new MainScreen(game));
				dispose();
				return;
			}
		}
		if (HOWL) {
			if (howl == null && game.manager.isLoaded(BoundPronouns.SND_COYOTE)) {
				howl = game.manager.get(BoundPronouns.SND_COYOTE, Music.class);
				howl.setLooping(false);
				howl.setVolume(1f);
				howl.play();
			}
		}
		if (!game.manager.isLoaded(BoundPronouns.IMG_LOADING)) {
			return;
		}
		if (loadingBar == null) {
			Gdx.app.log(this.getClass().getName(), "Loading Bar");
			final Texture texture = game.manager.get(BoundPronouns.IMG_LOADING, Texture.class);
			if (texture == null) {
				Gdx.app.log(this.getClass().getName(), "Failed loading 'Loading Bar' image!");
			}
			loadingBar = new Image(texture);
			table.add(loadingBar).align(Align.center).expand();
			loadingBar.setColor(Color.RED);
		}
	}

	@Override
	public void resize(final int width, final int height) {
		stage.setViewport(BoundPronouns.getFitViewport(stage.getCamera()));
		stage.getViewport().update(width, height);
	}

	@Override
	public void resume() {
		// Do nothing
	}

	@Override
	public void show() {
		switch (Gdx.app.getType()) {
		case Android:
		case iOS:
			SlotFolder.migrate();
			break;
		case Applet:
		case Desktop:
		case HeadlessDesktop:
		case WebGL:
		default:
		}
	}
}
