package com.cherokeelessons.bp;

import org.apache.commons.lang3.text.WordUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import com.cherokeelessons.util.GooglePlayGameServices.GameScores;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;

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
		initView();
	}

	private TiledDrawable d() {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		return new TiledDrawable(new TextureRegion(texture));
	}

	public FileHandle p0;

	public String[] ranks = { "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th" };

	public Callback<GameScores> success_show_scores = new Callback<GameScores>() {
		@Override
		public void success(GameScores data) {
			Gdx.app.log(this.getClass().getName(), "Scores received.");

			if (data == null) {
				message.setText("No scores for display");
				return;
			}
			message.setText("Top Scores");

			Table table = scrolltable;

			table.clear();
			table.defaults().expandX();
			String text = "Rank";
			table.add(new Label(text, new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).padLeft(15)
					.padRight(15).center();
			text = "Score";
			table.add(new Label(text, new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).center();
			text = "Skill Level";
			table.add(new Label(text, new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).center();
			text = "Display Name";
			table.add(new Label(text, new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).center();

			if (data.list.size()>100) {
				data.list.subList(100, data.list.size()).clear();
			}
			
			int c=0;
			for (GameScore score : data.list) {
				c++;
				Gdx.app.log(this.getClass().getSimpleName(), "doing label for score: "+c);
				table.row();
				table.add(new Label(score.rank, new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK)))
						.padLeft(15).padRight(15).center();
				table.add(new Label(score.value, new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).right()
						.padRight(30);
				table.add(new Label(score.tag, new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).center();
				table.add(new Label(score.user, new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).center();
			}

			for (int ix = data.list.size(); ix < ranks.length; ix++) {
				table.row();
				table.add(new Label(ranks[ix], new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).padLeft(15)
						.padRight(15).center();
				table.add(new Label("0", new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).right()
						.padRight(30);
				table.add(new Label("Newbie", new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).center();
				table.add(new Label("", new LabelStyle(game.getFont(Font.SerifMedium), Color.BLACK))).center();
			}
		}
	};

	private static final float bwidth = 84f;

	private void initView() {
		final TextButtonStyle tbs = skin.get(TextButtonStyle.class);
		tbs.font = game.getFont(Font.SerifXSmall);

		ButtonGroup<TextButton> bgroup = new ButtonGroup<TextButton>();
		bgroup.setMaxCheckCount(1);
		bgroup.setMinCheckCount(1);

		final TextButton back_button = new TextButton(BoundPronouns.BACK_ARROW, tbs);
		back_button.addListener(exit);

		final TextButton sync_button;
		if (!BoundPronouns.services.isLoggedIn()) {
			sync_button = new TextButton("Login to Sync", tbs);
		} else {
			sync_button = new TextButton("Logout of Sync", tbs);
		}

		final WindowStyle dws = new WindowStyle(skin.get(WindowStyle.class));
		final LabelStyle dls = new LabelStyle(skin.get(LabelStyle.class));
		dws.titleFont = game.getFont(Font.SerifLarge);
		dls.font = game.getFont(Font.SerifMedium);
		final Dialog login = new Dialog("Sync Service", dws);
		login.getTitleLabel().setAlignment(Align.center);
		login.text(new Label("Connecting to Sync Service ...", dls));
		login.button(new TextButton("DISMISS", tbs));
		login.getTitleLabel().setAlignment(Align.center);

		final Dialog[] error = new Dialog[1];
		error[0] = errorDialog(new Exception(""), null);
		sync_button.addListener(new ClickListener() {
			Callback<Void> success_in = new Callback<Void>() {
				@Override
				public void success(Void result) {
					error[0].hide();
					login.hide();
					sync_button.setText("Logout of Sync");
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
			Callback<Void> success_out = new Callback<Void>() {
				@Override
				public void success(Void result) {
					error[0].hide();
					login.hide();
					sync_button.setText("Login to Sync");
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
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (BoundPronouns.services.isLoggedIn()) {
					BoundPronouns.services.logout(success_out);
				} else {
					login.show(stage);
					BoundPronouns.services.login(success_in);
				}
				return true;
			}
		});

		LabelStyle ls = new LabelStyle(game.getFont(Font.SerifXSmall), Color.BLACK);
		message = new Label("...", ls);
		message.setAlignment(Align.center);

		container.add(back_button).left().top().width(bwidth);
		container.add(message).expandX().fillX().center();
		container.add(sync_button).right().top();
		container.row();

		int c = container.getColumns();
		scrolltable = new Table();
		scroll = new ScrollPane(scrolltable, skin);
		scroll.setColor(Color.DARK_GRAY);
		scroll.setFadeScrollBars(false);
		scroll.setSmoothScrolling(true);
		container.add(scroll).expand().fill().colspan(c);
		stage.setScrollFocus(scroll);
		stage.setKeyboardFocus(scroll);
		requestScores();
	}

	private void requestScores() {
		if (lb != null) {
			message.setText("Loading ...");
			lb.lb_getListFor(null, null, null, success_show_scores);
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
				if (done != null) {
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

	@Override
	public void render(float delta) {
		try {
			super.render(delta);
		} catch (Exception e) {
			e.printStackTrace();
			scrolltable.clear();
			scrolltable.remove();
			container.clear();
			container.remove();
			stage.clear();
			doExit.run();
		}
	}
}
