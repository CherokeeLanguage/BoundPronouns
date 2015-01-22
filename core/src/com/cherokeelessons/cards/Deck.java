package com.cherokeelessons.cards;

import java.util.ArrayList;
import java.util.List;

public class Deck {
	public List<Card> cards=new ArrayList<>();
	
	public static List<Long> intervals=new ArrayList<>();
	static {
		long ms=1000l;
		for (int i=0; i<15; i++) {
			ms*=5l;
			intervals.add(ms);
		}
	}
	public static long getNextInterval(int box) {
		if (box<0) {
			box=0;
		}
		if (box>intervals.size()-1){
			box=intervals.size()-1;
		}
		return intervals.get(box);
	}
}
