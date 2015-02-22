package com.cherokeelessons.bp.android;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
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
		try {
			str.append("https://www.facebook.com/dialog/feed?");
			str.append("&app_id=");
			str.append("148519351857873");
			str.append("&redirect_uri=");
			str.append(URLEncoder
					.encode("http://www.cherokeelessons.com/phpBB3/viewforum.php?f=24#",
							"UTF-8"));
			str.append("&link=");
			str.append(URLEncoder
					.encode("http://www.cherokeelessons.com/phpBB3/viewtopic.php?f=24&t=73#p230",
							"UTF-8"));
			str.append("&picture=");
			str.append(URLEncoder
					.encode("http://www.cherokeelessons.com/phpBB3/download/file.php?id=242",
							"UTF-8"));
			str.append("&caption=");
			str.append(URLEncoder.encode("Level: " + info.level.getLevel()
					+ " - " + info.level, "UTF-8"));
			str.append("&description=");
			str.append(URLEncoder.encode(text, "UTF-8"));
			str.append("&name=");
			str.append(URLEncoder.encode("Cherokee Language Bound Pronouns",
					"UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			return;
		}

		Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(str
				.toString()));
		startActivity(Intent.createChooser(sendIntent,
				"Share My Statistics Using Facebook Webview"));
	}

	public void fbshare_text(SlotInfo info) {
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
