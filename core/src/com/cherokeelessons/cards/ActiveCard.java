package com.cherokeelessons.cards;

import java.util.HashMap;
import java.util.Map;

public class ActiveCard {
	/**
	 * id of card in main deck
	 */
	public int id;
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
	 * How tries left for this session?
	 */
	public int tries_remaining;
	/**
	 * How many times has it been correct in a row?
	 * RECORDED PER ANSWER.
	 */
	public Map<String, Integer> correct_in_a_row=new HashMap<>();
	
	/**
	 * What Leitner proficiency box is this assigned to? This selects which
	 * "interval" is used to add for the "show again secs" value
	 */
	public int box;
	/**
	 * How long before this card should be tried again?
	 */
	public long show_again_ms;
}
