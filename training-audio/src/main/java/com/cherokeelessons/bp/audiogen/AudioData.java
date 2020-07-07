package com.cherokeelessons.bp.audiogen;

import java.io.File;

import com.cherokeelessons.deck.ICardData;

public class AudioData implements ICardData {
	
	private File answerFile;
	private File challengeFile;

	public File getAnswerFile() {
		return answerFile;
	}

	public void setAnswerFile(File answerFile) {
		this.answerFile = answerFile;
	}

	public File getChallengeFile() {
		return challengeFile;
	}

	public void setChallengeFile(File challengeFile) {
		this.challengeFile = challengeFile;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public float getAnswerDuration() {
		return answerDuration;
	}

	public void setAnswerDuration(float answerDuration) {
		this.answerDuration = answerDuration;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public float getChallengeDuration() {
		return challengeDuration;
	}

	public void setChallengeDuration(float challengeDuration) {
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
	private float answerDuration;
	/**
	 * The challenge phrase needing translating into Cherokee.
	 */
	private String challenge;
	/**
	 * Duration of the challenge in seconds.
	 */
	private float challengeDuration;

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
