package com.cherokeelessons.bp;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.SlotInfo;

public abstract class NewCardDialog extends Dialog {

	private final TextButton answer;

	private Table appNavBar;

	private final BoundPronouns game;

	public SlotInfo.Settings settings = new SlotInfo.Settings();

	private String title = "";

	private final Skin skin;

	private TextButton speak;

	protected Runnable playAudio = new Runnable() {
		@Override
		public void run() {
			speak.setDisabled(true);
			speak.setTouchable(Touchable.disabled);
			final FileHandle audioFile = game.audioFiles.get(pronounce);
			if (audioFile == null) {
				Gdx.app.log(this.getClass().getName(), "NO AUDIO FILE MATCHES: " + pronounce);
				return;
			}
			final Music newMusic = Gdx.audio.newMusic(audioFile);
			newMusic.setOnCompletionListener(musicDispose);
			newMusic.play();
		}
	};

	protected OnCompletionListener musicDispose = new OnCompletionListener() {
		@Override
		public void onCompletion(final Music music) {
			speak.setDisabled(false);
			speak.setTouchable(Touchable.enabled);
			music.dispose();
		}
	};

	private final TextButtonStyle answer_style;

	private String pronounce = "";

	protected Runnable runnableNoop = new Runnable() {
		@Override
		public void run() {
			// Do nothing
		}
	};

	private TextButton mute;

