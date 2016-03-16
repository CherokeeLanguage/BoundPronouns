package com.cherokeelessons.cards;

import java.util.Collection;
import java.util.HashMap;

public class ActiveCard {

	public boolean noErrors = false;

	public int getMinCorrectInARow() {
		int min = 3;
		for (String key : correct_in_a_row.keySet()) {
			if (correct_in_a_row.get(key) == null) {
				continue;
			}
			min=Math.min(min, correct_in_a_row.get(key));
		}
		return min;
	}

	public int getCorrectInARowFor(String answer) {
		Integer c = correct_in_a_row.get(answer);
		return c != null ? c : 0;
	}

	public void markCorrect(String answer) {
		Integer c = correct_in_a_row.get(answer);
		correct_in_a_row.put(answer, c != null ? c + 1 : 1);
	}

	public void resetCorrectInARow(Collection<String> answers) {
		if (answers.size() == 0) {
			throw new RuntimeException("EMPTY ANSWER LIST ?!?! [" + pgroup
					+ "|" + vgroup + "]");
		}
		correct_in_a_row.clear();
		for (String a : answers) {
			correct_in_a_row.put(a, 0);
		}
	}

	/**
	 * verb group for this card
	 */
	public String vgroup;
	/**
	 * pronoun group for this card
	 */
	public String pgroup;

	/**
	 * Is this a brand new card never seen before?
	 */
	public boolean newCard;
	/**
	 * How many tries left for this session?
	 */
	public int tries_remaining;
	/**
	 * How many times has it been correct in a row? RECORDED PER ANSWER.
	 */
	private HashMap<String, Integer> correct_in_a_row = new HashMap<String, Integer>();

	/**
	 * What proficiency box is this assigned to? This is used to select which
	 * SM2 "interval" is used to add for the "show again secs" value after a
	 * card is "retired" or "bumped"
	 */
	public int box;
	/**
	 * How long before this card should be tried again?
	 */
	public long show_again_ms;

	/*
	 * For "scoring" for leaderboards
	 */
	/**
	 * total elapsed time card has been displayed
	 */
	public float showTime = 0f;
	/**
	 * total times user was challenged with card
	 */
	public int showCount = 0;

	public int getAnswerCount() {
		int i = correct_in_a_row.size();
		return i > 0 ? i : 1;
	}

	/**
	 * Two active cards are equal if both the pgroup and vgroup and the same.
	 * All other attributes are ignored.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ActiveCard)) {
			return false;
		}
		ActiveCard other = (ActiveCard) obj;
		return vgroup.equals(other.vgroup) && pgroup.equals(other.pgroup);
	}

	/**
	 * How many times a card must be shown in the session. <br/>
	 * The value's pimsleur value must not exceed the sesson length or else the
	 * card will never be successfully marked as known! <br/>
	 * 5 minutes sessions can not have a value > 3!
	 * 
	 * @return
	 */
	private int getMyNextSessionThreshold() {
		if (box == 0) {
			return 3;
		}
		if (box == 1) {
			return 2;
		}
		return 1;
	};

	public void resetTriesRemaining() {
		tries_remaining = getMyNextSessionThreshold();
	}
}
