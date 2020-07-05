package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class Main {
	
	private final AudioDeck mainDeck;

	private static final String DECK_TSV = "../android/assets/review-sheet.tsv";
	private static final int CHEROKEE_ANSWER=5;
	private static final int CHALLENGES_START=6;
	private static final String[] VOICES = {"default", "Diogo", "f5"};
	
	public Main() {
		mainDeck = new AudioDeck();
	}
	
	public static void main(String[] args) throws IOException {
		new Main().execute();
	}

	public void execute() throws IOException {
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
