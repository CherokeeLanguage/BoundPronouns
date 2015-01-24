package com.cherokeelessons.cards;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SlotInfo implements Serializable {
	public String name="";
	public float learned=0f;
	public float proficiency=0f;
	public int activeCards=0;
}
