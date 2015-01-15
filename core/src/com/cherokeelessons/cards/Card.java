package com.cherokeelessons.cards;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Card implements Serializable {
	public int id;
	public int box;
	public int nextSession;
	public String challenge="";
	public String answer="";
}
