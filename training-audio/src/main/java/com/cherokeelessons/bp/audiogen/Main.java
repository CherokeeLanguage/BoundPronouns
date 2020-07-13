package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import javax.swing.plaf.ListUI;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.cherokeelessons.deck.CardStats;
import com.cherokeelessons.deck.CardUtils;

public class Main {

	private static final NumberFormat NF = NumberFormat.getInstance();
	private static final File WAVS_DIR = new File("tmp/wavs");
	private static final File EXCERCISES_DIR = new File("tmp/excercises");
	private static final String DECK_TSV = "../android/assets/review-sheet.tsv";
	private static final int CHEROKEE_TEXT = 6;
	private static final int ENGLISH_TEXT_START = 7;

	private final AudioDeck en2chrDeck;
	private final AudioDeck chr2enDeck;
	private final AudioDeck discardsDeck;
	private final Set<String> voiceVariants;

	private List<String> voices = new ArrayList<>();
	private String previousVoice = "";

	private int voiceShuffleSeed=0;
	public String nextVoice() {
		if (voices.isEmpty()) {
			voices.addAll(voiceVariants);
			do {
				Collections.shuffle(voices, new Random(voiceShuffleSeed++));
			} while (voices.get(0).equals(previousVoice));
		}
		return previousVoice = voices.remove(0);
	}

	public Main() {
		en2chrDeck = new AudioDeck();
		chr2enDeck = new AudioDeck();
		
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
		loadMainDecks();
		
		generateChr2EnWavFiles();
		buildChr2EnExerciseMp3Files();
		
		generateEn2ChrWavFiles();
		buildEn2ChrExerciseMp3Files();
//		generateDurationsReport();
	}

