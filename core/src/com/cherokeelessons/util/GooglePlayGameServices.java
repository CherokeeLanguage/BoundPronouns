package com.cherokeelessons.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.util.GooglePlayGameServices.FileMetaList.FileMeta;


public interface GooglePlayGameServices {
	
	public Callback<Void> noop=new Callback<Void>() {
		@Override
		public void success(Void result) {
		}
	};
	
	public void login(Callback<Void> callback);
	public void logout(Callback<Void> callback);
	
	public static abstract class Callback<T> {
		public Callback() {
		}
		
		public Runnable with(final Exception e) {
			return new Runnable() {				
				@Override
				public void run() {
					Callback.this.error(e);
				}
			};
		}
		
		public Runnable with(final T data) {
			return new Runnable() {				
				@Override
				public void run() {
					Callback.this.success(data);
				}
			};
		}
		
		public Runnable withNull() {
			return new Runnable() {				
				@Override
				public void run() {
					Callback.this.success(null);
				}
			};
		}
		
		public void error(Exception exception) {
			exception.printStackTrace();
			System.err.flush();
		};
		
		public abstract void success(T result);
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
		DAILY("Today's Best"), WEEKLY("This Week's Best"), ALL_TIME("Alltime Best");
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
		public List<GameAchievement> list =new ArrayList<GameAchievement>();		
	}
	
	public static class GameScores {
		public static class GameScore {
			public String rank;
			public String score;
			public String tag;
			public String user;
			public String imgUrl;
			public String activeCards;
		}
		public List<GameScore> list=new ArrayList<GameScore>();
	}
	
	public static class FileMetaList {
		public List<FileMeta> files=new ArrayList<FileMeta>();
		public static class FileMeta {
//			public Boolean isAppData;
			public Date created;
			public String id;
			public Date lastModified;			
			public String title;
			public String url;
		}
	}

	void drive_getFileById(String id, Callback<String> callback);
	void drive_getFileById(String id, FileHandle file, Callback<Void> callback);
//	void drive_getFileByUrl(String url, FileHandle file, Callback<Void> callback);
//	void drive_getFileByUrl(String url, Callback<String> callback);
	void drive_put(FileHandle file, String title, String description,
			Callback<String> callback);
	void drive_put(FileHandle file, Callback<String> callback);
	void drive_deleteByTitle(String title, Callback<Void> callback);
	void drive_deleteById(String id, Callback<Void> callback);
	void drive_list(Callback<FileMetaList> callback);
	void drive_getFileMetaById(String id, Callback<FileMeta> callback);
	void drive_getFileMetaByTitle(String title, Callback<FileMetaList> callback);
	void drive_getFileByTitle(String title, Callback<String> callback);
	void drive_replace(FileHandle file, String title, String description,
			Callback<String> callback);
	void drive_replace(FileHandle file, Callback<String> callback);
	boolean isLoggedIn();
}
