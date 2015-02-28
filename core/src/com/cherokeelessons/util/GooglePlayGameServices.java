package com.cherokeelessons.util;

import java.util.ArrayList;
import java.util.List;


public interface GooglePlayGameServices {
	public void login(Callback<Boolean> done);
	public void logout(Callback<Void> done);
	public void lb_submit(String boardId, long score, String label, Callback<Void> done);
	public void lb_getScoresFor(String boardId, Callback<GameScores> list);
	public void lb_getListFor(String boardId, Callback<GameScores> list);
	public void lb_getListWindowFor(String boardId, Callback<GameScores> list);
	public void ach_reveal(String id, Callback<Void> done);
	public void ach_unlocked(String id, Callback<Void> done);
	public void ach_list(Callback<GameAchievements> list);
	
	public static abstract class Callback<T> implements Runnable {		
		private T data;
		public T getData() {
			return data;
		}
		public void setData(T data) {
			this.data=data;
		}
		public Callback() {
		}
		@Override
		public abstract void run();
	}
	
	public static enum Collection {
		PUBLIC, SOCIAL;
	}
	
	public static enum TimeSpan {
		ALL_TIME, DAILY, WEEKLY;
	}
	
	public static class GameAchievements {
		public static class GameAchievement {
			public String id;
			public String state;
		}
		public List<GameAchievement> list =new ArrayList<>();
		
	}
	
	public static class GameScores {
		public static class GameScore {
			public String rank;
			public String value;
			public String tag;
		}
		public List<GameScore> list=new ArrayList<>();
	}
}
