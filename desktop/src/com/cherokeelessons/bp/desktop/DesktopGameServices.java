package com.cherokeelessons.bp.desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.util.GooglePlayGameServices;
import com.cherokeelessons.util.GooglePlayGameServices.GameAchievements.GameAchievement;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
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

public class DesktopGameServices implements GooglePlayGameServices {
	private File DATA_STORE_DIR;
	private Credential credential;
	private FileDataStoreFactory dataStoreFactory;
	private NetHttpTransport httpTransport;
	private FileHandle p0;
	private static final JacksonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	private Boolean initdone = false;

	private static void postRunnable(Runnable runnable) {
		if (runnable == null) {
			Gdx.app.log("DesktopGameServices", "NULL CALLBACK!");
			return;
		}
		Gdx.app.postRunnable(runnable);
	}

	public DesktopGameServices() {

	}

	private void init() {
		synchronized (initdone) {
			if (initdone) {
				return;
			}
			String path0 = ".config/CherokeeBoundPronouns/GooglePlayGameServices/";
			p0 = Gdx.files.external(path0);
			p0.mkdirs();
			DATA_STORE_DIR = p0.file();
			System.out.println("DATA STORE DIR: "
					+ DATA_STORE_DIR.getAbsolutePath());
			initdone = true;
		}
	}

	private Credential authorize() throws IOException {
		// load client secrets
		/*
		 * The JSON is a direct download from
		 * https://console.developers.google.com/ for
		 * "Client ID for native application"
		 */
		GoogleAuthorizationCodeFlow flow = getFlow();

		// authorize
		Credential authorize = null;

		authorize = new AuthorizationCodeInstalledApp(flow,
				new LocalServerReceiver()).authorize("user");

		authorize.refreshToken();
		return authorize;
	}

	public GoogleAuthorizationCodeFlow getFlow() throws IOException {
		InputStream in=null;
		InputStreamReader is=null;
		try {

			GoogleClientSecrets clientSecrets = null;

			File f = Gdx.files.internal("google.json").file();
			in = new FileInputStream(f);
			is = new InputStreamReader(in, "UTF-8");

			clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, is);

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
		} finally {
			in.close();
			is.close();
		}
	}

	@Override
	public void login(final Callback<Void> success,
			final Callback<Exception> error) {
		new Thread(new Runnable() {
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
		}).start();
	}

	private void _login() throws GeneralSecurityException, IOException {
		init();
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
		credential = authorize();
	}

	@Override
	public void logout(final Callback<Void> success,
			final Callback<Exception> error) {
		new Thread(new Runnable() {
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
		}).start();
	}

	@Override
	public void lb_submit(final String boardId, final long score,
			final String label, final Callback<Void> success,
			final Callback<Exception> error) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Submit submit = g.scores().submit(boardId, score);
					String tag = URLEncoder.encode(label, "UTF-8");
					submit.setScoreTag(tag);
				} catch (IOException | GeneralSecurityException e) {
					e.printStackTrace();
				}
				postRunnable(success);
			}
		}).start();
	}

	@Override
	public void lb_getScoresFor(final String boardId,
			final Callback<GameScores> success, final Callback<Exception> error) {
		new Thread(new Runnable() {
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
		new Thread(new Runnable() {
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
		}).start();
	}

	@Override
	public void lb_getListWindowFor(final String boardId,
			final Collection collection, final TimeSpan ts,
			final Callback<GameScores> success, final Callback<Exception> error) {
		new Thread(new Runnable() {
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
		}).start();
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
		new Thread(new Runnable() {
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
		}).start();
	}

	@Override
	public void ach_unlocked(final String id, final Callback<Void> success,
			final Callback<Exception> error) {
		new Thread(new Runnable() {
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
		}).start();
	}

	@Override
	public void ach_list(final Callback<GameAchievements> success,
			final Callback<Exception> error) {
		new Thread(new Runnable() {
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
