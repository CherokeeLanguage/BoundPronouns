package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.cherokeelessons.bp.audiogen.AudioData.AudioDataFile;
import com.cherokeelessons.deck.ICardData;

public class AudioData implements ICardData {
	
	public static class AudioDataFile {
		public File file;
		public float duration;
		public AudioDataFile() {
			this(null);
		}
		public AudioDataFile(final File file) {
			this(file, 0);
		}
		public AudioDataFile(final File file, final float duration) {
			this.file = file;
			this.duration = duration;
		}
		@Override
		public String toString() {
			return "AudioDataFile [file=" + file.getName() + ", duration=" + duration + "]";
		}
	}
	
	private final Random rand = new Random(1234);

	private String boundPronoun;
	private String verbStem;

	private final List<AudioDataFile> answerFile = new ArrayList<>();

	private final List<AudioDataFile> challengeFile = new ArrayList<>();

	private String id;
	
	private String sex;

	/**
	 * The answer to be rendered by TTS in Cherokee.
	 */
	private String answer;

	/**
	 * Duration of the answer audio in ms.
	 */
//	private float answerDuration;
	/**
	 * The challenge phrase to be rendered by TTS in English.
	 */
	private String challenge;

	/**
	 * Duration of the challenge in seconds.
	 */
//	private float challengeDuration;

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ICardData> T copy() {
		final AudioData copy = new AudioData();
		copy.setAnswer(this.getAnswer());
//		copy.setAnswerDuration(this.getAnswerDuration());
		copy.setChallenge(this.getChallenge());
//		copy.setChallengeDuration(this.getChallengeDuration());
		return (T) copy;
	}

	public String getAnswer() {
		return answer;
	}

//	public float getAnswerDuration() {
//		return answerDuration;
//	}

	public AudioDataFile getAnswerFile() {
		Collections.shuffle(answerFile, rand);
		return answerFile.get(0);
	}
	
	public List<AudioDataFile> getAnswerFiles() {
		return answerFile;
	}

	public String getBoundPronoun() {
		return boundPronoun;
	}

	public String getChallenge() {
		return challenge;
	}

//	public float getChallengeDuration() {
//		return challengeDuration;
//	}

	public AudioDataFile getChallengeFile() {
		Collections.shuffle(challengeFile, rand);
		return challengeFile.get(0);
	}

	public String getVerbStem() {
		return verbStem;
	}

	@Override
	public String id() {
		return id;
	}

	public void setAnswer(final String answer) {
		this.answer = answer;
	}

//	public void setAnswerDuration(final float answerDuration) {
//		this.answerDuration = answerDuration;
//	}

	public void addAnswerFile(AudioDataFile answerFile) {
		this.answerFile.add(answerFile);
	}
	
	public void setAnswerFile(final List<AudioDataFile> answerFile) {
		this.answerFile.clear();
		this.answerFile.addAll(answerFile);
	}

	public void setBoundPronoun(final String boundPronoun) {
		this.boundPronoun = boundPronoun;
	}

	public void setChallenge(final String challenge) {
		this.challenge = challenge;
	}

//	public void setChallengeDuration(final float challengeDuration) {
//		this.challengeDuration = challengeDuration;
//	}

	public void addChallengeFile(final AudioDataFile challengeFile) {
		this.challengeFile.add(challengeFile);
	}
	
	public void setChallengeFile(final List<AudioDataFile> challengeFile) {
		this.challengeFile.clear();
		this.challengeFile.addAll(challengeFile);
	}

	public void setId(final int id) {
		String tmp = String.valueOf(id);
		while (tmp.length() < 4) {
			tmp = "0" + tmp;
		}
		this.id = tmp;
	}

	public void setVerbStem(final String verbStem) {
		this.verbStem = verbStem;
	}

	@Override
	public String sortKey() {
		return null;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

}
