package com.cherokeelessons.util;

import java.util.ArrayList;
import java.util.List;


public interface GooglePlayGameServices {
	
	public void login(Callback<Void> success, Callback<Exception> error);
	public void logout(Callback<Void> success, Callback<Exception> error);
	public void lb_submit(String boardId, long score, String label, Callback<Void> success, Callback<Exception> error);
	public void lb_getScoresFor(String boardId, Callback<GameScores> success, Callback<Exception> error);
	public void lb_getListFor(String boardId, Collection collection, TimeSpan ts, Callback<GameScores> success, Callback<Exception> error);
	public void lb_getListWindowFor(String boardId, Collection collection, TimeSpan ts, Callback<GameScores> success, Callback<Exception> error);
	public void ach_reveal(String id, Callback<Void> success, Callback<Exception> error);
	public void ach_unlocked(String id, Callback<Void> success, Callback<Exception> error);
	public void ach_list(Callback<GameAchievements> success, Callback<Exception> error);
	
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
		PUBLIC("Top Public Scores"), SOCIAL("Top Circle Scores");
		final String english;
		public String getEnglish() {
			return english;
		}
		private Collection(String english) {
			this.english=english;
		}
		public Collection next() {
			Collection[] values = Collection.values();
			int ix=(ordinal()+1)%(values.length);
			return values[ix];
		}
	}
	
	public static enum TimeSpan {
		ALL_TIME("Alltime Best"), WEEKLY("This Week's Best"), DAILY("Today's Best");
		private TimeSpan(String engrish) {
			this.engrish=engrish;
		}
		private final String engrish;
		public String getEngrish() {
			return engrish;
		}
		public TimeSpan next() {
			TimeSpan[] values = TimeSpan.values();
			int ix=(ordinal()+1)%(values.length);
			return values[ix];
		}
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
			public String user;
			public String imgUrl;
		}
		public Collection collection;
		public TimeSpan ts;
		public List<GameScore> list=new ArrayList<>();
	}
}
