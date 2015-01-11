package com.cherokeelessons.bp;

import java.util.Arrays;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;

public class BoundPronouns extends Game {
	public static final String IMG_LOADINGBAR = "images/coyote.png";
	public static final String IMG_PAPER1 = "images/parchment.png";

	private static final String OVERLINE = "\u0305";
	private static final String UNDERDOT = "\u0323";
	private static final String DUNDERDOT = "\u0324";
	private static final String UNDERCIRCLE = "\u0325";
	private static final String UNDERLINE = "\u0332";
	private static final String DUNDERLINE = "\u0333";
	private static final String STHRU = "\u0336";
	private static final String UNDERX = "\u0353";
	private static final String UNDERCUBE = "\u033B";
	private static final String DSUNDERLINE = "\u0347";

	private static final String specials;
	static {
		specials = DSUNDERLINE + DUNDERDOT + DUNDERLINE + OVERLINE + STHRU
				+ UNDERCIRCLE + UNDERCUBE + UNDERDOT + UNDERLINE + UNDERX;
	}

	public SpriteBatch batch;
	public BitmapFont font;
	public AssetManager manager;

	@Override
	public void create() {
		manager = new AssetManager();
		font = new BitmapFont();
		initManager();
		this.setScreen(new LoadingScreen(this));
	}
	
	@Override
	public void setScreen(Screen screen) {
		super.setScreen(screen);
	}
	
	private void initManager() {
		FileHandleResolver resolver = new InternalFileHandleResolver();
		manager.setLoader(FreeTypeFontGenerator.class,
				new FreeTypeFontGeneratorLoader(resolver));
		manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(
				resolver));
		manager.setLoader(BitmapFont.class, ".otf", new FreetypeFontLoader(
				resolver));

		TextureParameter param = new TextureParameter();
		param.magFilter = TextureFilter.Linear;
		param.minFilter = TextureFilter.Linear;
		
		manager.load(IMG_LOADINGBAR, Texture.class, param);
		manager.load(IMG_PAPER1, Texture.class, param);

		addFreeSansFor(24);
		addFreeSansFor(28);
		addFreeSansFor(36);
		addFreeSansFor(48);
		addFreeSansFor(54);
		addFreeSansFor(72);
		addFreeSansFor(144);

	}

	public void addFreeSansFor(int size) {
		String defaultChars = FreeTypeFontGenerator.DEFAULT_CHARS;
		for (char c = 'Ꭰ'; c <= 'Ᏼ'; c++) {
			String valueOf = String.valueOf(c);
			if (!defaultChars.contains(valueOf)) {
				defaultChars += valueOf;
			}
		}
		for (char c : "ạẹịọụṿẠẸỊỌỤṾ¹²³⁴ɂ".toCharArray()) {
			String valueOf = String.valueOf(c);
			if (!defaultChars.contains(valueOf)) {
				defaultChars += valueOf;
			}
		}
		for (char c : specials.toCharArray()) {
			String valueOf = String.valueOf(c);
			if (!defaultChars.contains(valueOf)) {
				defaultChars += valueOf;
			}
		}
		FreeTypeFontLoaderParameter font = new FreeTypeFontLoaderParameter();
		font.fontFileName = "otf/FreeSans.otf";
		font.fontParameters.characters = defaultChars;
		font.fontParameters.kerning = true;
		font.fontParameters.size = size;
		font.fontParameters.magFilter = TextureFilter.Linear;
		font.fontParameters.minFilter = TextureFilter.Linear;
		manager.load("font" + size + ".ttf", BitmapFont.class, font);
		return;
	}

	@Override
	public void render() {
		super.render();
	}

	@Override
	public void dispose() {
		super.dispose();
		font.dispose();
	}

	public void log(Object parent, String... message) {
		Gdx.app.log(parent.getClass().getName(), Arrays.toString(message));
	}

	public void err(Object parent, Exception exception) {
		Gdx.app.log(parent.getClass().getName(), exception.getMessage(),
				exception);
	}

	public void err(Object parent, String message, Exception exception) {
		Gdx.app.log(parent.getClass().getName(), message, exception);
	}
}
