package com.cherokeelessons.bp.audiogen;

import com.cherokeelessons.deck.ICardData;

public class AudioData implements ICardData {

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public long getAnswerDuration() {
		return answerDuration;
	}

	public void setAnswerDuration(long answerDuration) {
		this.answerDuration = answerDuration;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public long getChallengeDuration() {
		return challengeDuration;
	}

	public void setChallengeDuration(long challengeDuration) {
		this.challengeDuration = challengeDuration;
	}

	private String id;
	
	/**
	 * The answer to be rendered by espeak-ng in Cherokee.
	 */
	private String answer;
	/**
	 * Duration of the answer audio in ms.
	 */
	private long answerDuration;
	/**
	 * The challenge phrase needing translating into Cherokee.
	 */
	private String challenge;
	/**
	 * Duration of the challenge in ms.
	 */
	private long challengeDuration;

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ICardData> T copy() {
		AudioData copy = new AudioData();
		copy.setAnswer(this.getAnswer());
		copy.setAnswerDuration(this.getAnswerDuration());
		copy.setChallenge(this.getChallenge());
		copy.setChallengeDuration(this.getChallengeDuration());
		return (T) copy;
	}

	@Override
	public String id() {
		return id;
	}
	
	public void setId(int id) {
		String tmp = String.valueOf(id);
		while (tmp.length()<4) {
			tmp = "0" + tmp;
		}
		this.id=tmp;
	}

	@Override
	public String sortKey() {
		return null;
	}

}
