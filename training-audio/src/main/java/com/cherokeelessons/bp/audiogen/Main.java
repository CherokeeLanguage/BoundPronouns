package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.SystemUtils;

public class Main {

	private static final NumberFormat NF = NumberFormat.getInstance();
	private static final File WAVS_DIR = new File("tmp/wavs");
	private static final String DECK_TSV = "../android/assets/review-sheet.tsv";
	private static final int CHEROKEE_ANSWER = 5;
	private static final int CHALLENGES_START = 6;

	private final AudioDeck mainDeck;
	private final Set<String> voiceVariants;

	private List<String> voices = new ArrayList<>();
	private String previousVoice = "";

	public String nextVoice() {
		if (voices.isEmpty()) {
			voices.addAll(voiceVariants);
			do {
				Collections.shuffle(voices);
			} while (voices.get(0).equals(previousVoice));
		}
		return previousVoice = voices.remove(0);
	}

	public Main() {
		mainDeck = new AudioDeck();
		voiceVariants = new TreeSet<>();
		// default
		voiceVariants.addAll(Arrays.asList(""));
		// magali's choices
		voiceVariants.addAll(Arrays.asList("Diogo", "f5"));
		// craig's choices
		voiceVariants.addAll(Arrays.asList("antonio", "Mr", "robosoft5"));
		// tommylee's choices
		voiceVariants.addAll(Arrays.asList("Diogo"));
	}

	public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
		new Main().execute();
	}

	public void execute() throws IOException, UnsupportedAudioFileException {
		buildMainDeck();
		generateWavFiles();
		generateDurationsReport();
	}

	private void generateDurationsReport() throws IOException {
		File reportFile = new File("tmp/durations.tsv");
		StringBuilder sb = new StringBuilder();
		sb.append("Challenge Wav");
		sb.append("\t");
		sb.append("Duration");
		sb.append("\t");
		sb.append("Answer Wav");
		sb.append("\t");
		sb.append("Duration");
		sb.append("\n");
		for (AudioCard card: mainDeck.getCards()) {
			AudioData data = card.getData();
			sb.append(data.getChallengeFile().getName());
			sb.append("\t");
			sb.append(NF.format(data.getChallengeDuration()));
			sb.append("\t");
			sb.append(data.getAnswerFile().getName());
			sb.append("\t");
			sb.append(NF.format(data.getAnswerDuration()));
			sb.append("\n");
		}
		FileUtils.writeStringToFile(reportFile, sb.toString(), StandardCharsets.UTF_8);
	}

	private void generateWavFiles() throws UnsupportedAudioFileException, IOException {
		FileUtils.deleteQuietly(WAVS_DIR);
		File espeakNgBin = new File(SystemUtils.getUserHome(), "espeak-ng/bin/espeak-ng");
		ESpeakNg espeak = new ESpeakNg(espeakNgBin);
		Set<String> already = new HashSet<>();
		for (AudioCard card : mainDeck.getCards()) {
			AudioData data = card.getData();
			String answer = data.getAnswer();
			String challenge = data.getChallenge();
			challenge = AudioGenUtil.removeEnglishFixedGenderMarks(AudioGenUtil.randomizeEnglishSexes(challenge));
			String challengeFilename = AudioGenUtil.asEnglishFilename(challenge);
			File challengeWavFile = new File(WAVS_DIR, "challenge-" + challengeFilename);
			String answerFilename = AudioGenUtil.asPhoneticFilename(answer);
			File answerWavFile = new File(WAVS_DIR, "answer-" + answerFilename);
			data.setAnswerFile(answerWavFile);
			data.setChallengeFile(challengeWavFile);
			if (!already.contains(challenge)) {
				String voice = nextVoice();
				if (!voice.trim().isEmpty()) {
					voice = "en-us+" + voice;
				} else {
					voice = "en-us";
				}
				System.out.println(" - "+challengeWavFile.getName()+" ["+voice+"]");
				espeak.generateWav(voice, challengeWavFile, challenge);
				float durationInSeconds = getDuration(challengeWavFile);
				data.setChallengeDuration(durationInSeconds);
			}
			if (!already.contains(answer)) {
				String voice = nextVoice();
				if (!voice.trim().isEmpty()) {
					voice = "chr+" + voice;
				} else {
					voice = "chr";
				}
				System.out.println(" - "+answerWavFile.getName()+" ["+voice+"]");
				espeak.generateWav(voice, answerWavFile, answer);
				already.add(answer);
				float durationInSeconds = getDuration(answerWavFile);
				data.setAnswerDuration(durationInSeconds);
			}
		}
	}

	private float getDuration(File answerWavFile) throws UnsupportedAudioFileException, IOException {
		AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(answerWavFile);
		AudioFormat format = audioFileFormat.getFormat();
		long audioFileLength = audioFileFormat.getFrameLength();
		//int frameSize = format.getFrameSize();
		float frameRate = format.getFrameRate();
		float durationInSeconds = (audioFileLength / frameRate);
		return durationInSeconds;
	}

	private void buildMainDeck() throws IOException {
		StringBuilder debug = new StringBuilder();
		File jsonFile = new File(DECK_TSV);
		System.out.println(jsonFile.getAbsolutePath());
		try (LineIterator li = FileUtils.lineIterator(jsonFile, StandardCharsets.UTF_8.name())) {
			li.next();
			int id = 0;
			while (li.hasNext()) {
				String line = li.next();
				String[] fields = line.split("\t");
				if (fields.length <= CHALLENGES_START) {
					System.out.println("; " + line);
					continue;
				}
				String answer = fields[CHEROKEE_ANSWER];
				for (int ix = CHALLENGES_START; ix < fields.length; ix++) {
					String challenge = fields[ix];
					if (challenge.trim().isEmpty()) {
						continue;
					}
					AudioData data = new AudioData();
					data.setAnswer(answer);
					data.setAnswerDuration(0);
					data.setChallenge(challenge);
					data.setChallengeDuration(0);
					data.setId(++id);
					AudioCard card = new AudioCard();
					card.setData(data);
					mainDeck.add(card);
					debug.append(data.id() + "\t" + challenge + "\t" + answer + "\n");
				}
			}
		}
		FileUtils.writeStringToFile(new File("exercise-set.tsv"), debug.toString(), StandardCharsets.UTF_8);
	}

}
