package com.cherokeelessons.cards;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Array;

public class Deck {
	public List<Card> cards=new ArrayList<>();
	
	public static Array<Integer> intervals=new Array<>();
	static {
		intervals.add(5);
		intervals.add(25);
		intervals.add(2*60);
		intervals.add(10*60);
	}
}