	private void buildEn2ChrExerciseMp3Files() {
		final AudioDeck activeDeck = new AudioDeck();
		
		File tmpDir = new File(EXCERCISES_DIR, "en2chr");
		FileUtils.deleteQuietly(tmpDir);
		tmpDir.mkdirs();
		File silenceWav = generateSilenceWav();
		List<File> audioEntries = new ArrayList<>();
		String prevCardId = "";
		float tick = 0f;
		
		/*
		 * Seed cards.
		 */
		for (int ix=0; ix<3 && en2chrDeck.hasCards(); ix++) {
			AudioCard topCard = (AudioCard) en2chrDeck.topCard();
			topCard.resetStats();
			topCard.resetTriesRemaining(6);
			topCard.getCardStats().setShowAgainDelay_ms(ix*5000l);
			activeDeck.add(topCard);
		}
		
		while (tick < 60f * 60f) {
			float deltaTick = 0f;
			if (!activeDeck.hasCards() && !en2chrDeck.hasCards()) {
				break;
			}
			if (!activeDeck.hasCards() || activeDeck.getNextShowTime()>5000l) {
				AudioCard topCard = (AudioCard) en2chrDeck.topCard();
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
			prevCardId=cardId;
			
			AudioData data = card.getData();
			audioEntries.add(data.getChallengeFile());
			deltaTick += data.getChallengeDuration();
			float answerDuration = data.getAnswerDuration();
			float gapDuration = answerDuration * 1.5f + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav);
				deltaTick += 1f;
			}
			
			/*
			 * First answer.
			 */
			audioEntries.add(data.getAnswerFile());
			deltaTick += answerDuration;

			gapDuration = answerDuration + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav);
				deltaTick += 1f;
			}
			/*
			 * Confirm answer.
			 */
			audioEntries.add(data.getAnswerFile());
			deltaTick += answerDuration;

			for (int trailingSilence = 0; trailingSilence < Math.max(3, answerDuration + 2f); trailingSilence++) {
				audioEntries.add(silenceWav);
				deltaTick += 1f;
			}
			
			System.out.println(data.id() + ") "+ data.getAnswer()+" "+NF.format(tick));
			activeDeck.updateTimeBy((long) (deltaTick*1000f));
			
			CardStats cardStats = card.getCardStats();
			
			long nextInterval = CardUtils.getNextInterval(cardStats.getPimsleurSlot());
			cardStats.setShowAgainDelay_ms(nextInterval);
			cardStats.triesRemainingDec();
			cardStats.pimsleurSlotInc();
			if (cardStats.getTriesRemaining()<1) {
				cardStats.leitnerBoxInc();
				discardsDeck.add(card);
			}
			tick+=deltaTick;
		}
		File wavOutputFile = new File(tmpDir, "en2chr-graduated-interval-recall-test-output-"+LocalDate.now().toString()+".wav");
		for (List<File> audioEntriesSublist: ListUtils.partition(audioEntries, 100)) {
			List<String> cmd = new ArrayList<>();
			cmd.add("sox");
			File tmp1 = new File(tmpDir, "temp1.wav");
			FileUtils.deleteQuietly(tmp1);
			if (wavOutputFile.exists()) {
				wavOutputFile.renameTo(tmp1);
				cmd.add(tmp1.getAbsolutePath());
			}
			for (File audioEntry : audioEntriesSublist) {
				cmd.add(audioEntry.getAbsolutePath());
			}
			cmd.add(wavOutputFile.getAbsolutePath());
			executeCmd(cmd);
			FileUtils.deleteQuietly(tmp1);
		}
		System.out.println("Total ticks: " + NF.format(tick) + " secs [" + NF.format(tick / 60f) + " mins]");
		File mp3OutputFile = new File(tmpDir, "en2chr-graduated-interval-recall-test-output-"+LocalDate.now().toString()+".mp3");
		List<String> cmd = new ArrayList<>();
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
	
	private void buildChr2EnExerciseMp3Files() {
		final AudioDeck activeDeck = new AudioDeck();
		
		File tmpDir = new File(EXCERCISES_DIR, "chr2en");
		FileUtils.deleteQuietly(tmpDir);
		tmpDir.mkdirs();
		File silenceWav = generateSilenceWav();
		List<File> audioEntries = new ArrayList<>();
		String prevCardId = "";
		float tick = 0f;
		
		/*
		 * Seed cards.
		 */
		for (int ix=0; ix<3 && chr2enDeck.hasCards(); ix++) {
			AudioCard topCard = (AudioCard) chr2enDeck.topCard();
			topCard.resetStats();
			topCard.resetTriesRemaining(6);
			topCard.getCardStats().setShowAgainDelay_ms(ix*5000l);
			activeDeck.add(topCard);
		}
		
		while (tick < 60f * 60f) {
			float deltaTick = 0f;
			if (!activeDeck.hasCards() && !chr2enDeck.hasCards()) {
				break;
			}
			if (!activeDeck.hasCards() || activeDeck.getNextShowTime()>5000l) {
				AudioCard topCard = (AudioCard) chr2enDeck.topCard();
				topCard.resetStats();
				topCard.resetTriesRemaining(6);
				activeDeck.add(topCard);
			}
			sortDeckByShowAgainDelay(activeDeck);
			AudioCard card = (AudioCard) activeDeck.topCard();
			String cardId = card.id();
			
			if (cardId.equals(prevCardId)) {
				card = (AudioCard) chr2enDeck.topCard();
				card.resetStats();
				card.resetTriesRemaining(6);
				cardId = card.id();
				activeDeck.add(card);
			}
			prevCardId=cardId;
			
			AudioData data = card.getData();
			
			/*
			 * First challenge.
			 */
			audioEntries.add(data.getChallengeFile());
			deltaTick += data.getChallengeDuration();
			
			float answerDuration = data.getAnswerDuration();
			float gapDuration = answerDuration * 1.5f + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav);
				deltaTick += 1f;
			}
			
			/*
			 * First answer.
			 */
			audioEntries.add(data.getAnswerFile());
			deltaTick += answerDuration;

			for (int trailingSilence = 0; trailingSilence < Math.max(3, answerDuration + 2f); trailingSilence++) {
				audioEntries.add(silenceWav);
				deltaTick += 1f;
			}
			
			System.out.println(data.id() + ") "+ data.getAnswer()+" "+NF.format(tick));
			activeDeck.updateTimeBy((long) (deltaTick*1000f));
			
			CardStats cardStats = card.getCardStats();
			
			long nextInterval = CardUtils.getNextInterval(cardStats.getPimsleurSlot());
			cardStats.setShowAgainDelay_ms(nextInterval);
			cardStats.triesRemainingDec();
			cardStats.pimsleurSlotInc();
			if (cardStats.getTriesRemaining()<1) {
				cardStats.leitnerBoxInc();
				discardsDeck.add(card);
			}
			tick+=deltaTick;
		}
		File wavOutputFile = new File(tmpDir, "chr2en-graduated-interval-recall-test-output-"+LocalDate.now().toString()+".wav");
		for (List<File> audioEntriesSublist: ListUtils.partition(audioEntries, 100)) {
			List<String> cmd = new ArrayList<>();
			cmd.add("sox");
			File tmp1 = new File(tmpDir, "temp1.wav");
			FileUtils.deleteQuietly(tmp1);
			if (wavOutputFile.exists()) {
				wavOutputFile.renameTo(tmp1);
				cmd.add(tmp1.getAbsolutePath());
			}
			for (File audioEntry : audioEntriesSublist) {
				cmd.add(audioEntry.getAbsolutePath());
			}
			cmd.add(wavOutputFile.getAbsolutePath());
			executeCmd(cmd);
			FileUtils.deleteQuietly(tmp1);
		}
		System.out.println("Total ticks: " + NF.format(tick) + " secs [" + NF.format(tick / 60f) + " mins]");
		File mp3OutputFile = new File(tmpDir, "chr2en-graduated-interval-recall-test-output-"+LocalDate.now().toString()+".mp3");
		List<String> cmd = new ArrayList<>();
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

	private void sortDeckByShowAgainDelay(AudioDeck deck) {
		List<AudioCard> cards = deck.getCards();
		Collections.shuffle(cards);
		Collections.sort(cards, new Comparator<AudioCard>() {
			@Override
			public int compare(final AudioCard o1, final AudioCard o2) {
				if (o1 == o2) {
					return 0;
				}
				if (o1 == null) {
					return -1;
				}
				if (o2 == null) {
					return 1;
				}
				return (int) (o1.getCardStats().getShowAgainDelay_ms())
						- (int) (o2.getCardStats().getShowAgainDelay_ms());
			}
		});		
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
		for (AudioCard card : en2chrDeck.getCards()) {
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

	private void generateEn2ChrWavFiles() throws UnsupportedAudioFileException, IOException {
		voiceShuffleSeed=0;
		File wavTmpDir = new File(WAVS_DIR, "en2chr");
		FileUtils.deleteQuietly(wavTmpDir);
		File espeakNgBin = new File(SystemUtils.getUserHome(), "espeak-ng/bin/espeak-ng");
		ESpeakNg espeak = new ESpeakNg(espeakNgBin);
		Set<String> already = new HashSet<>();
		for (AudioCard card : en2chrDeck.getCards()) {
			AudioData data = card.getData();
			String answer = data.getAnswer();
			String challenge = data.getChallenge();
			challenge = AudioGenUtil.removeEnglishFixedGenderMarks(AudioGenUtil.randomizeEnglishSexes(challenge));
			String challengeFilename = AudioGenUtil.asEnglishFilename(challenge);
			File challengeWavFile = new File(wavTmpDir, "challenge-" + challengeFilename+".wav");
			String answerFilename = AudioGenUtil.asPhoneticFilename(answer);
			File answerWavFile = new File(wavTmpDir, "answer-" + answerFilename+".wav");
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
	
	private void generateChr2EnWavFiles() throws UnsupportedAudioFileException, IOException {
		voiceShuffleSeed=0;
		File wavTmpDir = new File(WAVS_DIR, "chr2en");
		FileUtils.deleteQuietly(wavTmpDir);
		File espeakNgBin = new File(SystemUtils.getUserHome(), "espeak-ng/bin/espeak-ng");
		ESpeakNg espeak = new ESpeakNg(espeakNgBin);
		Set<String> already = new HashSet<>();
		for (AudioCard card : chr2enDeck.getCards()) {
			AudioData data = card.getData();
			String answer = data.getAnswer();
			answer = AudioGenUtil.removeEnglishFixedGenderMarks(AudioGenUtil.alternizeEnglishSexes(answer));
			String challenge = data.getChallenge();
			String challengeFilename = AudioGenUtil.asPhoneticFilename(challenge);
			File challengeWavFile = new File(wavTmpDir, "challenge-" + challengeFilename + ".wav");
			String answerFilename = AudioGenUtil.asEnglishFilename(answer);
			File answerWavFile = new File(wavTmpDir, "answer-" + answerFilename + ".wav");
			data.setAnswerFile(answerWavFile);
			data.setChallengeFile(challengeWavFile);
			if (!already.contains(challenge)) {
				String voice = nextVoice();
				if (!voice.trim().isEmpty()) {
					voice = "chr+" + voice;
				} else {
					voice = "chr";
				}
				System.out.println(" - " + challengeWavFile.getName() + " [" + voice + "]");
				espeak.generateWav(voice, challengeWavFile, challenge);
				float durationInSeconds = getDuration(challengeWavFile);
				data.setChallengeDuration(durationInSeconds);
			}
			if (!already.contains(answer)) {
				String voice = nextVoice();
				if (!voice.trim().isEmpty()) {
					voice = "en-us+" + voice;
				} else {
					voice = "en-us";
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

	private void loadMainDecks() throws IOException {
		StringBuilder reviewSheetChr2En = new StringBuilder();
		StringBuilder reviewSheetEn2Chr = new StringBuilder();
		File jsonFile = new File(DECK_TSV);
		System.out.println(jsonFile.getAbsolutePath());
		Map<String, AudioCard> cardsForCherokeeAnswers = new HashMap<>();
		Map<String, AudioCard> cardsForEnglishAnswers = new HashMap<>();
		try (LineIterator li = FileUtils.lineIterator(jsonFile, StandardCharsets.UTF_8.name())) {
			li.next();
			int idEn2Chr = 0;
			int idChr2En = 0;
			while (li.hasNext()) {
				String line = li.next();
				String[] fields = line.split("\t");
				if (fields.length <= ENGLISH_TEXT_START) {
					System.out.println("; " + line);
					continue;
				}
				String cherokeeText = fields[CHEROKEE_TEXT].trim();
				if (cherokeeText.isEmpty()) {
					continue;
				}
				cherokeeText=StringUtils.capitalize(cherokeeText);
				if (!cherokeeText.matches(".*[.?!]")) {
					cherokeeText+=".";
				}
				for (int ix = ENGLISH_TEXT_START; ix < fields.length; ix++) {
					String englishText = fields[ix].trim();
					if (englishText.isEmpty()) {
						continue;
					}
					englishText=StringUtils.capitalize(englishText);
					if (!englishText.matches(".*[.?!]")) {
						englishText+=".";
					}
					/*
					 * Each deck gets its own set of cards.
					 */
					AudioCard toChrCard;
					AudioData toChrData;
					if (cardsForCherokeeAnswers.containsKey(englishText)) {
						toChrCard = cardsForCherokeeAnswers.get(englishText);
						toChrData = toChrCard.getData();
						toChrData.setAnswer(toChrData.getAnswer()+" "+cherokeeText);
						cardsForCherokeeAnswers.put(englishText, toChrCard);
					} else {
						toChrCard = new AudioCard();
						toChrData = new AudioData();
						toChrData.setAnswer(cherokeeText);
						toChrData.setAnswerDuration(0);
						toChrData.setChallenge(englishText);
						toChrData.setChallengeDuration(0);
						toChrData.setId(++idEn2Chr);
						toChrCard.setData(toChrData);
						cardsForCherokeeAnswers.put(englishText, toChrCard);
						en2chrDeck.add(toChrCard);
					}
					reviewSheetEn2Chr.append(toChrData.id() + "\t" + toChrData.getChallenge() + "\t" + toChrData.getAnswer() + "\n");
					
					AudioCard toEnCard;
					AudioData toEnData;
					if (cardsForEnglishAnswers.containsKey(cherokeeText)) {
						toEnCard = cardsForEnglishAnswers.get(cherokeeText);
						toEnData = toEnCard.getData();
						toEnData.setAnswer(toEnData.getAnswer()+" "+englishText);
					} else {
						toEnCard = new AudioCard();
						toEnData = new AudioData();
						toEnData.setAnswer(englishText);
						toEnData.setAnswerDuration(0);
						toEnData.setChallenge(cherokeeText);
						toEnData.setChallengeDuration(0);
						toEnData.setId(++idChr2En);
						toEnCard.setData(toEnData);
						cardsForEnglishAnswers.put(cherokeeText, toEnCard);
						chr2enDeck.add(toEnCard);
					}
					reviewSheetChr2En.append(toEnData.id() + "\t" + toEnData.getChallenge() + "\t" + toEnData.getAnswer() + "\n");
				}
			}
		}
		FileUtils.writeStringToFile(new File("review-sheet-chr-en.tsv"), reviewSheetChr2En.toString(), StandardCharsets.UTF_8);
		FileUtils.writeStringToFile(new File("review-sheet-en-chr.tsv"), reviewSheetEn2Chr.toString(), StandardCharsets.UTF_8);
	}

}
