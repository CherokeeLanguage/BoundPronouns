package com.cherokeelessons.bp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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

public class BoundPronouns extends Game {

	public BoundPronouns() {
		System.out.println("BoundPronouns#instance");
	}

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

	public interface PlatformTextInput {
		void getTextInput(final TextInputListener listener, final String title, final String text,
				final String hint);
	}

	public static PlatformTextInput pInput = null;

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
		FitViewport fitViewport = new FitViewport(surrounds.width, surrounds.height, camera);
		fitViewport.update((int) surrounds.width, (int) surrounds.height, true);
		Gdx.app.log("com.cherokeelessons.bp.BoundPronouns",
				"Camera Size: " + (int) surrounds.getWidth() + "x" + (int) surrounds.getHeight());
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
		SPECIALS = DSUNDERLINE + DUNDERDOT + DUNDERLINE + OVERLINE + STHRU + UNDERCIRCLE + UNDERCUBE + UNDERDOT
				+ UNDERLINE + UNDERX + BACK_ARROW + DIAMOND + TRIANGLE_ASC + TRIANGLE_DESC + HEAVY_BALLOT_X
				+ HEAVY_CHECK_MARK + LEFT_ARROW + RIGHT_ARROW + DOT + LDQUOTE + RDQUOTE;
		ClearColor = new Color((float) 0xb3 / (float) 0xff, (float) 0xb3 / (float) 0xff, (float) 0xb1 / (float) 0xff,
				1);
	}

	public SpriteBatch batch;
	public AssetManager manager;

	private static Preferences prefs;

	public static Preferences getPrefs() {
		if (prefs==null) {
			prefs = Gdx.app.getPreferences(BoundPronouns.class.getName());
		}
		return prefs;
	}

	@Override
	public void create() {
		Gdx.app.log("BoundPronouns", "create");
		manager = new AssetManager();
		initManager();
		setScreen(new LoadingScreen(this));
		Gdx.input.setCatchBackKey(true);
	}

	@Override
	public void setScreen(Screen screen) {
		if (screen != null) {
			Gdx.app.log("BoundPronouns#setScreen", screen.getClass().getSimpleName());
		} else {
			Gdx.app.log("BoundPronouns#setScreen", "(null)");
		}
		super.setScreen(screen);
	}

	private void initManager() {

		Gdx.app.log("BoundPronouns#initManager", "start");

		FileHandleResolver resolver = new InternalFileHandleResolver();
		manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
		manager.setLoader(BitmapFont.class, ".otf", new FreetypeFontLoader(resolver));

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

		for (int ix = 0; ix < 11; ix++) {
			manager.load(levelImg(ix), Texture.class, param);
		}

		addFonts();
	}

	public static String levelImg(int level) {
		return "images/" + level + "-75.png";
	}

	private void addFonts() {
		for (Font font: Font.values()) {
			addFreeSerifFor(font.getSize(), font);
		}
	}

	public enum Font {
		SerifXSmall(38), SerifSmall(46), SerifMedium(52), SerifLarge(60), SerifXLarge(78), SerifLLarge(68);
		private final int size;
		public int getSize() {
			return size;
		}
		private Font(int size) {
			this.size=size;
		}
	}

	public BitmapFont getFont(Font font) {
		return manager.get(font.name() + ".ttf", BitmapFont.class);
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
		font.fontParameters.borderGamma=1.0f;
		font.fontParameters.borderStraight=false;
		font.fontParameters.characters = defaultChars;
		font.fontParameters.color=Color.WHITE;
		font.fontParameters.gamma=1.1f;
		font.fontParameters.kerning = true;
		font.fontParameters.magFilter = TextureFilter.Linear;
		font.fontParameters.minFilter = TextureFilter.Linear;
		font.fontParameters.size = size;
		font.fontParameters.spaceX=1;
		font.fontParameters.spaceY=1;
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
		Gdx.app.log(parent.getClass().getName(), exception.getMessage(), exception);
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

	private static final List<DataSet> pronouns = new ArrayList<>();

	public static final String INFO_JSON = "info.json";

	public static final String CredentialsFolder = ".config/CherokeeBoundPronouns/GooglePlayGameServices/";

	public static List<DataSet> loadPronounRecords() {
		Gdx.app.log("BoundPronouns", "loadPronounRecords");
		if (pronouns.size() != 0) {
			return new ArrayList<>(pronouns);
		}
		FileHandle csvlist = Gdx.files.internal("csv/pronouns-list-tab.csv");
		List<String[]> records = new ArrayList<>();
		try (BufferedReader reader = csvlist.reader(1024, "UTF-8")) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				records.add(line.split("\t"));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		Iterator<String[]> ipro = records.iterator();
		while (ipro.hasNext()) {
			String[] pronoun = ipro.next();
			String vtmode = StringUtils.strip(pronoun[0]);
			String syllabary = StringUtils.strip(pronoun[1]);
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
		for (String[] record : records) {
			String vtmode = record[0];
			if (StringUtils.isBlank(vtmode)) {
				continue;
			}
			String chr = record[1];
			if (chr.startsWith("#")) {
				continue;
			}
			String latin = record[2];
			String defin = record[3] + " + " + record[4];
			if (StringUtils.isBlank(record[3])) {
				String tmp = record[4];
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
		Gdx.app.log("BoundPronouns", "loadPronounRecords: " + pronouns.size());
		return new ArrayList<>(pronouns);
	}

	public static void glClearColor() {
		Gdx.gl.glClearColor(ClearColor.r, ClearColor.g, ClearColor.b, ClearColor.a);
	}
}
