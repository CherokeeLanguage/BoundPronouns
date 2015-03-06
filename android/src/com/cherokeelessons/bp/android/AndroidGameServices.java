package com.cherokeelessons.bp.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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
import android.content.DialogInterface.OnDismissListener;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.bp.BoundPronouns;
import com.cherokeelessons.util.GooglePlayGameServices;
import com.cherokeelessons.util.GooglePlayGameServices.GameAchievements.GameAchievement;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
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

@SuppressLint("DefaultLocale")
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

	public static class CodeReceiver implements VerificationCodeReceiver {
		private String code = "";

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		@Override
		public String getRedirectUri() throws IOException {
			return "urn:ietf:wg:oauth:2.0:oob:auto";
		}

		@Override
		public String waitForCode() throws IOException {
			return code;
		}

		@Override
		public void stop() throws IOException {
		}

	}

	CodeReceiver codeReceiver = new CodeReceiver();

	@SuppressLint("SetJavaScriptEnabled")
	private void webViewLogin(final String url) {
		Gdx.app.log("AndroidGameServices", "webViewLogin");
		application.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final Builder alert = new AlertDialog.Builder(application);
				alert.setTitle("Google Play Services");
				alert.setNegativeButton("CLOSE",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				Gdx.app.log("AndroidGameServices", "alert.show();");
				final WebView webView = new WebView(application) {
					@Override
					public boolean onCheckIsTextEditor() {
						return true;
					}
				};
				alert.setView(webView);
				alert.setCancelable(true);
				final AlertDialog adialog = alert.show();
				final OnDismissListener listener = new OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						webView.loadUrl("about:blank");
						if (codeReceiver.getCode()==null) {
							codeReceiver.setCode("");
						}
					}
				};
				adialog.setOnDismissListener(listener);
				webView.loadUrl("about:blank");

				application.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						WebSettings settings = webView.getSettings();
						settings.setBuiltInZoomControls(false);
						settings.setDefaultTextEncodingName("UTF-8");
						settings.setJavaScriptEnabled(true);
						settings.setJavaScriptCanOpenWindowsAutomatically(true);
						settings.setLoadsImagesAutomatically(true);
						settings.setSaveFormData(true);
						settings.setUseWideViewPort(false);

						settings.setFixedFontFamily("FreeMono");
						settings.setSansSerifFontFamily("FreeSans");
						settings.setSerifFontFamily("FreeSerif");
						settings.setStandardFontFamily("FreeSerif");
						settings.setLoadWithOverviewMode(true);

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

						webView.setWebViewClient(new WebViewClient() {
							@Override
							public void onReceivedError(WebView view,
									int errorCode, String description,
									String failingUrl) {
								super.onReceivedError(view, errorCode,
										description, failingUrl);
								Gdx.app.log(
										"AndroidGameServices#onReceivedError",
										"[" + errorCode + "] " + description);
							}

							@Override
							public void onReceivedSslError(WebView view,
									SslErrorHandler handler, SslError error) {
								super.onReceivedSslError(view, handler, error);
								Gdx.app.log(
										"AndroidGameServices#onReceivedSslError",
										error.toString());
							}

							@Override
							public void onPageFinished(WebView view, String url) {
								String title = StringUtils.defaultString(view
										.getTitle());
								if (title.toLowerCase().contains("denied")) {
									adialog.dismiss();
									codeReceiver.setCode("");
									return;
								}
								if (title.toLowerCase().contains("code=")) { // OAuth2ClientCredentials.OAUTH_CALLBACK_URL))
									webView.loadUrl("about:blank");
									String code = StringUtils.substringAfter(
											title, "code=");
									if (code.contains("&")) {
										code = StringUtils.substringBefore(
												code, "&");
									}
									if (StringUtils.isBlank(code)) {
										Gdx.app.log("AndroidGameServices",
												"Did not receive a code.");
										adialog.dismiss();
										codeReceiver.setCode("");
										return;
									}
									codeReceiver.setCode(code);
									Gdx.app.log("AndroidGameServices",
											"Received code: " + code);
									adialog.dismiss();
								}
							}
						});
						webView.loadUrl(url);
					}
				});
			}
		});
	}

	private void runTask(final Runnable runnable) {
		application.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						runnable.run();
						return null;
					}
				}.execute();
			}
		});
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

			httpTransport = AndroidHttp.newCompatibleTransport();
			try {
				dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("DATA STORE DIR: "
					+ DATA_STORE_DIR.getAbsolutePath());

			initdone = true;
		}
	}

	public GoogleAuthorizationCodeFlow getFlow() throws IOException {

		GoogleClientSecrets clientSecrets = null;

		String json = Gdx.files.internal("google.json").readString();

		StringReader sr = new StringReader(json);
		clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, sr);

		ArrayList<String> scopes = new ArrayList<String>();
		scopes.add(GamesScopes.DRIVE_APPDATA);
		scopes.add(GamesScopes.GAMES);
		scopes.add(GamesScopes.PLUS_LOGIN);

		GoogleAuthorizationCodeFlow.Builder builder = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, JSON_FACTORY, clientSecrets, scopes);
		builder.setScopes(scopes);
		GoogleAuthorizationCodeFlow flow = null;
		flow = builder.setAccessType("offline")
				.setDataStoreFactory(dataStoreFactory).build();
		return flow;

	}

	public static abstract class AsyncRun extends AsyncTask<Void, Void, Void>
			implements Runnable {
		@Override
		protected Void doInBackground(Void... params) {
			run();
			return null;
		}
	}

	@Override
	public void login(final Callback<Void> success) {
		runTask(new Runnable() {
			@Override
			public void run() {
				try {
					init();
					Callback<Credential> callback = new Callback<Credential>() {
						@Override
						public void run() {
							credential = getData();
						}
					};
					codeReceiver.code = null;
					credential = authorize(callback);
				} catch (RuntimeException | IOException e) {
					postRunnable(success.withError(e));
					return;
				}
				postRunnable(success);
			}
		});
	}

	private void _login() throws GeneralSecurityException, IOException {
		init();

		Callback<Credential> callback = new Callback<Credential>() {
			@Override
			public void run() {
				credential = getData();
			}
		};
		credential = authorize(callback);
	}

	private Credential authorize(final Callback<Credential> callback)
			throws IOException {
		GoogleAuthorizationCodeFlow flow = getFlow();
		Credential authorize = null;
		AuthorizationCodeInstalledApp acia = new AuthorizationCodeInstalledApp(
				flow, codeReceiver) {
			@Override
			public Credential authorize(final String userId) throws IOException {
				final AuthorizationCodeFlow flow = this.getFlow();
				final VerificationCodeReceiver receiver = this.getReceiver();
				try {
					Credential credential = flow.loadCredential(userId);
					if (credential != null
							&& (credential.getRefreshToken() != null || credential
									.getExpiresInSeconds() > 60)) {
						return credential;
					}
					// open in webview
					Gdx.app.log("AndroidGameServices", "Opening OAUTH Webview");
					final String redirectUri = receiver.getRedirectUri();
					AuthorizationCodeRequestUrl authorizationUrl = flow
							.newAuthorizationUrl().setRedirectUri(redirectUri);
					webViewLogin(authorizationUrl.build());
					long timeout = 1000l * 60l * 10l;// 10minutes;
					String code = null;
					while (timeout > 0) {
						code = receiver.waitForCode();
						if (code == null) {
							try {
								Thread.sleep(50l);
								timeout -= 50l;
								continue;
							} catch (InterruptedException e) {
								Gdx.app.log("AndroidGameServices",
										"Interrupted");
								return null;
							}
						}
						break;
					}
					code = receiver.waitForCode();
					TokenResponse response = flow.newTokenRequest(code)
							.setRedirectUri(redirectUri).execute();
					return flow.createAndStoreCredential(response, userId);
				} finally {
					receiver.stop();
				}
			}
		};
		authorize = acia.authorize("user");
		authorize.refreshToken();
		return authorize;
	}

	@Override
	public void logout(final Callback<Void> success) {
		runTask(new Runnable() {
			@Override
			public void run() {
				try {
					init();
					GoogleAuthorizationCodeFlow flow;
					flow = getFlow();
					flow.getCredentialDataStore().clear();
				} catch (IOException e) {
					postRunnable(success.withError(e));
					return;
				}
				postRunnable(success);
			}
		});
	}

	@Override
	public void lb_submit(final String boardId, final long score,
			final String label, final Callback<Void> success) {
		runTask(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Submit submit = g.scores().submit(boardId, score);
					String tag = URLEncoder.encode(label, "UTF-8");
					submit.setScoreTag(tag);
					submit.execute();
				} catch (IOException | GeneralSecurityException e) {
					postRunnable(success.withError(e));
					return;
				}
				postRunnable(success);
			}
		});
	}

	@Override
	public void lb_getScoresFor(final String boardId,
			final Callback<GameScores> success) {
		runTask(new Runnable() {
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
					postRunnable(success.withError(e));
					return;
				}
				success.setData(gscores);
				postRunnable(success);
			}
		});
	}

	@Override
	public void lb_getListFor(final String boardId,
			final Collection collection, final TimeSpan ts,
			final Callback<GameScores> success) {
		runTask(new Runnable() {
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
					postRunnable(success.withError(e));
					return;
				}
				success.setData(gscores);
				postRunnable(success);
			}
		});
	}

	@Override
	public void lb_getListWindowFor(final String boardId,
			final Collection collection, final TimeSpan ts,
			final Callback<GameScores> success) {
		runTask(new Runnable() {
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
					postRunnable(success.withError(e));
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
	public void ach_reveal(final String id, final Callback<Void> success) {
		runTask(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					ac.reveal(id).execute();
				} catch (IOException | GeneralSecurityException e) {
					postRunnable(success.withError(e));
					return;
				}
				postRunnable(success);
			}
		});
	}

	@Override
	public void ach_unlocked(final String id, final Callback<Void> success) {
		runTask(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					ac.unlock(id).execute();
				} catch (IOException | GeneralSecurityException e) {
					postRunnable(success.withError(e));
					return;
				}
				postRunnable(success);
			}
		});
	}

	@Override
	public void ach_list(final Callback<GameAchievements> success) {
		runTask(new Runnable() {
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
					postRunnable(success.withError(e));
					return;
				}
				success.setData(results);
				postRunnable(success);
			}
		});
	}
}
