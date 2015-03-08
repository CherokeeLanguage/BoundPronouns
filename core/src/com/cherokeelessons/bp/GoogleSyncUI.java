package com.cherokeelessons.bp;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.SlotInfo;
import com.cherokeelessons.util.GooglePlayGameServices;
import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.FileMetaList;
import com.cherokeelessons.util.JsonConverter;

public class GoogleSyncUI implements Runnable {

	private static final String BKUP_ACTIVE_DECK_JSON = "backup-ActiveDeck.json";
	private static final String BKUP_INFO_JSON = "backup-info.json";
	private static final String SYNC_ACTIVE_DECK_JSON = "sync-ActiveDeck.json";
	private static final String SYNC_INFO_JSON = "sync-info.json";
	private static final String ACTIVE_DECK_JSON = "ActiveDeck.json";
	private static final String INFO_JSON = "info.json";
	private final SlotInfo info;
	private final FileHandle p0;
	private final Stage stage;
	private final BoundPronouns game;
	private final GooglePlayGameServices gplay;
	private final String gfile_info;
	private final String gfile_deck;
	private final JsonConverter json;

	public GoogleSyncUI(BoundPronouns game, Stage stage, SlotInfo sync_info,
			FileHandle p0) {
		this.game = game;
		this.stage = stage;
		this.info = sync_info;
		this.p0 = p0;
		setDialogStyles();
		this.gplay = BoundPronouns.services;
		gfile_info = p0.name() + "-slotinfo.json";
		gfile_deck = p0.name() + "-activedeck.json";
		json = new JsonConverter();
	}

	@Override
	public void run() {
		if (!BoundPronouns.getPrefs().getBoolean(
				BoundPronouns.GooglePlayLogginIn, false)) {
			askToLogin();
			return;
		}
		busy = new Dialog("Google Play Services", dws) {
			@Override
			protected void result(Object object) {
				abort = true;
			}
		};
		busy.text(new Label("Retrieving Cloud Data ...", dls));
		busy.button(new TextButton("CANCEL SYNC", tbs));
		busy.show(stage);
		doSync();
	}

	private boolean abort = false;

	private void doResolveConflict(SlotInfo cloud_info) {
		if (abort) {
			return;
		}
	}

	private void download(SlotInfo cloud_info) {
		Gdx.app.log("GoogleSyncUI", "download");
		if (abort) {
			return;
		}
		busy.hide();
		Dialog notice = new Dialog("Google Play Services", dws);
		notice.button(new TextButton("OK", tbs));
		notice.text(new Label("Downloading Cloud Copy ...", dls));
		notice.show(stage);
		p0.child(ACTIVE_DECK_JSON).copyTo(p0.child(BKUP_ACTIVE_DECK_JSON));
		p0.child(INFO_JSON).copyTo(p0.child(BKUP_INFO_JSON));
		p0.child(SYNC_ACTIVE_DECK_JSON).copyTo(p0.child(ACTIVE_DECK_JSON));
		p0.child(SYNC_INFO_JSON).copyTo(p0.child(INFO_JSON));
		notice.hide();
		showDoneDialog();
	}

	private void showDoneDialog() {
		Dialog notice = new Dialog("Google Play Services", dws);
		notice.button(new TextButton("OK", tbs));
		notice.text(new Label("Sync Complete ...", dls));
		notice.show(stage);
	}

	private Dialog busy;

	private void doSync() {

		final Callback<Void> cb_infofile = new Callback<Void>() {
			@Override
			public void success(Void result) {
				if (abort) {
					return;
				}
				SlotInfo cloud_info = json.fromJson(SlotInfo.class,
						p0.child(SYNC_INFO_JSON));
				ActiveDeck cloud_deck = json.fromJson(ActiveDeck.class,
						p0.child(SYNC_ACTIVE_DECK_JSON));
				SlotInfo.calculateStats(cloud_info, cloud_deck);

				if (!cloud_info.getSignature().equals(info.getSignature())) {
					doResolveConflict(cloud_info);
					return;
				}
				if (cloud_info.activeCards > info.activeCards) {
					download(cloud_info);
					return;
				}
				if (cloud_info.fullScore > info.fullScore) {
					download(cloud_info);
					return;
				}
				if (cloud_info.lastScore > info.lastScore) {
					download(cloud_info);
					return;
				}
				upload();
			}

			@Override
			public void error(Exception exception) {
				errorDialog(exception);
			}
		};

		final Callback<FileMetaList> cb_info = new Callback<FileMetaList>() {
			@Override
			public void success(FileMetaList result) {
				if (abort) {
					return;
				}
				if (result.files.size() == 0) {
					upload();
					return;
				}
				gplay.drive_getFileById(result.files.get(0).id,
						p0.child(SYNC_INFO_JSON), cb_infofile);
			}

			@Override
			public void error(Exception exception) {
				errorDialog(exception);
			}
		};

		final Callback<Void> cb_infometa = new Callback<Void>() {
			@Override
			public void success(Void result) {
				if (abort) {
					return;
				}
				gplay.drive_getFileMetaByTitle(gfile_info, cb_info);
			}

			@Override
			public void error(Exception exception) {
				errorDialog(exception);
			}
		};

		final Callback<FileMetaList> cb_deck = new Callback<FileMetaList>() {
			@Override
			public void success(FileMetaList result) {
				if (abort) {
					return;
				}
				if (result.files.size() == 0) {
					Gdx.app.log("GoogleSyncUI", "No active deck in cloud... "+gfile_deck);
					upload();
					return;
				}
				gplay.drive_getFileById(result.files.get(0).id,
						p0.child(SYNC_ACTIVE_DECK_JSON), cb_infometa);
			}

			@Override
			public void error(Exception exception) {
				errorDialog(exception);
			}
		};

		gplay.drive_getFileMetaByTitle(gfile_deck, cb_deck);
	}

