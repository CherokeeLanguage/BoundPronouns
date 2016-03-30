package com.cherokeelessons.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.Preferences;
import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.Collection;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores;
import com.cherokeelessons.util.GooglePlayGameServices.TimeSpan;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;

public class DreamLo implements LeaderboardClient {
	private static final String DREAMLO_USERID = "dreamlo-userid";
	private static final String writeUrl = "http://dreamlo.com/lb/" + "1KLmiETsgkKwjRY-BUVjogsht9ozTZpUWLMmilJeSB-Q";
	private static final String readUrl = "http://dreamlo.com/lb/" + "56fb26656e51b603cc253197/pipe";
	/**
	 * boardId = "animal-slot#-timstamp-random";
	 */
	private final Preferences prefs;

	public DreamLo(Preferences prefs) {
		this.prefs = prefs;
	}

	private boolean registeredWithBoard() {
		if (prefs.getString(DREAMLO_USERID, "").length()==0) {
			if (!registeredListenerPending) {
				Gdx.app.log("DreamLo", "registeredWithBoard: false");
				registeredListenerPending = true;
				Gdx.app.postRunnable(registerWithBoard);
			}
			return false;
		}
		Gdx.app.log("DreamLo", "registeredWithBoard: true");
		return true;
	}

	private boolean registeredListenerPending = false;
	private HttpResponseListener registeredListener = new HttpResponseListener() {
		@Override
		public void handleHttpResponse(HttpResponse httpResponse) {
			registeredListenerPending = false;
		}

		@Override
		public void failed(Throwable t) {
			registeredListenerPending = false;
		}

		@Override
		public void cancelled() {
			registeredListenerPending = false;
		}
	};

	private Runnable registerWithBoard = new Runnable() {
		@Override
		public void run() {
			Gdx.app.log("DreamLo", "registerWithBoard");
			HttpRequest httpRequest = new HttpRequest("GET");
			httpRequest.setUrl(readUrl + "/pipe");
			Gdx.app.log("DreamLo", "URL: '"+httpRequest.getUrl()+"'");
			HttpResponseListener httpResponseListener = new HttpResponseListener() {
				@Override
				public void handleHttpResponse(HttpResponse httpResponse) {
					Gdx.app.log("DreamLo", "registerWithBoard: "+httpResponse.getResultAsString());
					String str_scores = httpResponse.getResultAsString();
					String[] scores = str_scores.split("\n");
					Random r = new Random();
					int id = 0;
					tryagain: while (true) {
						id = r.nextInt(Integer.MAX_VALUE) + 1;
						for (String score : scores) {
							if (score.contains(id + "")) {
								continue tryagain;
							}
						}
						break tryagain;
					}
					HttpRequest httpRequest = new HttpRequest("GET");
					httpRequest.setTimeOut(10000);
					httpRequest.setUrl(writeUrl + "/add/" + id + "-0/0/0/New+Player");
					Gdx.app.log("DreamLo", "URL: '"+httpRequest.getUrl()+"'");
					Gdx.net.sendHttpRequest(httpRequest, registeredListener);
					prefs.putString(DREAMLO_USERID, id+"");
					prefs.flush();
				}

				@Override
				public void failed(Throwable t) {
					Gdx.app.log("DreamLo", "registerWithBoard: ", t);
				}

				@Override
				public void cancelled() {
					Gdx.app.log("DreamLo", "registerWithBoard: TIMED OUT");
				}
			};
			Gdx.net.sendHttpRequest(httpRequest, httpResponseListener);
		}
	};

