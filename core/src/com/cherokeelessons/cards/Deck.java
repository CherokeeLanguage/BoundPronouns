package com.cherokeelessons.cards;

import java.util.ArrayList;
import java.util.List;

public class Deck {
	private static final List<Long> pimsleur_intervals = new ArrayList<>();
	private static final List<Long> sm2_intervals = new ArrayList<>();
	static {
		/*
		 * for Pimsleur
		 */
		long ms = 1000l;
		for (int i = 0; i < 15; i++) {
			ms *= 5l;
			pimsleur_intervals.add(ms);
		}
		/*
		 * for SM2 gaps
		 */
		final long ms_day = 1000l * 60l * 60l * 24;
		float days = 4f;
		sm2_intervals.add(ms_day);
		for (int i = 0; i < 15; i++) {
			sm2_intervals.add((long) (ms_day * days));
			days *= 1.7f;
		}
	}

	/**
	 * Pimsleur staggered intervals (powers of 5) seconds as ms
	 * 
	 * @param correct_in_a_row
	 * @return How long until next show.
	 */
	public static long getNextInterval(int correct_in_a_row) {
		if (correct_in_a_row < 0) {
			correct_in_a_row = 0;
		}
		return pimsleur_intervals.get(correct_in_a_row % pimsleur_intervals.size());
	}

	/**
	 * SM2 staggered intervals (powers of 1.7) days as ms
	 * 
	 * @param box
	 * @return How long before next show.
	 */
	public static long getNextSessionInterval(int box) {
		if (box >= sm2_intervals.size()) {
			box = sm2_intervals.size() - 1;
		}
		if (box < 0) {
			box = 0;
		}
		return sm2_intervals.get(box);
	}

	public int version = 390;
	public int size = 0;
	public List<Card> cards;

	public Deck() {
		cards = new ArrayList<>(2048);
	}

	public Deck(final Deck deck) {
		cards = new ArrayList<>(deck.cards);
		version = deck.version;
		size = cards.size();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cards == null) ? 0 : cards.hashCode());
		result = prime * result + size;
		result = prime * result + version;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Deck other = (Deck) obj;
		if (cards == null) {
			if (other.cards != null)
				return false;
		} else if (!cards.equals(other.cards))
			return false;
		if (size != other.size)
			return false;
		if (version != other.version)
			return false;
		return true;
	}
	
	
}
