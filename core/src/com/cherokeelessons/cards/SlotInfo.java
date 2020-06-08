package com.cherokeelessons.cards;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
public class SlotInfo implements Serializable {
	public enum DeckMode {
		Both("Both bound prefixes and conjugated forms"), Pronouns("Only the bound pronoun prefixes"),
		Conjugations("Only the conjugated forms");

		public static DeckMode getNext(final DeckMode mode) {
			for (int ix = 0; ix < values().length - 1; ix++) {
				if (values()[ix].equals(mode)) {
					return values()[ix + 1];
				}
			}
			return values()[0];
		}

		private String english;

		private DeckMode(final String english) {
			this.english = english.intern();
		}

		@Override
		public String toString() {
			return english;
		}
	}

	public enum DisplayMode {
		Both("Syllabary and Latin - Tones Marked"), //
		Syllabary("Syllabary - Tones Marked"), //
		Latin("Latin - Tones Marked"), //
		BOTH_NP("Syllabary and Latin - No Marks"), SYLLABARY_NP("Syllabary - No Marks"), //
		LATIN_NP("Latin - No Marks"), //
		NONE("Audio Only");

		public static DisplayMode getNext(final DisplayMode mode) {
			for (int ix = 0; ix < values().length - 1; ix++) {
				if (values()[ix].equals(mode)) {
					return values()[ix + 1];
				}
			}
			return values()[0];
		}

		private String english;

		private DisplayMode(final String english) {
			this.english = english.intern();
		}

		@Override
		public String toString() {
			return english;
		}
	}

	public enum LevelName {
		Newbie("Newbie", 0, "CgkI4pfA4J4KEAIQAQ"), Novice("Novice", 1, "CgkI4pfA4J4KEAIQAg"),
		Rookie("Rookie", 2, "CgkI4pfA4J4KEAIQAw"), Beginner("Beginner", 3, "CgkI4pfA4J4KEAIQBA"),
		Apprentice("Apprentice", 4, "CgkI4pfA4J4KEAIQBQ"), Intermediate("Intermediate", 5, "CgkI4pfA4J4KEAIQBg"),
		Advanced("Advanced", 6, "CgkI4pfA4J4KEAIQBw"), Proficient("Proficient", 7, "CgkI4pfA4J4KEAIQCA"),
		Expert("Expert", 8, "CgkI4pfA4J4KEAIQCQ"), Master("Master", 9, "CgkI4pfA4J4KEAIQCg"),
		GrandMaster("Grandmaster", 10, "CgkI4pfA4J4KEAIQCw");

		public static LevelName forLevel(final int level_number) {
			LevelName level = Newbie;
			for (final LevelName maybe : LevelName.values()) {
				if (maybe.level == level_number) {
					return maybe;
				}
				if (maybe.level > level.level && maybe.level < level_number) {
					level = maybe;
				}
			}
			return level;
		}

		public static LevelName getById(final String id) {
			for (final LevelName level : LevelName.values()) {
				if (level.id.equals(id)) {
					return level;
				}
			}
			return Newbie;
		}

		public static LevelName getNext(final LevelName current) {
			return current.next();
		}

		public static LevelName getNextById(final String id) {
			return getById(id).next();
		}

		private final int level;

		private final String english;

		private final String id;

		private LevelName(final String english, final int level, final String id) {
			this.english = english;
			this.level = level;
			this.id = id;
		}

		public int getAchievementPoints() {
			return (level + 1) * 5;
		}

		public String getEnglish() {
			return english;
		}

		public String getId() {
			return id;
		}

		public int getLevel() {
			return level;
		}

		public LevelName next() {
			final LevelName[] values = LevelName.values();
			final int ix = (ordinal() + 1) % values.length;
			return values[ix];
		}

		@Override
		public String toString() {
			return getEnglish();
		}

	}

	public enum SessionLength {
		XBrief("XBrief: 2 minutes", 2f), Brief("Brief: 5 minutes", 5f), Standard("Standard: 10 minutes", 10f),
		Long("Long: 15 minutes", 15f), BrainNumbing("Brain Numbing: very long", 60f);

