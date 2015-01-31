package com.cherokeelessons.bp;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.Answer;
import com.cherokeelessons.cards.Answer.AnswerList;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.SlotInfo;

public abstract class ChallengeCardDialog extends Dialog {
	
	public SlotInfo.Settings settings=new SlotInfo.Settings();

	public void setCounter(int cardcount) {
		setTitle(title + " [" + cardcount + "]");
	}

	private String title = "";

	private final BoundPronouns game;

	@Override
	protected void result(Object object) {
		super.result(object);
	}
	
	private TextButton mute;

	private final Skin skin;

	public ChallengeCardDialog(BoundPronouns game, Skin skin) {
		super("Challenge Card", skin);
		this.skin=skin;
		this.title = "Challenge Card";
		this.game = game;
//		getStyle().titleFont = game.getFont(Font.SansLarge);
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
		TextButtonStyle navStyle = new TextButtonStyle(
				skin.get(TextButtonStyle.class));
//		navStyle.font = game.getFont(Font.SansMedium);
		navStyle.font = game.getFont(Font.SerifMedium);
		TextButton main = new TextButton("Main Menu", navStyle);
		appNavBar.row();
		appNavBar.add(main).left();
		main.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				doNav();
				return true;
			}
		});
		
		mute = new TextButton("Unmute", navStyle);
		Cell<TextButton> c = appNavBar.add(mute).left().fillX();
		float tmp = c.getPrefWidth();
		c.width(tmp);
		mute.setText("Mute");
		mute.addListener(new ClickListener(){
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				settings.muted=!settings.muted;
				updateMuteButtonText();
				return true;
			}
		});
		
		final TextButton pause = new TextButton("Unpause", navStyle);
		c=appNavBar.add(pause).left().fillX();
		tmp = c.getPrefWidth();
		c.width(tmp);
		pause.setText("Pause");
		pause.addListener(new ClickListener(){
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				if (pause.isChecked()) {
					//being unchecked
					paused=false;
					getButtonTable().setVisible(true);
					pause.setText("Pause");
				} else {
					//being checked
					paused=true;
					getButtonTable().setVisible(false);
					pause.setText("Unpause");
				}
				return true;
			}
		});
		
		LabelStyle ls = new LabelStyle(skin.get(LabelStyle.class));
//		ls.font = game.getFont(Font.SansMedium);
		ls.font = game.getFont(Font.SerifMedium);
		timer = new Label("--", ls);
		appNavBar.add(timer).right().expandX();

		TextButtonStyle tbs_check = new TextButtonStyle(skin.get("default",
				TextButtonStyle.class));
//		tbs_check.font = game.getFont(Font.SansLarge);
		tbs_check.font = game.getFont(Font.SerifLarge);
		check = new TextButton("CHECK!", tbs_check);
		
		answer_style = new TextButtonStyle(skin.get("default",
				TextButtonStyle.class));
		answer_style.font = game.getFont(Font.SerifMedium);
	}
	
	public boolean paused=false;
	
	private final TextButtonStyle answer_style;

	private Label timer;

	public void setTimer(float time) {
		int x = (int) time;
		String z = (x < 10 ? "0" : "") + x;
		timer.setText(z);
	}

	protected abstract void doNav();

	private Table appNavBar;

	protected Card _deckCard;

	protected ActiveCard _activeCard;

	/**
	 * It is assumed that the Syllabary is entry #0 and the Latin is entry #1
	 * @param activeCard
	 * @param deckCard
	 */
	public void setCard(ActiveCard activeCard, Card deckCard) {
		
		this._activeCard = activeCard;
		this._deckCard = deckCard;

		String syllabary="";
		String latin="";
		Iterator<String> i = deckCard.challenge.iterator();
		if (i.hasNext()) {
			syllabary=i.next();
		}
		if (i.hasNext()) {
			latin=i.next();
		}		
		if (settings.display.equals(SlotInfo.DisplayMode.Latin)){
			syllabary="";
		} 
		if (settings.display.equals(SlotInfo.DisplayMode.Syllabary)){
			latin="";
		}
		
		Table ctable = getContentTable();
		ctable.clearChildren();
		ctable.row();
		
		TextButtonStyle chr_san_large = new TextButtonStyle(
				skin.get(TextButtonStyle.class));
//		chr_san_large.font = game.getFont(Font.SansXLarge);
		chr_san_large.font = game.getFont(Font.SerifXLarge);
		
		TextButton challenge_top = new TextButton("", chr_san_large);
		challenge_top.setDisabled(true);
		challenge_top.setTouchable(Touchable.disabled);
		challenge_top.clearChildren();
		
		ctable.add(challenge_top).fill().expand().align(Align.center);
		
		boolean shrink=false;
		if (syllabary.length()+latin.length()>32) {
			shrink=true;
		}
		Label msg;
		if (!StringUtils.isBlank(syllabary)) {
//			LabelStyle san_style = new LabelStyle(game.getFont(Font.SansXLarge), chr_san_large.fontColor);
			LabelStyle san_style = new LabelStyle(game.getFont(Font.SerifXLarge), chr_san_large.fontColor);
			if (shrink) {
				game.log(this, syllabary.length()+"");
//				san_style = new LabelStyle(game.getFont(Font.SansLLarge), chr_san_large.fontColor);
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
				game.log(this, latin.length()+"");
				serif_style = new LabelStyle(game.getFont(Font.SerifLLarge), chr_san_large.fontColor);
			}
			msg = new Label(latin, serif_style);
			msg.setAlignment(Align.center);
			msg.setWrap(true);
			challenge_top.add(msg).fill().expand();
		}
	}

	private TiledDrawable getDialogBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		background.setMinHeight(0);
		background.setMinWidth(0);
//		background.setTopHeight(game.getFont(Font.SansLarge).getCapHeight() + 20);
		background.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);
		return background;
	}

	final AnswerList selected = new AnswerList();
	
	public void setAnswers(AnswerList answers) {
		selected.list.clear();
		Table btable = getButtonTable();
		btable.clearChildren();
		boolean odd = true;
		for (final Answer answer : answers.list) {
			if (odd) {
				btable.row();
			}
			final TextButton a = new TextButton(answer.answer, answer_style);
			a.getLabel().setWrap(true);
			a.setUserObject(answer);
			a.addListener(new ClickListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					if (a.isChecked()) {
						// we are being unchecked
						a.setColor(Color.WHITE);
						selected.list.remove(answer);
					} else {
						// we are being checked
						a.setColor(Color.GREEN);
						selected.list.add(answer);
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
		setObject(check, selected);
		btable.add(check).colspan(2).fillX().expandX();
		btable.row();
		check.setVisible(true);
		check.setDisabled(false);
		check.setTouchable(Touchable.enabled);
	}
	
	public void setCheckVisible(boolean visible){
		check.setVisible(visible);
	}

	public void updateMuteButtonText() {
		mute.setText(settings.muted?"Unmute":"Mute");
	}

	private final TextButton check;

	public void updateSettings() {
		updateMuteButtonText();		
	}

}
