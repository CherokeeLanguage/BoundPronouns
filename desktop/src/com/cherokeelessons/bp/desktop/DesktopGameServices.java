package com.cherokeelessons.bp.desktop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.bp.LearningSession;
import com.cherokeelessons.bp.MainScreen;
import com.cherokeelessons.util.GooglePlayGameServices;
import com.cherokeelessons.util.GooglePlayGameServices.AppFiles.FileMeta;
import com.cherokeelessons.util.GooglePlayGameServices.GameAchievements.GameAchievement;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
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
	private java.io.File DATA_STORE_DIR;
	private Credential credential;
	private FileDataStoreFactory dataStoreFactory;
	private NetHttpTransport httpTransport;
	private FileHandle p0;
	private static final JacksonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	private Boolean initdone = false;
	protected Callback<AppFiles> cb_listFiles = new Callback<AppFiles>() {
		@Override
		public void success(AppFiles result) {
			for (FileMeta file : result.files) {
				log("File: " + file.id + " - " + file.title + " - "
						+ file.lastModified);
			}
		}
	};
	protected Callback<String> cb_bytitle = new Callback<String>() {
		@Override
		public void success(String result) {
			log("=== DOWNLOAD");
			log(result);
		}
	};

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

	@Override
	public void login(final Callback<Void> success) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					_login();
				} catch (GeneralSecurityException | IOException e) {
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with());
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
	public void logout(final Callback<Void> success) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					init();
					GoogleAuthorizationCodeFlow flow;
					flow = getFlow();
					flow.getCredentialDataStore().clear();
				} catch (IOException e) {
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with());
			}
		}).start();
	}

	@Override
	public void lb_submit(final String boardId, final long score,
			final String label, final Callback<Void> success) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Submit submit = g.scores().submit(boardId, score);
					String tag = URLEncoder.encode(label, "UTF-8");
					submit.setScoreTag(tag);
				} catch (IOException | GeneralSecurityException e) {
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with());
			}
		}).start();
	}

	@Override
	public void lb_getScoresFor(final String boardId,
			final Callback<GameScores> success) {
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
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with(gscores));
			}
		});
	}

	@Override
	public void lb_getListFor(final String boardId,
			final Collection collection, final TimeSpan ts,
			final Callback<GameScores> success) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				listFiles(cb_listFiles);
				getFileByTitle("Bound Pronouns - Active Cards - json",
						cb_bytitle);
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
						postRunnable(success.with(gscores));
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
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with(gscores));
			}
		}).start();
	}

	@Override
	public void lb_getListWindowFor(final String boardId,
			final Collection collection, final TimeSpan ts,
			final Callback<GameScores> success) {
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
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with(gscores));
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
	public void ach_reveal(final String id, final Callback<Void> success) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					ac.reveal(id).execute();
				} catch (IOException | GeneralSecurityException e) {
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with());
			}
		}).start();
	}

	@Override
	public void ach_unlocked(final String id, final Callback<Void> success) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Games g = _getGamesObject();
					Achievements ac = g.achievements();
					ac.unlock(id).execute();
				} catch (IOException | GeneralSecurityException e) {
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with());
			}
		}).start();
	}

	private Drive _getDriveObject() throws GeneralSecurityException,
			IOException {
		_login();
		Drive.Builder b = new Drive.Builder(httpTransport, JSON_FACTORY,
				credential);
		b.setApplicationName("Cherokee Bound Pronouns/1.0");
		Drive drive = b.build();
		return drive;
	}

	public void getFile(final String id, final Callback<String> callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					Drive drive = _getDriveObject();
					com.google.api.services.drive.model.File meta = drive
							.files().get(id).execute();
					MediaHttpDownloader downloader = new MediaHttpDownloader(
							httpTransport, credential);
					downloader.download(new GenericUrl(meta.getDownloadUrl()),
							baos);
					String result = new String(baos.toByteArray(),
							StandardCharsets.UTF_8).intern();
					postRunnable(callback.with(result));
				} catch (IOException | GeneralSecurityException e) {
					postRunnable(callback.with(e));
				}
			}
		}).start();
	}

	public static class Stashed<T> {
		public T data;

		public Stashed() {
		}

		public Stashed(T data) {
			this.data = data;
		}
	}

	public void getFileByTitle(final String title,
			final Callback<String> callback) {
		Callback<FileMeta> meta_cb = new Callback<FileMeta>() {
			@Override
			public void success(final FileMeta result) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							MediaHttpDownloader downloader = new MediaHttpDownloader(
									httpTransport, credential);
							downloader.download(new GenericUrl(result.url),
									baos);
							String result = new String(baos.toByteArray(),
									StandardCharsets.UTF_8).intern();
							postRunnable(callback.with(result));
						} catch (IOException e) {
							postRunnable(callback.with(e));
						}
					}
				});
			}

			@Override
			public void error(Exception exception) {
				postRunnable(callback.with(exception));
			}
		};
		getFileMetaByTitle(title, meta_cb);
	}

	public void getFileMetaByTitle(final String title,
			final Callback<FileMeta> callback) {
		Callback<AppFiles> findFile = new Callback<AppFiles>() {
			@Override
			public void success(AppFiles result) {
				for (FileMeta file : result.files) {
					if (file.title.equals(title)) {
						postRunnable(callback.with(file));
						return;
					}
				}
			}

			@Override
			public void error(Exception exception) {
				postRunnable(callback.with(exception));
			}
		};
		listFiles(findFile);
	}

	public void listFiles(final Callback<AppFiles> callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					AppFiles afs = new AppFiles();
					Drive drive = _getDriveObject();

					Files.List request = drive.files().list();
					request.setQ("'appfolder' in parents");
					FileList fl = request.execute();
					List<com.google.api.services.drive.model.File> items = fl
							.getItems();
					for (com.google.api.services.drive.model.File item : items) {
						FileMeta af = new FileMeta();
						af.isAppData = item.getAppDataContents();
						af.created = new Date(item.getCreatedDate().getValue());
						af.id = item.getId();
						af.lastModified = new Date(item.getModifiedDate()
								.getValue());
						af.title = item.getTitle();
						af.url = item.getDownloadUrl();
						afs.files.add(af);
					}
					postRunnable(callback.with(afs));
				} catch (IOException | GeneralSecurityException e) {
					postRunnable(callback.with(e));
				}
			}
		}).start();
	}
	
