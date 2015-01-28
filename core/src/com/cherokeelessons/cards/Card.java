package com.cherokeelessons.cards;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.cherokeelessons.bp.BoundPronouns;

@SuppressWarnings("serial")
public class Card implements Serializable, Comparable<Card> {
	private static final boolean debug = false;
	public int id;

	public List<String> challenge = new ArrayList<>();
	public List<String> answer = new ArrayList<>();
	public String key;
	public String pgroup;
	public String vgroup;

	public Card() {
	}

	public Card(Card card) {
		this.answer.addAll(card.answer);
		this.challenge.addAll(card.challenge);
		this.id = card.id;
		this.key = card.key;
		this.pgroup = card.pgroup;
		this.vgroup = card.vgroup;
	}

	@Override
	public int compareTo(Card o) {
		return -getKey().compareTo(o.getKey());
	}

	private String getKey() {
		StringBuilder key = new StringBuilder();
		if (challenge.size() != 0) {
			String tmp = challenge.get(0);
			tmp = tmp.replaceAll("[¹²³⁴ɂ" + BoundPronouns.SPECIALS + "]", "");
			tmp = StringUtils.substringBefore(tmp, ",");
			if (tmp.matches(".*[Ꭰ-Ᏼ].*")) {
				tmp = tmp.replaceAll("[^Ꭰ-Ᏼ]", "");
			}
			String length = tmp.length() + "";
			while (length.length() < 4) {
				length = "0" + length;
			}
			key.append(length);
			key.append("+");
			key.append(tmp);
			key.append("+");
		}
		for (String s : challenge) {
			key.append(s);
			key.append("+");
		}
		for (String s : answer) {
			key.append(s);
			key.append("+");
		}
		if (debug) {
			this.key = key.toString();
		}
		return key.toString();
	}

	/**
	 * id of card in main deck based on pgroup/vgroup combinations
	 */
	public String getId() {
		return pgroup + "+" + vgroup;
	};

}
