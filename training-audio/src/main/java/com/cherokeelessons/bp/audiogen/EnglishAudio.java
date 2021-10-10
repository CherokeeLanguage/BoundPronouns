package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;

import com.cherokeelessons.bp.audiogen.AudioData.AudioDataFile;

public class EnglishAudio {
	
	public static final String LANGUAGE_CULTURE_1 = "Language and culture which are not shared and taught openly and freely will die. If our language and culture die, then, as a people, so do we.";
	public static final String COPY_1 = "Production copyright "+LocalDate.now().getYear()+" by Michael Conrad.";
	public static final String COPY_BY_SA = "This work is licensed to you under the Creative Commons Attribution Share-Alike license. To obtain a copy of this license please look up Creative Commons online. You are free to share, copy, and distribute this work. If you alter, build upon, or transform this work, you must use the same license on the resulting work.";
	public static final String COPY_BY_NC = "This work is licensed to you under the Creative Commons Attribution Non-Commercial license. To obtain a copy of this license please look up Creative Commons online. You are free to share and adapt this work. If you alter, build upon, or transform this work, you must use the same license on the resulting work.";
	public static final String IS_DERIVED = "This is a derived work.";
	public static final String IS_DERIVED_CHEROKEE_NATION = "This is a derived work. Permission to use the original Cherokee audio and print materials for non-commercial use granted by \"The Cherokee Nation of Oklahoma\".";
	public static final String WALC1_ATTRIBUTION = "Original audio copyright 2018 by the Cherokee Nation of Oklahoma." //
			+ " The contents of the \"We are Learning Cherokee Level 1\" textbook were developed by Durbin Feeling," //
			+ " Patrick Rochford, Anna Sixkiller, David Crawler, John Ross, Dennis Sixkiller, Ed Fields, Edna Jones, Lula Elk," //
			+ " Lawrence Panther, Jeff Edwards, Zachary Barnes, Wade Blevins, and Roy Boney, Jr.";
	public static final String INTRO_1 = "In these sessions, you will learn Cherokee phrases by responding with each phrase's English translation. Each new phrase will be introduced with it's English translation. You will then be prompted to translate different phrases into English. It is important to respond aloud.";
	public static final String INTRO_2 = "In these sessions, you will learn by responding aloud in English. Each phrase will be introduced with an English translation. As the sessions progress you will be prompted to translate different phrases into English. It is important to respond aloud.";
	public static final String KEEP_GOING = "Do not become discouraged while doing these sessions. It is normal to have to repeat them several times. As you progress you will find the later sessions much easier.";
	public static final String BEGIN = "Let us begin.";
	public static final String LEARN_SOUNDS_FIRST = "Only after you have learned how the words in Cherokee sound and how they are used together will you be able to speak Cherokee.\n"
			+ "This material is designed to assist with learning these sounds and word combinations.\n"
			+ "This is the way young children learn their first language or languages.\n";
	
	public static AudioData createEnglishAudioFor(String text, File wavFile) throws IOException {
		if (wavFile.isDirectory()||wavFile.getName().endsWith("/")||wavFile.getName().endsWith("\\")) {
			wavFile = new File(wavFile.getPath(), "en-us-instructor-" + AudioGenUtil.asEnglishFilename(text) + ".wav");
		}
		if (!wavFile.getName().endsWith(".wav")) {
			wavFile = new File(wavFile.getPath()+".wav");
		}
		FileUtils.deleteQuietly(wavFile);
		final File srcFile = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, text);
		asWavFile(srcFile, wavFile);
		normalizeWav(wavFile);
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(wavFile);
		data.addAnswerFile(answerData);
		answerData.duration=getDuration(wavFile);
		return data;
	}

	public static void asWavFile(final File mp3File, final File wavFile) {
		final List<String> cmd = new ArrayList<>();
		cmd.add("ffmpeg");
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(mp3File.getAbsolutePath());
		cmd.add(wavFile.getAbsolutePath());
		executeCmd(cmd);
	}
	
	public static void normalizeWav(final File wavFile) {
		final List<String> cmd = new ArrayList<>();
		cmd.add("normalize-audio");
		cmd.add(wavFile.getAbsolutePath());
		executeCmd(cmd);
	}
	
	public static float getDuration(final File answerWavFile) {
		AudioFileFormat audioFileFormat;
		try {
			audioFileFormat = AudioSystem.getAudioFileFormat(answerWavFile);
		} catch (UnsupportedAudioFileException | IOException e) {
			return 0f;
		}
		final AudioFormat format = audioFileFormat.getFormat();
		final long audioFileLength = audioFileFormat.getFrameLength();
		// int frameSize = format.getFrameSize();
		final float frameRate = format.getFrameRate();
		final float durationInSeconds = audioFileLength / frameRate;
		return durationInSeconds;
	}
	
	public static void executeCmd(final List<String> cmd) {
		final String strCmd = StringUtils.join(cmd, " ");
		final ProcessBuilder b = new ProcessBuilder(cmd);
		Process process;
		try {
			process = b.start();
			process.waitFor();
			if (process.exitValue() != 0) {
				System.err.println("FATAL: Bad exit value from:\n   " + strCmd);
				System.out.println();
				IOUtils.copy(process.getInputStream(), System.out);
				System.out.println();
				IOUtils.copy(process.getErrorStream(), System.err);
				System.out.println();
				throw new RuntimeException("FATAL: Bad exit value from " + strCmd);
			}
			process.destroy();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
