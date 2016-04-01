package com.cherokeelessons.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.Collection;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores.GameScore;
import com.cherokeelessons.util.GooglePlayGameServices.TimeSpan;

public class LocalLeaderboard implements LeaderboardClient {

	private final Preferences prefs;

	public LocalLeaderboard(Preferences prefs) {
		this.prefs = prefs;
	}

	@Override
	public void lb_submit(String boardId, long score, String label, final Callback<Void> callback) {
		List<String> scores = new ArrayList<>(Arrays.asList(prefs.getString("leaderboard", "").split("\n")));
		scores.add(0, score + "\t" + label);
		if (scores.size() > 100) {
			scores = scores.subList(0, 100);
		}
		StringBuilder sb = new StringBuilder();
		for (String tmp : scores) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(tmp);
		}
		prefs.putString("leaderboard", sb.toString());
		prefs.flush();
		sb.setLength(0);
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				callback.success(null);
			}
		});
	}

	@Override
	public void lb_getScoresFor(String boardId, final Callback<GameScores> callback) {
		List<String> scores = new ArrayList<>(Arrays.asList(prefs.getString("leaderboard", "").split("\n")));
		final GameScores gss = new GameScores();
		gss.list = new ArrayList<>();
		for (String score : scores) {
			String[] s = score.split("\t");
			if (s == null || s.length == 0) {
				continue;
			}
			GameScore gs = new GameScore();
			gs.score = s[0].trim();
			if (s.length > 1) {
				gs.tag = s[1];
			}
			if (s.length > 2) {
				gs.user = s[2];
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
					v1 = Long.valueOf(o1.score);
				} catch (NumberFormatException e) {
					v1 = 0;
				}
				try {
					v2 = Long.valueOf(o2.score);
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
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				callback.success(gss);
			}
		});
	}

	@Override
	public void lb_getListFor(String boardId, Collection collection, TimeSpan ts, Callback<GameScores> callback) {
		lb_getScoresFor(boardId, callback);
	}

	@Override
	public void lb_getListWindowFor(String boardId, Collection collection, TimeSpan ts, Callback<GameScores> callback) {
		lb_getScoresFor(boardId, callback);
	}

}
