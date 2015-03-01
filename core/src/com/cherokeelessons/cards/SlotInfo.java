package com.cherokeelessons.cards;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SlotInfo implements Serializable {
	private static final int StatsVersion = 10;
	public static final int FULLY_LEARNED_BOX = 10;
	public static final int PROFICIENT_BOX = 5;
	public static final int JUST_LEARNED_BOX = 1;

	public static enum LevelName {
		Newbie("Newbie", 0, "CgkIy7GTtc0TEAIQBg"), Novice("Novice", 1,
				"CgkIy7GTtc0TEAIQBw"), Rookie("Rookie", 2, "CgkIy7GTtc0TEAIQCA"), Beginner(
				"Beginner", 3, "CgkIy7GTtc0TEAIQCQ"), Apprentice("Apprentice",
				4, "CgkIy7GTtc0TEAIQCg"), Intermediate("Intermediate", 5,
				"CgkIy7GTtc0TEAIQCw"), Advanced("Advanced", 6,
				"CgkIy7GTtc0TEAIQDA"), Proficient("Proficient", 7,
				"CgkIy7GTtc0TEAIQDQ"), Expert("Expert", 8, "CgkIy7GTtc0TEAIQDg"), Master(
				"Master", 9, "CgkIy7GTtc0TEAIQDw"), GrandMaster("Grandmaster",
				10, "CgkIy7GTtc0TEAIQEA");

		private final int level;
		private final String engrish;
		private final String id;

		public static LevelName getById(String id) {
			for (LevelName level : LevelName.values()) {
				if (level.id.equals(id)) {
					return level;
				}
			}
			return Newbie;
		}

		public static LevelName getNextById(String id) {
			LevelName current = getById(id);
			return getNext(current);
		}

		public static LevelName getNext(LevelName current) {
			if (current.equals(GrandMaster)) {
				return GrandMaster;
			}
			LevelName[] values = LevelName.values();
			for (int ix = 0; ix < values.length - 1; ix++) {
				LevelName level = values[ix];
				if (level.equals(current)) {
					return values[ix + 1];
				}
			}
			return Newbie;
		}

		public String getId() {
			return id;
		}

		public int getAchievementPoints() {
			return (level + 1) * 5;
		}

		private LevelName(String engrish, int level, String id) {
			this.engrish = engrish;
			this.level = level;
			this.id = id;
		}

		public static LevelName forLevel(int level_number) {
			LevelName level = Newbie;
			for (LevelName maybe : LevelName.values()) {
				if (maybe.level == level_number) {
					return maybe;
				}
				if (maybe.level > level.level && maybe.level < level_number) {
					level = maybe;
				}
			}
			return level;
		}

		public int getLevel() {
			return level;
		}

		public String getEngrish() {
			return engrish;
		}

		@Override
		public String toString() {
			return getEngrish();
		}

	}

	public static enum DisplayMode {
		Syllabary("Only show Syllabary"), Latin("Only show Latin"), Both(
				"Show both Syllabary and Latin");
		private DisplayMode(String engrish) {
			this.engrish = engrish.intern();
		}

		private String engrish;

		public String toString() {
			return engrish;
		};

		public static DisplayMode getNext(DisplayMode mode) {
			for (int ix = 0; ix < values().length - 1; ix++) {
				if (values()[ix].equals(mode)) {
					return values()[ix + 1];
				}
			}
			return values()[0];
		}
	}

	public static enum DeckMode {
		Both("Both bound prefixes and conjugated forms"), Pronouns(
				"Only the bound pronoun prefixes"), Conjugations(
				"Only the conjugated forms");

		private DeckMode(String engrish) {
			this.engrish = engrish.intern();
		}

		private String engrish;

		public String toString() {
			return engrish;
		};

		public static DeckMode getNext(DeckMode mode) {
			for (int ix = 0; ix < values().length - 1; ix++) {
				if (values()[ix].equals(mode)) {
					return values()[ix + 1];
				}
			}
			return values()[0];
		}
	}

	public static enum SessionLength {
		Brief("Brief: about 5-8 minutes", 5f), Standard(
				"Standard: about 10-15 minutes", 10f), Long(
				"Long: about 15-20 minutes", 15f), BrainNumbing(
				"Brain Numbing: very long", 60f);

		final private float seconds;
		final private String engrish;

		private SessionLength(String engrish, float minutes) {
			this.engrish = engrish.intern();
			this.seconds = minutes * 60f;
		}

		public float getSeconds() {
			return seconds;
		}

		public String toString() {
			return engrish;
		};

		public static SessionLength getNext(SessionLength mode) {
			for (int ix = 0; ix < values().length - 1; ix++) {
				if (values()[ix].equals(mode)) {
					return values()[ix + 1];
				}
			}
			return values()[0];
		}
	}

	/**
	 * Time limit in seconds per challenge.
	 * 
	 * @author mjoyner
	 *
	 */
	public static enum TimeLimit {
		Expert("Expert: Max 10 seconds", 10f), Standard(
				"Standard: Max 15 seconds", 15f), Novice(
				"Novice: Max 30 seconds", 30f), Newbie("Newbie: Max 1 hour",
				60f * 60f);

		private TimeLimit(String engrish, float seconds) {
			this.engrish = engrish.intern();
			this.seconds = seconds;
		}

		final private float seconds;

		public float getSeconds() {
			return seconds;
		}

		final private String engrish;

		public String toString() {
			return engrish;
		};

		public static TimeLimit getNext(TimeLimit mode) {
			for (int ix = 0; ix < values().length - 1; ix++) {
				if (values()[ix].equals(mode)) {
					return values()[ix + 1];
				}
			}
			return values()[0];
		}
	}

	public static class Settings {
		public String name = "";
		public DisplayMode display = DisplayMode.Both;
		public DeckMode deck = DeckMode.Both;
		public boolean muted = false;
		/**
		 * Time limit per session.
		 */
		public SessionLength sessionLength = SessionLength.Standard;
		/**
		 * Time limit per card.
		 */
		public TimeLimit timeLimit = TimeLimit.Standard;

		public Settings() {
		}

		public Settings(Settings settings) {
			this.deck = settings.deck;
			this.display = settings.display;
			this.muted = settings.muted;
			this.name = settings.name;
			this.sessionLength = settings.sessionLength;
			this.timeLimit = settings.timeLimit;
		}

		public void validate() {
			if (display == null) {
				display = DisplayMode.Both;
			}
			if (deck == null) {
				deck = DeckMode.Both;
			}
			if (sessionLength == null) {
				sessionLength = SessionLength.Standard;
			}
			if (timeLimit == null) {
				timeLimit = TimeLimit.Standard;
			}
		}
	}

	/**
	 * The summed "box" values for all active cards
	 */
	public int fullScore = 0;
	/**
	 * The summbed "box" values for the most recent learning session
	 */
	public int sessionScore = 0;

	public int activeCards = 0;
	public float shortTerm = 0f;
	public float mediumTerm = 0f;
	public float longTerm = 0f;
	public Settings settings = new Settings();
	private int version;
	public LevelName level;
	public int lastScore;

	public SlotInfo() {
	}

	public SlotInfo(SlotInfo info) {
		this.activeCards = info.activeCards;
		this.level = info.level;
		this.longTerm = info.longTerm;
		this.mediumTerm = info.mediumTerm;
		this.settings = new Settings(info.settings);

	}

	public void validate() {
		if (level == null) {
			level = LevelName.Newbie;
		}
		if (settings == null) {
			settings = new Settings();
		}
		settings.validate();
	}

	public int getVersion() {
		return StatsVersion;
	}

	public boolean updatedVersion() {
		return version == StatsVersion;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public static void calculateStats(SlotInfo info, ActiveDeck activeDeck) {
		if (activeDeck == null || info == null || activeDeck.deck.size() == 0) {
			return;
		}

		/*
		 * Set "level" to ceil(average box value) found in active deck. Negative
		 * box values are ignored.
		 */

		int boxsum = 0;
		for (ActiveCard card : activeDeck.deck) {
			boxsum += (card.box > 0 ? card.box : 0);
		}
		info.level = LevelName.forLevel((int) Math.ceil((double) (boxsum)
				/ (double) activeDeck.deck.size()));
		/*
		 * Set "fullScore" to sum of all box values found in actice deck
		 */
		boxsum = 0;
		for (ActiveCard card : activeDeck.deck) {
			boxsum += card.box;
		}
		info.fullScore = boxsum;
		/*
		 * Set last score based on timings of most recent session. Cards with
		 * errors count as "-1" each.
		 */
		float score = 0f;
		for (ActiveCard card : activeDeck.deck) {
			if (card.showCount == 0) {
				continue;
			}
			if (!card.noErrors) {
				score -= 1f;
				continue;
			}
			score += SlotInfo.TimeLimit.Standard.getSeconds()
					- Math.floor(card.showTime / (float) card.showCount);
		}
		info.lastScore = (int) Math.ceil(score);

		/*
		 * How many are "fully learned" out of the active deck?
		 */
		final float decksize = activeDeck.deck.size();
		float full = 0f;
		for (ActiveCard card : activeDeck.deck) {
			if (card.box >= FULLY_LEARNED_BOX) {
				full++;
			}
		}
		info.longTerm = full / decksize;

		/*
		 * count all active cards that aren't "fully learned"
		 */
		info.activeCards = activeDeck.deck.size() - (int) full;

		/*
		 * How many are "well known" out of the active deck? (excluding full
		 * learned ones)
		 */
		full = 0f;
		for (ActiveCard card : activeDeck.deck) {
			if (card.box >= PROFICIENT_BOX && card.box < FULLY_LEARNED_BOX) {
				full++;
			}
		}
		info.mediumTerm = full / decksize;

		/*
		 * How many are "short term known" out of the active deck? (excluding
		 * full learned ones)
		 */
		full = 0f;
		for (ActiveCard card : activeDeck.deck) {
			if (card.box >= JUST_LEARNED_BOX && card.box < PROFICIENT_BOX) {
				full++;
			}
		}
		info.shortTerm = full / decksize;
	}
}
