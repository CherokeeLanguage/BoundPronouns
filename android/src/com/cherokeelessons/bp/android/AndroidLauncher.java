package com.cherokeelessons.bp.android;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Application;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

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
		BoundPronouns.services = new AndroidGameServices(this);
		BoundPronouns game = new BoundPronouns();
		initialize(game, config);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void fbshare(SlotInfo info) {
		info.validate();

		String txt = "";
		txt += "Score: " + info.lastScore;
		txt += "\n";
		txt += info.activeCards + " cards";
		txt += ": ";
		txt += info.shortTerm + " short";
		txt += ", " + info.mediumTerm + " medium";
		txt += ", " + info.longTerm + " long";
		final StringBuilder str = new StringBuilder();
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
			str.append(URLEncoder.encode(info.level.getEngrish(), "UTF-8"));
			str.append("&description=");
			str.append(URLEncoder.encode(txt, "UTF-8"));
			str.append("&name=");
			str.append(URLEncoder.encode("Cherokee Language Bound Pronouns",
					"UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			return;
		}
		final Application application=this.getApplication();
		this.runOnUiThread(new Runnable() {			
			@SuppressLint("SetJavaScriptEnabled")
			@Override
			public void run() {
				final Builder alert = new AlertDialog.Builder(application);
				alert.setTitle("Facebook");
				alert.setNegativeButton("DISMISS",
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						dialog.dismiss();
					}
				});
				
				final WebView webView = new WebView(application) {
					@Override
					public boolean onCheckIsTextEditor() {
						return true;
					}
				};
				alert.setView(webView);
				alert.setCancelable(true);
				final AlertDialog adialog = alert.show();
				final OnDismissListener listener = new OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						webView.loadUrl("about:blank");				
					}
				};
				adialog.setOnDismissListener(listener);
				
				WebSettings settings = webView.getSettings();
				settings.setBuiltInZoomControls(false);
				settings.setDefaultTextEncodingName("UTF-8");
				settings.setJavaScriptEnabled(true);
				settings.setJavaScriptCanOpenWindowsAutomatically(true);
				settings.setLoadsImagesAutomatically(true);
				settings.setSaveFormData(true);
				settings.setUseWideViewPort(false);

				settings.setFixedFontFamily("FreeMono");
				settings.setSansSerifFontFamily("FreeSans");
				settings.setSerifFontFamily("FreeSerif");
				settings.setStandardFontFamily("FreeSerif");
				settings.setLoadWithOverviewMode(true);

				webView.requestFocus(View.FOCUS_DOWN);
				webView.setOnTouchListener(new View.OnTouchListener() {
					@SuppressLint("ClickableViewAccessibility")
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
						case MotionEvent.ACTION_UP:
							if (!v.hasFocus()) {
								v.requestFocus();
							}
							break;
						}
						return false;
					}
				});
				
				webView.loadUrl(str.toString());
			}
		});

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