		public static SessionLength getNext(final SessionLength mode) {
			for (int ix = 0; ix < values().length - 1; ix++) {
				if (values()[ix].equals(mode)) {
					return values()[ix + 1];
				}
			}
			return values()[0];
		}

		final private float seconds;

		final private String english;

		private SessionLength(final String english, final float minutes) {
			this.english = english.intern();
			this.seconds = minutes * 60f;
		}

		public float getSeconds() {
			return seconds;
		}

		@Override
		public String toString() {
			return english;
		}
	}

	public static class Settings {
		public String name = "";
		public DisplayMode display = DisplayMode.Both;
		public DeckMode deck = DeckMode.Conjugations;
		public boolean muted = false;
		/**
		 * Time limit per session.
		 */
		public SessionLength sessionLength = SessionLength.Brief;
		/**
		 * Time limit per card.
		 */
		public TimeLimit timeLimit = TimeLimit.Novice;

		public Settings() {
		}

		public Settings(final Settings settings) {
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
				deck = DeckMode.Conjugations;
			}
			if (sessionLength == null) {
				sessionLength = SessionLength.Brief;
			}
			if (timeLimit == null) {
				timeLimit = TimeLimit.Novice;
			}
		}
	}

	/**
	 * Time limit in seconds per challenge.
	 *
	 * @author mjoyner
	 *
	 */
	public enum TimeLimit {
		Expert("Expert: Max 10 seconds", 10f), Standard("Standard: Max 15 seconds", 15f),
		Novice("Novice: Max 60 seconds", 60f), Newbie("Newbie: Max 1 hour", 60f * 60f);

		public static TimeLimit getNext(final TimeLimit mode) {
			for (int ix = 0; ix < values().length - 1; ix++) {
				if (values()[ix].equals(mode)) {
					return values()[ix + 1];
				}
			}
			return values()[0];
		}

		final private float seconds;

		final private String english;

		private TimeLimit(final String english, final float seconds) {
			this.english = english.intern();
			this.seconds = seconds;
		}

		public float getSeconds() {
			return seconds;
		}

		@Override
		public String toString() {
			return english;
		}
	}

	private static final int StatsVersion = 34;
	public static final int FULLY_LEARNED_BOX = 10;

	public static final int PROFICIENT_BOX = 5;

	public static final int JUST_LEARNED_BOX = 1;

	public static void calculateStats(final SlotInfo info, final ActiveDeck activeDeck) {
		if (activeDeck == null || info == null || activeDeck.deck.size() == 0) {
			return;
		}

		/*
		 * Set "level" to ceil(average box value) found in active deck. Negative box
		 * values are ignored.
		 */

		int boxsum = 0;
		for (final ActiveCard card : activeDeck.deck) {
			boxsum += card.box > 0 ? card.box : 0;
		}
		info.level = LevelName.forLevel((int) Math.ceil((double) boxsum / (double) activeDeck.deck.size()));
		/*
		 * Set "fullScore" to sum of all box values found in active deck
		 */
		boxsum = 0;
		for (final ActiveCard card : activeDeck.deck) {
			boxsum += card.box;
		}
		info.fullScore = boxsum;

		/*
		 * Set last score based on timings of most recent session. Cards with errors
		 * count as "-1" each. Apply "boxlevel" values as bonus points.
		 */
		final float maxCardScore = SlotInfo.TimeLimit.Novice.getSeconds();
		float score = 0f;
		boolean perfect = true;
		for (final ActiveCard card : activeDeck.deck) {
			if (card.showCount == 0) {
				continue;
			}
			if (!card.noErrors) {
				score -= maxCardScore;
				perfect = false;
			}
			final double avgShowTime = card.showTime / card.showCount;
			double cardScore = maxCardScore - avgShowTime;
			if (cardScore < 1) {
				cardScore = 1;
			}
			score += cardScore + card.box;
		}
		if (perfect) {
			score *= 1.1f;
		}
		info.lastScore = (int) Math.ceil(score);
		info.perfect = perfect;
		/*
		 * Calculate total proficiency with active cards (based on most recent noErrors
		 * flag)
		 */
		final int totalCards = activeDeck.deck.size();
		int correctCount = 0;
		for (final ActiveCard card : activeDeck.deck) {
			if (card.noErrors) {
				correctCount++;
			}
			info.proficiency = 100 * correctCount / totalCards;
		}
		/*
		 * What is the total percentange of cards learned out of the master deck?
		 */

//		int maxBox = 1;
//		for (ActiveCard card : activeDeck.deck) {
//			maxBox=Math.max(maxBox, card.box);
//		}
//		if (activeDeck.deck.size()>0) {
//			int boxSum = 0;
//			for (ActiveCard card : activeDeck.deck) {
//				boxSum+=Math.min(card.box>0?card.box:0, maxBox);
//			}
//			info.proficiency=(100*boxSum/(maxBox*activeDeck.deck.size()));
//		} else {
//			info.proficiency=0;
//		}

		/*
		 * How many are "fully learned" out of the active deck?
		 */
		info.longTerm = 0;
		for (final ActiveCard card : activeDeck.deck) {
			if (card.box >= FULLY_LEARNED_BOX) {
				info.longTerm++;
			}
		}

		/*
		 * count all active cards that aren't "fully learned"
		 */
		info.activeCards = activeDeck.deck.size() - info.longTerm;

		/*
		 * How many are "well known" out of the active deck? (excluding full learned
		 * ones)
		 */
		info.mediumTerm = 0;
		for (final ActiveCard card : activeDeck.deck) {
			if (card.box >= PROFICIENT_BOX && card.box < FULLY_LEARNED_BOX) {
				info.mediumTerm++;
			}
		}

		/*
		 * How many are "short term known" out of the active deck? (excluding full
		 * learned ones)
		 */
		info.shortTerm = 0;
		for (final ActiveCard card : activeDeck.deck) {
			if (card.box >= JUST_LEARNED_BOX && card.box < PROFICIENT_BOX) {
				info.shortTerm++;
			}
		}
	}

	public static void calculateTotalCardCount(final SlotInfo info, final List<Card> cards) {
		final DeckMode deckMode = info.settings.deck;
		if (deckMode.equals(DeckMode.Both)) {
			info.setTotalCards(cards.size());
			return;
		}
		int count = 0;
		for (final Card card : cards) {
			switch (deckMode) {
			case Conjugations:
				if (!StringUtils.isBlank(card.vgroup)) {
					count++;
					continue;
				}
				break;
			case Pronouns:
				if (StringUtils.isBlank(card.vgroup)) {
					count++;
					continue;
				}
				break;
			case Both:
			default:
				break;
			}
		}
		info.setTotalCards(count);
	}

	private int totalCards;

	private String signature = "";

	/**
	 * The summed "box" values for all active cards
	 */
	public int fullScore = 0;
	/**
	 * The summed "box" values for the most recent learning session
	 */
	public int sessionScore = 0;

	public int activeCards = 0;
	public int shortTerm = 0;
	public int mediumTerm = 0;
	public int longTerm = 0;
	public int proficiency = 0;
	public Settings settings = new Settings();
	private int version;
	public LevelName level;
	public int lastScore;
	public boolean perfect;
	public long lastrun;

	public SlotInfo() {
	}

	public SlotInfo(final SlotInfo info) {
		this.activeCards = info.activeCards;
		this.level = info.level;
		this.longTerm = info.longTerm;
		this.mediumTerm = info.mediumTerm;
		this.settings = new Settings(info.settings);

	}

	public String getSignature() {
		return signature;
	}

	public int getTotalCards() {
		return totalCards;
	}

	public int getVersion() {
		return StatsVersion;
	}

	public void setSignature(final String signature) {
		this.signature = signature;
	}

	public void setTotalCards(final int totalCards) {
		this.totalCards = totalCards;
	}

	public void setVersion(final int version) {
		this.version = version;
	}

	public boolean updatedVersion() {
		return version == StatsVersion;
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
}
