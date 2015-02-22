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
		
		StringBuilder str = new StringBuilder();
		str.append("Cherokee Language Bound Pronouns");
		str.append("\n");
		str.append("Level: ");
		str.append(info.level.getLevel());
		str.append(" - ");
		str.append(info.level);
		str.append(" - ");
		str.append(info.activeCards);
		str.append(" active cards");
		str.append("\n");
		
		str.append(((int) (info.shortTerm * 100)));
		str.append("% short term memorized");
		str.append(", ");
		str.append(((int) (info.mediumTerm * 100)));
		str.append("% medium term memorized");
		str.append(", ");
		str.append(((int) (info.longTerm * 100)));
		str.append("% fully learned");

		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, str.toString());
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent,
				"Share My Statistics ..."));
	}
}
