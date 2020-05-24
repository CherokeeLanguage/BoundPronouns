package com.cherokeelessons.bp;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.Answer;
import com.cherokeelessons.cards.Answer.AnswerList;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.SlotInfo;

public abstract class ChallengeCardDialog extends Dialog {

	public static final String TAG = "ChallengeCardDialog";

	protected ActiveCard _activeCard;

	protected Card _deckCard;

	private final TextButtonStyle answer_style;

	private Table appNavBar;

	protected final TextButton check;

	private Runnable disableCard = new Runnable() {
		@Override
		public void run() {
			check.setDisabled(true);
			check.setVisible(true);
			check.setTouchable(Touchable.disabled);
			navEnable(false);
		}
	};

	private Runnable enableCard = new Runnable() {
		@Override
		public void run() {
			check.setDisabled(false);
			check.setVisible(true);
			check.setTouchable(Touchable.enabled);
			navEnable(true);
		}
	};

	private final BoundPronouns game;

	private final TextButton main;

	private TextButton mute;

	private TextButton speak;

	private final TextButton pause;

	public boolean paused = false;

	public SlotInfo.Settings settings = new SlotInfo.Settings();

	private final Skin skin;

	private Label timer;

	private String title = "";

	public ChallengeCardDialog(BoundPronouns game, Skin skin) {
		super("Challenge Card", skin);
		this.skin = skin;
		this.title = "Time Remaining: ";
		this.game = game;
		this.getTitleLabel().setAlignment(Align.center);
		getStyle().titleFont = game.getFont(Font.SerifLarge);
		getStyle().background = getDialogBackground();
		setStyle(getStyle());

		setModal(true);
		setFillParent(true);

		Cell<Table> tcell = getCell(getContentTable());
		tcell.expand();
		tcell.fill();

		Cell<Table> bcell = getCell(getButtonTable());
		bcell.expandX();
		bcell.fillX().bottom();

		row();
		add(appNavBar = new Table(skin)).expandX().fillX().bottom();
		appNavBar.defaults().space(6);

		TextButtonStyle navStyle = new TextButtonStyle(skin.get(TextButtonStyle.class));
		navStyle.font = game.getFont(Font.SerifMedium);
		main = new TextButton("Exit", navStyle);
		appNavBar.row();
		appNavBar.add(main).left();
		main.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				showMainMenu();
				return true;
			}
		});

		mute = new TextButton("Unmute", navStyle);
		Cell<TextButton> c = appNavBar.add(mute).left().fillX();
		float tmp = c.getPrefWidth();
		c.width(tmp);
		mute.setText("Mute");
		mute.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				settings.muted = !settings.muted;
				updateMuteButtonText();
				return true;
			}
		});

		pause = new TextButton("Unpause", navStyle);
		c = appNavBar.add(pause).left().fillX();
		tmp = c.getPrefWidth();
		c.width(tmp);
		pause.setText("Pause");
		pause.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				paused = !paused;
				if (!paused) {
					// being unchecked
					paused = false;
					getButtonTable().setVisible(true);
					pause.setText("Pause");
				} else {
					// being checked
					paused = true;
					getButtonTable().setVisible(false);
					pause.setText("Unpause");
				}
				return true;
			}
		});

		speak = new TextButton("Speak", navStyle);
		speak.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (!speak.isDisabled()) {
					Gdx.app.postRunnable(playAudio);
				}
				return true;
			}
		});

		c = appNavBar.add(speak).left().fillX();

		LabelStyle ls = new LabelStyle(skin.get(LabelStyle.class));
		ls.font = game.getFont(Font.SerifMedium);
		timer = new Label("--", ls);
		appNavBar.add(timer).right().expandX();

		TextButtonStyle tbs_check = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		tbs_check.font = game.getFont(Font.SerifLarge);
		check = new TextButton("CHECK!", tbs_check);

		answer_style = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		answer_style.font = game.getFont(Font.SerifMedium);
	}

	private TiledDrawable getDialogBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		background.setMinHeight(0);
		background.setMinWidth(0);
		BitmapFont font = game.getFont(Font.SerifLarge);
		background.setTopHeight(font.getCapHeight() + 20);
		return background;
	}

	@Override
	public void hide() {
		hide(null);
	}

	@Override
	public void hide(Action action) {
		disableCard.run();
		super.hide(action);
	}

	protected void navEnable(boolean enabled) {
		pause.setVisible(true);
		pause.setDisabled(!enabled);
		pause.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
		main.setVisible(true);
		main.setDisabled(!enabled);
		main.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
		mute.setVisible(true);
		mute.setDisabled(!enabled);
		mute.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
	}

	@Override
	protected void result(Object object) {
		super.result(object);
	}

	private String removexmarks(String answer) {
		answer = answer.replace("xHe", "He");
		answer = answer.replace("xShe", "She");
		answer = answer.replace("xhe", "he");
		answer = answer.replace("xshe", "she");
		return answer;
	}

	public void setAnswers(AnswerList tracked_answers, AnswerList displayed_answers) {
		Table btable = getButtonTable();
		btable.clearChildren();
		boolean odd = true;
		for (int ix = 0; ix < tracked_answers.list.size(); ix++) {
			final Answer tracked_answer = tracked_answers.list.get(ix);
			final Answer displayed_answer = displayed_answers.list.get(ix);
			if (odd) {
				btable.row();
			}
			final TextButton a = new TextButton(removexmarks(displayed_answer.answer), answer_style);
			a.getLabel().setWrap(true);
			a.setUserObject(tracked_answer);
			a.addListener(new ClickListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					if (a.isChecked()) {
						// we are being unchecked
						a.setColor(Color.WHITE);
					} else {
						// we are being checked
						a.setColor(Color.GREEN);
					}
					return true;
				}
			});
			a.setColor(Color.WHITE);
			Value percentWidth = Value.percentWidth(.49f, btable);
			btable.add(a).fillX().width(percentWidth).pad(0).space(0);
			odd = !odd;
		}
		btable.row();
		setObject(check, null);
		btable.add(check).colspan(2).fillX().expandX();
		btable.row();
	}

	/**
	 * It is assumed that the Syllabary is entry #0 and the Latin is entry #1
	 * 
	 * @param activeCard
	 * @param deckCard
	 */
	public void setCard(ActiveCard activeCard, Card deckCard) {

		this._activeCard = activeCard;
		this._deckCard = deckCard;

		boolean stripPronunciationMarks = false;
		String syllabary = "";
		String latin = "";
		Iterator<String> i = deckCard.challenge.iterator();
		if (i.hasNext()) {
			syllabary = i.next();
		}
		if (i.hasNext()) {
			latin = i.next();
		}
		if (settings.display.equals(SlotInfo.DisplayMode.NONE)) {
			latin = "";
			syllabary = "";
		}
		if (settings.display.equals(SlotInfo.DisplayMode.Latin)) {
			syllabary = "";
		}
		if (settings.display.equals(SlotInfo.DisplayMode.LATIN_NP)) {
			syllabary = "";
			stripPronunciationMarks = true;
		}
		if (settings.display.equals(SlotInfo.DisplayMode.Syllabary)) {
			latin = "";
		}
		if (settings.display.equals(SlotInfo.DisplayMode.SYLLABARY_NP)) {
			latin = "";
			stripPronunciationMarks = true;
		}
		if (settings.display.equals(SlotInfo.DisplayMode.BOTH_NP)) {
			stripPronunciationMarks = true;
		}
		
		if (stripPronunciationMarks) {
			syllabary = syllabary.replace(BoundPronouns.UNDERDOT, "");
			syllabary = syllabary.replace(BoundPronouns.UNDERX, "");
			syllabary = syllabary.replaceAll("[¹²³⁴]", "");
			
			latin = latin.replace(BoundPronouns.UNDERDOT, "");
			latin = latin.replace(BoundPronouns.UNDERX, "");
			latin = latin.replaceAll("[¹²³⁴]", "");
			for (String[] px: new String[][] {{"ạ", "a"}, {"ẹ", "e"}, {"ị", "i"}, {"ọ", "o"}, {"ụ", "u"}, {"ṿ", "v"}}) {
				latin = latin.replace(px[0], px[1]);
				latin = latin.replace(px[0].toUpperCase(), px[1].toUpperCase());
			}
		}

		Table ctable = getContentTable();
		ctable.clearChildren();
		ctable.row();

		TextButtonStyle chr_san_large = new TextButtonStyle(skin.get(TextButtonStyle.class));
		chr_san_large.font = game.getFont(Font.SerifXLarge);

		TextButton challenge_top = new TextButton("", chr_san_large);
		challenge_top.setDisabled(true);
		challenge_top.setTouchable(Touchable.disabled);
		challenge_top.clearChildren();

		ctable.add(challenge_top).fill().expand().align(Align.center);

		boolean shrink = false;
		if (syllabary.length() + latin.length() > 32) {
			shrink = true;
		}
		Label msg;
		if (!StringUtils.isBlank(syllabary)) {
			LabelStyle san_style = new LabelStyle(game.getFont(Font.SerifXLarge), chr_san_large.fontColor);
			if (shrink) {
				game.log(this, syllabary.length() + "");
				san_style = new LabelStyle(game.getFont(Font.SerifLLarge), chr_san_large.fontColor);
			}
			msg = new Label(syllabary, san_style);
			msg.setAlignment(Align.center);
			msg.setWrap(true);
			challenge_top.add(msg).fill().expand();
		}
		if (!StringUtils.isBlank(latin)) {
			LabelStyle serif_style = new LabelStyle(game.getFont(Font.SerifXLarge), chr_san_large.fontColor);
			if (shrink) {
				game.log(this, latin.length() + "");
				serif_style = new LabelStyle(game.getFont(Font.SerifLLarge), chr_san_large.fontColor);
			}
			msg = new Label(latin, serif_style);
			msg.setAlignment(Align.center);
			msg.setWrap(true);
			challenge_top.add(msg).fill().expand();
		}
	}

	public void setCheckVisible(boolean visible) {
		check.setVisible(visible);
	}

	public void setTimeRemaining(float seconds) {
		int min = MathUtils.floor(seconds / 60f);
		int sec = MathUtils.floor(seconds - min * 60f);
		getTitleLabel().setText(title + " " + min + ":" + (sec < 10 ? "0" + sec : sec));
		getTitleLabel().setAlignment(Align.center);
	}

	public void setTimer(float time) {
		int x = (int) time;
		String z = (x < 10 ? "0" : "") + x;
		timer.setText(z);
	}
	
	protected Runnable runnableNoop = new Runnable() {
		@Override
		public void run() {
			// Do nothing
		}
	};

	@Override
	public Dialog show(Stage stage) {
		paused = false;
		Gdx.app.postRunnable(disableCard);
		RunnableAction enable = Actions.run(enableCard);
		RunnableAction audio;
		if (settings.muted) {
			speak.setDisabled(false);
			speak.setTouchable(Touchable.enabled);
			audio = Actions.run(runnableNoop);
		} else {
			audio = Actions.run(playAudio);
		}

		if (Gdx.input.isTouched()) {
			DelayAction delay = Actions.delay(.2f);
			show(stage, sequence(Actions.alpha(0), delay, Actions.fadeIn(0.4f, Interpolation.fade), enable, audio));
		} else {
			show(stage, sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Interpolation.fade), enable, audio));
		}
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
		return this;
	}

	protected Runnable playAudio = new Runnable() {
		@Override
		public void run() {
			speak.setDisabled(true);
			FileHandle audioFile = game.audioFiles.get(_deckCard.challenge.get(1));
			if (audioFile == null) {
				Gdx.app.log(this.getClass().getName(), "NO AUDIO FILE MATCHES: " + _deckCard.challenge.get(1));
				return;
			}
			Music newMusic = Gdx.audio.newMusic(audioFile);
			newMusic.setOnCompletionListener(musicDispose);
			newMusic.play();
		}
	};

	protected OnCompletionListener musicDispose = new OnCompletionListener() {
		@Override
		public void onCompletion(Music music) {
			speak.setDisabled(false);
			music.dispose();
		}
	};

	protected abstract void showMainMenu();

	public void updateMuteButtonText() {
		mute.setText(settings.muted ? "Unmute" : "Mute");
	}

	public void updateSettings() {
		updateMuteButtonText();
	}

}
