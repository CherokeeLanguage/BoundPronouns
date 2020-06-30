package com.cherokeelessons.cards;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.cherokeelessons.bp.BoundPronouns;

@SuppressWarnings("serial")
public class Card implements Serializable, Comparable<Card> {
	private static final boolean DEBUG = true;
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

	public String sortKey() {
		final StringBuilder sortKey = new StringBuilder();

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
		if (DEBUG) {
			this.key = sortKey.toString();
		}
		return sortKey.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((answer == null) ? 0 : answer.hashCode());
		result = prime * result + ((challenge == null) ? 0 : challenge.hashCode());
		result = prime * result + id;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((pgroup == null) ? 0 : pgroup.hashCode());
		result = prime * result + pset;
		result = prime * result + (reversed ? 1231 : 1237);
		result = prime * result + ((vgroup == null) ? 0 : vgroup.hashCode());
		result = prime * result + vset;
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
		Card other = (Card) obj;
		if (answer == null) {
			if (other.answer != null)
				return false;
		} else if (!answer.equals(other.answer))
			return false;
		if (challenge == null) {
			if (other.challenge != null)
				return false;
		} else if (!challenge.equals(other.challenge))
			return false;
		if (id != other.id)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (pgroup == null) {
			if (other.pgroup != null)
				return false;
		} else if (!pgroup.equals(other.pgroup))
			return false;
		if (pset != other.pset)
			return false;
		if (reversed != other.reversed)
			return false;
		if (vgroup == null) {
			if (other.vgroup != null)
				return false;
		} else if (!vgroup.equals(other.vgroup))
			return false;
		if (vset != other.vset)
			return false;
		return true;
	}

	
	/**
	 * id of card in main deck based on pgroup/vgroup combinations
	 */
//	public String getId() {
//		return (pgroup + "+" + vgroup).intern();
//	};

}
