package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class ShowAbout implements Screen {

	private final BoundPronouns game;
	private final Screen caller;
	private final FitViewport viewport;
	private final Stage stage;

	private ClickListener die = new ClickListener() {
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.click();
			game.setScreen(caller);
			dispose();
			return true;
		};
	};
	
	private final Skin skin;
	private final Table table;
	private final ScrollPane scroll;
	
	public ShowAbout(BoundPronouns game, MainMenuScreen mainMenuScreen) {
		this.game=game;
		this.caller=mainMenuScreen;
		stage = new Stage();
		viewport = new FitViewport(1280, 720, stage.getCamera());
		viewport.update(1280, 720, true);
		stage.setViewport(viewport);
		
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
		back.addListener(die);
		
		table = new Table(skin);
		table.setBackground(d);
		
		scroll = new ScrollPane(table, skin);
		scroll.setColor(Color.DARK_GRAY);
		scroll.setFadeScrollBars(false);
		scroll.setSmoothScrolling(true);
		
		String text = Gdx.files.internal("text/about.txt").readString("UTF-8");
		
		Label label = new Label(text, ls);
		label.setWrap(true);
		
		table.row();
		table.add(label).expand().fill().left().padLeft(20).padRight(20);
		
		container.row();
		container.add(scroll).expand().fill();
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void render(float delta) {
		stage.act();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
		Gdx.input.setInputProcessor(null);
	}

	@Override
	public void dispose() {
		skin.dispose();
		stage.dispose();
	}

}
