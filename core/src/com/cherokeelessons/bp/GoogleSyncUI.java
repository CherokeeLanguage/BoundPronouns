package com.cherokeelessons.bp;

import java.util.Iterator;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
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
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.ActiveDeck;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.SlotInfo;
import com.cherokeelessons.util.GooglePlayGameServices;
import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.FileMetaList;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;
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
	private static final String TAG = "GoogleSyncUI";
	private final FileHandle p0;
	private final Stage stage;
	private final BoundPronouns game;
	private final GooglePlayGameServices gplay;
	private final String gfile_info;
	private final String gfile_deck;
	private final JsonConverter json;
	private Runnable whenAutoSyncDone;

	@Override
	public void dispose() {
		skin.dispose();
		skin = null;
	}

	public static void dispose_skin() {
		if (skin != null) {
			skin.dispose();
		}
		skin = null;
	}

	public GoogleSyncUI(BoundPronouns game, Stage stage, FileHandle p0,
			Runnable whenAutoSyncDone) {
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
		this.whenAutoSyncDone = whenAutoSyncDone;
	}

	private void done() {
		if (busy != null) {
			busy.hide();
		}
		if (whenAutoSyncDone != null) {
			Gdx.app.postRunnable(whenAutoSyncDone);
		}
	}

	@Override
	public void run() {
		if (!gplay.isLoggedIn()) {
			askToLoginForSync();
			return;
		}
		busy = new Dialog("Sync Service", dws) {
			@Override
			protected void result(Object object) {
				done();
			}
		};
		busy.getTitleLabel().setAlignment(Align.center);
		busy.text(new Label("Syncing ...", dls));
		busy.button(new TextButton("OK", tbs));
		busy.show(stage);
		doSync();
	}

	private void recalculateCloudStats() {
		if (!p0.child(SYNC_INFO_JSON).exists()) {
			return;
		}
		if (!p0.child(SYNC_ACTIVE_DECK_JSON).exists()) {
			return;
		}
		SlotInfo cloud_info = json.fromJson(SlotInfo.class,
				p0.child(SYNC_INFO_JSON));
		ActiveDeck deck = json.fromJson(ActiveDeck.class,
				p0.child(SYNC_ACTIVE_DECK_JSON));
		cloud_info.lastrun = deck.lastrun;
		/*
		 * Make sure we don't have active cards pointing to no longer existing
		 * master deck cards
		 */
		boolean save = false;
		Iterator<ActiveCard> ipending = deck.deck.iterator();
		while (ipending.hasNext()) {
			ActiveCard active = ipending.next();
			if (getCardById(active.pgroup, active.vgroup) != null) {
				continue;
			}
			ipending.remove();
			save = true;
			game.log(this, "Removed no longer valid entry: " + active.pgroup
					+ " - " + active.vgroup);
		}
		SlotInfo.calculateStats(cloud_info, deck);
		json.toJson(cloud_info, p0.child(SYNC_INFO_JSON));
		if (save) {
			json.toJson(deck, p0.child(SYNC_ACTIVE_DECK_JSON));
		}
	}

	private void recalculateDeviceStats() {
		if (!p0.child(INFO_JSON).exists()) {
			return;
		}
		if (!p0.child(ACTIVE_DECK_JSON).exists()) {
			return;
		}
		SlotInfo device_info = json.fromJson(SlotInfo.class,
				p0.child(INFO_JSON));
		device_info = json.fromJson(SlotInfo.class, p0.child(INFO_JSON));
		ActiveDeck deck = json.fromJson(ActiveDeck.class,
				p0.child(ACTIVE_DECK_JSON));
		device_info.lastrun = deck.lastrun;
		/*
		 * Make sure we don't have active cards pointing to no longer existing
		 * master deck cards
		 */
		boolean save = false;
		Iterator<ActiveCard> ipending = deck.deck.iterator();
		while (ipending.hasNext()) {
			ActiveCard active = ipending.next();
			if (getCardById(active.pgroup, active.vgroup) != null) {
				continue;
			}
			ipending.remove();
			save = true;
			game.log(this, "Removed no longer valid entry: " + active.pgroup
					+ " - " + active.vgroup);
		}
		SlotInfo.calculateStats(device_info, deck);
		json.toJson(device_info, p0.child(INFO_JSON));
		if (save) {
			json.toJson(deck, p0.child(ACTIVE_DECK_JSON));
		}
	}

	private Card getCardById(String pgroup, String vgroup) {
		for (Card card : game.deck.cards) {
			if (!card.pgroup.equals(pgroup)) {
				continue;
			}
			if (!card.vgroup.equals(vgroup)) {
				continue;
			}
			return card;
		}
		return null;
	}

	private void doResolveConflict(final SlotInfo cloud_info,
			final SlotInfo device_info) {
		final String download = "CLOUD COPY";
		final String upload = "DEVICE COPY";
		final String cancel = "CANCEL";
		if (busy != null) {
			done();
		}

		final Dialog notice = new Dialog("CONFLICT DETECTED", dws) {
			@Override
			protected void result(Object object) {
				if (upload.equals(object)) {
					upload();
					done();
					return;
				}
				if (download.equals(object)) {
					download();
					done();
					return;
				}
			}
		};
		notice.getTitleLabel().setAlignment(Align.center);

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
				done();
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
				done();
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
		if (busy != null) {
			busy.hide();
		}
		try {
			if (p0.child(ACTIVE_DECK_JSON).exists()) {
				p0.child(ACTIVE_DECK_JSON).copyTo(
						p0.child(BKUP_ACTIVE_DECK_JSON));
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
	}

	private Dialog busy;

	private void doSync() {

		final Callback<Void> cb_infofile = new Callback<Void>() {
			@Override
			public void success(Void result) {
				if (!p0.child(INFO_JSON).exists()) {
					download();
					done();
					return;
				}
				if (p0.child(INFO_JSON).readString("UTF-8")
						.equals(p0.child(SYNC_INFO_JSON).readString("UTF-8"))) {
					if (busy != null) {
						busy.hide();
					}
					done();
					return;
				}
				recalculateDeviceStats();
				recalculateCloudStats();
				SlotInfo cloud_info = json.fromJson(SlotInfo.class,
						p0.child(SYNC_INFO_JSON));
				SlotInfo device_info = json.fromJson(SlotInfo.class,
						p0.child(INFO_JSON));
				if (!cloud_info.getSignature().equals(
						device_info.getSignature())) {
					doResolveConflict(cloud_info, device_info);
					return;
				}
				if (cloud_info.activeCards > device_info.activeCards) {
					download();
					done();
					return;
				}

				long cloud_time = json.fromJson(ActiveDeck.class,
						p0.child(SYNC_ACTIVE_DECK_JSON)).lastrun;
				long device_time = json.fromJson(ActiveDeck.class,
						p0.child(ACTIVE_DECK_JSON)).lastrun;

				if (cloud_time > device_time) {
					download();
					done();
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

	public void upload() {
		upload(null);
	}

	public void uploadHidden(final Runnable whenDone) {
		if (!p0.child(INFO_JSON).exists()) {
			return;
		}
		if (!p0.child(ACTIVE_DECK_JSON).exists()) {
			return;
		}
		SlotInfo device_info = json.fromJson(SlotInfo.class,
				p0.child(INFO_JSON));
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
				if (whenDone != null) {
					Gdx.app.postRunnable(whenDone);
				}
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
			}
		};
		gplay.drive_replace(p0.child(ACTIVE_DECK_JSON), gfile_deck, gfile_deck,
				upload_info);
	}

	public void upload(Runnable whenDone) {
		Gdx.app.log("GoogleSyncUI", "upload");
		if (!p0.child(INFO_JSON).exists()) {
			done();
			return;
		}
		if (!p0.child(ACTIVE_DECK_JSON).exists()) {
			done();
			return;
		}
		SlotInfo device_info = json.fromJson(SlotInfo.class,
				p0.child(INFO_JSON));
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
				done();
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
				done();
				errorDialog(exception);
			}
		};
		gplay.drive_replace(p0.child(ACTIVE_DECK_JSON), gfile_deck, gfile_deck,
				upload_info);
	}

	public void doLogin(final Runnable afterLogin) {
		final Dialog login = new Dialog("Sync Service", dws);
		login.getTitleLabel().setAlignment(Align.center);
		login.text(new Label("Connecting to Sync Service ...", dls));
		login.button(new TextButton("DISMISS", tbs));
		Callback<Void> success_in = new Callback<Void>() {
			@Override
			public void success(Void result) {
				login.hide();
				if (afterLogin != null) {
					Gdx.app.postRunnable(afterLogin);
				}
			}

			@Override
			public void error(Exception exception) {
				exception.printStackTrace();
				login.hide();
				errorDialog(exception);
			}
		};
		login.show(stage);
		gplay.login(success_in);
	}

	private void errorDialog(Exception e) {
		if (busy != null) {
			busy.hide();
		}
		Dialog error = new Dialog("Sync Service", dws) {
			@Override
			protected void result(Object object) {
				done();
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
		error.show(stage);
		e.printStackTrace();
	}

	public void askToLoginForSync() {
		askToLoginForSync(GoogleSyncUI.this);
	}

	public void askToLogin(final Callback<Void> afterLogin) {
		askToLoginForSync(afterLogin.withNull());
	}

	public void askToLoginForSync(final Runnable afterLogin) {
		String reasonMsg = "Syncing requires login ...";
		askToLoginFor(afterLogin, reasonMsg);
	}

	public void askToLoginFor(final Runnable afterLogin, String reasonMsg) {
		final Dialog login = new Dialog("Sync Service", dws) {
			@Override
			protected void result(Object object) {
				if (!Boolean.TRUE.equals(object)) {
					done();
					return;
				}
				doLogin(afterLogin);
			}
		};
		login.getTitleLabel().setAlignment(Align.center);
		login.setKeepWithinStage(true);
		Table contentTable = login.getContentTable();
		contentTable.row();
		contentTable.add(new Label(reasonMsg, dls));
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

	public void showScores(String title, GameScores gamescores,
			final Runnable whenDone) {
		setDialogStyles();
		Dialog scores = new Dialog(title, dws) {
			Dialog scores = this;
			{
				final Texture background = game.manager.get(
						BoundPronouns.IMG_MAYAN, Texture.class);
				final TextureRegion region = new TextureRegion(background);
				final TiledDrawable tiled = new TiledDrawable(region);
				tiled.setMinHeight(0);
				tiled.setMinWidth(0);
				tiled.setTopHeight(game.getFont(Font.SerifLarge).getCapHeight() + 20);
				scores.background(tiled);

				LabelStyle lstyle = new LabelStyle(skin.get(LabelStyle.class));
				lstyle.font = game.getFont(Font.SerifMedium);
			}

			@Override
			protected void result(Object object) {
				if (whenDone != null) {
					Gdx.app.postRunnable(whenDone);
				}
			}
		};
		scores.getTitleLabel().setAlignment(Align.center);
		scores.setFillParent(true);

		Table container = scores.getContentTable();

		Table table = new Table();

		table.clear();
		table.defaults().expandX();
		String text = "Rank";
		table.add(new Label(text, dls)).padLeft(15).padRight(15).center();
		text = "Score";
		table.add(new Label(text, dls)).center();
		text = "Skill Level";
		table.add(new Label(text, dls)).center();
		text = "Display Name";
		table.add(new Label(text, dls)).center();

		for (GameScore score : gamescores.list) {
			table.row();
			table.add(new Label(score.rank, dls)).padLeft(15).padRight(15)
					.center();
			table.add(new Label(score.value, dls)).right().padRight(30);
			table.add(new Label(score.tag, dls)).center();
			table.add(new Label(score.user, dls)).center();
		}

		for (int ix = gamescores.list.size(); ix < 5; ix++) {
			Gdx.app.log(TAG, "ix: " + ix);
			table.row();
			table.add(new Label("", dls)).padLeft(15).padRight(15).center();
			table.add(new Label("0", dls)).right().padRight(30);
			table.add(new Label(SlotInfo.LevelName.Newbie.getEnglish(), dls))
					.center();
			table.add(new Label("", dls)).center();
		}

		scores.button(new TextButton("OK", tbs));

		ScrollPane scroll = new ScrollPane(table, skin);
		scroll.setColor(Color.DARK_GRAY);
		scroll.setFadeScrollBars(false);
		scroll.setSmoothScrolling(true);
		container.row();
		container.add(scroll).expand().fill();
		stage.setScrollFocus(scroll);
		stage.setKeyboardFocus(scroll);

		scores.show(stage);
	}

}
