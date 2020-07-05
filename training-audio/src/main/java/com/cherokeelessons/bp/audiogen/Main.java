package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.SystemUtils;

public class Main {
	
	private static final File WAVS_DIR = new File("tmp/wavs");
	private static final String DECK_TSV = "../android/assets/review-sheet.tsv";
	private static final int CHEROKEE_ANSWER=5;
	private static final int CHALLENGES_START=6;
	
	private final AudioDeck mainDeck;
	private final Set<String> voiceVariants;
	
	public Main() {
		mainDeck = new AudioDeck();
		voiceVariants = new TreeSet<>();
		//default
		voiceVariants.addAll(Arrays.asList(""));
		//magali's choices
		voiceVariants.addAll(Arrays.asList("Diogo", "f5"));
		//craig's choices
		voiceVariants.addAll(Arrays.asList("antonio", "Mr", "robosoft5"));
		//tommylee's choices
		voiceVariants.addAll(Arrays.asList("Diogo"));
	}
	
	public static void main(String[] args) throws IOException {
		new Main().execute();
	}

	public void execute() throws IOException {
		buildMainDeck();
		generateWavFiles();
	}

	private void generateWavFiles() {
		File espeakNgBin = new File(SystemUtils.getUserHome(), "espeak-ng/bin/espeak-ng");
		ESpeakNg espeak = new ESpeakNg(espeakNgBin);
	}

	private void buildMainDeck() throws IOException {
		StringBuilder debug = new StringBuilder();
		File jsonFile = new File(DECK_TSV);
		System.out.println(jsonFile.getAbsolutePath());
		try (LineIterator li = FileUtils.lineIterator(jsonFile, StandardCharsets.UTF_8.name())) {
			li.next();
			int id=0;
			while(li.hasNext()) {
				String line = li.next();
				String[] fields = line.split("\t");
				if (fields.length<=CHALLENGES_START) {
					System.out.println("; "+line);
					continue;
				}
				String answer = fields[CHEROKEE_ANSWER];
				for (int ix=CHALLENGES_START; ix<fields.length; ix++) {
					String challenge = fields[ix];
					if (challenge.trim().isEmpty()) {
						continue;
					}
					AudioData data=new AudioData();
					data.setAnswer(answer);
					data.setAnswerDuration(0);
					data.setChallenge(challenge);
					data.setChallengeDuration(0);
					data.setId(++id);
					AudioCard card = new AudioCard();
					card.setData(data);
					mainDeck.add(card);
					debug.append(data.id() + "\t"+ challenge + "\t" + answer+"\n");
				}
			}
		}
		FileUtils.writeStringToFile(new File("exercise-set.tsv"), debug.toString(), StandardCharsets.UTF_8);
	}

}
