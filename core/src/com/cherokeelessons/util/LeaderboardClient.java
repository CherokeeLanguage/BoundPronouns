package com.cherokeelessons.util;

import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.Collection;
import com.cherokeelessons.util.GooglePlayGameServices.GameScores;
import com.cherokeelessons.util.GooglePlayGameServices.TimeSpan;

public interface LeaderboardClient {
	public void lb_submit(String boardId, long score, String label, Callback<Void> callback);
	public void lb_getScoresFor(String boardId, Callback<GameScores> callback);
	public void lb_getListFor(String boardId, Collection collection, TimeSpan ts, Callback<GameScores> callback);
	public void lb_getListWindowFor(String boardId, Collection collection, TimeSpan ts, Callback<GameScores> callback);
}
