package com.cherokeelessons.cards;

import java.util.ArrayList;
import java.util.List;

public class Deck {
	public List<Card> cards=new ArrayList<>();
	
	public static List<Integer> intervals=new ArrayList<>();
	static {
		int secs=1;
		for (int i=0; i<15; i++) {
			secs*=5;
			intervals.add(secs);
		}
	}
}
