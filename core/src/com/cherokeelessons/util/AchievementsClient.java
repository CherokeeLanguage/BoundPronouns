package com.cherokeelessons.util;

import com.cherokeelessons.util.GooglePlayGameServices.Callback;
import com.cherokeelessons.util.GooglePlayGameServices.GameAchievements;

public interface AchievementsClient {
	public void ach_reveal(String id, Callback<Void> callback);
	public void ach_unlocked(String id, Callback<Void> callback);
	public void ach_list(Callback<GameAchievements> callback);
}