	public NewCardDialog(final BoundPronouns game, final Skin skin) {
		super("New Vocabulary", skin);

		this.title = "New Vocabulary - Time Remaining: ";
		this.skin = skin;
		this.game = game;
		this.getTitleLabel().setAlignment(Align.center);
		getStyle().titleFont = game.getFont(Font.SerifLarge);
		getStyle().background = getDialogBackground();
		setStyle(getStyle());

		setModal(true);
		setFillParent(true);

		getTitleLabel().setText(title);
		getTitleLabel().setAlignment(Align.center);

		answer = new TextButton("", skin);
		answer.setDisabled(true);
		answer.setTouchable(Touchable.disabled);

		final TextButtonStyle answerStyle = new TextButtonStyle(answer.getStyle());
		answerStyle.font = game.getFont(Font.SerifSmall);
		answer.setStyle(answerStyle);
		answer.align(Align.bottom);

		final Table ctable = getContentTable();
		ctable.row();
		ctable.row();
		ctable.add(answer).fill().expand();

		final Cell<Table> tcell = getCell(getContentTable());
		tcell.expand();
		tcell.fill();

		final Cell<Table> bcell = getCell(getButtonTable());
		bcell.expandX();
		bcell.fillX().bottom();

		row();
		add(appNavBar = new Table(skin)).left().expandX().bottom();
		appNavBar.defaults().space(6);

		final TextButtonStyle navStyle = new TextButtonStyle(skin.get(TextButtonStyle.class));
		navStyle.font = game.getFont(Font.SerifMedium);
		final TextButton exit = new TextButton("Exit", navStyle);
		appNavBar.row();
		appNavBar.add(exit).left().expandX();

		exit.addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
					final int button) {
				showMainMenu();
				return true;
			}
		});

		mute = new TextButton("Unmute", navStyle);
		final Cell<TextButton> c = appNavBar.add(mute).left().fillX();
		final float tmp = c.getPrefWidth();
		c.width(tmp);
		mute.setText("Mute");
		mute.addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
					final int button) {
				settings.muted = !settings.muted;
				updateMuteButtonText();
				return true;
			}
		});

		speak = new TextButton("Speak", navStyle);
		speak.addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
					final int button) {
				if (!speak.isDisabled()) {
					Gdx.app.postRunnable(playAudio);
				}
				return true;
			}
		});
		appNavBar.add(speak).left().expandX();

		final Table btable = getButtonTable();
		btable.clearChildren();
		btable.row();
		final TextButtonStyle tbs_check = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		tbs_check.font = game.getFont(Font.SerifMedium);
		final TextButton a = new TextButton("TAP HERE WHEN READY!", tbs_check);
		btable.add(a).fill().expandX().bottom();
		setObject(a, null);

		answer_style = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		answer_style.font = game.getFont(Font.SerifMedium);
	}

	@Override
	public void act(final float delta) {
		super.act(delta);
	}

	private TiledDrawable getDialogBackground() {
		final Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TextureRegion region = new TextureRegion(texture);
		final TiledDrawable background = new TiledDrawable(region);
		background.setMinHeight(0);
		background.setMinWidth(0);
		background.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);
		return background;
	}

	@Override
	public void hide() {
		super.hide(null);
	}

	private String removexmarks(@SuppressWarnings("hiding") String answer) {
		answer = answer.replace("xHe", "He");
		answer = answer.replace("xShe", "She");
		answer = answer.replace("xhe", "he");
		answer = answer.replace("xshe", "she");
		return answer;
	}

	protected void setAnswers(final Card the_card) {
		final Table ctable = getContentTable();
		boolean odd = true;
		final List<String> answers = new ArrayList<>(the_card.answer);
		Collections.sort(answers);
		for (@SuppressWarnings("hiding")
		String answer : answers) {
			answer = removexmarks(answer);
			if (odd) {
				ctable.row();
			}
			final TextButton a = new TextButton(answer, answer_style);
			a.getLabel().setWrap(true);
			a.setColor(Color.WHITE);
			a.setTouchable(Touchable.disabled);
			a.setDisabled(true);
			final Value percentWidth = Value.percentWidth(.49f, ctable);
			ctable.add(a).fillX().width(percentWidth).pad(0).space(0);
			odd = !odd;
		}
		if (!odd && answers.size() != 1) {
			final TextButton a = new TextButton("", answer_style);
			a.getLabel().setWrap(true);
			a.setColor(Color.WHITE);
			a.setTouchable(Touchable.disabled);
			a.setDisabled(true);
			final Value percentWidth = Value.percentWidth(.49f, ctable);
			ctable.add(a).fillX().width(percentWidth).pad(0).space(0);
		}
		ctable.row();
	}

	public void setCard(final Card the_card) {
		String syllabary = "";
		String latin = "";
		final Iterator<String> i = the_card.challenge.iterator();
		if (i.hasNext()) {
			syllabary = i.next();
		}
		if (i.hasNext()) {
			latin = i.next();
			pronounce = latin;
		}

		boolean stripPronunciationMarks = false;
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
			for (final String[] px : new String[][] { { "ạ", "a" }, { "ẹ", "e" }, { "ị", "i" }, { "ọ", "o" },
					{ "ụ", "u" }, { "ṿ", "v" } }) {
				latin = latin.replace(px[0], px[1]);
				latin = latin.replace(px[0].toUpperCase(), px[1].toUpperCase());
			}
		}

		final Table ctable = getContentTable();
		ctable.clearChildren();
		ctable.row();

		final TextButtonStyle chr_san_large = new TextButtonStyle(skin.get(TextButtonStyle.class));
		chr_san_large.font = game.getFont(Font.SerifXLarge);

		final TextButton challenge_top = new TextButton("", chr_san_large);
		challenge_top.setDisabled(true);
		challenge_top.setTouchable(Touchable.disabled);
		challenge_top.clearChildren();

		final Cell<TextButton> challenge = ctable.add(challenge_top).fill().expand().align(Align.center);
		if (the_card.answer.size() != 1) {
			challenge.colspan(2);
		}

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

		setAnswers(the_card);
	}

	public void setTimeRemaining(final float seconds) {
		final int min = MathUtils.floor(seconds / 60f);
		final int sec = MathUtils.floor(seconds - min * 60f);
		getTitleLabel().setText(title + " " + min + ":" + (sec < 10 ? "0" + sec : sec));
		getTitleLabel().setAlignment(Align.center);
	}

	@Override
	public Dialog show(final Stage stage) {
		RunnableAction audio;
		if (settings.muted) {
			speak.setDisabled(false);
			audio = Actions.run(runnableNoop);
		} else {
			audio = Actions.run(playAudio);
		}
		show(stage, sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Interpolation.fade), audio));
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2), //
				Math.round((stage.getHeight() - getHeight()) / 2));
		return this;
	}

	protected abstract void showMainMenu();

	public void updateMuteButtonText() {
		mute.setText(settings.muted ? "Unmute" : "Mute");
	}

	public void updateSettings() {
		updateMuteButtonText();
	}
}
