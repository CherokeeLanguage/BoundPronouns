package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.Collection;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;
import com.cherokeelessons.util.GooglePlayGameServices.TimeSpan;

public class ShowLeaderboards extends ChildScreen implements Screen {

	private final Skin skin;
	private Table container;
	private ScrollPane scroll;
	private Table scrolltable;
	private Label message;

	public ShowLeaderboards(BoundPronouns game, Screen caller) {
		super(game, caller);
		skin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));
		container = new Table(skin);
		container.setBackground(d());
		container.setFillParent(true);
		stage.addActor(container);
	}

	@Override
	public void show() {
		super.show();
		container.addAction(Actions.delay(.1f, Actions.run(new InitView())));
	}

	private TiledDrawable d() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		return new TiledDrawable(new TextureRegion(texture));
	}

	private TimeSpan ts = TimeSpan.WEEKLY;
	public FileHandle p0;

	public String[] ranks = { "1st", "2nd", "3rd", "4th", "5th", "6th", "7th",
			"8th", "9th", "10th" };

	public Callback<GameScores> success_show_scores = new Callback<GameScores>() {
		@Override
		public void run() {
			Gdx.app.log("success_show_scores", "Scores received.");
			GameScores data = getData();
			if (data==null) {
				message.setText("You must login to Google Play for Leaderboard Support");
				return;
			}
			if (data.collection==null) {
				data.collection=lb_collection;
			}
			if (data.ts==null) {
				data.ts=ts;
			}
			if (data.collection.equals(Collection.PUBLIC)) {
				message.setText(data.ts.getEngrish() + " Top Public Scores");
			}
			if (data.collection.equals(Collection.SOCIAL)) {
				message.setText(data.ts.getEngrish() + " Top Circle Scores");
			}

			Table table = scrolltable;

			LabelStyle ls = new LabelStyle(game.getFont(Font.SerifLarge),
					Color.BLACK);

			table.clear();
			table.defaults().expandX();
			String text = "Rank";
			table.add(new Label(text, ls)).padLeft(15).padRight(15).center();
			text = "Score";
			table.add(new Label(text, ls)).center();
			text = "Skill Level";
			table.add(new Label(text, ls)).center();
			text = "Display Name";
			table.add(new Label(text, ls)).center();

			for (GameScore score : data.list) {
				table.row();
				table.add(new Label(score.rank, ls)).padLeft(15).padRight(15)
						.center();
				table.add(new Label(score.value, ls)).right().padRight(30);
				table.add(new Label(score.tag, ls)).center();
				table.add(new Label(score.user, ls)).center();
			}

			for (int ix = data.list.size(); ix < ranks.length; ix++) {
				table.row();
				table.add(new Label(ranks[ix], ls)).padLeft(15).padRight(15)
						.center();
				table.add(new Label("0", ls)).right().padRight(30);
				table.add(new Label("Newbie", ls)).center();
				table.add(new Label("", ls)).center();
			}
			
			Preferences prefs = BoundPronouns.getPrefs();
			if (!prefs.getBoolean(BoundPronouns.GooglePlayLogginIn, false)) {
				message.setText("You must login to Google Play for Leaderboard Support");
			}
		}
	};

	public Callback<Exception> noop_error = new Callback<Exception>() {
		@Override
		public void run() {
			Gdx.app.log("Google Play Leaderboard Error: ", getData()
					.getMessage());
		}
	};

	public static final String BoardId = "CgkIy7GTtc0TEAIQAw";

	public Collection lb_collection = Collection.PUBLIC;

	private class InitView implements Runnable {
		@Override
		public void run() {
			TextButton button;

			TextButtonStyle tbs = skin.get(TextButtonStyle.class);
			tbs.font = game.getFont(Font.SerifSmall);

			ButtonGroup<TextButton> bgroup = new ButtonGroup<>();
			bgroup.setMaxCheckCount(1);
			bgroup.setMinCheckCount(1);

			button = new TextButton(BoundPronouns.BACK_ARROW, tbs);
			container.add(button).center().top()
					.width(BoundPronouns.BACK_WIDTH);
			button.addListener(exit);

			button = new TextButton(ts.getEngrish(), tbs);
			button.setChecked(false);
			container.add(button).center().top().expandX().fillX();
			final TextButton ts_button = button;

			button = new TextButton(lb_collection.getEnglish(), tbs);
			button.setChecked(true);
			container.add(button).center().top().expandX().fillX();
			bgroup.add(button);
			final TextButton lb_button = button;
			lb_button.addListener(new ClickListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					lb_collection = lb_collection.next();
					lb_button.setText(lb_collection.getEnglish());
					requestScores();
					return true;
				}
			});
			ts_button.addListener(new ClickListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					ts = ts.next();
					float width = ts_button.getLabel().getWidth();
					ts_button.setText(ts.getEngrish());
					ts_button.getLabel().setWidth(width);
					requestScores();
					return true;
				}
			});
			
			LabelStyle ls = new LabelStyle(game.getFont(Font.SerifSmall),
					Color.BLACK);
			message = new Label("...", ls);

			Preferences prefs = BoundPronouns.getPrefs();
			if (!prefs.getBoolean(BoundPronouns.GooglePlayLogginIn, false)) {
				button = new TextButton("Login to Google Play", tbs);
				message.setText("You must login to Google Play for Leaderboard Support");
			} else {
				button = new TextButton("Logout of Google Play", tbs);
			}
			final TextButton play_button = button;
			play_button.addListener(new ClickListener(){
				Callback<Void> success_in=new Callback<Void>() {							
					@Override
					public void run() {
						Preferences prefs = BoundPronouns.getPrefs();
						prefs.putBoolean(BoundPronouns.GooglePlayLogginIn, true);
						prefs.flush();
						requestScores();
						play_button.setText("Logout of Google Play");
					}
				};
				Callback<Void> success_out=new Callback<Void>() {							
					@Override
					public void run() {
						Preferences prefs = BoundPronouns.getPrefs();
						prefs.putBoolean(BoundPronouns.GooglePlayLogginIn, false);
						prefs.flush();
						requestScores();
						play_button.setText("Login to Google Play");
					}
				};
				Callback<Exception> error=new Callback<Exception>() {
					@Override
					public void run() {
						success_out.run();
					}
				};
				@Override
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					Preferences prefs = BoundPronouns.getPrefs();
					if (prefs.getBoolean(BoundPronouns.GooglePlayLogginIn, false)) {
						BoundPronouns.services.logout(success_out, error);
					} else {
						BoundPronouns.services.login(success_in, error);
					}
					return true;
				}
			});
			container.add(button).center().top().expandX().fillX();

			final int c = container.getCell(button).getColumn() + 1;

			container.row();
			message.setAlignment(Align.center);
			container.add(message).expandX().fillX().colspan(c).center();

			scrolltable = new Table();
			scroll = new ScrollPane(scrolltable, skin);
			scroll.setColor(Color.DARK_GRAY);
			scroll.setFadeScrollBars(false);
			scroll.setSmoothScrolling(true);
			container.row();
			container.add(scroll).expand().fill().colspan(c);
			stage.setScrollFocus(scroll);
			stage.setKeyboardFocus(scroll);
			requestScores();
		}

	}

	private void requestScores() {
		if (BoundPronouns.getPrefs().getBoolean(BoundPronouns.GooglePlayLogginIn,
				false)) {
			BoundPronouns.services.lb_getListFor(BoardId, lb_collection, ts,
					success_show_scores, noop_error);
			message.setText("Loading ...");
		} else {
			success_show_scores.setData(new GameScores());
			Gdx.app.postRunnable(success_show_scores);
		}
	}
}
