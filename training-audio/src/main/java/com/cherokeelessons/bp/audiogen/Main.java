package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class Main {

	private static final String DECK_TSV = "../android/assets/review-sheet.tsv";
	private static final int ESPEAK_CHALLENGE=4;
	private static final int ANSWERS_START=5;
	
	public static void main(String[] args) throws IOException {
		File jsonFile = new File(DECK_TSV);
		System.out.println(jsonFile.getAbsolutePath());
		try (LineIterator li = FileUtils.lineIterator(jsonFile, StandardCharsets.UTF_8.name())) {
			li.next();
			while(li.hasNext()) {
				//
			}
		}
	}

}
