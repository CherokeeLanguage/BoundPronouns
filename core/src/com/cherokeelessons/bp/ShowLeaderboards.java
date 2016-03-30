package com.cherokeelessons.bp;

import org.apache.commons.lang3.text.WordUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.util.DreamLo;
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
	private final DreamLo lb;

	public ShowLeaderboards(BoundPronouns game, Screen caller) {
		super(game, caller);
		skin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));
		container = new Table(skin);
		container.setBackground(d());
		container.setFillParent(true);
		stage.addActor(container);
		this.lb = new DreamLo(BoundPronouns.getPrefs());
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

	private TimeSpan ts = TimeSpan.DAILY;
	public FileHandle p0;

	public String[] ranks = { "1st", "2nd", "3rd", "4th", "5th", "6th", "7th",
			"8th", "9th", "10th" };

	public Callback<GameScores> success_show_scores = new Callback<GameScores>() {
		@Override
		public void success(GameScores data) {
			Gdx.app.log(this.getClass().getName(), "Scores received.");
			if (data==null) {
				message.setText("No scores for display");
				return;
			}
			if (data.collection==null) {
				data.collection=lb_collection;
			}
			if (data.ts==null) {
				data.ts=ts;
			}
//			if (data.collection.equals(Collection.PUBLIC)) {
//				message.setText(data.ts.getEngrish() + " Top Public Scores");
//			}
//			if (data.collection.equals(Collection.SOCIAL)) {
//				message.setText(data.ts.getEngrish() + " Top Circle Scores");
//			}
			message.setText("Top Local Scores");

			Table table = scrolltable;

			LabelStyle ls = new LabelStyle(game.getFont(Font.SerifMedium),
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
			
			if (!BoundPronouns.services.isLoggedIn()) {
//				message.setText("You must login for Leaderboards");
			}
		}
	};

	public static final String LeaderBoardId = "CgkI4pfA4J4KEAIQDA";

	public Collection lb_collection = Collection.PUBLIC;

	private static final float bwidth = 84f;
	private class InitView implements Runnable {
		@Override
		public void run() {
			TextButton button;

			final TextButtonStyle tbs = skin.get(TextButtonStyle.class);
			tbs.font = game.getFont(Font.SerifXSmall);

			ButtonGroup<TextButton> bgroup = new ButtonGroup<TextButton>();
			bgroup.setMaxCheckCount(1);
			bgroup.setMinCheckCount(1);

			button = new TextButton(BoundPronouns.BACK_ARROW, tbs);
			container.add(button).center().top()
					.width(bwidth);
			button.addListener(exit);

			button = new TextButton("Show "+ts.next().getEngrish(), tbs);
			button.setChecked(false);
			container.add(button).center().top().expandX().fillX();
			final TextButton ts_button = button;

//			button = new TextButton("Show "+lb_collection.next().getEnglish(), tbs);
//			button.setChecked(true);
//			container.add(button).center().top().expandX().fillX();
//			bgroup.add(button);
//			final TextButton lb_button = button;
//			lb_button.addListener(new ClickListener() {
//				@Override
//				public boolean touchDown(InputEvent event, float x, float y,
//						int pointer, int button) {
//					lb_collection = lb_collection.next();
//					lb_button.setText("Show "+lb_collection.next().getEnglish());
//					requestScores();
//					return true;
//				}
//			});
			ts_button.addListener(new ClickListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {
					ts = ts.next();
					float width = ts_button.getLabel().getWidth();
					ts_button.setText("Show "+ts.next().getEngrish());
					ts_button.getLabel().setWidth(width);
					requestScores();
					return true;
				}
			});
			
			LabelStyle ls = new LabelStyle(game.getFont(Font.SerifXSmall),
					Color.BLACK);
			message = new Label("...", ls);

			if (!BoundPronouns.services.isLoggedIn()) {
				button = new TextButton("Login to Sync", tbs);
//				message.setText("You must login for Leaderboards");
			} else {
				button = new TextButton("Logout of Sync", tbs);
			}
			
			final WindowStyle dws=new WindowStyle(skin.get(WindowStyle.class));
			final LabelStyle dls=new LabelStyle(skin.get(LabelStyle.class));
			dws.titleFont=game.getFont(Font.SerifLarge);
			dls.font=game.getFont(Font.SerifMedium);
			final TextButton play_button = button;
			final Dialog login = new Dialog("Sync Service", dws);
			login.getTitleLabel().setAlignment(Align.center);
			login.text(new Label("Connecting to Sync Service ...", dls));
			login.button(new TextButton("DISMISS", tbs));
			login.getTitleLabel().setAlignment(Align.center);
			
			final Dialog[] error = new Dialog[1];
			error[0]=errorDialog(new Exception(""), null);
			play_button.addListener(new ClickListener(){				
				Callback<Void> success_in=new Callback<Void>() {							
					@Override
					public void success(Void result) {
						error[0].hide();
						login.hide();
						requestScores();
						play_button.setText("Logout of Sync");
					}
					@Override
					public void error(Exception e) {
						error[0].hide();
						login.hide();
						success_out.withNull().run();
						error[0] = errorDialog(e, null);
						error[0].show(stage);
					}
				};
				Callback<Void> success_out=new Callback<Void>() {							
					@Override
					public void success(Void result) {
						error[0].hide();
						login.hide();
						requestScores();
						play_button.setText("Login to Sync");
					}
					@Override
					public void error(Exception exception) {
						error[0].hide();
						login.hide();
						success_out.withNull().run();
						error[0] = errorDialog(exception, null);
						error[0].show(stage);
					}
				};
				@Override
				public boolean touchDown(InputEvent event, float x, float y,
						int pointer, int button) {					
					if (BoundPronouns.services.isLoggedIn()) {
						BoundPronouns.services.logout(success_out);
					} else {
						login.show(stage);
						BoundPronouns.services.login(success_in);
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
		if (lb!=null) {
			lb.lb_getListFor(LeaderBoardId, lb_collection, ts, success_show_scores);
			return;
		}
		if (BoundPronouns.services.isLoggedIn()) {
			BoundPronouns.services.lb_getListFor(LeaderBoardId, lb_collection, ts,
					success_show_scores);
			message.setText("Loading ...");
		} else {
			Gdx.app.postRunnable(success_show_scores.with(new GameScores()));
		}
	}
	
	private Dialog errorDialog(final Exception e, final Runnable done) {
		WindowStyle dws;
		LabelStyle dls;
		TextButtonStyle tbs;
		
		dws = new WindowStyle(skin.get(WindowStyle.class));
		dls = new LabelStyle(skin.get(LabelStyle.class));
		dws.titleFont = game.getFont(Font.SerifLarge);
		dls.font = game.getFont(Font.SerifMedium);
		tbs = new TextButtonStyle(skin.get(TextButtonStyle.class));
		tbs.font = game.getFont(Font.SerifXSmall);

		Dialog error = new Dialog("Sync Service", dws) {
			@Override
			protected void result(Object object) {
				if (done!=null) {
					Gdx.app.postRunnable(done);
				}
			}
		};
		error.getTitleLabel().setAlignment(Align.center);
		error.button(new TextButton("OK", tbs));
		String msgtxt = e.getMessage();
		msgtxt = WordUtils.wrap(msgtxt, 45, "\n", true);
		Label label = new Label(msgtxt, dls);
		label.setAlignment(Align.center);
		error.text(label);
		error.setKeepWithinStage(true);
		return error;
	}
}
