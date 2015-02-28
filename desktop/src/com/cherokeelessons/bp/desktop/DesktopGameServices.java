package com.cherokeelessons.bp.desktop;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.bp.BoundPronouns;
import com.cherokeelessons.util.GooglePlayGameServices;
import com.cherokeelessons.util.GooglePlayGameServices.GameAchievements.GameAchievement;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
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
import com.google.api.services.games.model.AchievementRevealResponse;
import com.google.api.services.games.model.AchievementUnlockResponse;
import com.google.api.services.games.model.LeaderboardEntry;
import com.google.api.services.games.model.LeaderboardScores;
import com.google.api.services.games.model.PlayerAchievement;
import com.google.api.services.games.model.PlayerAchievementListResponse;
import com.google.api.services.games.model.PlayerLeaderboardScore;
import com.google.api.services.games.model.PlayerLeaderboardScoreListResponse;
import com.google.api.services.games.model.PlayerScoreResponse;

public class DesktopGameServices implements GooglePlayGameServices {
	private File DATA_STORE_DIR;
	private Credential credential;
	private FileDataStoreFactory dataStoreFactory;
	private NetHttpTransport httpTransport;
	private FileHandle p0;
	private static final JacksonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	private Boolean initdone = false;

	public DesktopGameServices() {

	}

	private void init() {
		synchronized (initdone) {
			if (initdone) {
				return;
			}
			String path0 = "BoundPronouns/GooglePlayGameServices/";
			p0 = Gdx.files.external(path0);
			p0.mkdirs();
			DATA_STORE_DIR = p0.child("datastore").file();
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
		try {
			authorize = new AuthorizationCodeInstalledApp(flow,
					new LocalServerReceiver()).authorize("user");
			authorize.refreshToken();
		} catch (TokenResponseException e) {
			e.printStackTrace();
			if (authorize != null) {
				Gdx.app.log(this.getClass().getName(),
						"Bad Tokens. Resetting and requesting new ...");
				flow.getCredentialDataStore().clear();
				return authorize();
			}
		} catch (Exception e) {
			Gdx.app.log(this.getClass().getName(), e.getClass().getName());
		}

		return authorize;
	}

	public GoogleAuthorizationCodeFlow getFlow() {
		GoogleClientSecrets clientSecrets = null;
		try {
			clientSecrets = GoogleClientSecrets.load(
					JSON_FACTORY,
					new InputStreamReader(BoundPronouns.class
							.getResourceAsStream("/client_secrets.json")));
		} catch (IOException e) {
			e.printStackTrace();
		}

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
		} catch (IOException e) {
			e.printStackTrace();
		}
		return flow;
	}

	@Override
	public void login(final Callback<Boolean> done) {
		new Thread(new Runnable() {			
			@Override
			public void run() {
				done.setData(_login());
				Gdx.app.postRunnable(done);
			}

			
		}).start();
	}
	
