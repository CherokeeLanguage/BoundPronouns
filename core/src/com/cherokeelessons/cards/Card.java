package com.cherokeelessons.cards;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.cherokeelessons.bp.BoundPronouns;

@SuppressWarnings("serial")
public class Card implements Serializable, Comparable<Card> {
	public int id;
	public int group;
	public int proficiency_level;
	public int correct_in_a_row;
	public int nextSession;
	public List<String> challenge=new ArrayList<>();
	public List<String> answer=new ArrayList<>();
	
	
	@Override
	public int compareTo(Card o) {
		return getKey().compareTo(o.getKey());
	}

	private String getKey() {
		StringBuilder key=new StringBuilder();
		if (challenge.size()!=0) {
			String tmp = challenge.get(0);
			tmp=tmp.replaceAll("[¹²³⁴ɂ"+BoundPronouns.SPECIALS+"]", "");			
			tmp=StringUtils.substringBefore(tmp, ",");
			tmp=StringUtils.strip(tmp);			
			String length = tmp.length()+"";
			while (length.length()<4) {
				length="0"+length;
			}
			key.append(length);
			key.append("+");			
		}
		for (String s: challenge) {
			key.append(s);
			key.append("+");
		}
		for (String s: answer) {
			key.append(s);
			key.append("+");
		}
		return key.toString();
	}
	
	
}
