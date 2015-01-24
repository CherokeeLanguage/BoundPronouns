package com.cherokeelessons.bp;

import java.util.Iterator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.Answer;
import com.cherokeelessons.cards.Answer.AnswerList;
import com.cherokeelessons.cards.Card;

public abstract class ChallengeCardDialog extends Dialog {

	@Override
	public void act(float delta) {
		super.act(delta);
	}

	public void setCounter(int cardcount) {
		setTitle(title + " [" + cardcount + "]");
	}

	private String title = "";

	private final BoundPronouns game;

	private final TextButton challenge_top;

	private Skin skin;

	@Override
	protected void result(Object object) {
		super.result(object);
	}

	public ChallengeCardDialog(BoundPronouns game, Skin skin) {
		super("Challenge Card", skin);
		this.title = "Challenge Card";
		this.game = game;
		this.skin = skin;
		getStyle().titleFont = sans54();
		getStyle().background = getDialogBackground();
		setStyle(getStyle());

		setModal(true);
		setFillParent(true);

		challenge_top = new TextButton("", skin);
		challenge_top.setDisabled(true);
		challenge_bottom = new Label("", skin);

		TextButtonStyle chr_san_large = new TextButtonStyle(
				challenge_top.getStyle());
		chr_san_large.font = sans_large();
		challenge_top.setStyle(chr_san_large);

		LabelStyle pronounce_large = new LabelStyle(challenge_bottom.getStyle());
		pronounce_large.font = serif54();
		challenge_bottom.setStyle(pronounce_large);

		challenge_top.add(challenge_bottom).pad(0).top();

		Table ctable = getContentTable();
		ctable.row();
		ctable.add(challenge_top).fill().expand();

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
		navStyle.font = sans36();
		TextButton main = new TextButton("Main Menu", navStyle);
		appNavBar.row();
		appNavBar.add(main).left().expandX();
		main.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				doNav();
				return true;
			}
		});
		LabelStyle ls = new LabelStyle(skin.get(LabelStyle.class));
		ls.font = sans36();
		timer = new Label("--", ls);
		appNavBar.add(timer).right().expandX();

		TextButtonStyle tbs_check = new TextButtonStyle(skin.get("default",
				TextButtonStyle.class));
		tbs_check.font = sans36();
		check = new TextButton("CHECK!", tbs_check);
		
		answer_style = new TextButtonStyle(skin.get("default",
				TextButtonStyle.class));
		answer_style.font = serif36();
	}
	
	private final TextButtonStyle answer_style;

	private Label timer;

	public void setTimer(float time) {
		int x = (int) time;
		String z = (x < 10 ? "0" : "") + x;
		timer.setText(z);
	}

	protected abstract void doNav();

	private Table appNavBar;

	private final Label challenge_bottom;

	final private StringBuilder showCardSb = new StringBuilder();

	protected Card _deckCard;

	protected ActiveCard _activeCard;

	public void setCard(ActiveCard activeCard, Card deckCard) {
		this._activeCard = activeCard;
		this._deckCard = deckCard;

		Iterator<String> i = deckCard.challenge.iterator();
		challenge_top.setText(i.next());
		showCardSb.setLength(0);
		while (i.hasNext()) {
			showCardSb.append(i.next());
			showCardSb.append("\n");
		}
		challenge_bottom.setText(showCardSb.toString());
		showCardSb.setLength(0);
	}

	private TiledDrawable getDialogBackground() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TextureRegion region = new TextureRegion(texture);
		TiledDrawable background = new TiledDrawable(region);
		background.setMinHeight(0);
		background.setMinWidth(0);
		background.setTopHeight(sans54().getCapHeight() + 20);
		return background;
	}

	private BitmapFont sans_large() {
		return game.manager.get("sans72.ttf", BitmapFont.class);
	}

	private BitmapFont sans54() {
		return game.manager.get("sans54.ttf", BitmapFont.class);
	}

	private BitmapFont serif54() {
		return game.manager.get("serif54.ttf", BitmapFont.class);
	}

	private BitmapFont sans36() {
		return game.manager.get("sans36.ttf", BitmapFont.class);
	}

	private BitmapFont serif36() {
		return game.manager.get("serif36.ttf", BitmapFont.class);
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
			Value percentWidth = Value.percentWidth(.5f, btable);
			btable.add(a).fillX().width(percentWidth).padLeft(0).padRight(0).spaceLeft(0).spaceRight(0);
			odd = !odd;
		}
		btable.row();
		setObject(check, selected);
		btable.add(check).colspan(2).fillX().expandX();
		btable.row();
		check.setVisible(true);
		check.setDisabled(false);
	}
	
	public void setCheckVisible(boolean visible){
		check.setVisible(visible);
	}

	private final TextButton check;

}
