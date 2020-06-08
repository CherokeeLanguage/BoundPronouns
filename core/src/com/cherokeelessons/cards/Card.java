package com.cherokeelessons.cards;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
	public boolean reversed;

	private int vset;
	private int pset;

	public Card() {
	}

	public Card(final Card card) {
		this.answer.addAll(card.answer);
		this.challenge.addAll(card.challenge);
		this.id = card.id;
		this.key = card.key;
		this.pgroup = card.pgroup;
		this.vgroup = card.vgroup;
		this.pset = card.pset;
		this.vset = card.vset;
	}

	@Override
	public int compareTo(final Card o) {
		return sortKey().compareTo(o.sortKey());
	}

	public int getPset() {
		return pset;
	}

	public int getVset() {
		return vset;
	}

	public void setPset(final int pset) {
		this.pset = pset;
	}

	public void setVset(final int vset) {
		this.vset = vset;
	}

	private String sortKey() {
		final StringBuilder sortKey = new StringBuilder();

		if (vgroup == null || vgroup.length() == 0) {
			sortKey.append("0-");
		} else {
			sortKey.append("1-");
		}

		sortKey.append(StringUtils.leftPad(pset + "", 4, "0"));
		sortKey.append("-");
		sortKey.append(StringUtils.leftPad(vset + "", 4, "0"));
		sortKey.append("-");

		if (challenge.size() != 0) {
			String tmp = challenge.get(0);
			tmp = tmp.replaceAll("[¹²³⁴ɂ" + BoundPronouns.SPECIALS + "]", "");
			tmp = StringUtils.substringBefore(tmp, ",");
			if (tmp.matches(".*[Ꭰ-Ᏼ].*")) {
				tmp = tmp.replaceAll("[^Ꭰ-Ᏼ]", "");
			}
			final String length = StringUtils.leftPad(tmp.length() + "", 4, "0");
			sortKey.append(length);
			sortKey.append("+");
			sortKey.append(tmp);
			sortKey.append("+");
		}
		for (final String s : challenge) {
			sortKey.append(s);
			sortKey.append("+");
		}
		for (final String s : answer) {
			sortKey.append(s);
			sortKey.append("+");
		}
		if (debug) {
			this.key = sortKey.toString();
		}
		return sortKey.toString();
	}

	/**
	 * id of card in main deck based on pgroup/vgroup combinations
	 */
//	public String getId() {
//		return (pgroup + "+" + vgroup).intern();
//	};

}
