package com.cherokeelessons.bp;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.SlotInfo.SessionLength;
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

	private Table topScoresPublic;
	private Table topScoresCircle;

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
	protected Callback<GameScores> success_populate_circle = new Callback<GameScores>() {
		@Override
		public void run() {
			GameScores data = getData();
			Table table = topScoresCircle;

			table.clear();
			table.defaults().expandX().fillX();
			LabelStyle ls = new LabelStyle(game.getFont(Font.SerifMedium),
					Color.BLACK);
			for (GameScore score : data.list) {
				table.row();
				table.add(new Label(score.rank, ls));
				table.add(new Label(score.value, ls));
				table.add(new Label(score.tag, ls));
			}

		}
	};
	public Callback<GameScores> success_populate_public = new Callback<GameScores>() {
		@Override
		public void run() {
			GameScores data = getData();
			Table table = topScoresPublic;

			LabelStyle ls = new LabelStyle(game.getFont(Font.SerifLarge),
					Color.BLACK);
			
			table.clear();
			table.defaults().expandX().fillX();
			
//			table.add().width(50).height(50).fill(false).expand(false, false).padLeft(15).padRight(15);
			table.add(new Label("Rank", ls));
			table.add(new Label("Score", ls));
			table.add(new Label("Skill Level", ls));
			table.add(new Label("Display Name", ls));
			
			String httpMethod = Net.HttpMethods.GET;
			HttpRequest httpRequest = new HttpRequest(httpMethod);
			
//			Gdx.files.getLocalStoragePath();

			for (GameScore score : data.list) {
				table.row();
				httpRequest.setUrl(score.imgUrl);
				httpRequest.setContent(null);
//				final Image avatar = new Image(); 
//				Gdx.net.sendHttpRequest(httpRequest,
//						new HttpResponseListener() {
//							@Override
//							public void handleHttpResponse(
//									HttpResponse httpResponse) {								
//								final byte[] rawImageBytes = httpResponse
//										.getResult();
//								Gdx.app.postRunnable(new Runnable() {
//									public void run() {
//										Pixmap pixmap = new Pixmap(
//												rawImageBytes, 0,
//												rawImageBytes.length);
//										Texture texture = new Texture(pixmap);
//										avatar.setDrawable(new TextureRegionDrawable(new TextureRegion(texture)));
//									}
//								});
//							}
//
//							@Override
//							public void failed(Throwable t) {
//								// TODO Auto-generated method stub
//
//							}
//
//							@Override
//							public void cancelled() {
//								// TODO Auto-generated method stub
//
//							}
//
//						});
//
//				table.add(avatar).width(50).height(50).fill(false).expand(false, false).padLeft(20).padRight(10);
//				table.add(new Label(score.imgUrl, ls));
				table.add(new Label(score.rank, ls));
				table.add(new Label(score.value, ls));
				table.add(new Label(score.tag, ls));
				table.add(new Label(StringUtils.substringBeforeLast(score.user,
						" "), ls));
			}

			BoundPronouns.services.lb_getListFor(BoardId, Collection.SOCIAL,
					success_populate_circle, noop_error);

			scrolltable.clear();
			scrolltable.add(table).expand().fill();
		}
	};
	public Callback<Exception> noop_error = new Callback<Exception>() {
		@Override
		public void run() {
			Gdx.app.log("Google Play Leaderboard Error: ", getData()
					.getMessage());
		}
	};

	private static final String BoardId = SessionLength.Standard.getId();

	private class InitView implements Runnable {
		@Override
		public void run() {
			TextButton button;

			TextButtonStyle tbs = skin.get(TextButtonStyle.class);
			tbs.checkedFontColor = Color.BLUE;
			tbs.font = game.getFont(Font.SerifSmall);

			button = new TextButton(BoundPronouns.BACK_ARROW, tbs);
			container.add(button).center().top()
					.width(BoundPronouns.BACK_WIDTH);
			button.addListener(exit);

			button = new TextButton("Top Public Scores", tbs);
			button.setChecked(true);
			container.add(button).center().top().expandX().fillX();

			button = new TextButton("Top Circle Scores", tbs);
			button.setChecked(false);
			container.add(button).center().top().expandX().fillX();

			button = new TextButton(ts.toString(), tbs);
			button.setChecked(false);
			container.add(button).center().top().expandX().fillX();

			button = new TextButton("Google Play Setup", tbs);
			button.setChecked(false);
			container.add(button).center().top().expandX().fillX();

			final int c = container.getCell(button).getColumn() + 1;

			LabelStyle ls = new LabelStyle(game.getFont(Font.SerifSmall),
					Color.BLACK);
			message = new Label("...", ls);
			container.row();
			container.add(message).expandX().fillX().colspan(c).left()
					.padLeft(20);

			scrolltable = new Table();
			scroll = new ScrollPane(scrolltable, skin);
			scroll.setColor(Color.DARK_GRAY);
			scroll.setFadeScrollBars(false);
			scroll.setSmoothScrolling(true);
			container.row();
			container.add(scroll).expand().fill().colspan(c);

			topScoresCircle = new Table();
			topScoresPublic = new Table();

			if (BoundPronouns.getPrefs().getBoolean(
					BoundPronouns.GoogleLoginPref, true)) {
				BoundPronouns.services.lb_getListFor(BoardId,
						Collection.PUBLIC, success_populate_public, noop_error);
			}
		}
	}
}
