package com.cherokeelessons.bp.android;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.cherokeelessons.bp.BoundPronouns;
import com.cherokeelessons.cards.SlotInfo;

public class AndroidLauncher extends AndroidApplication implements
		BoundPronouns.FBShareStatistics {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		BoundPronouns.fb = this;
		BoundPronouns game = new BoundPronouns();
		initialize(game, config);
	}

	@Override
	public void fbshare(SlotInfo info) {
		info.validate();

		String text = "";
		text += info.activeCards + " active cards";
		text += " - ";
		text += ((int) (info.shortTerm * 100)) + "% short term memorized";
		text += ", " + ((int) (info.mediumTerm * 100))
				+ "% medium term memorized";
		text += ", " + ((int) (info.longTerm * 100)) + "% fully learned";
		
		StringBuilder str = new StringBuilder();
		str.append("Cherokee Language Bound Pronouns");
		str.append("\n");
		str.append("Level: ");
		str.append(info.level.getLevel());
		str.append(" - ");
		str.append(info.level);
		str.append("\n");
		str.append(text);
		str.append("\n");

		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, str.toString());
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent,
				"Share My Statistics ..."));
	}
}
