package com.cherokeelessons.cards;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SlotInfo implements Serializable {
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

	public static class Settings {
		public String name = "";
		public DisplayMode display = DisplayMode.Both;
		public DeckMode deck = DeckMode.Both;
		public boolean muted = false;
	}

	public int activeCards = 0;
	public float shortTerm = 0f;
	public float mediumTerm = 0f;
	public float longTerm = 0f;
	public Settings settings = new Settings();
}
