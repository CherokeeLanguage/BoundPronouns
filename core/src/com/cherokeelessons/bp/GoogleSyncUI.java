package com.cherokeelessons.bp;

import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.SlotInfo;
import com.cherokeelessons.util.GooglePlayGameServices;
import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.FileMetaList;
import com.cherokeelessons.util.JsonConverter;

/**
 * 
 * @author mjoyner
 *
 * 
 *         sync dialog:
 * 
 *         [if not logged in] Prompt to login.
 * 
 *         [if logged in] Indicate sync in progress.
 * 
 *         If local is blank, pull from server, no prompting. Dismiss dialog.
 *         Dialog syncdone. Reload slots.
 * 
 *         If remote does not have a copy to sync with, upload local copy with
 *         new random signature, no prompting. Dialog syncdone. Reload slots.
 * 
 *         If local signature matches remote server, no prompting, sync to
 *         highest card count + highest full score + highest last run time +
 *         upload count. Dialog syncdone. Reload slots.
 * 
 *         If local signature is blank or does not match remote server, prompt
 *         with: use server copy, use local copy (with new random signature),
 *         abort. Dialog syncdone. Reload slots.
 */

public class GoogleSyncUI implements Runnable, Disposable {

	private static final String BKUP_ACTIVE_DECK_JSON = "backup-ActiveDeck.json";
	private static final String BKUP_INFO_JSON = "backup-info.json";
	private static final String SYNC_ACTIVE_DECK_JSON = "sync-ActiveDeck.json";
	private static final String SYNC_INFO_JSON = "sync-info.json";
	private static final String ACTIVE_DECK_JSON = "ActiveDeck.json";
	private static final String INFO_JSON = "info.json";
	private SlotInfo device_info;
	private final FileHandle p0;
	private final Stage stage;
	private final BoundPronouns game;
	private final GooglePlayGameServices gplay;
	private final String gfile_info;
	private final String gfile_deck;
	private final JsonConverter json;
	private Runnable whenDone;

	@Override
	public void dispose() {
		skin.dispose();
		skin = null;
	}

	public static void dispose_skin() {
		skin.dispose();
		skin = null;
	}

