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

public class EnglishAudio {
	
	public static final String LANGUAGE_CULTURE_1 = "Language and culture which are not shared and taught openly and freely will die. If our language and culture die, then, as a people, so do we.";
	public static final String COPY_1 = "Copyright 2020 by Michael Conrad.";
	public static final String COPY_2 = "This work is licensed to you under the Creative Commons Attribution Share-Alike license. To obtain a copy of this license please look up Creative Commons online. You are free to share, copy, and distribute this work. If you alter, build upon, or transform this work, you must use the same license on the resulting work.";
	public static final String INTRO_1 = "In this exercise you will learn Cherokee phrases by responding with each phrase's English translation. Each new phrase will be introduced with it's English translation. You will then be prompted to translate different phrases into English. You should respond aloud.";
	public static final String KEEP_GOING = "When doing these exercises do not become discouraged. It is normal to have to repeat these exercises several times. As you progress beyond the first few of the exercises you will find the later exercises easier.";
	public static final String BEGIN = "Let us begin.";
	
	public static AudioData createEnglishAudioFor(String text, File wavFile) throws IOException {
		FileUtils.deleteQuietly(wavFile);
		final File srcFile = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, text);
		asWavFile(srcFile, wavFile);
		normalizeWav(wavFile);
		final AudioData data = new AudioData();
		data.setAnswerFile(wavFile);
		data.setAnswerDuration(getDuration(wavFile));
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
