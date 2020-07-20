package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.cherokeelessons.deck.CardStats;
import com.cherokeelessons.deck.CardUtils;

public class Main {
	
	public static final String UNDERDOT = "\u0323";

	private static final NumberFormat NF = NumberFormat.getInstance();
	private static final File WAVS_DIR = new File("tmp/wavs");
	private static final File EXCERCISES_DIR = new File("tmp/excercises");
	private static final String DECK_TSV = "../android/assets/review-sheet.tsv";
	private static final int PRONOUN = 3;
	private static final int VERB_STEM = 4;
	private static final int CHEROKEE_TEXT = 6;
	private static final int ENGLISH_TEXT_START = 7;

	public static void main(final String[] args) throws IOException, UnsupportedAudioFileException {
		new Main().execute();
	}

	private final AudioDeck en2chrDeck;
	private final AudioDeck chr2enDeck;
	private final AudioDeck discardsDeck;

	private final Map<String, Integer> chrVoiceSpeekingRates;
	private final Map<String, Integer> enVoiceSpeekingRates;
	private final Set<String> voiceVariants;
	private final List<String> voices = new ArrayList<>();

	private String previousVoice = "";
	private int voiceShuffleSeed = 0;

	public Main() {
		en2chrDeck = new AudioDeck();
		chr2enDeck = new AudioDeck();

		discardsDeck = new AudioDeck();

		chrVoiceSpeekingRates = new HashMap<>();
		enVoiceSpeekingRates = new HashMap<>();

		voiceVariants = new TreeSet<>();
		// default
//		voiceVariants.add("");
		// magali's choices
//		voiceVariants.add("Diogo");
		voiceVariants.add("f5");
//		voiceVariants.add("f5");
		voiceVariants.add("f2");
		// craig's choices
//		voiceVariants.add("antonio");//, "Mr", "robosoft5"));
		voiceVariants.add("Mr");
//		voiceVariants.add("robosoft5");

		// tommylee's choices
//		voiceVariants.add("Diogo");

		// voice speed adjustments (word per minute espeak -s parameter)
		chrVoiceSpeekingRates.put("", 200);
		chrVoiceSpeekingRates.put("Diogo", 200);
		chrVoiceSpeekingRates.put("f5", 200);
		chrVoiceSpeekingRates.put("f2", 200);
		chrVoiceSpeekingRates.put("antonio", 100);
		chrVoiceSpeekingRates.put("Mr", 170);
		chrVoiceSpeekingRates.put("robosoft5", 90);

		enVoiceSpeekingRates.put("", 90);
		enVoiceSpeekingRates.put("Diogo", 90);
		enVoiceSpeekingRates.put("f5", 90);
		enVoiceSpeekingRates.put("f2", 90);
		enVoiceSpeekingRates.put("antonio", 90);
		enVoiceSpeekingRates.put("Mr", 90);
		enVoiceSpeekingRates.put("robosoft5", 90);
	}

