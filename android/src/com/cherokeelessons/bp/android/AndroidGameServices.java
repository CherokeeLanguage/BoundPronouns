package com.cherokeelessons.bp.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.net.http.SslError;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.bp.BoundPronouns;
import com.cherokeelessons.util.GooglePlayGameServices;
import com.cherokeelessons.util.GooglePlayGameServices.GameAchievements.GameAchievement;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.games.Games;
import com.google.api.services.games.Games.Achievements;
import com.google.api.services.games.Games.Scores;
import com.google.api.services.games.Games.Scores.Submit;
import com.google.api.services.games.GamesScopes;
import com.google.api.services.games.model.LeaderboardEntry;
import com.google.api.services.games.model.LeaderboardScores;
import com.google.api.services.games.model.PlayerAchievement;
import com.google.api.services.games.model.PlayerAchievementListResponse;
import com.google.api.services.games.model.PlayerLeaderboardScore;
import com.google.api.services.games.model.PlayerLeaderboardScoreListResponse;

public class AndroidGameServices implements GooglePlayGameServices {
	private File DATA_STORE_DIR;
	private Credential credential;
	private FileDataStoreFactory dataStoreFactory;
	private HttpTransport httpTransport;
	private FileHandle p0;
	private static final JacksonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	private Boolean initdone = false;

	final private AndroidApplication application;

	private static void postRunnable(Runnable runnable) {
		if (runnable == null) {
			Gdx.app.log("DesktopGameServices", "NULL CALLBACK!");
			return;
		}
		Gdx.app.postRunnable(runnable);
	}

	VerificationCodeReceiver receiver = new VerificationCodeReceiver() {
		@Override
		public String waitForCode() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void stop() throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public String getRedirectUri() throws IOException {
			return "urn:ietf:wg:oauth:2.0:oob:auto";
		}
	};

	public static class AuthorizationCodeInstalledAndroidApp extends
			AuthorizationCodeInstalledApp {

		public static void browse(String url) {

		}

		public AuthorizationCodeInstalledAndroidApp(AuthorizationCodeFlow flow,
				VerificationCodeReceiver receiver) {
			super(flow, receiver);
		}

	}

