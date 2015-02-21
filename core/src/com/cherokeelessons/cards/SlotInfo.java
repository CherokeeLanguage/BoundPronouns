package com.cherokeelessons.cards;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SlotInfo implements Serializable {
	public static final int StatsVersion = 1;

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

	public static enum TimeLimit {
		Expert("Expert: Max 10 seconds", 10f), Standard(
				"Standard: Max 15 seconds", 15f), Novice(
				"Novice: Max 30 seconds", 30f), Newbie("Newbie: Max 1 hour", 60f*60f);

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

	public int activeCards = 0;
	public float shortTerm = 0f;
	public float mediumTerm = 0f;
	public float longTerm = 0f;
	public Settings settings = new Settings();
	public int version;

	public void validate() {
		if (settings == null) {
			settings = new Settings();
		}
		settings.validate();
	}
}
