package com.cherokeelessons.cards;

import java.util.ArrayList;
import java.util.List;

public class ActiveDeck {
	private String signature = "";
	public long lastrun = 0;
	public List<ActiveCard> deck = new ArrayList<>();

	public String getSignature() {
		return signature;
	}

	public void setSignature(final String signature) {
		this.signature = signature;
	}
}