	@SuppressLint("SetJavaScriptEnabled")
	private void webViewLogin(final String url) {
		final WebView webView = new WebView(application) {
			@Override
			public boolean onCheckIsTextEditor() {
				return true;
			}
		};
		WebSettings settings = webView.getSettings();
		webView.setVisibility(View.GONE);
		settings.setBuiltInZoomControls(true);
		settings.setDefaultTextEncodingName("UTF-8");
		settings.setJavaScriptEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(true);
		settings.setLoadsImagesAutomatically(true);
		settings.setSaveFormData(true);
		settings.setUseWideViewPort(false);

		webView.requestFocus(View.FOCUS_DOWN);
		webView.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
					if (!v.hasFocus()) {
						v.requestFocus();
					}
					break;
				}
				return false;
			}
		});

		final Builder alert = new AlertDialog.Builder(application);
		alert.setView(webView);

		webView.setVisibility(View.VISIBLE);
		webView.setInitialScale(800);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				Gdx.app.log("AndroidGameServices#onReceivedError", "["
						+ errorCode + "] " + description);
			}

			@Override
			public void onReceivedSslError(WebView view,
					SslErrorHandler handler, SslError error) {
				super.onReceivedSslError(view, handler, error);
				Gdx.app.log("AndroidGameServices#onReceivedSslError",
						error.toString());
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				String title = view.getTitle();
				if (title.contains("denied")) {

				}
				Gdx.app.log("AndroidGameServices#onPageFinished", title);
				if (url.contains("xxxxx")) { // OAuth2ClientCredentials.OAUTH_CALLBACK_URL))
					webView.loadUrl("about:blank");
					webView.setVisibility(View.INVISIBLE);
					application.setContentView(((AndroidGraphics) application
							.getGraphics()).getView());
					try {

						if (url.indexOf("code=") != -1) {
							String code = StringUtils.substringBetween(url,
									"code=", "&");

							AuthorizationCodeInstalledAndroidApp acia = new AuthorizationCodeInstalledAndroidApp(
									getFlow(), receiver);
							acia.authorize("user");

						}

					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				System.out.println("onPageFinished : " + url);

			}
		});

		alert.setTitle("Google Play Services");

		alert.setNegativeButton("CLOSE", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		webView.loadUrl(url);
		alert.show();
	}

	@SuppressLint("SetJavaScriptEnabled")
	public AndroidGameServices(AndroidApplication application) {
		this.application = application;
	}

	private void init() {
		synchronized (initdone) {
			if (initdone) {
				return;
			}
			String path0 = ".config/CherokeeBoundPronouns/GooglePlayGameServices/";
			p0 = Gdx.files.local(path0);
			p0.mkdirs();
			DATA_STORE_DIR = p0.file();
			System.out.println("DATA STORE DIR: "
					+ DATA_STORE_DIR.getAbsolutePath());
			initdone = true;
		}
	}

	public GoogleAuthorizationCodeFlow getFlow() throws IOException {
		GoogleClientSecrets clientSecrets = null;

		clientSecrets = GoogleClientSecrets.load(
				JSON_FACTORY,
				new InputStreamReader(BoundPronouns.class
						.getResourceAsStream("/client_secrets.json")));

		ArrayList<String> scopes = new ArrayList<String>();
		scopes.add(GamesScopes.DRIVE_APPDATA);
		scopes.add(GamesScopes.GAMES);
		scopes.add(GamesScopes.PLUS_LOGIN);

		GoogleAuthorizationCodeFlow.Builder builder = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, JSON_FACTORY, clientSecrets, scopes);
		builder.setScopes(scopes);
		GoogleAuthorizationCodeFlow flow = null;
		try {
			flow = builder.setAccessType("offline")
					.setDataStoreFactory(dataStoreFactory).build();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return flow;
	}

	@Override
	public void login(final Callback<Void> success,
			final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					_login();
				} catch (GeneralSecurityException | IOException e) {
					error.setData(e);
					postRunnable(error);
					return;
				}
				postRunnable(success);
			}
		});
	}

	private void _login() throws GeneralSecurityException, IOException {
		init();
		httpTransport = AndroidHttp.newCompatibleTransport();
		dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
		GoogleAuthorizationCodeFlow flow = getFlow();
		final GoogleAuthorizationCodeRequestUrl url = flow
				.newAuthorizationUrl();
		webViewLogin(url.build());

		credential = null;
	}

	@Override
	public void logout(final Callback<Void> success,
			final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					init();
					GoogleAuthorizationCodeFlow flow;
					flow = getFlow();
					flow.getCredentialDataStore().clear();
				} catch (IOException e) {
					error.setData(e);
					postRunnable(error);
					return;
				}
				postRunnable(success);
			}
		});
	}

	@Override
	public void lb_submit(final String boardId, final long score,
			final String label, final Callback<Void> success,
			final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Submit submit = g.scores().submit(boardId, score);
					String tag = URLEncoder.encode(label, "UTF-8");
					submit.setScoreTag(tag);
					submit.execute();
				} catch (IOException | GeneralSecurityException e) {
					e.printStackTrace();
				}
				postRunnable(success);
			}
		});
	}

	@Override
	public void lb_getScoresFor(final String boardId,
			final Callback<GameScores> success, final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				GameScores gscores = new GameScores();
				try {
					Games g = _getGamesObject();
					Scores.Get scores = g.scores().get("me", boardId,
							TimeSpan.ALL_TIME.name());
					scores.setMaxResults(30);
					PlayerLeaderboardScoreListResponse result = scores
							.execute();
					List<PlayerLeaderboardScore> list = result.getItems();
					for (PlayerLeaderboardScore e : list) {
						GameScore gs = new GameScore();
						gs.rank = "";
						gs.tag = URLDecoder.decode(e.getScoreTag(), "UTF-8");
						gs.value = e.getScoreString();
						gs.user = "";
						gscores.list.add(gs);
					}
				} catch (IOException | GeneralSecurityException e) {
					e.printStackTrace();
				}
				success.setData(gscores);
				postRunnable(success);
			}
		});
	}

	@Override
	public void lb_getListFor(final String boardId,
			final Collection collection, final TimeSpan ts,
			final Callback<GameScores> success, final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				GameScores gscores = new GameScores();
				try {
					Gdx.app.log("DesktopGameServices", "Loading Leaderboard: "
							+ collection.name() + " - " + ts.name() + " - "
							+ boardId);

					Games g = _getGamesObject();
					Scores.List scores = g.scores().list(boardId,
							collection.name(), ts.toString());
					scores.setMaxResults(30);
					LeaderboardScores result = scores.execute();
					List<LeaderboardEntry> list = result.getItems();
					if (list == null) {
						success.setData(gscores);
						postRunnable(success);
						return;
					}
					for (LeaderboardEntry e : list) {
						GameScore gs = new GameScore();
						gs.rank = e.getFormattedScoreRank();
						gs.tag = URLDecoder.decode(e.getScoreTag(), "UTF-8");
						gs.value = e.getFormattedScore();
						gs.user = e.getPlayer().getDisplayName();
						gs.imgUrl = e.getPlayer().getAvatarImageUrl();
						gscores.list.add(gs);
					}
					gscores.collection = collection;
					gscores.ts = ts;
				} catch (IOException | GeneralSecurityException e) {
					e.printStackTrace();
				}
				success.setData(gscores);
				postRunnable(success);
			}
		});
	}

	@Override
	public void lb_getListWindowFor(final String boardId,
			final Collection collection, final TimeSpan ts,
			final Callback<GameScores> success, final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				GameScores gscores = new GameScores();
				try {
					Games g = _getGamesObject();
					Scores.ListWindow scores = g.scores().listWindow(boardId,
							collection.name(), ts.name());
					scores.setMaxResults(30);
					LeaderboardScores result = scores.execute();
					List<LeaderboardEntry> list = result.getItems();
					for (LeaderboardEntry e : list) {
						GameScore gs = new GameScore();
						gs.rank = e.getFormattedScoreRank();
						gs.tag = URLDecoder.decode(e.getScoreTag(), "UTF-8");
						gs.value = e.getFormattedScore();
						gs.user = e.getPlayer().getDisplayName();
						gs.imgUrl = e.getPlayer().getAvatarImageUrl();
						gscores.list.add(gs);
					}
					gscores.collection = collection;
					gscores.ts = ts;
				} catch (IOException | GeneralSecurityException e) {
					error.setData(e);
					postRunnable(error);
					return;
				}
				success.setData(gscores);
				postRunnable(success);
			}
		});
	}

	private Games _getGamesObject() throws GeneralSecurityException,
			IOException {
		_login();
		Games.Builder b = new Games.Builder(httpTransport, JSON_FACTORY,
				credential);
		b.setApplicationName("Cherokee Bound Pronouns/1.0");
		Games g = b.build();
		return g;
	}

	@Override
	public void ach_reveal(final String id, final Callback<Void> success,
			final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					ac.reveal(id).execute();
				} catch (IOException | GeneralSecurityException e) {
					error.setData(e);
					postRunnable(error);
					return;
				}
				postRunnable(success);
			}
		});
	}

	@Override
	public void ach_unlocked(final String id, final Callback<Void> success,
			final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					ac.unlock(id).execute();
				} catch (IOException | GeneralSecurityException e) {
					error.setData(e);
					postRunnable(error);
					return;
				}
				postRunnable(success);
			}
		});
	}

	@Override
	public void ach_list(final Callback<GameAchievements> success,
			final Callback<Exception> error) {
		application.handler.post(new Runnable() {
			@Override
			public void run() {
				GameAchievements results = new GameAchievements();
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					PlayerAchievementListResponse response = ac.list("me")
							.execute();
					List<PlayerAchievement> list = response.getItems();
					for (PlayerAchievement pa : list) {
						GameAchievement a = new GameAchievement();
						a.id = pa.getId();
						a.state = pa.getAchievementState();
						results.list.add(a);
					}
				} catch (IOException | GeneralSecurityException e) {
					error.setData(e);
					postRunnable(error);
					return;
				}
				success.setData(results);
				postRunnable(success);
			}
		});
	}
}
