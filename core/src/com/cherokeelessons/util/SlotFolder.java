package com.cherokeelessons.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.bp.BoundPronouns;

public class SlotFolder {

	public static final String base = "BoundPronouns";

	public static FileHandle getDeckSlot() {
		return getFolder("deck");
	}

	public static FileHandle getFolder(final String child) {
		final FileHandle p0;
		final String path0 = base + "/slots";
		switch (Gdx.app.getType()) {
		case Android:
			p0 = Gdx.files.local(path0);
			break;
		case Applet:
			p0 = Gdx.files.external(path0);
			break;
		case Desktop:
			p0 = Gdx.files.external(path0);
			break;
		case HeadlessDesktop:
			p0 = Gdx.files.external(path0);
			break;
		case WebGL:
			p0 = Gdx.files.external(path0);
			break;
		case iOS:
			p0 = Gdx.files.local(path0);
			break;
		default:
			p0 = Gdx.files.external(path0);
		}
		p0.child(child).mkdirs();
		return p0.child(child);
	}

	public static void migrate() {
		final Preferences prefs = BoundPronouns.getPrefs();
		final String key = "migrate-" + base;
		if (prefs.getBoolean(key, false)) {
			Gdx.app.log("Migrate", "Marked as already migrated.");
			return;
		}
		final FileHandle lpath = Gdx.files.local(base);
		if (lpath.exists() && !lpath.isDirectory()) {
			lpath.deleteDirectory();
		}
		final FileHandle epath = Gdx.files.external(base);
		if (epath.file().getAbsolutePath().equals(lpath.file().getAbsolutePath())) {
			return;
		}
		if (epath.child("slots").child("0").isDirectory()) {
			Gdx.app.log("Migrate",
					"Moving: " + epath.file().getAbsolutePath() + " to " + lpath.file().getAbsolutePath());
			lpath.deleteDirectory();
			epath.moveTo(lpath);
			Gdx.app.log("Migrate", "Done.");
		}
		prefs.remove(key);
		prefs.putBoolean(key, true);
		prefs.flush();
	}
}
