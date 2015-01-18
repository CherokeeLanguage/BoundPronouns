package com.cherokeelessons.bp;

import java.util.Arrays;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.MusicLoader.MusicParameter;
import com.badlogic.gdx.assets.loaders.SoundLoader.SoundParameter;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class BoundPronouns extends Game {

	public static final String SKIN = "skins/holo/Holo-light-xhdpi.json";
	public static final String DIAMOND = "\u25c8";
	public static final String TRIANGLE_ASC = "\u25bc";
	public static final String TRIANGLE_DESC = "\u25b2";
	public static final String BACK_ARROW = "\u21a9";
	public static final String OVERLINE = "\u0305";
	public static final String UNDERDOT = "\u0323";
	public static final String DUNDERDOT = "\u0324";
	public static final String UNDERCIRCLE = "\u0325";
	public static final String UNDERLINE = "\u0332";
	public static final String DUNDERLINE = "\u0333";
	public static final String STHRU = "\u0336";
	public static final String UNDERX = "\u0353";
	public static final String UNDERCUBE = "\u033B";
	public static final String DSUNDERLINE = "\u0347";

	public static final String SPECIALS;
	public static final String IMG_SCROLLBAR = "scrollpane/basic-vbar.png";
	public static final String IMG_SCROLLBUTTON = "scrollpane/basic-vbutton.png";
	
	public static final String IMG_LOADING = "images/coyote.png";
	public static final String IMG_PAPER1 = "images/parchment.png";
	public static final String IMG_MAYAN = "images/MayanStone.png";
	public static final String IMG_MAYAN_DIALOG = "images/MayanStoneSmall.png";
	public static final String SND_MENU = "audio/click.wav";
	public static final String SND_HOWL = "audio/wolfhowls.ogg";
	public static final float BACK_WIDTH = 168f;
	static {
		SPECIALS = DSUNDERLINE + DUNDERDOT + DUNDERLINE + OVERLINE + STHRU
				+ UNDERCIRCLE + UNDERCUBE + UNDERDOT + UNDERLINE + UNDERX
				+ BACK_ARROW + DIAMOND + TRIANGLE_ASC + TRIANGLE_DESC;
	}

	public SpriteBatch batch;
	public AssetManager manager;

	@Override
	public void create() {
		manager = new AssetManager();
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

		MusicParameter mus_param=new MusicParameter();
		manager.load(SND_HOWL, Music.class, mus_param);
		
		SoundParameter snd_param=new SoundParameter();
		manager.load(SND_MENU, Sound.class, snd_param);		
		
		TextureParameter param = new TextureParameter();
		param.magFilter = TextureFilter.Linear;
		param.minFilter = TextureFilter.Linear;

		manager.load(IMG_LOADING, Texture.class, param);
		manager.load(IMG_PAPER1, Texture.class, param);
		manager.load(IMG_MAYAN, Texture.class, param);
		manager.load(IMG_MAYAN_DIALOG, Texture.class, param);
		manager.load(IMG_SCROLLBAR, Texture.class, param);
		manager.load(IMG_SCROLLBUTTON, Texture.class, param);
		manager.load(SKIN, Skin.class);
		
		addFreeSansFor(36);
		addFreeSansFor(54);
	}

	private void addFreeSansFor(int size) {
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
		for (char c : SPECIALS.toCharArray()) {
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
		manager.load("sans" + size + ".ttf", BitmapFont.class, font);
		return;
	}
	
	@SuppressWarnings("unused")
	private void addFreeSerifFor(int size) {
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
		for (char c : SPECIALS.toCharArray()) {
			String valueOf = String.valueOf(c);
			if (!defaultChars.contains(valueOf)) {
				defaultChars += valueOf;
			}
		}
		FreeTypeFontLoaderParameter font = new FreeTypeFontLoaderParameter();
		font.fontFileName = "otf/FreeSerif.otf";
		font.fontParameters.characters = defaultChars;
		font.fontParameters.kerning = true;
		font.fontParameters.size = size;
		font.fontParameters.magFilter = TextureFilter.Linear;
		font.fontParameters.minFilter = TextureFilter.Linear;
		manager.load("serif" + size + ".ttf", BitmapFont.class, font);
		return;
	}

	@Override
	public void render() {
		super.render();
	}

	@Override
	public void dispose() {
		super.dispose();
		manager.dispose();
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

	private Sound click = null;
	public void click() {
		if (click==null) {
			click=manager.get(SND_MENU);
		}
		click.play(1f);
	}
}
