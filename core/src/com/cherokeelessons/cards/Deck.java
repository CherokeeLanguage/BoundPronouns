package com.cherokeelessons.cards;

import java.util.ArrayList;
import java.util.List;

public class Deck {
	public int version=-1;
	public int size=0;
	public List<Card> cards=new ArrayList<>();
	
	private static final List<Long> pimsleur_intervals=new ArrayList<>();
	private static final List<Long> sm2_intervals=new ArrayList<>();
	
	static {
		/*
		 * for Pimsleur
		 */
		long ms=1000l;
		for (int i=0; i<15; i++) {
			ms*=5l;
			pimsleur_intervals.add(ms);
		}
		/*
		 * for SM2 gaps
		 */
		long ms_day=1000l*60l*60l*24;
		float days=4f;
		sm2_intervals.add(ms_day);
		for (int i=0; i<15; i++) {
			sm2_intervals.add((long) (ms_day*days));
			days*=1.7f;
		}		
	}
	/**
	 * Pimsleur staggered intervals (powers of 5) seconds as ms
	 * @param correct_in_a_row
	 * @return
	 */
	public static long getNextInterval(int correct_in_a_row) {
		if (correct_in_a_row<0) {
			correct_in_a_row=0;
		}
		if (correct_in_a_row>pimsleur_intervals.size()-1){
			correct_in_a_row=pimsleur_intervals.size()-1;
		}
		return pimsleur_intervals.get(correct_in_a_row);
	}
	/**
	 * SM2 staggered intervals (powers of 1.7) days as ms
	 * @param box
	 * @return
	 */
	public static long getNextSessionInterval(int box) {
		if (box>=sm2_intervals.size()) {
			box=sm2_intervals.size()-1;
		}
		return sm2_intervals.get(box);
	}
}