	private boolean _login() {
		init();
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			credential = authorize();
			return true;
		} catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void logout(final Callback<Void> callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				init();
				GoogleAuthorizationCodeFlow flow = getFlow();
				try {
					flow.getCredentialDataStore().clear();
				} catch (IOException e) {
				}
				Gdx.app.postRunnable(callback);
			}
		}).start();
	}

	@Override
	public void lb_submit(final String boardId, final long score, final String label, final Callback<Void> callback) {
		new Thread(new Runnable() {			
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Submit submit = g.scores().submit(boardId, score);
					String tag = URLEncoder.encode(label, "UTF-8");
					submit.setScoreTag(tag);
					PlayerScoreResponse response = submit.execute();
					Gdx.app.log(this.getClass().getName(), response.getFormattedScore());
				} catch (IOException e) {
					e.printStackTrace();
				}
				Gdx.app.postRunnable(callback);
			}
		}).start();
	}

	@Override
	public void lb_getScoresFor(final String boardId, final Callback<GameScores> list) {
		new Thread(new Runnable() {			
			@Override
			public void run() {
				GameScores gscores = new GameScores();
				try {
					Games g = _getGamesObject();
					Scores.Get scores = g.scores().get("me", boardId, TimeSpan.ALL_TIME.name());
					scores.setMaxResults(30);
					PlayerLeaderboardScoreListResponse result = scores.execute();
					List<PlayerLeaderboardScore> list = result.getItems();
					for (PlayerLeaderboardScore e : list) {
						GameScore gs = new GameScore();
						gs.rank="";
						gs.tag=URLDecoder.decode(e.getScoreTag(), "UTF-8");
						gs.value=e.getScoreString();
						gscores.list.add(gs);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				list.setData(gscores);
				Gdx.app.postRunnable(list);
			}
		});
	}

	@Override
	public void lb_getListFor(final String boardId, final Callback<GameScores> callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				GameScores gscores=new GameScores();
				try {
					Games g = _getGamesObject();
					Scores.List scores = g.scores().list(boardId,
							Collection.PUBLIC.name(), TimeSpan.WEEKLY.name());
					scores.setMaxResults(30);
					LeaderboardScores result = scores.execute();
					List<LeaderboardEntry> list = result.getItems();
					for (LeaderboardEntry e : list) {
						GameScore gs=new GameScore();
						gs.rank=e.getFormattedScoreRank();
						gs.tag=URLDecoder.decode(e.getScoreTag(), "UTF-8");
						gs.value=e.getFormattedScore();
						gscores.list.add(gs);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				callback.setData(gscores);
				Gdx.app.postRunnable(callback);
			}
		}).start();
	}

	@Override
	public void lb_getListWindowFor(final String boardId, final Callback<GameScores> callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				GameScores gscores=new GameScores();
				try {
					Games g = _getGamesObject();
					Scores.ListWindow scores = g.scores().listWindow(boardId,
							Collection.PUBLIC.name(), TimeSpan.WEEKLY.name());
					scores.setMaxResults(30);
					LeaderboardScores result = scores.execute();
					List<LeaderboardEntry> list = result.getItems();
					for (LeaderboardEntry e : list) {
						GameScore gs=new GameScore();
						gs.rank=e.getFormattedScoreRank();
						gs.tag=URLDecoder.decode(e.getScoreTag(), "UTF-8");
						gs.value=e.getFormattedScore();
						gscores.list.add(gs);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				callback.setData(gscores);
				Gdx.app.postRunnable(callback);
			}
		}).start();
	}

	private Games _getGamesObject() {
		_login();
		Games.Builder b = new Games.Builder(httpTransport, JSON_FACTORY,
				credential);
		b.setApplicationName("Cherokee Bound Pronouns/1.0");
		Games g = b.build();
		return g;
	}

	@Override
	public void ach_reveal(final String id, final Callback<Void> done) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					AchievementRevealResponse result = ac.reveal(id).execute();
					Gdx.app.log(this.getClass().getName(),
							id + " - " + result.getCurrentState());
				} catch (IOException e) {
					e.printStackTrace();
				}
				Gdx.app.postRunnable(done);
			}
		}).start();;
	}

	@Override
	public void ach_unlocked(final String id, final Callback<Void> done) {
		new Thread(new Runnable() {			
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();			
					Achievements ac = g.achievements();			
					AchievementUnlockResponse result = ac.unlock(id).execute();
					Gdx.app.log(this.getClass().getName(),
							id + " - " + result.getNewlyUnlocked());
				} catch (IOException e) {
					e.printStackTrace();
				}
				Gdx.app.postRunnable(done);
			}
		}).start();	
	}

	@Override
	public void ach_list(final Callback<GameAchievements> callback) {
		new Thread(new Runnable() {			
			@Override
			public void run() {
				GameAchievements results = new GameAchievements();
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					PlayerAchievementListResponse response = ac.list("me").execute();
					List<PlayerAchievement> list = response.getItems();
					for (PlayerAchievement pa : list) {
						GameAchievement a = new GameAchievement();
						a.id=pa.getId();
						a.state=pa.getAchievementState();
						results.list.add(a);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				callback.setData(results);
				Gdx.app.postRunnable(callback);
			}
		});
	}
}
