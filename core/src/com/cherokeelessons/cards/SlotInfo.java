package com.cherokeelessons.cards;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SlotInfo implements Serializable {
	public String name="";
	public int activeCards=0;
	public float shortTerm=0f;
	public float mediumTerm=0f;
	public float longTerm=0f;
}
