package com.cherokeelessons.bp.ios;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.foundation.NSURLRequest;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIEvent;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.apple.uikit.UIWebView;
import org.robovm.apple.uikit.UIWebViewDelegateAdapter;
import org.robovm.apple.uikit.UIWindow;
import org.robovm.rt.bro.Bro;
import org.robovm.rt.bro.NativeObject;
import org.robovm.rt.bro.annotation.Bridge;
import org.robovm.rt.bro.annotation.Library;

import com.badlogic.gdx.Gdx;
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


@Library("Foundation")
public class Platform implements PlatformInterface {

	static {
		Bro.bind();
	}

	private static class iOSCodeReceiver implements
			VerificationCodeReceiver {
		private String code = null;

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

	final private iOSCodeReceiver codeReceiver;

	public Platform() {
		codeReceiver = new iOSCodeReceiver();
	}
	
	private class WebViewDelegate extends UIWebViewDelegateAdapter {
		@Override
		public void didStartLoad(UIWebView webView) {
			UIApplication.getSharedApplication()
					.setNetworkActivityIndicatorVisible(true);
			Gdx.app.log("Platform", "didStartLoad: " + webView.getRequest().getURL().getAbsoluteString());
		}

		@Override
		public void didFinishLoad(UIWebView webView) {
			UIApplication.getSharedApplication()
					.setNetworkActivityIndicatorVisible(false);
			Gdx.app.log("Platform", "didFinishLoad: " + webView.getRequest().getURL().getAbsoluteString());
		}

		@Override
		public void didFailLoad(UIWebView webView, NSError error) {
			super.didFailLoad(webView, error);
			UIApplication.getSharedApplication()
			.setNetworkActivityIndicatorVisible(false);
			Gdx.app.log("Platform", "didFailLoad: " + webView.getRequest().getURL().getAbsoluteString());
		}		
	}	

	@Override
	public Credential getCredential(GoogleAuthorizationCodeFlow flow)
			throws IOException {
		return new AuthorizationCodeInstalledApp(flow, codeReceiver) {
			@Override
			public Credential authorize(final String userId) throws IOException {
				final AuthorizationCodeFlow flow = this.getFlow();
				try {
					Credential credential = flow.loadCredential(userId);
					if (credential != null
							&& (credential.getRefreshToken() != null || credential
									.getExpiresInSeconds() > 60)) {
						return credential;
					}
					// open in webview
					Gdx.app.log("AndroidGameServices", "Opening OAUTH Webview");
					final String redirectUri = codeReceiver.getRedirectUri();
					AuthorizationCodeRequestUrl authorizationUrl = flow
							.newAuthorizationUrl().setRedirectUri(redirectUri);
					login(authorizationUrl.build());
					long timeout = 1000l * 60l * 10l;// 10minutes;
					String code = null;
					Gdx.app.log("AndroidGameServices",
							"Waiting For Authorization Code");
					while (timeout > 0) {
						code = codeReceiver.waitForCode();
						if (code == null) {
							try {
								Thread.sleep(250l);
								timeout -= 250l;
								continue;
							} catch (InterruptedException e) {
								Gdx.app.log("AndroidGameServices",
										"Interrupted");
								return null;
							}
						}
						Gdx.app.log("AndroidGameServices",
								"Received Authorization Code");
						break;
					}
					code = codeReceiver.waitForCode();
					TokenResponse response = flow.newTokenRequest(code)
							.setRedirectUri(redirectUri).execute();
					return flow.createAndStoreCredential(response, userId);
				} finally {
					codeReceiver.stop();
				}
			}
		}.authorize("user");
	}

	
	private void login(final String url) {
		Gdx.app.log("Platform", "webViewLogin");
		final UIWebView wv = new UIWebView();
		
		NSURL nsurl=new NSURL(url);
		NSURLRequest request=new NSURLRequest(nsurl);
		wv.setDelegate(new WebViewDelegate());
		wv.loadRequest(request);
		
		 wv.setFrame(new CGRect(115.0f, 158.0f, 800.0f, 1080.0f));
		 final int[] clickCount=new int[]{0};
		 final UIButton button = UIButton.create(UIButtonType.RoundedRect);
	        button.setFrame(new CGRect(115.0f, 121.0f, 91.0f, 37.0f));
	        button.setTitle("Click me!", UIControlState.Normal);

	        button.addOnTouchUpInsideListener(new UIControl.OnTouchUpInsideListener() {
	            @Override
	            public void onTouchUpInside(UIControl control, UIEvent event) {
	                String res = "" + (++clickCount[0]);
	                wv.evaluateJavaScript("document.getElementById('h').innerHTML='" + res + "';");
	                button.setTitle("Click #" + res, UIControlState.Normal);
	            }				
	        });
	        
	        UIWindow window = new UIWindow(UIScreen.getMainScreen().getBounds());
	        window.setBackgroundColor(UIColor.lightGray());
	        window.addSubview(button);
	        window.addSubview(wv);
	        window.makeKeyAndVisible();
		
	}
//	@SuppressLint("SetJavaScriptEnabled")
//	private void login(final String url) {
//		Gdx.app.log("AndroidGameServices", "webViewLogin");
//		application.runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				final Builder alert = new AlertDialog.Builder(application);
//				alert.setTitle("Google Play Services");
//				alert.setNegativeButton("CLOSE",
//						new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog,
//									int which) {
//								dialog.dismiss();
//							}
//						});
//				final WebView webView = new WebView(application) {
//					@Override
//					public boolean onCheckIsTextEditor() {
//						return true;
//					}
//				};
//				alert.setView(webView);
//				alert.setCancelable(true);
//				final AlertDialog adialog = alert.show();
//				final OnDismissListener listener = new OnDismissListener() {
//					@Override
//					public void onDismiss(DialogInterface dialog) {
//						webView.loadUrl("about:blank");
//						if (codeReceiver.getCode() == null) {
//							codeReceiver.setCode("");
//						}
//					}
//				};
//				adialog.setOnDismissListener(listener);
//				webView.loadUrl("about:blank");
//
//				application.runOnUiThread(new Runnable() {
//					@Override
//					public void run() {
//						WebSettings settings = webView.getSettings();
//						settings.setBuiltInZoomControls(false);
//						settings.setDefaultTextEncodingName("UTF-8");
//						settings.setJavaScriptEnabled(true);
//						settings.setJavaScriptCanOpenWindowsAutomatically(true);
//						settings.setLoadsImagesAutomatically(true);
//						settings.setSaveFormData(true);
//						settings.setUseWideViewPort(false);
//
//						settings.setFixedFontFamily("FreeMono");
//						settings.setSansSerifFontFamily("FreeSans");
//						settings.setSerifFontFamily("FreeSerif");
//						settings.setStandardFontFamily("FreeSerif");
//						settings.setLoadWithOverviewMode(true);
//
//						webView.requestFocus(View.FOCUS_DOWN);
//						webView.setOnTouchListener(new View.OnTouchListener() {
//							@SuppressLint("ClickableViewAccessibility")
//							@Override
//							public boolean onTouch(View v, MotionEvent event) {
//								switch (event.getAction()) {
//								case MotionEvent.ACTION_DOWN:
//								case MotionEvent.ACTION_UP:
//									if (!v.hasFocus()) {
//										v.requestFocus();
//									}
//									break;
//								}
//								return false;
//							}
//						});
//
//						webView.setWebViewClient(new WebViewClient() {
//							@Override
//							public void onPageFinished(WebView view, String url) {
//								String title = StringUtils.defaultString(view
//										.getTitle());
//								if (title.toLowerCase().contains("denied")) {
//									adialog.dismiss();
//									codeReceiver.setCode("");
//									return;
//								}
//								if (title.toLowerCase().contains("code=")) { // OAuth2ClientCredentials.OAUTH_CALLBACK_URL))
//									webView.loadUrl("about:blank");
//									String code = StringUtils.substringAfter(
//											title, "code=");
//									if (code.contains("&")) {
//										code = StringUtils.substringBefore(
//												code, "&");
//									}
//									if (StringUtils.isBlank(code)) {
//										Gdx.app.log("AndroidGameServices",
//												"Did not receive a code.");
//										adialog.dismiss();
//										codeReceiver.setCode("");
//										return;
//									}
//									codeReceiver.setCode(code);
//									Gdx.app.log("AndroidGameServices",
//											"Received code: " + code);
//									adialog.dismiss();
//								}
//							}
//
//							@Override
//							public void onReceivedError(WebView view,
//									int errorCode, String description,
//									String failingUrl) {
//								super.onReceivedError(view, errorCode,
//										description, failingUrl);
//								Gdx.app.log(
//										"AndroidGameServices#onReceivedError",
//										"[" + errorCode + "] " + description);
//							}
//
//							@Override
//							public void onReceivedSslError(WebView view,
//									SslErrorHandler handler, SslError error) {
//								super.onReceivedSslError(view, handler, error);
//								Gdx.app.log(
//										"AndroidGameServices#onReceivedSslError",
//										error.toString());
//							}
//						});
//						webView.loadUrl(url);
//					}
//				});
//			}
//		});
//	}

	@Override
	public void runTask(final Runnable runnable) {
		new Thread(runnable).start();
	}

	@Override
	public HttpTransport getTransport() {
		return AndroidHttp.newCompatibleTransport();
	}
}
