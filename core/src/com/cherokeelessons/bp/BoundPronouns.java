package com.cherokeelessons.bp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.Preferences;
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
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.cherokeelessons.bp.BuildDeck.DataSet;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.util.GooglePlayGameServices;

public class BoundPronouns extends Game {
	
	@Override
	public void pause() {
		super.pause();
		Gdx.app.log("BoundPronouns", "Pause");
	}
	
	@Override
	public void resume() {
		super.resume();
		Gdx.app.log("BoundPronouns", "Resume");
	}
	
	public static interface PlatformTextInput {
		public void getTextInput(final TextInputListener listener,
				final String title, final String text, final String hint);
	}

	public static PlatformTextInput pInput = null;

	public static GooglePlayGameServices services = null;

	private final static Rectangle minSize = new Rectangle(0, 0, 1280, 720);

	private static Rectangle fittedSize() {
		int h = Gdx.graphics.getHeight();
		int w = Gdx.graphics.getWidth();
		Rectangle surrounds = new Rectangle(0, 0, w, h);
		surrounds.fitOutside(minSize);
		return surrounds;
	}

	public static FitViewport getFitViewport(Camera camera) {
		Rectangle surrounds = fittedSize();
		FitViewport fitViewport = new FitViewport(surrounds.width,
				surrounds.height, camera);
		fitViewport.update((int) surrounds.width, (int) surrounds.height, true);
		Gdx.app.log("com.cherokeelessons.bp.BoundPronouns",
				"Camera Size: " + (int) surrounds.getWidth() + "x"
						+ (int) surrounds.getHeight());
		return fitViewport;
	}

	public final static Color ClearColor;

	public final Deck deck = new Deck();

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
	public static final String DOT = "•";
	public static final String RDQUOTE = "”";
	public static final String LDQUOTE = "“";

	public static final String SPECIALS;
	public static final String IMG_SCROLLBAR = "scrollpane/basic-vbar.png";
	public static final String IMG_SCROLLBUTTON = "scrollpane/basic-vbutton.png";

	public static final String IMG_PIXEL = "images/a-white-pixel.png";
	public static final String IMG_LOADING = "images/coyote.png";
	public static final String IMG_PAPER1 = "images/parchment.png";
	public static final String IMG_MAYAN = "images/MayanStone.png";
	public static final String IMG_MAYAN_DIALOG = "images/MayanStoneSmall.png";
	
	public static final String IMG_SETTINGS = "images/gear.png";
	public static final String IMG_ERASE = "images/trash.png";
	public static final String IMG_SYNC = "images/refresh.png";	

	public static final String SND_MENU = "audio/click.mp3";
	public static final String SND_COYOTE = "audio/coyote.mp3";
	public static final String SND_BUZZ = "audio/buzzer2.mp3";
	public static final String SND_COW = "audio/cow1.mp3";
	public static final String SND_TICKTOCK = "audio/ticktock.mp3";
	public static final String SND_DING = "audio/ding.mp3";

	public static final float BACK_WIDTH = 168f;
	static {
		SPECIALS = DSUNDERLINE + DUNDERDOT + DUNDERLINE + OVERLINE + STHRU
				+ UNDERCIRCLE + UNDERCUBE + UNDERDOT + UNDERLINE + UNDERX
				+ BACK_ARROW + DIAMOND + TRIANGLE_ASC + TRIANGLE_DESC
				+ HEAVY_BALLOT_X + HEAVY_CHECK_MARK + LEFT_ARROW + RIGHT_ARROW
				+ DOT + LDQUOTE + RDQUOTE;
		ClearColor = new Color((float) 0xb3 / (float) 0xff, (float) 0xb3
				/ (float) 0xff, (float) 0xb1 / (float) 0xff, 1);
	}

	public SpriteBatch batch;
	public AssetManager manager;

	private static Preferences prefs;

	public static Preferences getPrefs() {
		return prefs;
	}

