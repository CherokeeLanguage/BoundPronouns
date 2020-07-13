package com.cherokeelessons.bp.audiogen;

import java.io.File;

import com.cherokeelessons.deck.ICardData;

public class AudioData implements ICardData {

	private File answerFile;
	private File challengeFile;

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
		final AudioData copy = new AudioData();
		copy.setAnswer(this.getAnswer());
		copy.setAnswerDuration(this.getAnswerDuration());
		copy.setChallenge(this.getChallenge());
		copy.setChallengeDuration(this.getChallengeDuration());
		return (T) copy;
	}

	public String getAnswer() {
		return answer;
	}

	public float getAnswerDuration() {
		return answerDuration;
	}

	public File getAnswerFile() {
		return answerFile;
	}

	public String getChallenge() {
		return challenge;
	}

	public float getChallengeDuration() {
		return challengeDuration;
	}

	public File getChallengeFile() {
		return challengeFile;
	}

	@Override
	public String id() {
		return id;
	}

	public void setAnswer(final String answer) {
		this.answer = answer;
	}

	public void setAnswerDuration(final float answerDuration) {
		this.answerDuration = answerDuration;
	}

	public void setAnswerFile(final File answerFile) {
		this.answerFile = answerFile;
	}

	public void setChallenge(final String challenge) {
		this.challenge = challenge;
	}

	public void setChallengeDuration(final float challengeDuration) {
		this.challengeDuration = challengeDuration;
	}

	public void setChallengeFile(final File challengeFile) {
		this.challengeFile = challengeFile;
	}

	public void setId(final int id) {
		String tmp = String.valueOf(id);
		while (tmp.length() < 4) {
			tmp = "0" + tmp;
		}
		this.id = tmp;
	}

	@Override
	public String sortKey() {
		return null;
	}

}