//	public void drive_updateById(String id, final java.io.File file,
//			final Callback<String> callback);
//	
//	public void drive_updateByTitle(String id, final java.io.File file,
//			final Callback<String> callback);

	public void drive_add(final java.io.File file,
			final Callback<String> callback) {
		drive_add(file, file.getName(), file.getName(), callback);
	}

	public void drive_add(final java.io.File file, final String title,
			final String description, final Callback<String> callback) {
		final Stashed<String> _title = new Stashed<String>(title);
		if (title == null || title.trim().length() == 0) {
			_title.data = file.getName();
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Drive drive = _getDriveObject();
					com.google.api.services.drive.model.File meta = new com.google.api.services.drive.model.File();
					meta.setParents(Arrays.asList(new ParentReference()
							.setId("appfolder")));
					meta.setTitle(_title.data);
					FileContent content = new FileContent("application/json", file);
					Insert insert = drive.files().insert(meta, content);
					insert.getMediaHttpUploader().setProgressListener(
							new MediaHttpUploaderProgressListener() {
								@Override
								public void progressChanged(
										MediaHttpUploader uploader)
										throws IOException {
									log("uploaded: "
											+ ((int) (uploader.getProgress() * 100))
											+ "%");
								}
							});
					com.google.api.services.drive.model.File i = insert
							.execute();
					postRunnable(callback.with(i.getId()));
				} catch (GeneralSecurityException | IOException e) {
					postRunnable(callback.with(e));
				}
			}
		}).start();
	}

	private static void log(String msg) {
		Gdx.app.log("DesktopGameServices", msg);
	}

	@Override
	public void ach_list(final Callback<GameAchievements> success) {
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
					postRunnable(success.with(e));
					return;
				}
				postRunnable(success.with(results));
			}
		});
	}
}