	@Override
	public void create() {
		manager = new AssetManager();
		initManager();
		this.setScreen(new LoadingScreen(this));
		Gdx.input.setCatchBackKey(true);		
		prefs = Gdx.app.getPreferences(this.getClass().getName());
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
		manager.load(IMG_PIXEL, Texture.class, param);
		manager.load(IMG_PAPER1, Texture.class, param);
		manager.load(IMG_MAYAN, Texture.class, param);
		manager.load(IMG_MAYAN_DIALOG, Texture.class, param);
		manager.load(IMG_SCROLLBAR, Texture.class, param);
		manager.load(IMG_SCROLLBUTTON, Texture.class, param);
		manager.load(IMG_SETTINGS, Texture.class, param);
		manager.load(IMG_ERASE, Texture.class, param);
		manager.load(IMG_SYNC, Texture.class, param);
		manager.load(SKIN, Skin.class);
		
		for (int ix=0; ix<11; ix++) {
			manager.load(levelImg(ix), Texture.class, param);
		}

		addFonts();

	}
	
	public static String levelImg(int level) {
		return "images/"+level+"-75.png";
	}

	private void addFonts() {
		addFreeSerifFor(36, Font.SerifSmall);
		addFreeSerifFor(42, Font.SerifMedium);
		addFreeSerifFor(58, Font.SerifLarge);
		addFreeSerifFor(62, Font.SerifLLarge);
		addFreeSerifFor(72, Font.SerifXLarge);
	}

	public static enum Font {
		xSerifXSmall, SerifSmall, SerifMedium, SerifLarge, SerifXLarge, xSerifMediumLarge, SerifLLarge;
	}

	public BitmapFont getFont(Font font) {
		return manager.get(font.name() + ".ttf", BitmapFont.class);
	}

	@SuppressWarnings("unused")
	private void addFreeSansFor(int size, Font fontname) {
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
		manager.load(fontname.name() + ".ttf", BitmapFont.class, font);
		return;
	}

	private void addFreeSerifFor(int size, Font fontname) {
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
		manager.load(fontname.name() + ".ttf", BitmapFont.class, font);
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

	public static final String GooglePlayLogginIn = "GooglePlayEnabled";

	public static final String CredentialsFolder = ".config/CherokeeBoundPronouns/GooglePlayGameServices/";

	public static List<DataSet> loadPronounRecords() {
		if (pronouns.size() != 0) {
			return new ArrayList<DataSet>(pronouns);
		}
		FileHandle csvlist = Gdx.files.internal("csv/pronouns-list.csv");
		List<CSVRecord> records;
		CSVParser parse=null;
		try {
			parse = CSVParser.parse(csvlist.readString("UTF-8"), 
					CSVFormat.RFC4180);
			records = parse.getRecords();
		} catch (IOException e) {
			return null;
		} finally {
			try {
				parse.close();
			} catch (IOException e) {
			}
		}

		Iterator<CSVRecord> ipro = records.iterator();
		while (ipro.hasNext()) {
			CSVRecord pronoun = ipro.next();
			String vtmode = StringUtils.strip(pronoun.get(0));
			String syllabary = StringUtils.strip(pronoun.get(1));
			if (StringUtils.isBlank(vtmode)) {
				ipro.remove();
				continue;
			}
			if (vtmode.startsWith("#")) {
				ipro.remove();
				continue;
			}
			if (syllabary.startsWith("#")) {
				ipro.remove();
				continue;
			}
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
						defin = tmp + " (being)";
						break passive;
					}
					if (tmp.equalsIgnoreCase("i")) {
						defin = tmp + " (being)";
						break passive;
					}
					defin = tmp + " (being)";
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
		return new ArrayList<DataSet>(pronouns);
	}

	public static void glClearColor() {
		Gdx.gl.glClearColor(ClearColor.r, ClearColor.g, ClearColor.b,
				ClearColor.a);
	}
}
