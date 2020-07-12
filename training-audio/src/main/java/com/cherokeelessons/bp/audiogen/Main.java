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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.omg.CORBA.SystemException;

import com.cherokeelessons.deck.CardStats;
import com.cherokeelessons.deck.CardUtils;
import com.cherokeelessons.deck.DeckStats;
import com.cherokeelessons.deck.ICard;

public class Main {

	private static final NumberFormat NF = NumberFormat.getInstance();
	private static final File WAVS_DIR = new File("tmp/wavs");
	private static final File EXCERCISES_DIR = new File("tmp/excercises");
	private static final String DECK_TSV = "../android/assets/review-sheet.tsv";
	private static final int CHEROKEE_ANSWER = 6;
	private static final int CHALLENGES_START = 7;

	private final AudioDeck mainDeck;
	private final AudioDeck activeDeck;
	private final AudioDeck discardsDeck;
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
		activeDeck = new AudioDeck();
		discardsDeck = new AudioDeck();
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
		loadMainDeck();
		generateWavFiles();
		generateDurationsReport();
		buildExerciseMp3Files();
	}

	private void buildExerciseMp3Files() {
		FileUtils.deleteQuietly(EXCERCISES_DIR);
		File silenceWav = generateSilenceWav();
		List<File> audioEntries = new ArrayList<>();
		
		float tick = 0f;
		while (tick < 60f * 10f) {
			if (!activeDeck.hasCards() && !mainDeck.hasCards()) {
				break;
			}
			if (activeDeck.getNextShowTime() / 1000f > 4 || !activeDeck.hasCards()) {
				AudioCard topCard = (AudioCard) mainDeck.topCard();
				topCard.resetStats();
				topCard.resetTriesRemaining();
				activeDeck.add(topCard);
			}
			activeDeck.shuffleThenSortByNextSession();
			activeDeck.updateTimeBy(activeDeck.getNextShowTime());
			AudioCard card = (AudioCard) activeDeck.topCard();
			
			AudioData data = card.getData();
			audioEntries.add(data.getChallengeFile());
			tick += data.getChallengeDuration();
			float answerDuration = data.getAnswerDuration();
			float gapDuration = answerDuration * 1.5f + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav);
				tick += 1f;
			}
			/*
			 * First answer.
			 */
			audioEntries.add(data.getAnswerFile());
			tick += answerDuration;

			gapDuration = answerDuration + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav);
				tick += 1f;
			}
			/*
			 * Confirm answer.
			 */
			audioEntries.add(data.getAnswerFile());
			tick += answerDuration;

			for (int trailingSilence = 0; trailingSilence < Math.max(3, answerDuration + 2f); trailingSilence++) {
				audioEntries.add(silenceWav);
				tick += 1f;
			}
			
			CardStats cardStats = card.getCardStats();
			cardStats.pimsleurSlotInc();
			long nextInterval = CardUtils.getNextInterval(cardStats.getPimsleurSlot());
			cardStats.setShowAgainDelay_ms(nextInterval);
			cardStats.triesRemainingDec();
			if (cardStats.getTriesRemaining()<1) {
				cardStats.leitnerBoxInc();
				discardsDeck.add(card);
			}
			
		}
		List<String> cmd = new ArrayList<>();
		cmd.add("sox");
		for (File audioEntry : audioEntries) {
			cmd.add(audioEntry.getAbsolutePath());
		}
		File wavOutputFile = new File(EXCERCISES_DIR, "test-output.wav");
		cmd.add(wavOutputFile.getAbsolutePath());
		executeCmd(cmd);
		System.out.println("Total ticks: " + NF.format(tick) + " secs [" + NF.format(tick / 60f) + " mins]");
		File mp3OutputFile = new File(EXCERCISES_DIR, "test-output.mp3");
		cmd.clear();
		cmd.add("ffmpeg");
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(wavOutputFile.getAbsolutePath());
		cmd.add("-codec:a");
		cmd.add("libmp3lame");
		cmd.add("-qscale:a");
		cmd.add("6");
		cmd.add(mp3OutputFile.getAbsolutePath());
		executeCmd(cmd);
	}

	private File generateSilenceWav() {
		File silenceWav = new File(EXCERCISES_DIR, "silence-1-second.wav");
		EXCERCISES_DIR.mkdirs();
		List<String> cmd = Arrays.asList("sox", "-n", "-r", "22050", //
				"-c", "1", silenceWav.getAbsolutePath(), "trim", "0.0", "1.0");
		executeCmd(cmd);
		return silenceWav;
	}

	private void executeCmd(List<String> cmd) {
		ProcessBuilder b = new ProcessBuilder(cmd);
		Process process;
		try {
			process = b.start();
			process.waitFor();
			if (process.exitValue() != 0) {
				System.err.println("FATAL: Bad exit value from\n" + StringUtils.join(cmd, " "));
			}
			process.destroy();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
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
		for (AudioCard card : mainDeck.getCards()) {
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
				System.out.println(" - " + challengeWavFile.getName() + " [" + voice + "]");
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
				System.out.println(" - " + answerWavFile.getName() + " [" + voice + "]");
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
		// int frameSize = format.getFrameSize();
		float frameRate = format.getFrameRate();
		float durationInSeconds = (audioFileLength / frameRate);
		return durationInSeconds;
	}

	private void loadMainDeck() throws IOException {
		loadMainDeck(false);
	}

	private void loadMainDeck(boolean debugSize) throws IOException {
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
		if (debugSize) {
			mainDeck.getCards().subList(200, mainDeck.getCards().size()).clear();
		}
		FileUtils.writeStringToFile(new File("exercise-set.tsv"), debug.toString(), StandardCharsets.UTF_8);
	}

}
