package com.cherokeelessons.bp.android;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.cherokeelessons.play.GameServices.PlatformInterface;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;

@SuppressLint("DefaultLocale")
public class Platform implements PlatformInterface {
	
	public static AndroidApplication application;

	private static class AndroidCodeReceiver implements VerificationCodeReceiver {
		private String code = "";

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		@Override
		public String getRedirectUri() throws IOException {
			return "urn:ietf:wg:oauth:2.0:oob:auto";
		}

		@Override
		public String waitForCode() throws IOException {
			return code;
		}

		@Override
		public void stop() throws IOException {
		}

	}
	
	final AndroidCodeReceiver codeReceiver;
	public Platform() {
		codeReceiver = new AndroidCodeReceiver();
	}

	@Override
	public Credential getCredential(GoogleAuthorizationCodeFlow flow) throws IOException {		
		return new AuthorizationCodeInstalledApp(
				flow, codeReceiver) {

			@Override
			public Credential authorize(final String userId) throws IOException {
				final AuthorizationCodeFlow flow = this.getFlow();
				final VerificationCodeReceiver receiver = this.getReceiver();
				try {
					Credential credential = flow.loadCredential(userId);
					if (credential != null
							&& (credential.getRefreshToken() != null || credential
									.getExpiresInSeconds() > 60)) {
						return credential;
					}
					// open in webview
					Gdx.app.log("AndroidGameServices", "Opening OAUTH Webview");
					final String redirectUri = receiver.getRedirectUri();
					AuthorizationCodeRequestUrl authorizationUrl = flow
							.newAuthorizationUrl().setRedirectUri(redirectUri);
					login(authorizationUrl.build());
					long timeout = 1000l * 60l * 10l;// 10minutes;
					String code = null;
					while (timeout > 0) {
						code = receiver.waitForCode();
						if (code == null) {
							try {
								Thread.sleep(50l);
								timeout -= 50l;
								continue;
							} catch (InterruptedException e) {
								Gdx.app.log("AndroidGameServices",
										"Interrupted");
								return null;
							}
						}
						break;
					}
					code = receiver.waitForCode();
					TokenResponse response = flow.newTokenRequest(code)
							.setRedirectUri(redirectUri).execute();
					return flow.createAndStoreCredential(response, userId);
				} finally {
					receiver.stop();
				}
			}
		}.authorize("user");		
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void login(final String url) {
		Gdx.app.log("AndroidGameServices", "webViewLogin");
		application.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final Builder alert = new AlertDialog.Builder(application);
				alert.setTitle("Google Play Services");
				alert.setNegativeButton("CLOSE",
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
						if (codeReceiver.getCode() == null) {
							codeReceiver.setCode("");
						}
					}
				};
				adialog.setOnDismissListener(listener);
				webView.loadUrl("about:blank");

				application.runOnUiThread(new Runnable() {
					@Override
					public void run() {
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

						webView.setWebViewClient(new WebViewClient() {
							@Override
							public void onPageFinished(WebView view, String url) {
								String title = StringUtils.defaultString(view
										.getTitle());
								if (title.toLowerCase().contains("denied")) {
									adialog.dismiss();
									codeReceiver.setCode("");
									return;
								}
								if (title.toLowerCase().contains("code=")) { // OAuth2ClientCredentials.OAUTH_CALLBACK_URL))
									webView.loadUrl("about:blank");
									String code = StringUtils.substringAfter(
											title, "code=");
									if (code.contains("&")) {
										code = StringUtils.substringBefore(
												code, "&");
									}
									if (StringUtils.isBlank(code)) {
										Gdx.app.log("AndroidGameServices",
												"Did not receive a code.");
										adialog.dismiss();
										codeReceiver.setCode("");
										return;
									}
									codeReceiver.setCode(code);
									Gdx.app.log("AndroidGameServices",
											"Received code: " + code);
									adialog.dismiss();
								}
							}

							@Override
							public void onReceivedError(WebView view,
									int errorCode, String description,
									String failingUrl) {
								super.onReceivedError(view, errorCode,
										description, failingUrl);
								Gdx.app.log(
										"AndroidGameServices#onReceivedError",
										"[" + errorCode + "] " + description);
							}

							@Override
							public void onReceivedSslError(WebView view,
									SslErrorHandler handler, SslError error) {
								super.onReceivedSslError(view, handler, error);
								Gdx.app.log(
										"AndroidGameServices#onReceivedSslError",
										error.toString());
							}
						});
						webView.loadUrl(url);
					}
				});
			}
		});
	}

	@Override
	public void runTask(final Runnable runnable) {
		application.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						runnable.run();
						return null;
					}
				}.execute();
			}
		});		
	}

	@Override
	public HttpTransport getTransport() {
		return AndroidHttp.newCompatibleTransport();
	}
}
