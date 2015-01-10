package com.cherokeelessons.bp;

import com.badlogic.gdx.Game;
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

public class BoundPronouns extends Game {
	public static final String IMG_LOADINGBAR = "images/coyote.png";
	public SpriteBatch batch;
	public BitmapFont font;
	public AssetManager manager;
	
	@Override
	public void create () {
		manager = new AssetManager();
		font = new BitmapFont();
		initManager();
		this.setScreen(new LoadingScreen(this));
	}
	
	private void initManager() {
		FileHandleResolver resolver = new InternalFileHandleResolver();
		manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
		manager.setLoader(BitmapFont.class, ".otf", new FreetypeFontLoader(resolver));
		
		
		
		TextureParameter param = new TextureParameter();
		param.magFilter=TextureFilter.Linear;
		param.minFilter=TextureFilter.Linear;
		manager.load(IMG_LOADINGBAR, Texture.class, param);
		
		addFreeSansFor(24);
		addFreeSansFor(28);
		addFreeSansFor(36);
		addFreeSansFor(54);
		addFreeSansFor(72);
		addFreeSansFor(144);
		
	}

	public void addFreeSansFor(int size) {
		String defaultChars = FreeTypeFontGenerator.DEFAULT_CHARS;
		for (char c='Ꭰ'; c<='Ᏼ'; c++) {
			String valueOf = String.valueOf(c);
			if (!defaultChars.contains(valueOf)){
				defaultChars+=valueOf;
			};
		}
		for (char c: "ạẹịọụṿẠẸỊỌỤṾ¹²³⁴ɂ".toCharArray()) {
			String valueOf = String.valueOf(c);
			if (!defaultChars.contains(valueOf)){
				defaultChars+=valueOf;
			};
		}
		FreeTypeFontLoaderParameter font = new FreeTypeFontLoaderParameter();
		font.fontFileName="otf/FreeSans.otf";		
		font.fontParameters.characters=defaultChars;
		font.fontParameters.kerning=true;
		font.fontParameters.size=size;
		font.fontParameters.magFilter=TextureFilter.Linear;
		font.fontParameters.minFilter=TextureFilter.Linear;
		manager.load("font"+size+".ttf", BitmapFont.class, font);
		return;
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		font.dispose();
	}
}