	@Override
	public void lb_submit(final String boardId, final long score, final String label, final Callback<Void> callback) {
		if (!registeredWithBoard()) {
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					DreamLo.this.lb_submit(boardId, score, label, callback);
				}
			});
			return;
		}
		String encoded;
		try {
			encoded = URLEncoder.encode(label, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			encoded = label.replaceAll("[^a-zA-Z0-9]", "+");
		}
		HttpRequest httpRequest = new HttpRequest("GET");
		httpRequest.setTimeOut(10000);
		String url = writeUrl + "/add/" + prefs.getString(DREAMLO_USERID, "") + "-"+boardId+"/"+score+"/0/"+encoded;
		httpRequest.setUrl(url);
		Gdx.app.log("DreamLo", "URL: '"+httpRequest.getUrl()+"'");
		Gdx.net.sendHttpRequest(httpRequest, new HttpResponseListener() {
			@Override
			public void handleHttpResponse(HttpResponse httpResponse) {
				Gdx.app.log("DreamLo", "lb_sumbit: "+httpResponse.getResultAsString());
				callback.success(null);
			}
			
			@Override
			public void failed(Throwable t) {
				Gdx.app.log("DreamLo", "lb_submit", t);
				callback.error(new RuntimeException(t));
			}
			
			@Override
			public void cancelled() {
				Gdx.app.log("DreamLo", "lb_submit: timed out");
				callback.error(new RuntimeException("TIMED OUT"));
			}
		});
	}

	@Override
	public void lb_getScoresFor(final String boardId, final Callback<GameScores> callback) {
		if (!registeredWithBoard()) {
			Gdx.app.postRunnable(new Runnable() {
				public void run() {
					DreamLo.this.lb_getScoresFor(boardId, callback);
				}
			});
			return;
		}
		HttpRequest httpRequest = new HttpRequest("GET");
		httpRequest.setTimeOut(10000);
		httpRequest.setUrl(readUrl + "/pipe");
		Gdx.app.log("DreamLo", "URL: '"+httpRequest.getUrl()+"'");
		Gdx.net.sendHttpRequest(httpRequest, new HttpResponseListener() {
			@Override
			public void handleHttpResponse(HttpResponse httpResponse) {
				String myId=prefs.getString(DREAMLO_USERID, "")+"-";
				Gdx.app.log("DreamLo", "lb_getScoresFor: success");
				List<String> scores = new ArrayList<>(Arrays.asList(httpResponse.getResultAsString().split("\n")));
				final GameScores gss = new GameScores();
				gss.collection = Collection.PUBLIC;
				gss.list = new ArrayList<>();
				gss.ts = TimeSpan.WEEKLY;
				for (String score : scores) {
					if (score==null||score.length()==0) {
						continue;
					}
					String[] s = score.split("\\|");
					if (s == null || s.length == 0) {
						continue;
					}
					/*
					 * 0: username
					 * 1: score
					 * 2: time
					 * 3: tag
					 * 4: date 
					 * 5: index
					 */
					GameScore gs = new GameScore();
					gs.value = s[1].trim();
					if (s.length > 2) {
						String decoded_tag;
						try {
							decoded_tag = URLDecoder.decode(s[3], "UTF-8");
						} catch (UnsupportedEncodingException e) {
							decoded_tag = s[3].replace("+", " ");
						}
						gs.tag = StringUtils.substringBefore(decoded_tag, "!!!");
					}
					if (s.length > 2) {
						String decoded_other_name;
						try {
							decoded_other_name = URLDecoder.decode(s[3], "UTF-8");
						} catch (UnsupportedEncodingException e) {
							decoded_other_name = s[3].replace("+", " ");
						}
						decoded_other_name=StringUtils.substringAfter(decoded_other_name, "!!!");
						String decoded;
						try {
							decoded = URLDecoder.decode(s[0], "UTF-8");
						} catch (UnsupportedEncodingException e) {
							decoded = s[0].replace("+", " ");
						}
						gs.user = decoded;
						if (gs.user.startsWith(myId)) {
							gs.user=decoded_other_name+" ("+gs.user.replace(myId, "")+")";
						} else {
							if (!decoded_other_name.matches(".*?[a-zA-Z].*?")){
								gs.user=decoded_other_name+" ("+StringUtils.substringAfterLast(gs.user, "-")+")";
							}
						}
					}
					gss.list.add(gs);
				}
				Comparator<GameScore> descending = new Comparator<GooglePlayGameServices.GameScores.GameScore>() {
					@Override
					public int compare(GameScore o1, GameScore o2) {
						if (o1 == o2) {
							return 0;
						}
						if (o2 == null) {
							return -1;
						}
						if (o1 == null) {
							return 1;
						}
						long v1;
						long v2;
						try {
							v1 = Long.valueOf(o1.value);
						} catch (NumberFormatException e) {
							v1 = 0;
						}
						try {
							v2 = Long.valueOf(o2.value);
						} catch (NumberFormatException e) {
							v2 = 0;
						}
						return v1 < v2 ? 1 : v1 > v2 ? -1 : 0;
					}
				};
				Collections.sort(gss.list, descending);
				for (int ix = 0; ix < gss.list.size(); ix++) {
					GameScore tmp = gss.list.get(ix);
					switch (ix+1) {
					case 1:
						tmp.rank = "1st";
						break;
					case 2:
						tmp.rank = "2nd";
						break;
					case 3:
						tmp.rank = "3rd";
						break;
					default:
						tmp.rank = (ix+1) + "th";
						break;
					}
				}
				callback.success(gss);
			}
			
			@Override
			public void failed(Throwable t) {
				Gdx.app.log("DreamLo", "lb_getScoresFor:", t);
				callback.error(new RuntimeException(t));
			}
			
			@Override
			public void cancelled() {
				Gdx.app.log("DreamLo", "lb_getScoresFor: timed out");
				callback.error(new RuntimeException("TIMED OUT"));
			}
		});
	}

	@Override
	public void lb_getListFor(final String boardId, Collection collection, TimeSpan ts,
			final Callback<GameScores> callback) {
		Gdx.app.postRunnable(new Runnable() {
			public void run() {
				DreamLo.this.lb_getScoresFor(boardId, callback);
			}
		});
	}

	@Override
	public void lb_getListWindowFor(final String boardId, Collection collection, TimeSpan ts,
			final Callback<GameScores> callback) {
		Gdx.app.postRunnable(new Runnable() {
			public void run() {
				DreamLo.this.lb_getScoresFor(boardId, callback);
			}
		});
	}
}
