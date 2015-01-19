package com.cherokeelessons.cards;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Array;

public class Deck {
	public List<Card> cards=new ArrayList<>();
	
	public static Array<Integer> intervals=new Array<>();
	static {
		int secs=1;
		for (int i=0; i<15; i++) {
			secs*=5;
			intervals.add(secs);
		}
	}
}
