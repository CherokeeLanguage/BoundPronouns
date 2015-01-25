package com.cherokeelessons.bp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

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
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.cherokeelessons.bp.BuildDeck.DataSet;

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
	public static final String HEAVY_BALLOT_X = "\u2717";
	public static final String HEAVY_CHECK_MARK = "\u2713";
	public static final String LEFT_ARROW = "\u21e6";
	public static final String RIGHT_ARROW = "\u27a1";

	public static final String SPECIALS;
	public static final String IMG_SCROLLBAR = "scrollpane/basic-vbar.png";
	public static final String IMG_SCROLLBUTTON = "scrollpane/basic-vbutton.png";

	public static final String IMG_LOADING = "images/coyote.png";
	public static final String IMG_PAPER1 = "images/parchment.png";
	public static final String IMG_MAYAN = "images/MayanStone.png";
	public static final String IMG_MAYAN_DIALOG = "images/MayanStoneSmall.png";

	public static final String SND_MENU = "audio/click.wav";
	public static final String SND_COYOTE = "audio/coyote.ogg";
	public static final String SND_BUZZ = "audio/buzzer2.ogg";
	public static final String SND_COW = "audio/cow1.ogg";
	public static final String SND_TICKTOCK = "audio/ticktock.wav";
	public static final String SND_DING = "audio/ding.ogg";

	public static final float BACK_WIDTH = 168f;
	static {
		SPECIALS = DSUNDERLINE + DUNDERDOT + DUNDERLINE + OVERLINE + STHRU
				+ UNDERCIRCLE + UNDERCUBE + UNDERDOT + UNDERLINE + UNDERX
				+ BACK_ARROW + DIAMOND + TRIANGLE_ASC + TRIANGLE_DESC
				+ HEAVY_BALLOT_X + HEAVY_CHECK_MARK + LEFT_ARROW + RIGHT_ARROW
				;
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

		MusicParameter mus_param = new MusicParameter();
		manager.load(SND_COYOTE, Music.class, mus_param);

		SoundParameter snd_param = new SoundParameter();
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

		addFreeSerifFor(36);
		addFreeSerifFor(54);

		addFreeSansFor(30);
		addFreeSansFor(36);
		addFreeSansFor(54);
		addFreeSansFor(72);

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
		if (click == null) {
			click = manager.get(SND_MENU);
		}
		click.play(1f);
	}

	private static final List<DataSet> pronouns = new ArrayList<BuildDeck.DataSet>();

	public static final String INFO_JSON = "info.json";

	public static List<DataSet> loadPronounRecords() {
		if (pronouns.size() != 0) {
			return new ArrayList<>(pronouns);
		}
		FileHandle csvlist = Gdx.files.internal("csv/pronouns-list.csv");
		List<CSVRecord> records;
		try (CSVParser parse = CSVParser.parse(csvlist.readString(),
				CSVFormat.RFC4180)) {
			records = parse.getRecords();
		} catch (IOException e) {
			return null;
		}

		String prevLatin = "";
		String prevChr = "";
		for (CSVRecord record : records) {
			String vtmode = record.get(0);
			if (StringUtils.isBlank(vtmode)) {
				continue;
			}
			String chr = record.get(1);
			if (chr.startsWith("#")) {
				continue;
			}
			String latin = record.get(2);
			String defin = record.get(3) + " + " + record.get(4);
			if (StringUtils.isBlank(record.get(3))) {
				String tmp = record.get(4);
				passive: {
					if (tmp.equalsIgnoreCase("he")) {
						defin = tmp + " (was being)";
						break passive;
					}
					if (tmp.equalsIgnoreCase("i")) {
						defin = tmp + " (was being)";
						break passive;
					}
					defin = tmp + " (were being)";
					break passive;
				}
			}
			if (StringUtils.isBlank(latin)) {
				latin = prevLatin;
			}
			if (StringUtils.isBlank(chr)) {
				chr = prevChr;
			}
			DataSet data = new DataSet();
			data.chr = chr;
			data.latin = latin;
			data.def = defin;
			pronouns.add(data);
			prevLatin = latin;
			prevChr = chr;
		}
		return new ArrayList<>(pronouns);
	}
}