	private void buildChr2EnExerciseMp3Files() throws IOException {
		System.out.println("=== buildChr2EnExerciseMp3Files");
		final AudioDeck activeDeck = new AudioDeck();

		final File tmpDir = new File(EXCERCISES_DIR, "chr2en");
		FileUtils.deleteQuietly(tmpDir);
		tmpDir.mkdirs();
		final AudioData audioSilence = generateSilenceWav();
		final AudioData audioNewPhrase = generateNewPhrase();
		final AudioData audioTranslatePhrase = generateTranslatePhrase();
		final AudioData audioListenAgain = listenAgain();
		final AudioData audioItsTranslationIs = itsTranslationIs();
		final AudioData audioExerciseConclusion = thisConcludesThisExercise();
		final List<File> audioEntries = new ArrayList<>();
		String prevCardId = "";
		float tick = 0f;

		/*
		 * Seed cards.
		 */
//		SessionUtil session = new SessionUtil(chr2enDeck, 5);
		for (int ix = 0; ix < 2 && chr2enDeck.hasCards(); ix++) {
			final AudioCard topCard = (AudioCard) chr2enDeck.topCard();
			topCard.resetStats();
			topCard.resetTriesRemaining(5);
			topCard.getCardStats().setShowAgainDelay_ms(ix * 5000l);
			topCard.getCardStats().setNewCard(true);
			activeDeck.add(topCard);
		}

		final float trailingBuffer = audioExerciseConclusion.getAnswerDuration() + 5f;
		while (tick < 60f * 60f - trailingBuffer) {
			float deltaTick = 0f;
			if (!activeDeck.hasCards() && !chr2enDeck.hasCards()) {
				break;
			}
			if (!activeDeck.hasCards() || activeDeck.getNextShowTime() > 5000l) {
				final AudioCard topCard = (AudioCard) chr2enDeck.topCard();
				topCard.resetStats();
				topCard.resetTriesRemaining(5);
				topCard.getCardStats().setNewCard(true);
				activeDeck.add(topCard);
			}
			sortDeckByShowAgainDelay(activeDeck);
			AudioCard card = (AudioCard) activeDeck.topCard();
			String cardId = card.id();

			if (cardId.equals(prevCardId)) {
				card = (AudioCard) chr2enDeck.topCard();
				card.resetStats();
				card.resetTriesRemaining(5);
				card.getCardStats().setNewCard(true);
				cardId = card.id();
				activeDeck.add(card);
			}
			prevCardId = cardId;

			final AudioData data = card.getData();

			final boolean newCard = card.getCardStats().isNewCard();
			if (newCard) {
				card.getCardStats().setNewCard(false);
				audioEntries.add(audioNewPhrase.getAnswerFile());
				deltaTick += audioNewPhrase.getAnswerDuration();
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;
			} else {
				audioEntries.add(audioTranslatePhrase.getAnswerFile());
				deltaTick += audioTranslatePhrase.getAnswerDuration();
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;
			}
			/*
			 * First challenge.
			 */
			audioEntries.add(data.getChallengeFile());
			deltaTick += data.getChallengeDuration();

			/*
			 * Repeat challenge if new card
			 */
			final float answerDuration = data.getAnswerDuration();
			if (newCard) {
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;
				audioEntries.add(audioListenAgain.getAnswerFile());
				deltaTick += audioListenAgain.getAnswerDuration();
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;
				audioEntries.add(data.getChallengeFile());
				deltaTick += data.getChallengeDuration();
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;
				audioEntries.add(audioItsTranslationIs.getAnswerFile());
				deltaTick += audioItsTranslationIs.getAnswerDuration();
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;

			} else {
				float gapDuration = answerDuration * 1.1f + 2f;
				while (gapDuration-- > 0f) {
					audioEntries.add(audioSilence.getAnswerFile());
					deltaTick += 1f;
				}
			}

			/*
			 * The answer.
			 */
			audioEntries.add(data.getAnswerFile());
			deltaTick += answerDuration;

			for (int trailingSilence = 0; trailingSilence < 3f; trailingSilence++) {
				audioEntries.add(audioSilence.getAnswerFile());
				deltaTick += 1f;
			}

			System.out.println(data.id() + ") " + data.getAnswer() + " " + NF.format(tick));
			activeDeck.updateTimeBy((long) (deltaTick * 1000f));

			final CardStats cardStats = card.getCardStats();

			final long nextInterval = CardUtils.getNextInterval(cardStats.getPimsleurSlot());
			cardStats.setShowAgainDelay_ms(nextInterval);
			cardStats.triesRemainingDec();
			cardStats.pimsleurSlotInc();
			if (cardStats.getTriesRemaining() < 1) {
				cardStats.leitnerBoxInc();
				discardsDeck.add(card);
			}
			tick += deltaTick;
		}
		audioEntries.add(audioSilence.getAnswerFile());
		audioEntries.add(audioSilence.getAnswerFile());
		audioEntries.add(audioSilence.getAnswerFile());
		audioEntries.add(audioExerciseConclusion.getAnswerFile());
		audioEntries.add(audioSilence.getAnswerFile());
		audioEntries.add(audioSilence.getAnswerFile());

		final File wavOutputFile = new File(tmpDir,
				"chr2en-graduated-interval-recall-test-output-" + LocalDate.now().toString() + ".wav");
		for (final List<File> audioEntriesSublist : ListUtils.partition(audioEntries, 200)) {
			final List<String> cmd = new ArrayList<>();
			cmd.add("sox");
			final File tmp1 = new File(tmpDir, "temp1.wav");
			FileUtils.deleteQuietly(tmp1);
			if (wavOutputFile.exists()) {
				wavOutputFile.renameTo(tmp1);
				cmd.add(tmp1.getAbsolutePath());
			}
			for (final File audioEntry : audioEntriesSublist) {
				cmd.add(audioEntry.getAbsolutePath());
			}
			cmd.add(wavOutputFile.getAbsolutePath());
			executeCmd(cmd);
			FileUtils.deleteQuietly(tmp1);
		}
		System.out.println("Total ticks: " + NF.format(tick) + " secs [" + NF.format(tick / 60f) + " mins]");
		final File mp3OutputFile = new File(tmpDir,
				"chr2en-graduated-interval-recall-test-output-" + LocalDate.now().toString() + ".mp3");
		final List<String> cmd = new ArrayList<>();
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

	private AudioData thisConcludesThisExercise() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "concludes-this-exercise.wav");
		FileUtils.deleteQuietly(newPhrase);
		File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "This concludes this exercise.");
		List<String> cmd = new ArrayList<>();
		cmd.add("ffmpeg");
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(tmp.getAbsolutePath());
		cmd.add(newPhrase.getAbsolutePath());
		executeCmd(cmd);
		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(newPhrase.getAbsolutePath());
		executeCmd(cmd);
		AudioData data = new AudioData();
		data.setAnswerFile(newPhrase);
		data.setAnswerDuration(getDuration(newPhrase));
		return data;
	}

	private void buildEn2ChrExerciseMp3Files() {
		final AudioDeck activeDeck = new AudioDeck();

		final File tmpDir = new File(EXCERCISES_DIR, "en2chr");
		FileUtils.deleteQuietly(tmpDir);
		tmpDir.mkdirs();
		final AudioData silenceWav = generateSilenceWav();
		final List<File> audioEntries = new ArrayList<>();
		String prevCardId = "";
		float tick = 0f;

		/*
		 * Seed cards.
		 */
		for (int ix = 0; ix < 3 && en2chrDeck.hasCards(); ix++) {
			final AudioCard topCard = (AudioCard) en2chrDeck.topCard();
			topCard.resetStats();
			topCard.resetTriesRemaining(6);
			topCard.getCardStats().setShowAgainDelay_ms(ix * 5000l);
			activeDeck.add(topCard);
		}

		while (tick < 60f * 60f) {
			float deltaTick = 0f;
			if (!activeDeck.hasCards() && !en2chrDeck.hasCards()) {
				break;
			}
			if (!activeDeck.hasCards() || activeDeck.getNextShowTime() > 5000l) {
				final AudioCard topCard = (AudioCard) en2chrDeck.topCard();
				topCard.resetStats();
				topCard.resetTriesRemaining(6);
				activeDeck.add(topCard);
			}
			sortDeckByShowAgainDelay(activeDeck);
			AudioCard card = (AudioCard) activeDeck.topCard();
			String cardId = card.id();

			if (cardId.equals(prevCardId)) {
				card = (AudioCard) en2chrDeck.topCard();
				card.resetStats();
				card.resetTriesRemaining(6);
				cardId = card.id();
				activeDeck.add(card);
			}
			prevCardId = cardId;

			final AudioData data = card.getData();
			audioEntries.add(data.getChallengeFile());
			deltaTick += data.getChallengeDuration();
			final float answerDuration = data.getAnswerDuration();
			float gapDuration = answerDuration * 1.5f + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav.getAnswerFile());
				deltaTick += 1f;
			}

			/*
			 * First answer.
			 */
			audioEntries.add(data.getAnswerFile());
			deltaTick += answerDuration;

			gapDuration = answerDuration + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav.getAnswerFile());
				deltaTick += 1f;
			}
			/*
			 * Confirm answer.
			 */
			audioEntries.add(data.getAnswerFile());
			deltaTick += answerDuration;

			for (int trailingSilence = 0; trailingSilence < Math.max(3, answerDuration + 2f); trailingSilence++) {
				audioEntries.add(silenceWav.getAnswerFile());
				deltaTick += 1f;
			}

			System.out.println(data.id() + ") " + data.getAnswer() + " " + NF.format(tick));
			activeDeck.updateTimeBy((long) (deltaTick * 1000f));

			final CardStats cardStats = card.getCardStats();

			final long nextInterval = CardUtils.getNextInterval(cardStats.getPimsleurSlot());
			cardStats.setShowAgainDelay_ms(nextInterval);
			cardStats.triesRemainingDec();
			cardStats.pimsleurSlotInc();
			if (cardStats.getTriesRemaining() < 1) {
				cardStats.leitnerBoxInc();
				discardsDeck.add(card);
			}
			tick += deltaTick;
		}
		final File wavOutputFile = new File(tmpDir,
				"en2chr-graduated-interval-recall-test-output-" + LocalDate.now().toString() + ".wav");
		for (final List<File> audioEntriesSublist : ListUtils.partition(audioEntries, 100)) {
			final List<String> cmd = new ArrayList<>();
			cmd.add("sox");
			final File tmp1 = new File(tmpDir, "temp1.wav");
			FileUtils.deleteQuietly(tmp1);
			if (wavOutputFile.exists()) {
				wavOutputFile.renameTo(tmp1);
				cmd.add(tmp1.getAbsolutePath());
			}
			for (final File audioEntry : audioEntriesSublist) {
				cmd.add(audioEntry.getAbsolutePath());
			}
			cmd.add(wavOutputFile.getAbsolutePath());
			executeCmd(cmd);
			FileUtils.deleteQuietly(tmp1);
		}
		System.out.println("Total ticks: " + NF.format(tick) + " secs [" + NF.format(tick / 60f) + " mins]");
		final File mp3OutputFile = new File(tmpDir,
				"en2chr-graduated-interval-recall-test-output-" + LocalDate.now().toString() + ".mp3");
		final List<String> cmd = new ArrayList<>();
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

	public void execute() throws IOException, UnsupportedAudioFileException {
		loadMainDecks();

		generateChr2EnWavFiles();
		buildChr2EnExerciseMp3Files();

//		generateEn2ChrWavFiles();
//		buildEn2ChrExerciseMp3Files();
//		generateDurationsReport();
	}

	private void executeCmd(final List<String> cmd) {
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

	private void generateChr2EnWavFiles() throws UnsupportedAudioFileException, IOException {
		System.out.println("=== generateChr2EnWavFiles");
		voiceShuffleSeed = 0;
		final File wavTmpDir = new File(WAVS_DIR, "chr2en");
		FileUtils.deleteQuietly(wavTmpDir);
		final File espeakNgBin = new File(SystemUtils.getUserHome(), "espeak-ng/bin/espeak-ng");
		final ESpeakNg espeak = new ESpeakNg(espeakNgBin);
		final Set<String> already = new HashSet<>();
		for (final AudioCard card : chr2enDeck.getCards()) {
			final AudioData data = card.getData();
			String answer = data.getAnswer();
			answer = AudioGenUtil.removeEnglishFixedGenderMarks(AudioGenUtil.alternizeEnglishSexes(answer));
			final String challenge = data.getChallenge();
			final String challengeFilename = AudioGenUtil.asPhoneticFilename(challenge);
			final File challengeWavFile = new File(wavTmpDir, "challenge-" + challengeFilename + ".wav");
			final String answerFilename = AudioGenUtil.asEnglishFilename(answer);
			final File answerWavFile = new File(wavTmpDir, "answer-" + answerFilename + ".wav");
			data.setAnswerFile(answerWavFile);
			data.setChallengeFile(challengeWavFile);
			if (!already.contains(challenge)) {
				String voice = nextVoice(answer);
				int speed;
				if (chrVoiceSpeekingRates.containsKey(voice)) {
					speed = chrVoiceSpeekingRates.get(voice);
				} else {
					speed = 0;
				}
				if (!voice.trim().isEmpty()) {
					voice = "chr+" + voice;
				} else {
					voice = "chr";
				}
				System.out.println(" - " + challengeWavFile.getName() + " [" + voice + "]");
				espeak.generateWav(voice, speed, challengeWavFile, challenge);
				final float durationInSeconds = getDuration(challengeWavFile);
				data.setChallengeDuration(durationInSeconds);
			}
			if (!already.contains(answer)) {
				String voice = nextVoice(answer);
				int speed;
				if (enVoiceSpeekingRates.containsKey(voice)) {
					speed = enVoiceSpeekingRates.get(voice);
				} else {
					speed = 0;
				}
				if (!voice.trim().isEmpty()) {
					voice = "en-us+" + voice;
				} else {
					voice = "en-us";
				}
				System.out.println(" - " + answerWavFile.getName());
				// espeak.generateWav(voice, speed, answerWavFile, answer);
				if (voice.contains("+f")) {
					File tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_FEMALE_1, answer);
					List<String> cmd = new ArrayList<>();
					cmd.add("ffmpeg");
					cmd.add("-y");
					cmd.add("-i");
					cmd.add(tmp.getAbsolutePath());
					cmd.add(answerWavFile.getAbsolutePath());
					executeCmd(cmd);
					cmd.clear();
					cmd.add("normalize-audio");
					cmd.add(answerWavFile.getAbsolutePath());
					executeCmd(cmd);
				} else {
					File tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_MALE_1, answer);
					List<String> cmd = new ArrayList<>();
					cmd.add("ffmpeg");
					cmd.add("-y");
					cmd.add("-i");
					cmd.add(tmp.getAbsolutePath());
					cmd.add(answerWavFile.getAbsolutePath());
					executeCmd(cmd);
					cmd.clear();
					cmd.add("normalize-audio");
					cmd.add(answerWavFile.getAbsolutePath());
					executeCmd(cmd);
				}
				already.add(answer);
				final float durationInSeconds = getDuration(answerWavFile);
				data.setAnswerDuration(durationInSeconds);
			}
		}
	}

	private void generateDurationsReport() throws IOException {
		final File reportFile = new File("tmp/durations.tsv");
		final StringBuilder sb = new StringBuilder();
		sb.append("Challenge Wav");
		sb.append("\t");
		sb.append("Duration");
		sb.append("\t");
		sb.append("Answer Wav");
		sb.append("\t");
		sb.append("Duration");
		sb.append("\n");
		for (final AudioCard card : en2chrDeck.getCards()) {
			final AudioData data = card.getData();
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

	private void generateEn2ChrWavFiles() throws UnsupportedAudioFileException, IOException {
		voiceShuffleSeed = 0;
		final File wavTmpDir = new File(WAVS_DIR, "en2chr");
		FileUtils.deleteQuietly(wavTmpDir);
		final File espeakNgBin = new File(SystemUtils.getUserHome(), "espeak-ng/bin/espeak-ng");
		final ESpeakNg espeak = new ESpeakNg(espeakNgBin);
		final Set<String> already = new HashSet<>();
		for (final AudioCard card : en2chrDeck.getCards()) {
			final AudioData data = card.getData();
			final String answer = data.getAnswer();
			String challenge = data.getChallenge();
			challenge = AudioGenUtil.removeEnglishFixedGenderMarks(AudioGenUtil.randomizeEnglishSexes(challenge));
			final String challengeFilename = AudioGenUtil.asEnglishFilename(challenge);
			final File challengeWavFile = new File(wavTmpDir, "challenge-" + challengeFilename + ".wav");
			final String answerFilename = AudioGenUtil.asPhoneticFilename(answer);
			final File answerWavFile = new File(wavTmpDir, "answer-" + answerFilename + ".wav");
			data.setAnswerFile(answerWavFile);
			data.setChallengeFile(challengeWavFile);
			if (!already.contains(challenge)) {
				String voice = nextVoice(challenge);
				if (!voice.trim().isEmpty()) {
					voice = "en-us+" + voice;
				} else {
					voice = "en-us";
				}
				System.out.println(" - " + challengeWavFile.getName());
				if (voice.contains("+f")) {
					AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_FEMALE_1, challenge);
				} else {
					AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_MALE_1, challenge);
				}
				final float durationInSeconds = getDuration(challengeWavFile);
				data.setChallengeDuration(durationInSeconds);
			}
			if (!already.contains(answer)) {
				String voice = nextVoice(challenge);
				int speed;
				if (chrVoiceSpeekingRates.containsKey(voice)) {
					speed = chrVoiceSpeekingRates.get(voice);
				} else {
					speed = 0;
				}
				if (!voice.trim().isEmpty()) {
					voice = "chr+" + voice;
				} else {
					voice = "chr";
				}
				System.out.println(" - " + answerWavFile.getName() + " [" + voice + "]");
				espeak.generateWav(voice, speed, answerWavFile, answer);
				already.add(answer);
				final float durationInSeconds = getDuration(answerWavFile);
				data.setAnswerDuration(durationInSeconds);
			}
		}
	}

	private AudioData generateSilenceWav() {
		EXCERCISES_DIR.mkdirs();
		final File silenceWav = new File(EXCERCISES_DIR, "silence-1-second.wav");
		FileUtils.deleteQuietly(silenceWav);
		final List<String> cmd = Arrays.asList("sox", "-n", "-r", "22050", //
				"-c", "1", silenceWav.getAbsolutePath(), "trim", "0.0", "1.0");
		executeCmd(cmd);
		AudioData data = new AudioData();
		data.setAnswerFile(silenceWav);
		data.setAnswerDuration(getDuration(silenceWav));
		return data;
	}

	private AudioData generateNewPhrase() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "here-is-a-new-phrase.wav");
		FileUtils.deleteQuietly(newPhrase);
		File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR,
				"Here is a new phrase to learn. Listen carefully:");
		List<String> cmd = new ArrayList<>();
		cmd.add("ffmpeg");
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(tmp.getAbsolutePath());
		cmd.add(newPhrase.getAbsolutePath());
		executeCmd(cmd);
		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(newPhrase.getAbsolutePath());
		executeCmd(cmd);
		AudioData data = new AudioData();
		data.setAnswerFile(newPhrase);
		data.setAnswerDuration(getDuration(newPhrase));
		return data;
	}

	private AudioData listenAgain() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "listen-again.wav");
		FileUtils.deleteQuietly(newPhrase);
		File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Here is the phrase again:");
		List<String> cmd = new ArrayList<>();
		cmd.add("ffmpeg");
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(tmp.getAbsolutePath());
		cmd.add(newPhrase.getAbsolutePath());
		executeCmd(cmd);
		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(newPhrase.getAbsolutePath());
		executeCmd(cmd);
		AudioData data = new AudioData();
		data.setAnswerFile(newPhrase);
		data.setAnswerDuration(getDuration(newPhrase));
		return data;
	}

	private AudioData itsTranslationIs() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "its-translation-is.wav");
		FileUtils.deleteQuietly(newPhrase);
		File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Here it is in English:");
		List<String> cmd = new ArrayList<>();
		cmd.add("ffmpeg");
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(tmp.getAbsolutePath());
		cmd.add(newPhrase.getAbsolutePath());
		executeCmd(cmd);
		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(newPhrase.getAbsolutePath());
		executeCmd(cmd);
		AudioData data = new AudioData();
		data.setAnswerFile(newPhrase);
		data.setAnswerDuration(getDuration(newPhrase));
		return data;
	}

	private AudioData generateTranslatePhrase() throws IOException {
		final File translateIntoEnglish = new File(EXCERCISES_DIR, "translate-into-english.wav");
		FileUtils.deleteQuietly(translateIntoEnglish);
		File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Translate into English:");
		List<String> cmd = new ArrayList<>();
		cmd.add("ffmpeg");
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(tmp.getAbsolutePath());
		cmd.add(translateIntoEnglish.getAbsolutePath());
		executeCmd(cmd);
		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(translateIntoEnglish.getAbsolutePath());
		executeCmd(cmd);
		AudioData data = new AudioData();
		data.setAnswerFile(translateIntoEnglish);
		data.setAnswerDuration(getDuration(translateIntoEnglish));
		return data;
	}

	private float getDuration(final File answerWavFile) {
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

	private void loadMainDecks() throws IOException {
		final StringBuilder reviewSheetChr2En = new StringBuilder();
		final StringBuilder reviewSheetEn2Chr = new StringBuilder();
		final File jsonFile = new File(DECK_TSV);
		System.out.println(jsonFile.getAbsolutePath());
		final Map<String, AudioCard> cardsForCherokeeAnswers = new HashMap<>();
		final Map<String, AudioCard> cardsForEnglishAnswers = new HashMap<>();
		try (LineIterator li = FileUtils.lineIterator(jsonFile, StandardCharsets.UTF_8.name())) {
			li.next();
			int idEn2Chr = 0;
			int idChr2En = 0;
			while (li.hasNext()) {
				final String line = li.next();
				final String[] fields = line.split("\t");
				if (fields.length <= ENGLISH_TEXT_START) {
					System.out.println("; " + line);
					continue;
				}
				String verbStem = fields[VERB_STEM].replaceAll("[¹²³⁴"+UNDERDOT+"]", "").trim();
				String boundPronoun = fields[PRONOUN].replaceAll("[¹²³⁴"+UNDERDOT+"]", "").trim();
				String cherokeeText = fields[CHEROKEE_TEXT].trim();
				if (cherokeeText.isEmpty()) {
					continue;
				}
				cherokeeText = StringUtils.capitalize(cherokeeText);
				if (!cherokeeText.matches(".*[.?!]")) {
					cherokeeText += ".";
				}
				for (int ix = ENGLISH_TEXT_START; ix < fields.length; ix++) {
					String englishText = fields[ix].trim();
					if (englishText.isEmpty()) {
						continue;
					}
					englishText = StringUtils.capitalize(englishText);
					if (!englishText.matches(".*[.?!]")) {
						englishText += ".";
					}
					if (englishText.contains(" (")) {
						// English text pronunciation adjustments
						englishText = englishText.replace(" (1)", " one");
						englishText = englishText.replace(" (animate)", ", animate");
						englishText = englishText.replace(" (inanimate)", ", inanimate");
					}
					/*
					 * Each deck gets its own set of cards.
					 */
					AudioCard toChrCard;
					AudioData toChrData;
					if (cardsForCherokeeAnswers.containsKey(englishText)) {
						toChrCard = cardsForCherokeeAnswers.get(englishText);
						toChrData = toChrCard.getData();
						toChrData.setAnswer(toChrData.getAnswer() + ", " + cherokeeText);
						cardsForCherokeeAnswers.put(englishText, toChrCard);
					} else {
						toChrCard = new AudioCard();
						toChrData = new AudioData();
						toChrData.setBoundPronoun(boundPronoun);
						toChrData.setVerbStem(verbStem);
						toChrData.setAnswer(cherokeeText);
						toChrData.setAnswerDuration(0);
						toChrData.setChallenge(englishText);
						toChrData.setChallengeDuration(0);
						toChrData.setId(++idEn2Chr);
						toChrCard.setData(toChrData);
						cardsForCherokeeAnswers.put(englishText, toChrCard);
						en2chrDeck.add(toChrCard);
					}
					reviewSheetEn2Chr.append(
							toChrData.id() + "\t" + toChrData.getChallenge() + "\t" + toChrData.getAnswer() + "\n");

					AudioCard toEnCard;
					AudioData toEnData;
					if (cardsForEnglishAnswers.containsKey(cherokeeText)) {
						toEnCard = cardsForEnglishAnswers.get(cherokeeText);
						toEnData = toEnCard.getData();
						toEnData.setAnswer(toEnData.getAnswer() + " Or, " + englishText);
					} else {
						toEnCard = new AudioCard();
						toEnData = new AudioData();
						toEnData.setBoundPronoun(boundPronoun);
						toEnData.setVerbStem(verbStem);
						toEnData.setAnswer(englishText);
						toEnData.setAnswerDuration(0);
						toEnData.setChallenge(cherokeeText);
						toEnData.setChallengeDuration(0);
						toEnData.setId(++idChr2En);
						toEnCard.setData(toEnData);
						cardsForEnglishAnswers.put(cherokeeText, toEnCard);
						chr2enDeck.add(toEnCard);
					}
					reviewSheetChr2En.append(toEnData.id());
					reviewSheetChr2En.append("\t");
					reviewSheetChr2En.append(toEnData.getBoundPronoun());
					reviewSheetChr2En.append("\t");
					reviewSheetChr2En.append(toEnData.getVerbStem());
					reviewSheetChr2En.append("\t");
					reviewSheetChr2En.append(toEnData.getChallenge());
					reviewSheetChr2En.append("\t");
					reviewSheetChr2En.append(toEnData.getAnswer());
					reviewSheetChr2En.append("\n");
				}
			}
		}
		FileUtils.writeStringToFile(new File("review-sheet-chr-en.tsv"), reviewSheetChr2En.toString(),
				StandardCharsets.UTF_8);
		FileUtils.writeStringToFile(new File("review-sheet-en-chr.tsv"), reviewSheetEn2Chr.toString(),
				StandardCharsets.UTF_8);
	}

	public String nextVoice(String englishText) {
		if (voices.isEmpty()) {
			voices.addAll(voiceVariants);
			do {
				Collections.shuffle(voices, new Random(voiceShuffleSeed++));
			} while (voices.get(0).equals(previousVoice) && voiceVariants.size() > 2);
		}
		String lc = englishText.toLowerCase();
		if (lc.contains("mother")
				&& (lc.matches("\bi\b") || lc.matches("\bme\b") || lc.matches("\bwe\b") || lc.matches("\bus\b"))) {
			if (!voices.get(0).startsWith("f")) {
				return nextVoice(englishText);
			}
		}
		if (lc.contains("father")
				&& (lc.matches("\bi\b") || lc.matches("\bme\b") || lc.matches("\bwe\b") || lc.matches("\bus\b"))) {
			if (voices.get(0).startsWith("f")) {
				return nextVoice(englishText);
			}
		}
		return previousVoice = voices.remove(0);
	}

	private void sortDeckByShowAgainDelay(final AudioDeck deck) {
		final List<AudioCard> cards = deck.getCards();
		Collections.shuffle(cards);
		Collections.sort(cards, (o1, o2) -> {
			if (o1 == o2) {
				return 0;
			}
			if (o1 == null) {
				return -1;
			}
			if (o2 == null) {
				return 1;
			}
			return (int) o1.getCardStats().getShowAgainDelay_ms() - (int) o2.getCardStats().getShowAgainDelay_ms();
		});
	}

}