	private void upload() {
		Gdx.app.log("GoogleSyncUI", "upload");
		busy.hide();
		final Dialog notice = new Dialog("Google Play Services", dws);
		notice.button(new TextButton("OK", tbs));
		notice.text(new Label("Uploading Device Copy to Cloud...", dls));
		notice.show(stage);
		if (info.getSignature() == null || info.getSignature().length() == 0) {
			String s1 = Long.toString(System.currentTimeMillis(),
					Character.MAX_RADIX);
			String s2 = Integer.toString(new Random().nextInt(Integer.MAX_VALUE),
					Character.MAX_RADIX);
			info.setSignature(s1 + "-" + s2);
			json.toJson(info, p0.child(INFO_JSON));
		}
		final Callback<String> upload_done=new Callback<String>() {			
			@Override
			public void success(String result) {
				notice.hide();
				showDoneDialog();
			}
			@Override
			public void error(Exception exception) {
				errorDialog(exception);
			}
		};
		final Callback<String> upload_info=new Callback<String>() {			
			@Override
			public void success(String result) {
				gplay.drive_replace(p0.child(INFO_JSON), gfile_info, gfile_info, upload_done);				
			}
			@Override
			public void error(Exception exception) {
				errorDialog(exception);
				notice.hide();
			}
		};
		gplay.drive_replace(p0.child(ACTIVE_DECK_JSON), gfile_deck, gfile_deck, upload_info);		
	}

	private void doLogin() {
		final Dialog login = new Dialog("Google Play Services", dws);
		login.text(new Label("Connecting to Google Play Services ...", dls));
		login.button(new TextButton("DISMISS", tbs));
		Callback<Void> success_in = new Callback<Void>() {
			@Override
			public void success(Void result) {
				login.hide();
				Preferences prefs = BoundPronouns.getPrefs();
				prefs.putBoolean(BoundPronouns.GooglePlayLogginIn, true);
				prefs.flush();
				Gdx.app.postRunnable(GoogleSyncUI.this);
			}

			@Override
			public void error(Exception exception) {
				exception.printStackTrace();
				login.hide();
				Preferences prefs = BoundPronouns.getPrefs();
				prefs.putBoolean(BoundPronouns.GooglePlayLogginIn, false);
				prefs.flush();
				errorDialog(exception);
			}
		};
		login.show(stage);
		gplay.login(success_in);
	}

	private void errorDialog(Exception e) {
		busy.hide();
		Dialog error = new Dialog("Google Play Services", dws);
		error.button(new TextButton("OK", tbs));
		error.text(new Label(e.getMessage(), dls));
		error.show(stage);
	}

	private void askToLogin() {
		final Dialog login = new Dialog("Google Play Services", dws) {
			@Override
			protected void result(Object object) {
				if (!Boolean.TRUE.equals(object)) {
					return;
				}
				doLogin();
			}
		};
		login.setKeepWithinStage(true);
		Table contentTable = login.getContentTable();
		contentTable.row();
		contentTable.add(new Label(
				"Cloud Sync between devices requires Google Play.", dls));
		contentTable.row();
		contentTable.add(new Label("Would you like to login now?", dls));
		login.button(new TextButton("YES - LOGIN", tbs), Boolean.TRUE);
		login.button(new TextButton("NO - CANCEL", tbs), Boolean.FALSE);
		login.show(stage);
	}

	private Skin skin;
	private WindowStyle dws;
	private LabelStyle dls;
	private TextButtonStyle tbs;

	private void setDialogStyles() {
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		dws = new WindowStyle(skin.get(WindowStyle.class));
		dls = new LabelStyle(skin.get(LabelStyle.class));
		dws.titleFont = game.getFont(Font.SerifLLarge);
		dls.font = game.getFont(Font.SerifLarge);
		tbs = new TextButtonStyle(skin.get(TextButtonStyle.class));
		tbs.font = game.getFont(Font.SerifSmall);
	}

}