	public GoogleSyncUI(BoundPronouns game, Stage stage, FileHandle p0,
			Runnable whenDone) {
		if (skin == null) {
			skin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));
		}
		this.game = game;
		this.stage = stage;
		this.p0 = p0;
		setDialogStyles();
		this.gplay = BoundPronouns.services;
		gfile_info = p0.name() + "-slotinfo.json";
		gfile_deck = p0.name() + "-activedeck.json";
		json = new JsonConverter();
		this.whenDone = whenDone;
	}

	private void done() {
		if (whenDone != null) {
			Gdx.app.postRunnable(whenDone);
		}
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
		recalculateDeviceStats();
		doSync();
	}

	private void recalculateDeviceStats() {
		if (!p0.child(INFO_JSON).exists()) {
			return;
		}
		if (!p0.child(ACTIVE_DECK_JSON).exists()) {
			return;
		}
		device_info = json.fromJson(SlotInfo.class, p0.child(INFO_JSON));
		ActiveDeck deck = json.fromJson(ActiveDeck.class,
				p0.child(ACTIVE_DECK_JSON));
		SlotInfo.calculateStats(device_info, deck);
		json.toJson(device_info, p0.child(INFO_JSON));
	}

	private boolean abort = false;

	private void doResolveConflict(final SlotInfo cloud_info) {
		final String upload = "CLOUD COPY";
		final String download = "DEVICE COPY";
		final String cancel = "CANCEL";
		if (abort) {
			return;
		}
		busy.hide();

		final Dialog notice = new Dialog("CONFLICT DETECTED", dws) {
			@Override
			protected void result(Object object) {
				if (upload.equals(object)) {
					upload();
					return;
				}
				if (download.equals(object)) {
					download();
					return;
				}
			}
		};

		Table content = notice.getContentTable();
		content.row();
		content.add(new Label("Please choose which copy to keep:", dls))
				.colspan(2).center();
		content.row();
		content.add(new Label("CLOUD COPY:", dls));

		String txt = "";

		txt += cloud_info.level;
		txt += " ";
		txt += (StringUtils.isBlank(cloud_info.settings.name)) ? "ᎤᏲᏒ ᏥᏍᏕᏥ!"
				: cloud_info.settings.name;
		txt += " - ";
		txt += "Score: " + cloud_info.lastScore;
		txt += "\n";
		txt += cloud_info.activeCards + " cards";
		txt += ": ";
		txt += cloud_info.shortTerm + " short";
		txt += ", " + cloud_info.mediumTerm + " medium";
		txt += ", " + cloud_info.longTerm + " long";

		TextButtonStyle tbs = new TextButtonStyle(
				skin.get(TextButtonStyle.class));
		tbs.font = game.getFont(Font.SerifMedium);

		TextButton textb = new TextButton(txt, tbs);
		content.add(textb).expandX().fill();
		textb.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				notice.hide();
				download();
				return true;
			}
		});

		// SlotInfo device_info = json.fromJson(SlotInfo.class,
		// p0.child(INFO_JSON));
		content.row();
		content.add(new Label("DEVICE COPY:", dls));

		txt = "";
		txt += device_info.level;
		txt += " ";
		txt += (StringUtils.isBlank(device_info.settings.name)) ? "ᎤᏲᏒ ᏥᏍᏕᏥ!"
				: cloud_info.settings.name;
		txt += " - ";
		txt += "Score: " + device_info.lastScore;
		txt += "\n";
		txt += device_info.activeCards + " cards";
		txt += ": ";
		txt += device_info.shortTerm + " short";
		txt += ", " + device_info.mediumTerm + " medium";
		txt += ", " + device_info.longTerm + " long";

		tbs.font = game.getFont(Font.SerifMedium);
		textb = new TextButton(txt, tbs);
		textb.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				notice.hide();
				upload();
				return true;
			}
		});
		content.add(textb).expandX().fill();

		notice.button(new TextButton(upload, tbs), upload);
		notice.button(new TextButton(download, tbs), download);
		notice.button(new TextButton(cancel, tbs), cancel);
		notice.show(stage);
	}

	private void download() {
		Gdx.app.log("GoogleSyncUI", "download");
		if (abort) {
			return;
		}
		busy.hide();
		Dialog notice = new Dialog("Google Play Services", dws);
		notice.button(new TextButton("OK", tbs));
		notice.text(new Label("Downloading Cloud Copy ...", dls));
		notice.show(stage);
		try {
			if (p0.child(ACTIVE_DECK_JSON).exists()) {
				p0.child(ACTIVE_DECK_JSON).copyTo(p0.child(BKUP_ACTIVE_DECK_JSON));
			}
			if (p0.child(INFO_JSON).exists()) { 
				p0.child(INFO_JSON).copyTo(p0.child(BKUP_INFO_JSON));
			}			
		} catch (GdxRuntimeException e) {
			errorDialog(e);
		}
		try {
			p0.child(SYNC_ACTIVE_DECK_JSON).copyTo(p0.child(ACTIVE_DECK_JSON));		
			p0.child(SYNC_INFO_JSON).copyTo(p0.child(INFO_JSON));
		} catch (GdxRuntimeException e) {
			errorDialog(e);
		}
		notice.hide();
		showDoneDialog();
	}

	private void showDoneDialog() {
		Dialog notice = new Dialog("Google Play Services", dws) {
			@Override
			protected void result(Object object) {
				done();
			}
		};
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
				if (!p0.child(INFO_JSON).exists()){
					download();
					return;
				}
				if (p0.child(INFO_JSON).readString("UTF-8")
						.equals(p0.child(SYNC_INFO_JSON).readString("UTF-8"))) {
					busy.hide();
					dialogNothingToDo();
					return;
				}
				SlotInfo cloud_info = json.fromJson(SlotInfo.class,
						p0.child(SYNC_INFO_JSON));
				ActiveDeck cloud_deck = json.fromJson(ActiveDeck.class,
						p0.child(SYNC_ACTIVE_DECK_JSON));
				SlotInfo.calculateStats(cloud_info, cloud_deck);

				if (!cloud_info.getSignature().equals(
						device_info.getSignature())) {
					doResolveConflict(cloud_info);
					return;
				}
				if (cloud_info.activeCards > device_info.activeCards) {
					download();
					return;
				}
				if (cloud_info.fullScore > device_info.fullScore) {
					download();
					return;
				}
				if (cloud_info.lastScore > device_info.lastScore) {
					download();
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
					Gdx.app.log("GoogleSyncUI", "No active deck in cloud... "
							+ gfile_deck);
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

	private void dialogNothingToDo() {
		final Dialog notice = new Dialog("Google Play Services", dws);
		notice.button(new TextButton("OK", tbs));
		notice.text(new Label("Nothing to Sync...", dls));
		notice.show(stage);
	}

	private void upload() {
		Gdx.app.log("GoogleSyncUI", "upload");
		busy.hide();
		if (!p0.child(INFO_JSON).exists()) {
			dialogNothingToDo();
			return;
		}
		if (!p0.child(ACTIVE_DECK_JSON).exists()) {
			dialogNothingToDo();
			return;
		}
		final Dialog notice = new Dialog("Google Play Services", dws);
		notice.button(new TextButton("OK", tbs));
		notice.text(new Label("Uploading Device Copy to Cloud...", dls));
		notice.show(stage);
		if (device_info.getSignature() == null
				|| device_info.getSignature().length() == 0) {
			String s1 = Long.toString(System.currentTimeMillis(),
					Character.MAX_RADIX);
			String s2 = Integer.toString(
					new Random().nextInt(Integer.MAX_VALUE),
					Character.MAX_RADIX);
			device_info.setSignature(s1 + "-" + s2);
			json.toJson(device_info, p0.child(INFO_JSON));
		}
		final Callback<String> upload_done = new Callback<String>() {
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
		final Callback<String> upload_info = new Callback<String>() {
			@Override
			public void success(String result) {
				gplay.drive_replace(p0.child(INFO_JSON), gfile_info,
						gfile_info, upload_done);
			}

			@Override
			public void error(Exception exception) {
				errorDialog(exception);
				notice.hide();
			}
		};
		gplay.drive_replace(p0.child(ACTIVE_DECK_JSON), gfile_deck, gfile_deck,
				upload_info);
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
		Dialog error = new Dialog("Google Play Services", dws) {
			@Override
			protected void result(Object object) {
				done();
			}
		};
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

	private static Skin skin;
	private WindowStyle dws;
	private LabelStyle dls;
	private TextButtonStyle tbs;

	private void setDialogStyles() {
		dws = new WindowStyle(skin.get(WindowStyle.class));
		dls = new LabelStyle(skin.get(LabelStyle.class));
		dws.titleFont = game.getFont(Font.SerifLLarge);
		dls.font = game.getFont(Font.SerifLarge);
		tbs = new TextButtonStyle(skin.get(TextButtonStyle.class));
		tbs.font = game.getFont(Font.SerifSmall);
	}

}
