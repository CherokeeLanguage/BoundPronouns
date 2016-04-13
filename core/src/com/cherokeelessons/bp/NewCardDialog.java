package com.cherokeelessons.bp;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
	
	public SlotInfo.Settings settings=new SlotInfo.Settings();
	
	private String title="";

	private final Skin skin;
	
	public NewCardDialog(BoundPronouns game, Skin skin) {
		super("New Vocabulary", skin);
		
		this.title = "New Vocabulary - Time Remaining: ";
		this.skin=skin;
		this.game = game;
		this.getTitleLabel().setAlignment(Align.center);
		getStyle().titleFont=game.getFont(Font.SerifLarge);
		getStyle().background=getDialogBackground();
		setStyle(getStyle());
		
		setModal(true);
		setFillParent(true);
		
		getTitleLabel().setText(title);
		getTitleLabel().setAlignment(Align.center);
		
		answer=new TextButton("", skin);
		answer.setDisabled(true);
		answer.setTouchable(Touchable.disabled);
		
		TextButtonStyle answerStyle = new TextButtonStyle(answer.getStyle());		
		answerStyle.font=game.getFont(Font.SerifSmall);
		answer.setStyle(answerStyle);
		answer.align(Align.bottom);
		
		Table ctable = getContentTable();
		ctable.row();
		ctable.row();
		ctable.add(answer).fill().expand();
		
		Cell<Table> tcell = getCell(getContentTable());
		tcell.expand();
		tcell.fill();
		
		Cell<Table> bcell = getCell(getButtonTable());
		bcell.expandX();
		bcell.fillX().bottom();
		
		row();
		add(appNavBar=new Table(skin)).left().expandX().bottom();
		appNavBar.defaults().space(6);
		
		TextButtonStyle navStyle = new TextButtonStyle(skin.get(TextButtonStyle.class));
		navStyle.font=game.getFont(Font.SerifMedium);
		TextButton main = new TextButton("Main Menu", navStyle);
		appNavBar.row();		
		appNavBar.add(main).left().expandX();
		main.addListener(new ClickListener(){
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				showMainMenu();
				return true;
			}
		});
		
		Table btable = getButtonTable();
		btable.clearChildren();
		btable.row();
		TextButtonStyle tbs_check = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		tbs_check.font=game.getFont(Font.SerifMedium);
		TextButton a = new TextButton("TAP HERE WHEN READY!", tbs_check);
		btable.add(a).fill().expandX().bottom();
		setObject(a, null);
		
		answer_style = new TextButtonStyle(skin.get("default",
				TextButtonStyle.class));
		answer_style.font = game.getFont(Font.SerifMedium);
	}
	
	@Override
	public void act(float delta) {
		super.act(delta);
	}
	
	private TiledDrawable getDialogBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		background.setMinHeight(0);
		background.setMinWidth(0);
		background.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight()+20);
		return background;
	}
	@Override
	public void hide() {
		super.hide(null);
	}
	
	private final TextButtonStyle answer_style;
	
	protected void setAnswers(Card the_card) {
		Table ctable = getContentTable();
		boolean odd = true;
		List<String> answers = new ArrayList<>(the_card.answer);
		Collections.sort(answers);
		for (String answer: answers) {
			answer=removexmarks(answer);
			if (odd) {
				ctable.row();
			}
			final TextButton a = new TextButton(answer, answer_style);
			a.getLabel().setWrap(true);
			a.setColor(Color.WHITE);
			a.setTouchable(Touchable.disabled);
			a.setDisabled(true);
			Value percentWidth = Value.percentWidth(.49f, ctable);
			ctable.add(a).fillX().width(percentWidth).pad(0).space(0);
			odd = !odd;
		}
		if (!odd && answers.size()!=1) {
			final TextButton a = new TextButton("", answer_style);
			a.getLabel().setWrap(true);
			a.setColor(Color.WHITE);
			a.setTouchable(Touchable.disabled);
			a.setDisabled(true);
			Value percentWidth = Value.percentWidth(.49f, ctable);
			ctable.add(a).fillX().width(percentWidth).pad(0).space(0);
		}
		ctable.row();
	}

	public void setCard(Card the_card) {
		String syllabary = "";
		String latin = "";
		Iterator<String> i = the_card.challenge.iterator();
		if (i.hasNext()) {
			syllabary = i.next();
		}
		if (i.hasNext()) {
			latin = i.next();
		}
		if (settings.display.equals(SlotInfo.DisplayMode.Latin)) {
			syllabary = "";
		}
		if (settings.display.equals(SlotInfo.DisplayMode.Syllabary)) {
			latin = "";
		}
		
		Table ctable = getContentTable();
		ctable.clearChildren();
		ctable.row();

		TextButtonStyle chr_san_large = new TextButtonStyle(
				skin.get(TextButtonStyle.class));
		chr_san_large.font = game.getFont(Font.SerifXLarge);

		TextButton challenge_top = new TextButton("", chr_san_large);
		challenge_top.setDisabled(true);
		challenge_top.setTouchable(Touchable.disabled);
		challenge_top.clearChildren();

		Cell<TextButton> challenge = ctable.add(challenge_top).fill().expand().align(Align.center);
		if (the_card.answer.size()!=1) {
			challenge.colspan(2);
		}

		boolean shrink = false;
		if (syllabary.length() + latin.length() > 32) {
			shrink = true;
		}
		
		Label msg;
		if (!StringUtils.isBlank(syllabary)) {
			LabelStyle san_style = new LabelStyle(
					game.getFont(Font.SerifXLarge), chr_san_large.fontColor);
			if (shrink) {
				game.log(this, syllabary.length() + "");
				san_style = new LabelStyle(game.getFont(Font.SerifLLarge),
						chr_san_large.fontColor);
			}
			msg = new Label(syllabary, san_style);
			msg.setAlignment(Align.center);
			msg.setWrap(true);
			challenge_top.add(msg).fill().expand();
		}
		if (!StringUtils.isBlank(latin)) {
			LabelStyle serif_style = new LabelStyle(
					game.getFont(Font.SerifXLarge), chr_san_large.fontColor);
			if (shrink) {
				game.log(this, latin.length() + "");
				serif_style = new LabelStyle(game.getFont(Font.SerifLLarge),
						chr_san_large.fontColor);
			}
			msg = new Label(latin, serif_style);
			msg.setAlignment(Align.center);
			msg.setWrap(true);
			challenge_top.add(msg).fill().expand();
		}
		
		setAnswers(the_card);
	}
	
	private String removexmarks(String answer) {
		answer = answer.replace("xHe", "He");
		answer = answer.replace("xShe", "She");
		answer = answer.replace("xhe", "he");
		answer = answer.replace("xshe", "she");
		return answer;
	}

	public void setTimeRemaining(float seconds) {
		int min = MathUtils.floor(seconds/60f);
		int sec = MathUtils.floor(seconds-min*60f);
		getTitleLabel().setText(title + " " + min + ":" + (sec<10?"0"+sec:sec));
		getTitleLabel().setAlignment(Align.center);
	}
	
	@Override
	public Dialog show(Stage stage) {
		show(stage,
				sequence(Actions.alpha(0),
						Actions.fadeIn(0.4f, Interpolation.fade)));
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2),
				Math.round((stage.getHeight() - getHeight()) / 2));
		return this;
	}

	protected abstract void  showMainMenu();
}
