package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.cherokeelessons.bp.audiogen.AudioData.AudioDataFile;
import com.cherokeelessons.deck.CardStats;
import com.cherokeelessons.deck.CardUtils;

public class Main {

	private static final ExcerciseSet SET = ExcerciseSet.WALC1;
	
	private static final boolean PRERECORDED_AUDIO = true;
	
	private static final float MAX_SESSION_LENGTH = 60f * 60f;

	private static final boolean GRIFFIN_LIM = false;

	private static final boolean USE_DEBUG_DECK = false;
	private static final int DEBUG_DECK_SIZE = 20;

	private static final int SESSIONS_TO_CREATE = 99;
	private static final boolean CREATE_ALL_SESSIONS = true;

	private static final int MAX_TRIES_PER_REVIEW_CARD = 4;
	private static final int TRIES_PER_REVIEW_CARD_DECREMENT = 0;

	private static final int MAX_TRIES_PER_NEW_CARD = 6;
	private static final int TRIES_PER_NEW_CARD_DECREMENT = 0;

	private static final int BASE_NEW_CARDS_PER_SESSION = 10;
	private static final int NEW_CARDS_INCREMENT = 5;

	private static final int REVIEW_CARDS_PER_SESSION = 21;

	public static final String UNDERDOT = "\u0323";

	private static final NumberFormat NF = NumberFormat.getInstance();
	private static final File WAVS_DIR = new File("tmp/wavs");
	private static final File EXCERCISES_DIR = new File("tmp/excercises");
	private final String deckSourceText;
	private static final boolean sortDeckBySize = false;
	private boolean autoSplitCherokee = true;

	private static final int IX_PRONOUN = 3;
	private static final int IX_VERB = 4;
	private static final int IX_GENDER = 5;
	private static final int IX_SYLLABARY = 6;
	private static final int IX_PRONOUNCE = 7;
	private static final int IX_ENGLISH = 8;

	public static void main(final String[] args) throws IOException, UnsupportedAudioFileException {
		new Main().execute();
	}

	private final AudioDeck en2chrDeck;
	private final AudioDeck chr2enDeck;

	private final AudioDeck discardsDeck;
	private final AudioDeck finishedDeck;
	private final AudioDeck activeDeck;

	private final Map<String, AtomicInteger> vstemCounts;
	private final Map<String, AtomicInteger> pboundCounts;

	private final Set<TtsVoice> voiceVariants;
	private final List<TtsVoice> voices = new ArrayList<>();

	private TtsVoice previousVoice = null;
	private int voiceShuffleSeed = 1234;
	private boolean maxCardsReached = false;

	private String chr2enPrefix = "chr2en";

	public Main() {
		switch (SET) {
		case CLL1:
			chr2enPrefix = "Cherokee-Language-Lessons-1-v3";
			deckSourceText = "cll1-v3.txt";
			autoSplitCherokee = false;
			break;
		case BOUND_PRONOUNS:
			chr2enPrefix = "Bound-Pronouns-App";
			deckSourceText = "bound-pronouns.txt";
			break;
		case BRAGGING_HUNTERS:
			chr2enPrefix = "Two-Hunters";
			deckSourceText = "two-men-hunting.txt";
			break;
		case CED:
			chr2enPrefix = "Cherokee-English-Dictionary";
			deckSourceText = "x1.tsv";
			break;
		case OSIYO_THEN_WHAT:
			chr2enPrefix = "Conversation-Starters-In-Cherokee";
			deckSourceText = "osiyo-tohiju-then-what-mco.txt";
			break;
		case WWACC:
			chr2enPrefix = "wwacc";
			deckSourceText = "wwacc.txt";
			break;
		case ANIMALS:
			chr2enPrefix = "Animals";
			deckSourceText = "animals-mco.txt";
			break;
		case WALC1:
			chr2enPrefix = "walc-1";
			deckSourceText = "walc-1.txt";
			break;
		default:
			chr2enPrefix = "chr2en";
			deckSourceText = "x1.tsv";
			break;
		}
		en2chrDeck = new AudioDeck();
		chr2enDeck = new AudioDeck();

		activeDeck = new AudioDeck();
		discardsDeck = new AudioDeck();
		finishedDeck = new AudioDeck();

		// 
		
		voiceVariants = new HashSet<>();
//		voiceVariants.add(new TtsVoice("female", SexualGender.FEMALE));
		voiceVariants.add(new TtsVoice("cno-f-chr_1", SexualGender.FEMALE));
		voiceVariants.add(new TtsVoice("cno-f-chr_2", SexualGender.FEMALE));
		voiceVariants.add(new TtsVoice("cno-f-chr_3", SexualGender.FEMALE));
		voiceVariants.add(new TtsVoice("cno-f-chr_5", SexualGender.FEMALE));

//		List<String> en_female = Arrays.asList("239-en", "264-en");//, "250-en", "261-en", "283-en");
//		for (String v : en_female) {
//			voiceVariants.add(new TtsVoice(v, SexualGender.FEMALE));
//		}
		
//		voiceVariants.add(new TtsVoice("male", SexualGender.MALE));
		voiceVariants.add(new TtsVoice("cno-m-chr_1", SexualGender.MALE));
		voiceVariants.add(new TtsVoice("cno-m-chr_2", SexualGender.MALE));

//		List<String> en_male = Arrays.asList("259-en", "247-en");//, "263-en", "286-en", "274-en");
//		for (String v : en_male) {
//			voiceVariants.add(new TtsVoice(v, SexualGender.MALE));
//		}

		vstemCounts = new HashMap<String, AtomicInteger>();
		pboundCounts = new HashMap<String, AtomicInteger>();
	}

	private AudioData audioSilence;
	private int reviewCount = 0;

	private void saveStemCounts(AudioDeck deck) {
		vstemCounts.clear();
		pboundCounts.clear();
		for (AudioCard card : deck.getCards()) {
			AudioData data = card.getData();
			String bp = data.getBoundPronoun();
			if (bp.trim().isEmpty()) {
				continue;
			}
			if (!pboundCounts.containsKey(bp)) {
				pboundCounts.put(bp, new AtomicInteger());
			}
			pboundCounts.get(bp).incrementAndGet();
			String vs = data.getVerbStem();
			if (!vstemCounts.containsKey(vs)) {
				vstemCounts.put(vs, new AtomicInteger());
			}
			vstemCounts.get(vs).incrementAndGet();
		}
//		for (Entry<String, AtomicInteger> e: pboundCounts.entrySet()) {
//			if (e.getValue().get()>2) {
//				System.out.println("   p: "+e.getKey()+" = "+e.getValue().get());
//			}
//		}
	}

	private boolean skipNew(AudioCard card) {
		AudioData data = card.getData();
		if (data.id().contains("*")) {
			return true;
		}
		String bp = data.getBoundPronoun();
		String vs = data.getVerbStem();
		if (bp.equals("*") && vs.equals("*")) {
			return true;
		}
		if (!pboundCounts.containsKey(bp)) {
			return false;
		}
		if (!vstemCounts.containsKey(vs)) {
			return false;
		}
		return pboundCounts.get(bp).get() > 2 && vstemCounts.get(vs).get() > 4;
	}

	private void buildChr2EnExerciseMp3Files() throws IOException {
		System.out.println("=== buildChr2EnExerciseMp3Files");

		/*
		 * using in functions
		 */
		audioSilence = generateSilenceWav();

		final File tmpDir = new File(EXCERCISES_DIR, "chr2en");
		FileUtils.deleteQuietly(tmpDir);
		tmpDir.mkdirs();

		/*
		 * lead in audio
		 */
		AudioDataFile lc1 = EnglishAudio.createEnglishAudioFor(EnglishAudio.LANGUAGE_CULTURE_1,
				new File(EXCERCISES_DIR, "language-culture-1.wav")).getAnswerFile();
		/*
		 * lead in audio
		 */
		AudioDataFile intro2 = EnglishAudio
				.createEnglishAudioFor(EnglishAudio.KEEP_GOING, new File(EXCERCISES_DIR, "intro-2.wav"))
				.getAnswerFile();
		/*
		 * lead in audio
		 */
		AudioDataFile intro3 = EnglishAudio.createEnglishAudioFor(EnglishAudio.LEARN_SOUNDS_FIRST, new File(EXCERCISES_DIR, "intro-3.wav")).getAnswerFile();
		/*
		 * lead in audio
		 */
		AudioDataFile intro1 = EnglishAudio
				.createEnglishAudioFor(EnglishAudio.INTRO_2, new File(EXCERCISES_DIR, "intro-1-these.wav"))
				.getAnswerFile();
		/*
		 * lead in audio
		 */
		AudioDataFile begin1 = EnglishAudio
				.createEnglishAudioFor(EnglishAudio.BEGIN, new File(EXCERCISES_DIR, "begin-1.wav")).getAnswerFile();
		/*
		 * in exercise audio
		 */
		final AudioDataFile audioNewPhrase = generateNewPhrase().getAnswerFile();
		/*
		 * in exercise audio
		 */
		final AudioDataFile audioNewPhraseShort = generateNewPhraseShort().getAnswerFile();
		/*
		 * in exercise audio
		 */
		final AudioDataFile audioTranslatePhrase = generateTranslatePhrase().getAnswerFile();
		/*
		 * in exercise audio
		 */
		final AudioDataFile audioTranslatePhraseShort = generateTranslatePhraseShort().getAnswerFile();
		/*
		 * in exercise audio
		 */
		final AudioDataFile audioListenAgain = listenAgain().getAnswerFile();
		/*
		 * in exercise audio
		 */
		final AudioDataFile audioListenAgainShort = listenAgainShort().getAnswerFile();
		/*
		 * in exercise audio
		 */
		final AudioDataFile audioItsTranslationIs = itsTranslationIs().getAnswerFile();
		/*
		 * in exercise audio
		 */
		final AudioDataFile audioItsTranslationIsShort = inEnglish().getAnswerFile();

		/*
		 * trailing audio
		 */
		final AudioDataFile audioExerciseConclusion = thisConcludesThisExercise().getAnswerFile();
		/*
		 * trailing audio
		 */
		AudioDataFile copy1 = EnglishAudio
				.createEnglishAudioFor(EnglishAudio.COPY_1, new File(EXCERCISES_DIR, "copyright-1.wav"))
				.getAnswerFile();
		/*
		 * trailing audio
		 */
		AudioDataFile copy2 = EnglishAudio
				.createEnglishAudioFor(EnglishAudio.COPY_2, new File(EXCERCISES_DIR, "copyright-2.wav"))
				.getAnswerFile();
		List<String> sessionStats = new ArrayList<>();
		
		boolean keep_going = CREATE_ALL_SESSIONS;
		int sessions = SESSIONS_TO_CREATE;
		for (int _exerciseSet = 0; _exerciseSet < sessions || keep_going; _exerciseSet++) {
			String exerciseSet = String.format("%04d", _exerciseSet+1);
			System.out.println("=== EXERCISE SET: " + exerciseSet);
			final List<File> audioEntries = new ArrayList<>();
			String prevCardId = "";

			float tick = 0f;

			/*
			 * Account for trailing audio lengths
			 */
			// added at END
			tick += copy1.duration;
			tick += addSilence(2f, audioEntries);

			// added at END
			tick += copy2.duration;
			tick += addSilence(2f, audioEntries);

			tick += 3f;
			tick += audioExerciseConclusion.duration;
			tick += 3f;

			tick += copy1.duration;
			tick += 2f;

			tick += copy2.duration;
			tick += 3f;

			/*
			 * leadin silence
			 */
			tick += addSilence(1f, audioEntries);

			File wavFile;
			AudioDataFile tmpData;

//			wavFile = new File(EXCERCISES_DIR, "two-hungers-session-" + exerciseSet + ".wav");
//			tmpData = EnglishAudio.createEnglishAudioFor("The Two Hunters in Cherokee.", wavFile);
//			audioEntries.add(tmpData.getAnswerFile());
//			tick += tmpData.getAnswerDuration();
//			tick += addSilence(1f, audioEntries);
			
			/*
			 * Exercise set title
			 */
			switch (SET) {
			case CLL1:
//				wavFile = new File(EXCERCISES_DIR, "cll1-v3-session-" + exerciseSet + ".wav");
//				tmpData = EnglishAudio.createEnglishAudioFor("Cherokee Language Lessons 1.", wavFile);
//				audioEntries.add(tmpData.file);
//				tick += tmpData.getAnswerDuration();
//				tick += addSilence(1f, audioEntries);
				break;
			case BOUND_PRONOUNS:
				wavFile = new File(EXCERCISES_DIR, "bound-pronouns-session-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor("Bound Pronouns Training.", wavFile)
						.getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			case BRAGGING_HUNTERS:
				break;
			case CED:
				wavFile = new File(EXCERCISES_DIR, "ced-session-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor("C.E.D. Vocabulary Cram.", wavFile).getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			case OSIYO_THEN_WHAT:
				wavFile = new File(EXCERCISES_DIR, "conversation-starters-session-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor("Conversation Starters in Cherokee.", wavFile)
						.getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			case WWACC:
				wavFile = new File(EXCERCISES_DIR, "wwacc-session-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor("Advanced Conversational Cherokee.", wavFile)
						.getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			case WALC1:
				wavFile = new File(EXCERCISES_DIR, "walc-1-session-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor("We are learning Cherokee - Book 1.", wavFile)
						.getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			case ANIMALS:
				wavFile = new File(EXCERCISES_DIR, "animals-session-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor("Cherokee animal names.", wavFile)
						.getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			default:
				break;

			}

			/*
			 * Source notice - only for first session
			 */
			if (_exerciseSet==0) {
			switch (SET) {
			case CLL1:
//				wavFile = new File(EXCERCISES_DIR, "source-is-cll1-v3-" + exerciseSet + ".wav");
//				tmpData = EnglishAudio
//						.createEnglishAudioFor("Cherokee Language Lessons 1, 3rd edition. Audio excercises.", wavFile)
//						.getAnswerFile();
//				audioEntries.add(tmpData.file);
//				tick += tmpData.duration;
//				tick += addSilence(1f, audioEntries);

				wavFile = new File(EXCERCISES_DIR, "cll1-v3-" + exerciseSet + ".wav");
					tmpData = EnglishAudio.createEnglishAudioFor(
							"By the time you have completed these excercises you should be able to understand the core vocabulary in Cherokee Language Lessons 1.",
							wavFile).getAnswerFile();
					audioEntries.add(tmpData.file);
					tick += tmpData.duration;
					tick += addSilence(1f, audioEntries);
				break;
			case BOUND_PRONOUNS:
				wavFile = new File(EXCERCISES_DIR, "source-is-bound-pronouns-app-" + exerciseSet + ".wav");
				tmpData = EnglishAudio
						.createEnglishAudioFor(
								"These sessions closely follow the vocabulary from the Bound Pronouns app.", wavFile)
						.getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);

				wavFile = new File(EXCERCISES_DIR, "bound-pronouns-app-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor(
						"These exercises are designed to assist" //
						+ " with learning the different singular" //
						+ " and plural bound pronoun combinations." //
						, wavFile).getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			case BRAGGING_HUNTERS:
				wavFile = new File(EXCERCISES_DIR, "source-is-ced-two-hunters-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor(
						"These sessions closely follow the vocabulary from the story entitled, 'Two Hunters', as recorded in the Cherokee English Dictionary, 1st edition.",
						wavFile).getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);

				wavFile = new File(EXCERCISES_DIR, "will-understand-two-hunters-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor(
						"By the time you have completed these exercises you should be able to understand the full spoken story without any difficulty.",
						wavFile).getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			case CED:
				/*
				 * Exercise set title before describing source
				 */
//				wavFile = new File(EXCERCISES_DIR, "ced-session-" + exerciseSet + ".wav");
//				tmpData = EnglishAudio.createEnglishAudioFor("C.E.D. Vocabulary Cram.", wavFile).getAnswerFile();
//				audioEntries.add(tmpData.file);
//				tick += tmpData.duration;
//				tick += addSilence(1f, audioEntries);

				wavFile = new File(EXCERCISES_DIR, "source-is-ced-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor(
						"These sessions use vocabulary taken from the Cherokee English Dictionary, 1st Edition.. The pronunciations are based on the pronunciation markings as found in the dictionary.",
						wavFile).getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
			case OSIYO_THEN_WHAT:
				/*
				 * Exercise set title before describing source
				 */
//				wavFile = new File(EXCERCISES_DIR, "conversation-starters-session-" + exerciseSet + ".wav");
//				tmpData = EnglishAudio.createEnglishAudioFor("Conversation Starters in Cherokee.", wavFile)
//						.getAnswerFile();
//				audioEntries.add(tmpData.file);
//				tick += tmpData.duration;
//				tick += addSilence(1f, audioEntries);

				wavFile = new File(EXCERCISES_DIR,
						"source-is-conversation-starters-book-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor(
						"These sessions closely follow the book entitled, 'Conversation Starters in Cherokee', by Prentice Robinson. The pronunciations are based on the pronunciation markings as found in the official Cherokee English Dictionary - 1st Edition.",
						wavFile).getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
				
			case WWACC:
				/*
				 * Exercise set title before describing source
				 */
//				wavFile = new File(EXCERCISES_DIR, "wwacc-session-" + exerciseSet + ".wav");
//				tmpData = EnglishAudio.createEnglishAudioFor("Advanced Conversational Cherokee.", wavFile)
//						.getAnswerFile();
//				audioEntries.add(tmpData.file);
//				tick += tmpData.duration;
//				tick += addSilence(1f, audioEntries);

				wavFile = new File(EXCERCISES_DIR,
						"source-is-wwacc-booklet-" + exerciseSet + ".wav");
				tmpData = EnglishAudio.createEnglishAudioFor(
						"These sessions closely follow the booklet entitled, 'Advanced Conversational Cherokee', by Willard Walker.",
						wavFile).getAnswerFile();
				audioEntries.add(tmpData.file);
				tick += tmpData.duration;
				tick += addSilence(1f, audioEntries);
				break;
				
			case WALC1:
//				wavFile = new File(EXCERCISES_DIR, "walc1-session-" + exerciseSet + ".wav");
//				tmpData = EnglishAudio.createEnglishAudioFor("We are learning Cherokee. Book 1.", wavFile)
//						.getAnswerFile();
//				audioEntries.add(tmpData.file);
//				tick += tmpData.duration;
//				tick += addSilence(1f, audioEntries);

					wavFile = new File(EXCERCISES_DIR,
							"source-is-walc-1-" + exerciseSet + ".wav");
					tmpData = EnglishAudio.createEnglishAudioFor(
							"These sessions closely follow the lesson material 'We are learning Cherokee - Book 1'.",
							wavFile).getAnswerFile();
					audioEntries.add(tmpData.file);
					tick += tmpData.duration;
					tick += addSilence(1f, audioEntries);
				break;
				
			case ANIMALS:
				/*
				 * Exercise set title before describing source
				 */
//				wavFile = new File(EXCERCISES_DIR, "animals-session-" + exerciseSet + ".wav");
//				tmpData = EnglishAudio.createEnglishAudioFor("Cherokee animal names.", wavFile)
//						.getAnswerFile();
//				audioEntries.add(tmpData.file);
//				tick += tmpData.duration;
//				tick += addSilence(1f, audioEntries);

//				wavFile = new File(EXCERCISES_DIR,
//						"source-is-conversation-starters-book-" + exerciseSet + ".wav");
//				tmpData = EnglishAudio.createEnglishAudioFor(
//						"These sessions closely follow the book entitled, 'Conversation Starters in Cherokee', by Prentice Robinson. The pronunciations are based on the pronunciation markings as found in the official Cherokee English Dictionary - 1st Edition.",
//						wavFile).getAnswerFile();
//				audioEntries.add(tmpData.file);
//				tick += tmpData.duration;
//				tick += addSilence(1f, audioEntries);
				break;
			default:
				break;
			}
			}
			
			/*
			 * Start with pre-lesson verbiage.
			 */

			if (_exerciseSet == 0) {
				audioEntries.add(lc1.file);
				tick += lc1.duration;
				tick += addSilence(2f, audioEntries);

				audioEntries.add(intro2.file);
				tick += intro2.duration;
				tick += addSilence(2f, audioEntries);
				
				audioEntries.add(intro3.file);
				tick += intro3.duration;
				tick += addSilence(3f, audioEntries);

				audioEntries.add(intro1.file);
				tick += intro1.duration;
				tick += addSilence(2f, audioEntries);
			}

			/*
			 * Let us begin
			 */
			if (_exerciseSet == 0) {
				audioEntries.add(begin1.file);
				tick += begin1.duration;
				tick += addSilence(1f, audioEntries);
			}
			
			/*
			 * Indicate which session
			 */
			wavFile = new File(EXCERCISES_DIR, "session-" + exerciseSet + ".wav");
			tmpData = EnglishAudio.createEnglishAudioFor("Session " + exerciseSet.replaceAll("^0+", ""), wavFile).getAnswerFile();
			audioEntries.add(tmpData.file);
			tick += tmpData.duration;
			tick += addSilence(1f, audioEntries);
			

			/*
			 * A single audio lesson file.
			 */
			int newCardCount = 0;
			int introducedCardCount = 0;
			int hiddenCardCount = 0;
			int challengeCardCount = 0;
			List<String> newVocabulary = new ArrayList<>();
			maxCardsReached = false;
			reviewCount = 0;
			updateTime(finishedDeck, MAX_SESSION_LENGTH * 24f /* one day seconds */);
			sortDeckByShowAgainDelay(finishedDeck);
			saveStemCounts(finishedDeck);
			if (finishedDeck.hasCards()) {
				System.out.println("--- Have " + NF.format(finishedDeck.getCards().size())
						+ " previously finished cards for possible use.");
			}

			final int maxNewCardsThisSession = BASE_NEW_CARDS_PER_SESSION + _exerciseSet * NEW_CARDS_INCREMENT;

			List<String> callSheet = new ArrayList<>();
			while (tick < MAX_SESSION_LENGTH) {
				float deltaTick = 0f;

				AudioCard card = getNextCard(_exerciseSet, prevCardId);
				if (card == null) {
					break;
				}

				String counter = String.format("%04d", callSheet.size()+1);
				String answer = card.getData().getAnswer();
				answer = AudioGenUtil
				.removeEnglishFixedGenderMarks(AudioGenUtil.alternizeEnglishSexes(answer));
				callSheet.add(counter+"|"+card.getData().getChallenge()+"|"+answer+"|");

				if (keep_going) {
					keep_going = chr2enDeck.hasCards();
					/**
					 * Create a final set of review only sessions.
					 */
					sessions = _exerciseSet + 2;
				}

				final String cardId = card.id();
				CardStats cardStats = card.getCardStats();
				final boolean newCard = cardStats.isNewCard();
				final boolean introduceCard = cardStats.isNewCard() && !skipNew(card);

				float extraDelay = cardStats.getShowAgainDelay_ms() / 1000l;
				final AudioData data = card.getData();

				if (cardId.equals(prevCardId)) {
					card.getCardStats().setShowAgainDelay_ms(32000l);
					continue;
				}
				prevCardId = cardId;

				if (newCard) {
					int lastIdx = callSheet.size()-1;
					if (introduceCard) {
						System.out.println("   Introduced card: " + data.getChallenge() + " ["
								+ cardStats.getTriesRemaining() + "]");
						newVocabulary.add(counter + "|" + card.getData().getChallenge() + "|" + answer + "|");
						callSheet.set(lastIdx, callSheet.get(lastIdx)+"[n]");
					} else {
						cardStats.setNewCard(false);
						card.resetTriesRemaining(Math.max(MAX_TRIES_PER_REVIEW_CARD / 2,
								MAX_TRIES_PER_REVIEW_CARD - TRIES_PER_REVIEW_CARD_DECREMENT * _exerciseSet));
						System.out.println("   Hidden new card: " + data.getChallenge() + ": "
								+ answer + " [" + cardStats.getTriesRemaining() + "]");
						callSheet.set(lastIdx, callSheet.get(lastIdx)+"[*]");
					}
				}

				if (newCard) {
					if (introduceCard) {
						introducedCardCount++;
					} else {
						hiddenCardCount++;
					}
					if (++newCardCount >= maxNewCardsThisSession) {
						maxCardsReached = true;
					}
					deltaTick += addSilence(2, audioEntries);
					card.getCardStats().setNewCard(false);
					if (newCardCount < 6 && _exerciseSet == 0) {
						if (newCardCount == 1) {
							AudioDataFile firstNewPhrase = generateFirstNewPhrase().getAnswerFile();
							audioEntries.add(firstNewPhrase.file);
							deltaTick += firstNewPhrase.duration;
						} else {
							audioEntries.add(audioNewPhrase.file);
							deltaTick += audioNewPhrase.duration;
						}
					} else {
						audioEntries.add(audioNewPhraseShort.file);
						deltaTick += audioNewPhraseShort.duration;
					}
					deltaTick += addSilence(1, audioEntries);
				} else {
					challengeCardCount++;

					// extra leadin silence based on when card was supposed to show again
					if (extraDelay > 0) {
						deltaTick += addSilence(Math.min(7f, extraDelay), audioEntries);
					}

					if (challengeCardCount < 16 && _exerciseSet == 0) {
						audioEntries.add(audioTranslatePhrase.file);
						deltaTick += audioTranslatePhrase.duration;
					} else {
						audioEntries.add(audioTranslatePhraseShort.file);
						deltaTick += audioTranslatePhraseShort.duration;
					}
					deltaTick += addSilence(1, audioEntries);
				}
				/*
				 * First challenge.
				 */
				AudioDataFile dataChallengeFile = data.getChallengeFile();
				audioEntries.add(dataChallengeFile.file);
				deltaTick += dataChallengeFile.duration;

				/*
				 * Repeat challenge with a different voice if new card
				 */
				if (introduceCard) {
					dataChallengeFile = data.getChallengeFile();
					deltaTick += addSilence(2, audioEntries);
					if (newCardCount < 8 && _exerciseSet == 0) {
						audioEntries.add(audioListenAgain.file);
						deltaTick += audioListenAgain.duration;
					} else {
						audioEntries.add(audioListenAgainShort.file);
						deltaTick += audioListenAgainShort.duration;
					}
					deltaTick += addSilence(2, audioEntries);
					audioEntries.add(dataChallengeFile.file);
					deltaTick += dataChallengeFile.duration;
					deltaTick += addSilence(2, audioEntries);
					if (newCardCount < 10 && _exerciseSet == 0) {
						audioEntries.add(audioItsTranslationIs.file);
						deltaTick += audioItsTranslationIs.duration;
					} else {
						audioEntries.add(audioItsTranslationIsShort.file);
						deltaTick += audioItsTranslationIsShort.duration;
					}
					deltaTick += addSilence(2, audioEntries);
				} else {
					float gapDuration = dataChallengeFile.duration * 1.1f + 2f;
					deltaTick += addSilence(gapDuration, audioEntries);
				}

				/*
				 * The answer.
				 */
				AudioDataFile dataAnswerFile = data.getAnswerFile();
				if (!dataAnswerFile.file.exists()) {
					System.err.println(data.getAnswerFiles());
					throw new FileNotFoundException(dataAnswerFile.file.getAbsolutePath());
				}
				audioEntries.add(dataAnswerFile.file);
				deltaTick += dataAnswerFile.duration;

				// trailing silence
				if (_exerciseSet == 0) {
					deltaTick += addSilence(3f, audioEntries);
				} else if (_exerciseSet < 5) {
					deltaTick += addSilence(2f, audioEntries);
				} else {
					deltaTick += addSilence(1f, audioEntries);
				}

				updateTime(activeDeck, deltaTick);
				updateTime(discardsDeck, deltaTick);
				updateTime(finishedDeck, deltaTick);

				cardStats.pimsleurSlotInc();
				final long nextInterval = CardUtils.getNextInterval(cardStats.getPimsleurSlot())
						+ (long) (deltaTick * 1000l);
				cardStats.setShowAgainDelay_ms(nextInterval);
				tick += deltaTick;
			}
			
			FileUtils.writeLines(new File("tmp/" + chr2enPrefix + "-call_sheet-" + exerciseSet + ".txt"), StandardCharsets.UTF_8.name(),
					callSheet);

			ListIterator<String> li = callSheet.listIterator();
			while (li.hasNext()) {
				String next = li.next();
				int idx = next.indexOf("|");
				idx = next.indexOf("|", idx+1);
				li.set(next.substring(0, idx));
			}
			FileUtils.writeLines(new File("tmp/" + chr2enPrefix + "-challenge_sheet-" + exerciseSet + ".txt"), StandardCharsets.UTF_8.name(),
					callSheet);

			bumpCompletedCards();
			/*
			 * Move all into discards - session time is up
			 */
			int secondsOffset = 0;
			for (AudioCard card : new ArrayList<>(activeDeck.getCards())) {
				discardsDeck.add(card);
			}
			if (activeDeck.hasCards()) {
				throw new IllegalStateException("Active Deck has cards after session reset!");
			}
			for (AudioCard card : new ArrayList<>(discardsDeck.getCards())) {
				CardStats cardStats = card.getCardStats();
				if (cardStats.getShown() >= cardStats.getTriesRemaining()) {
					/* Early bump, used at least 50% of total max uses. */
					cardStats.setTriesRemaining(0);
					bumpCompletedCards();
					continue;
				}
				cardStats.setShowAgainDelay_ms(secondsOffset += 1000);
				finishedDeck.add(card);
			}
			if (discardsDeck.hasCards()) {
				throw new IllegalStateException("Discard Deck has cards after session reset!");
			}

			System.out.println("TOTAL INTRODUCED CARDS IN SET: " + introducedCardCount);
			System.out.println("TOTAL HIDDEN NEW CARDS IN SET: " + hiddenCardCount);
			System.out.println(
					"TOTAL NEW CARDS IN SET: " + newCardCount + " out of a possible allowed " + maxNewCardsThisSession);

			FileUtils.writeLines(new File("tmp/" + chr2enPrefix + "-new-vocabulary-" + exerciseSet + ".txt"), StandardCharsets.UTF_8.name(),
					newVocabulary);

			/*
			 * Per session trailing audio
			 */
			addSilence(3f, audioEntries);
			audioEntries.add(audioExerciseConclusion.file);
			addSilence(3f, audioEntries);

			audioEntries.add(copy1.file);
			addSilence(2f, audioEntries);

			audioEntries.add(copy2.file);
			addSilence(3f, audioEntries);

			final AudioDataFile produced = EnglishAudio
					.createEnglishAudioFor(
							"This audio file was produced on "
									+ LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
									+ " by Michael Conrad.",
							new File(EXCERCISES_DIR,
									"produced-" + exerciseSet + "-" + LocalDate.now().toString() + ".wav"))
					.getAnswerFile();
			audioEntries.add(produced.file);
			addSilence(3f, audioEntries);

			String outputBasename = chr2enPrefix + "_" + LocalDate.now().toString() + "_"
					+ exerciseSet;

			final File wavOutputFile = new File(tmpDir, outputBasename + ".wav");
			for (final List<File> audioEntriesSublist : ListUtils.partition(audioEntries, 500)) {
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
					if (!audioEntry.exists()) {
						throw new FileNotFoundException(audioEntry.getAbsolutePath());
					}
				}
				cmd.add(wavOutputFile.getAbsolutePath());
				executeCmd(cmd);
				FileUtils.deleteQuietly(tmp1);
			}
			System.out.println("Total ticks: " + NF.format(tick) + " secs [" + NF.format(tick / 60f) + " mins]");
			final File mp3OutputFile = new File(tmpDir, outputBasename + ".mp3");
			sessionStats.add(mp3OutputFile.getName()+"|"+NF.format(tick/60f)+" mins");
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
			wavOutputFile.delete();
		}
		FileUtils.writeLines(new File("tmp/" + chr2enPrefix + "-stats.txt"), StandardCharsets.UTF_8.name(),
				sessionStats);
	}

	private float addSilence(float seconds, List<File> audioEntries) {
		float deltaTick = 0f;
		for (int trailingSilence = 0; trailingSilence < seconds; trailingSilence++) {
			audioEntries.add(audioSilence.getAnswerFile().file);
			deltaTick += 1f;
		}
		return deltaTick;
	}

	private AudioCard getNextCard(int exerciseSet, String prevCardId) {
		bumpCompletedCards();
		if (activeDeck.hasCards()) {
			AudioCard card = (AudioCard) activeDeck.topCard();
			discardsDeck.add(card);
			if (!card.id().equals(prevCardId)) {
				CardStats cardStats = card.getCardStats();
				cardStats.triesRemainingDec();
				cardStats.setShown(cardStats.getShown() + 1);
				return card;
			}
			if (activeDeck.hasCards()) {
				return getNextCard(exerciseSet, prevCardId);
			}
		}
		// see if we can pull in a review card
		if (finishedDeck.getNextShowTime() <= 0 && finishedDeck.hasCards()
				&& reviewCount < (REVIEW_CARDS_PER_SESSION + exerciseSet * 2)) {
			reviewCount++;
			AudioCard reviewCard = (AudioCard) finishedDeck.topCard();
			CardStats cardStats = reviewCard.getCardStats();
			cardStats.setNewCard(false);
			reviewCard.resetStats();
			reviewCard.resetTriesRemaining(Math.max(MAX_TRIES_PER_REVIEW_CARD / 2,
					MAX_TRIES_PER_REVIEW_CARD - TRIES_PER_REVIEW_CARD_DECREMENT * exerciseSet));
			discardsDeck.add(reviewCard);
			System.out.println("   Review card: " + reviewCard.getData().getChallenge() + " ["
					+ reviewCard.getCardStats().getTriesRemaining() + "]");
			return reviewCard;
		}
		long extraDelay = discardsDeck.getNextShowTime();
		updateTime(discardsDeck, extraDelay);
		scanForCardsToShowAgain();
		if (!maxCardsReached && chr2enDeck.hasCards()) {
			AudioCard newCard = (AudioCard) chr2enDeck.topCard();
			newCard.getCardStats().setNewCard(true);
			newCard.resetTriesRemaining(Math.max(MAX_TRIES_PER_NEW_CARD / 2,
					MAX_TRIES_PER_NEW_CARD - TRIES_PER_NEW_CARD_DECREMENT * exerciseSet));
			discardsDeck.add(newCard);
			return newCard;
		}
		if (!activeDeck.hasCards()) {
			return null;
		}
		sortDeckByShowAgainDelay(activeDeck);
		AudioCard card = (AudioCard) activeDeck.topCard();
		discardsDeck.add(card);
		card.getCardStats().setShowAgainDelay_ms(extraDelay);
		CardStats cardStats = card.getCardStats();
		cardStats.triesRemainingDec();
		cardStats.setShown(cardStats.getShown() + 1);
		return card;
	}

	private void updateTime(AudioDeck deck, float delta) {
		updateTime(deck, (long) (1000l * delta));
	}

	private void updateTime(AudioDeck deck, long delta) {
		for (AudioCard card : deck.getCards()) {
			long showAgainDelay_ms = card.getCardStats().getShowAgainDelay_ms();
			showAgainDelay_ms -= delta;
			card.getCardStats().setShowAgainDelay_ms(Math.max(showAgainDelay_ms, 0));
		}
	}

	private void scanForCardsToShowAgain() {
		for (final AudioCard tmp : new ArrayList<>(discardsDeck.getCards())) {
			final CardStats discardStats = tmp.getCardStats();
			if (discardStats.getShowAgainDelay_ms() > 0) {
				continue;
			}
			activeDeck.add(tmp);
		}
	}

	private void bumpCompletedCards() {
		for (final AudioCard card : new ArrayList<>(discardsDeck.getCards())) {
			final CardStats cardStats = card.getCardStats();
			if (cardStats.getTriesRemaining() < 1) {
				int leitnerBox = cardStats.getLeitnerBox();
				long nextSessionInterval_ms = CardUtils.getNextSessionInterval_ms(leitnerBox);
				cardStats.setShowAgainDelay_ms(nextSessionInterval_ms);
				finishedDeck.add(card);
//				System.out.println("   Bumping " + card.getData().getChallenge() + " to box " + (leitnerBox+1));
				cardStats.leitnerBoxInc();
			}
		}
	}

	@SuppressWarnings("unused")
	private void buildEn2ChrExerciseMp3Files() {
		int exerciseSet = 0;
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
			topCard.resetTriesRemaining(Math.max(MAX_TRIES_PER_NEW_CARD / 2,
					MAX_TRIES_PER_NEW_CARD - TRIES_PER_NEW_CARD_DECREMENT * exerciseSet));
			topCard.getCardStats().setShowAgainDelay_ms(ix * 5000l);
			activeDeck.add(topCard);
		}

		while (tick < MAX_SESSION_LENGTH) {
			float deltaTick = 0f;
			if (!activeDeck.hasCards() && !en2chrDeck.hasCards()) {
				break;
			}
			if (!activeDeck.hasCards() || activeDeck.getNextShowTime() > 5000l) {
				final AudioCard topCard = (AudioCard) en2chrDeck.topCard();
				topCard.resetStats();
				topCard.resetTriesRemaining(Math.max(MAX_TRIES_PER_NEW_CARD / 2,
						MAX_TRIES_PER_NEW_CARD - TRIES_PER_NEW_CARD_DECREMENT * exerciseSet));
				activeDeck.add(topCard);
			}
			sortDeckByShowAgainDelay(activeDeck);
			AudioCard card = (AudioCard) activeDeck.topCard();
			String cardId = card.id();

			if (cardId.equals(prevCardId)) {
				card = (AudioCard) en2chrDeck.topCard();
				card.resetStats();
				card.resetTriesRemaining(Math.max(MAX_TRIES_PER_NEW_CARD / 2,
						MAX_TRIES_PER_NEW_CARD - TRIES_PER_NEW_CARD_DECREMENT * exerciseSet));
				cardId = card.id();
				activeDeck.add(card);
			}
			prevCardId = cardId;

			final AudioData data = card.getData();
			AudioDataFile dataChallengeFile = data.getChallengeFile();
			audioEntries.add(dataChallengeFile.file);
			deltaTick += dataChallengeFile.duration;
			final float answerDuration = dataChallengeFile.duration;
			float gapDuration = answerDuration * 1.5f + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav.getAnswerFile().file);
				deltaTick += 1f;
			}

			/*
			 * First answer.
			 */
			audioEntries.add(dataChallengeFile.file);
			deltaTick += answerDuration;

			gapDuration = answerDuration + 2f;
			while (gapDuration-- > 0f) {
				audioEntries.add(silenceWav.getAnswerFile().file);
				deltaTick += 1f;
			}
			/*
			 * Confirm answer.
			 */
			audioEntries.add(dataChallengeFile.file);
			deltaTick += answerDuration;

			for (int trailingSilence = 0; trailingSilence < Math.max(3, answerDuration + 2f); trailingSilence++) {
				audioEntries.add(silenceWav.getAnswerFile().file);
				deltaTick += 1f;
			}

			System.out.println(data.id() + ") " + data.getAnswer() + " " + NF.format(tick));
			activeDeck.updateTimeBy((long) (deltaTick * 1000f));

			final CardStats cardStats = card.getCardStats();

			cardStats.pimsleurSlotInc();
			final long nextInterval = CardUtils.getNextInterval(cardStats.getPimsleurSlot());
			cardStats.setShowAgainDelay_ms(nextInterval);
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
		if (sortDeckBySize) {
			sortMainDecks();
		}
		generateChr2EnWavFiles();
		buildChr2EnExerciseMp3Files();

//		generateEn2ChrWavFiles();
//		buildEn2ChrExerciseMp3Files();
//		generateDurationsReport();
	}

	private void sortMainDecks() {
		Collections.sort(chr2enDeck.getCards(), (a, b) -> {
			AudioData d1 = a.getData();
			AudioData d2 = b.getData();
			String c1 = d1.getChallenge();
			String c2 = d2.getChallenge();
			if (c1.length() != c2.length()) {
				return Integer.compare(c1.length(), c2.length());
			}
			return c1.compareToIgnoreCase(c2);
		});
		Collections.sort(en2chrDeck.getCards(), (a, b) -> {
			AudioData d1 = a.getData();
			AudioData d2 = b.getData();
			String c1 = d1.getAnswer();
			String c2 = d2.getAnswer();
			if (c1.length() != c2.length()) {
				return Integer.compare(c1.length(), c2.length());
			}
			return c1.compareToIgnoreCase(c2);
		});
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
		TTS tts = tts();
		tts.setGriffinLim(GRIFFIN_LIM);
		voiceShuffleSeed = 0;
		final File wavTmpDir = new File(WAVS_DIR, "chr2en");
		FileUtils.deleteQuietly(wavTmpDir);
		final Set<String> already = new HashSet<>();
		for (final AudioCard card : new ArrayList<>(chr2enDeck.getCards())) {
			final AudioData data = card.getData();
			final String answer = AudioGenUtil
					.removeEnglishFixedGenderMarks(AudioGenUtil.alternizeEnglishSexes(data.getAnswer()));
			final String challenge = data.getChallenge();
			System.out.println(card.id() + ") " + challenge + ", " + answer);
			final String challengeFilename = AudioGenUtil.asPhoneticFilename(challenge);

			SexualGender sex = SexualGender.NOT_SPECIFIED;
			if (data.getSex().toLowerCase().startsWith("female")) {
				sex = SexualGender.FEMALE;
			}
			if (data.getSex().toLowerCase().startsWith("male")) {
				sex = SexualGender.MALE;
			}
			List<TtsVoice> voices = nextVoices(challenge, sex);
			for (TtsVoice voice : voices) {
				final File challengeWavFile = new File(wavTmpDir,
						"challenge-" + voice.id + "-" + challengeFilename + ".wav");
				AudioDataFile challengeData = new AudioDataFile(challengeWavFile, 0);
				data.addChallengeFile(challengeData);
				tts.generateWav(voice.id, challengeWavFile, challenge);
				final float durationInSeconds = getDuration(challengeWavFile);
				challengeData.duration = durationInSeconds;
			}

			TtsVoice voice = nextVoice(challenge, sex);
			final String answerFilename = AudioGenUtil.asEnglishFilename(answer);
			final File answerWavFile = new File(wavTmpDir,
					"answer-" + voice.sex.name() + "-" + answerFilename + ".wav");
			answerWavFile.getParentFile().mkdirs();
			AudioDataFile answerData = new AudioDataFile(answerWavFile, 0);
			data.addAnswerFile(answerData);
			String alreadyKey = answer+"|"+voice.sex.name();
			if (!already.contains(alreadyKey)) {
				final File tmp;
				if (voice.sex.equals(SexualGender.FEMALE)) {
					tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_FEMALE_1, answer);
				} else if (voice.sex.equals(SexualGender.MALE)) {
					tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_MALE_1, answer);
				} else {
					if (voiceShuffleSeed % 2 == 0) {
						tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_FEMALE_1, answer);
					} else {
						tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_MALE_1, answer);
					}
				}

				convertToWav(tmp, answerWavFile);
				if (!answerWavFile.exists()) {
					throw new FileNotFoundException(answerWavFile.getAbsolutePath());
				}
				
				normalizeWav(answerWavFile);
				if (!answerWavFile.exists()) {
					throw new FileNotFoundException(answerWavFile.getAbsolutePath());
				}

				already.add(alreadyKey);
				final float durationInSeconds = getDuration(answerWavFile);
				answerData.duration = durationInSeconds;
			}
		}
	}

	private TTS tts() {
		if (PRERECORDED_AUDIO) {
			return new Prerecorded();
		} else {
			return new CherokeeTTS();
		}
	}

	private void normalizeWav(final File answerWavFile) {
		final List<String> cmd = new ArrayList<>();
		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(answerWavFile.getAbsolutePath());
		executeCmd(cmd);
	}

	private void convertToWav(final File tmp, final File answerWavFile) {
		final List<String> cmd = new ArrayList<>();
		cmd.add("ffmpeg");
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(tmp.getAbsolutePath());
		cmd.add(answerWavFile.getAbsolutePath());
		executeCmd(cmd);
	}

	@SuppressWarnings("unused")
	private void generateDurationsReport() throws IOException {
		final File reportFile = new File("tmp/durations.txt");
		final StringBuilder sb = new StringBuilder();
		sb.append("Challenge Wav");
		sb.append("|");
		sb.append("Duration");
		sb.append("|");
		sb.append("Answer Wav");
		sb.append("|");
		sb.append("Duration");
		sb.append("\n");
		for (final AudioCard card : new ArrayList<>(en2chrDeck.getCards())) {
			final AudioData data = card.getData();
//			sb.append(data.getChallengeFile().getName());
			sb.append("|");
//			sb.append(NF.format(data.getChallengeDuration()));
			sb.append("|");
//			sb.append(data.getAnswerFile().getName());
			sb.append("|");
//			sb.append(NF.format(data.getAnswerDuration()));
			sb.append("\n");
		}
		FileUtils.writeStringToFile(reportFile, sb.toString(), StandardCharsets.UTF_8);
	}

	private void generateEn2ChrWavFiles() throws UnsupportedAudioFileException, IOException {
		voiceShuffleSeed = 0;
		TTS tts = tts();
		tts.setGriffinLim(GRIFFIN_LIM);
		final File wavTmpDir = new File(WAVS_DIR, "en2chr");
		FileUtils.deleteQuietly(wavTmpDir);
		final File espeakNgBin = new File(SystemUtils.getUserHome(), "espeak-ng/bin/espeak-ng");
		final ESpeakNg espeak = new ESpeakNg(espeakNgBin);
		final Set<String> already = new HashSet<>();
		for (final AudioCard card : new ArrayList<>(en2chrDeck.getCards())) {
			final AudioData data = card.getData();
			final String answer = data.getAnswer();
			String challenge = data.getChallenge();
			challenge = AudioGenUtil.removeEnglishFixedGenderMarks(AudioGenUtil.randomizeEnglishSexes(challenge));
			final String challengeFilename = AudioGenUtil.asEnglishFilename(challenge);
			final File challengeWavFile = new File(wavTmpDir, "challenge-" + challengeFilename + ".wav");
			final String answerFilename = AudioGenUtil.asPhoneticFilename(answer);
			final File answerWavFile = new File(wavTmpDir, "answer-" + answerFilename + ".wav");
			AudioDataFile answerData = new AudioDataFile(challengeWavFile);
			data.addAnswerFile(answerData);
//			data.setAnswerFile(answerWavFile);
			AudioDataFile challengeData = new AudioDataFile(challengeWavFile);
			data.addChallengeFile(challengeData);
//			data.setChallengeFile(challengeWavFile);
			if (!already.contains(challenge)) {
				SexualGender sex = SexualGender.NOT_SPECIFIED;
				if (data.getSex().toLowerCase().startsWith("female")) {
					sex = SexualGender.FEMALE;
				}
				if (data.getSex().toLowerCase().startsWith("male")) {
					sex = SexualGender.MALE;
				}
				TtsVoice voice = nextVoice(challenge, sex);
				System.out.println(" - " + challengeWavFile.getName());
				final File tmp;
				if (voice.sex.equals(SexualGender.FEMALE)) {
					tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_FEMALE_1, challenge);
				} else if (voice.sex.equals(SexualGender.MALE)) {
					tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_MALE_1, challenge);
				} else {
					if (voiceShuffleSeed % 2 == 0) {
						tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_FEMALE_1, challenge);
					} else {
						tmp = AwsPolly.generateEnglishAudio(AwsPolly.PRESENTER_MALE_1, challenge);
					}
				}
				final float durationInSeconds = getDuration(challengeWavFile);
				challengeData.duration = durationInSeconds;
			}
			if (!already.contains(answer)) {
				SexualGender sex = SexualGender.NOT_SPECIFIED;
				if (data.getSex().toLowerCase().startsWith("female")) {
					sex = SexualGender.FEMALE;
				}
				if (data.getSex().toLowerCase().startsWith("male")) {
					sex = SexualGender.MALE;
				}
				TtsVoice voice = nextVoice(challenge, sex);
				System.out.println(" - " + answerWavFile.getName() + " [" + voice + "]");
				tts.generateWav(voice.id, answerWavFile, answer);
				already.add(answer);
				final float durationInSeconds = getDuration(answerWavFile);
				answerData.duration = durationInSeconds;
			}
		}
	}

	private AudioData generateNewPhrase() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "here-is-a-new-phrase.wav");
		FileUtils.deleteQuietly(newPhrase);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR,
				"Here is a new phrase to learn. Listen carefully:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(newPhrase);
		data.addAnswerFile(answerData);
//		data.setAnswerFile(newPhrase);
		answerData.duration = getDuration(newPhrase);
		return data;
	}

	private AudioData generateFirstNewPhrase() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "here-is-first-new-phrase.wav");
		FileUtils.deleteQuietly(newPhrase);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR,
				"Here is your first phrase to learn for this session. Listen carefully:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(newPhrase);
		data.addAnswerFile(answerData);
//		data.setAnswerFile(newPhrase);
		answerData.duration = getDuration(newPhrase);
		return data;
	}

	private AudioData generateNewPhraseShort() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "short-a-new-phrase.wav");
		FileUtils.deleteQuietly(newPhrase);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Here is a new phrase:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(newPhrase);
		data.addAnswerFile(answerData);
//		data.setAnswerFile(newPhrase);
		answerData.duration = getDuration(newPhrase);
		return data;
	}

	private AudioData generateSilenceWav() {
		EXCERCISES_DIR.mkdirs();
		final File silenceWav = new File(EXCERCISES_DIR, "silence-1-second.wav");
		FileUtils.deleteQuietly(silenceWav);
		final List<String> cmd = Arrays.asList("sox", "-n", "-r", "22050", //
				"-c", "1", silenceWav.getAbsolutePath(), "trim", "0.0", "1.0");
		executeCmd(cmd);
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(silenceWav);
		data.addAnswerFile(answerData);
//		data.setAnswerFile(silenceWav);
		answerData.duration = getDuration(silenceWav);
		return data;
	}

	private AudioData generateTranslatePhrase() throws IOException {
		final File translateIntoEnglish = new File(EXCERCISES_DIR, "translate-into-english.wav");
		FileUtils.deleteQuietly(translateIntoEnglish);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Translate into English:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(translateIntoEnglish);
		data.addAnswerFile(answerData);
//		data.setAnswerFile(translateIntoEnglish);
		answerData.duration = getDuration(translateIntoEnglish);
		return data;
	}

	private AudioData generateTranslatePhraseShort() throws IOException {
		final File translateIntoEnglish = new File(EXCERCISES_DIR, "short-translate.wav");
		FileUtils.deleteQuietly(translateIntoEnglish);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Translate:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(translateIntoEnglish);
		data.addAnswerFile(answerData);
//		data.setAnswerFile(translateIntoEnglish);
		answerData.duration = getDuration(translateIntoEnglish);
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

	private AudioData itsTranslationIs() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "its-translation-is.wav");
		FileUtils.deleteQuietly(newPhrase);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Here it is in English:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(newPhrase);
		data.addAnswerFile(answerData);
		answerData.duration = getDuration(newPhrase);
		return data;
	}

	private AudioData inEnglish() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "in-english.wav");
		FileUtils.deleteQuietly(newPhrase);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "In English:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(newPhrase);
		data.addAnswerFile(answerData);
		answerData.duration = getDuration(newPhrase);
		return data;
	}

	private AudioData listenAgain() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "listen-again.wav");
		FileUtils.deleteQuietly(newPhrase);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Here is the phrase again:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(newPhrase);
		data.addAnswerFile(answerData);
		answerData.duration = getDuration(newPhrase);
		return data;
	}

	private AudioData listenAgainShort() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "short-listen-again.wav");
		FileUtils.deleteQuietly(newPhrase);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "Again:");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(newPhrase);
		data.addAnswerFile(answerData);
		answerData.duration = getDuration(newPhrase);
		return data;
	}

	private void loadMainDecks() throws IOException {
		final Set<String> dupePronunciationCheck = new HashSet<>();
		final StringBuilder reviewSheetChr2En = new StringBuilder();
		final StringBuilder reviewSheetEn2Chr = new StringBuilder();
		final File textFile = new File(deckSourceText);
		System.out.println(textFile.getAbsolutePath());
		final Map<String, AudioCard> cardsForCherokeeAnswers = new HashMap<>();
		final Map<String, AudioCard> cardsForEnglishAnswers = new HashMap<>();
		int lineNo = 0;
		try (LineIterator li = FileUtils.lineIterator(textFile, StandardCharsets.UTF_8.name())) {
			li.next();
			lineNo++;
			int idEn2Chr = 0;
			int idChr2En = 0;
			while (li.hasNext()) {
				lineNo++;
				final String line = li.next();
				if (line.trim().startsWith("#") || line.trim().isEmpty()) {
					continue;
				}
				final String[] fields = line.split("\\|");
				if (fields.length < IX_ENGLISH + 1) {
					System.out.println("; " + line);
					continue;
				}
				final boolean skipAsNew = fields[0].contains("*");
				final String verbStem = fields[IX_VERB].replaceAll("[¹²³⁴" + UNDERDOT + "]", "").trim();
				String boundPronoun = fields[IX_PRONOUN].replaceAll("[¹²³⁴" + UNDERDOT + "]", "").trim();
				/*
				 * tag the boundPronoun with the stem's lead character so that pronunciation
				 * based counting of sets happens
				 */
//				if (!verbStem.isEmpty()) {
//					boundPronoun += verbStem.substring(0, 1);
//				}

				String cherokeeText = fields[IX_PRONOUNCE].trim();
				if (cherokeeText.isEmpty()) {
					continue;
				}
				if (cherokeeText.trim().startsWith("#")) {
					continue;
				}
				if (cherokeeText.contains(",") && autoSplitCherokee) {
					cherokeeText = cherokeeText.substring(0, cherokeeText.indexOf(",")).trim();
				}
				cherokeeText = StringUtils.capitalize(cherokeeText);
				if (!cherokeeText.matches(".*[,.?!]")) {
					cherokeeText += ".";
				}
				String checkText = cherokeeText.replaceAll("(?i)[.,!?;]", "").trim();
				if (dupePronunciationCheck.contains(checkText)) {
					throw new IllegalStateException(
							"DUPLICATE PRONUNCIATION: (" + lineNo + ") " + checkText + "\n" + Arrays.toString(fields));
				}
				dupePronunciationCheck.add(checkText);

				String sex = fields[IX_GENDER].trim();

				String englishText = fields[IX_ENGLISH].trim();
				if (englishText.isEmpty()) {
					continue;
				}
				if (englishText.contains(";")) {
					englishText = englishText.replace(";", ", Or, ");
				}
				if (!englishText.matches(".*[,.?!]")) {
					englishText += ".";
				}
				if (englishText.contains("v.t.") || englishText.contains("v.i.")) {
					englishText = englishText.replaceAll("(?i)v\\.\s*t\\.\s*", "");
					englishText = englishText.replaceAll("(?i)v\\.\s*i\\.\s*", "");
				}
				if (englishText.contains("1.")) {
					englishText = englishText.replaceAll("\\b1\\.", "");
					englishText = englishText.replaceAll("\\b2\\.", ". Or, ");
					englishText = englishText.replaceAll("\\b3\\.", ". Or, ");
					englishText = englishText.replaceAll("\\b4\\.", ". Or, ");
				}
				if (englishText.contains(" (")) {
					// English text pronunciation adjustments
					englishText = englishText.replace(" (1)", " one");
					englishText = englishText.replace(" (animate)", ", animate");
					englishText = englishText.replace(" (inanimate)", ", inanimate");
				}
				if (englishText.contains("/")) {
					englishText = englishText.replace("/", " or ");
				}
				if (englishText.contains(", i")) {
					englishText = englishText.replace(", it", " or it");
				}
				if (englishText.contains("'s")) {
					englishText = englishText.replace("he's", "he is");
					englishText = englishText.replace("she's", "she is");
					englishText = englishText.replace("it's", "it is");
					englishText = englishText.replace("He's", "He is");
					englishText = englishText.replace("She's", "She is");
					englishText = englishText.replace("It's", "It is");
				}
				if (englishText.contains("'re")) {
					englishText = englishText.replace("'re", " are");
				}
				englishText = StringUtils.capitalize(englishText);
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
//					toChrData.setAnswerDuration(0);
					toChrData.setChallenge(englishText);
//					toChrData.setChallengeDuration(0);
					toChrData.setId(++idEn2Chr);
					toChrData.setSex(sex);
					if (skipAsNew) {
						toChrData.setBoundPronoun("*");
						toChrData.setVerbStem("*");
					}
					toChrCard.setData(toChrData);
					cardsForCherokeeAnswers.put(englishText, toChrCard);
					en2chrDeck.add(toChrCard);
				}
				reviewSheetEn2Chr.append(toChrData.id());
				reviewSheetEn2Chr.append("|");
				reviewSheetEn2Chr.append(toChrData.getChallenge());
				reviewSheetEn2Chr.append("|");
				reviewSheetEn2Chr.append(toChrData.getAnswer());
				reviewSheetEn2Chr.append("\n");

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
//					toEnData.setAnswerDuration(0);
					toEnData.setChallenge(cherokeeText);
//					toEnData.setChallengeDuration(0);
					toEnData.setId(++idChr2En);
					toEnData.setSex(sex);
					if (skipAsNew) {
						toEnData.setBoundPronoun("*");
						toEnData.setVerbStem("*");
					}
					toEnCard.setData(toEnData);
					cardsForEnglishAnswers.put(cherokeeText, toEnCard);
					chr2enDeck.add(toEnCard);
				}
				reviewSheetChr2En.append(toEnData.id());
				reviewSheetChr2En.append("|");
				reviewSheetChr2En.append(toEnData.getBoundPronoun());
				reviewSheetChr2En.append("|");
				reviewSheetChr2En.append(toEnData.getVerbStem());
				reviewSheetChr2En.append("|");
				reviewSheetChr2En.append(toEnData.getChallenge());
				reviewSheetChr2En.append("|");
				reviewSheetChr2En.append(toEnData.getAnswer());
				reviewSheetChr2En.append("\n");

				if (USE_DEBUG_DECK && chr2enDeck.size() >= DEBUG_DECK_SIZE) {
					break;
				}
			}
		}
		FileUtils.writeStringToFile(new File("review-sheet-chr-en.txt"), reviewSheetChr2En.toString(),
				StandardCharsets.UTF_8);
		FileUtils.writeStringToFile(new File("review-sheet-en-chr.txt"), reviewSheetEn2Chr.toString(),
				StandardCharsets.UTF_8);
	}

	public static enum SexualGender {
		FEMALE, MALE, NOT_SPECIFIED;
	}

	public TtsVoice nextVoice(final String ttsText, final SexualGender sex) {
		if (voices.isEmpty()) {
			voices.addAll(voiceVariants);
			do {
				Collections.shuffle(voices, new Random(voiceShuffleSeed++));
			} while (voices.get(0).equals(previousVoice) && voiceVariants.size() > 2);
		}
		TtsVoice voice = voices.get(0);
		// final String lc = englishText.toLowerCase();
		if (SexualGender.FEMALE.equals(sex) && !voice.sex.equals(SexualGender.FEMALE)) {
			voices.remove(0);
			return nextVoice(ttsText, sex);
		}
		if (SexualGender.MALE.equals(sex) && !voice.sex.equals(SexualGender.MALE)) {
			voices.remove(0);
			return nextVoice(ttsText, sex);
		}
		return previousVoice = voices.remove(0);
	}

	public List<TtsVoice> nextVoices(final String ttsText, final SexualGender sex) {
		List<TtsVoice> tempVoices = new ArrayList<>(voiceVariants);
		Collections.sort(tempVoices, (a, b) -> a.id.compareToIgnoreCase(b.id));
		Iterator<TtsVoice> iter = tempVoices.iterator();
		while (iter.hasNext()) {
			TtsVoice voice = iter.next();
			if (SexualGender.FEMALE.equals(sex) && !voice.sex.equals(SexualGender.FEMALE)) {
				iter.remove();
				continue;
			}
			if (SexualGender.MALE.equals(sex) && !voice.sex.equals(SexualGender.MALE)) {
				iter.remove();
				continue;
			}
		}
		return tempVoices;
	}

	private void sortDeckByShowAgainDelay(AudioDeck deck) {
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
			return Long.compare(o1.getCardStats().getShowAgainDelay_ms(), o2.getCardStats().getShowAgainDelay_ms());
		});
	}

	private AudioData thisConcludesThisExercise() throws IOException {
		final File newPhrase = new File(EXCERCISES_DIR, "concludes-this-exercise-3.wav");
		FileUtils.deleteQuietly(newPhrase);
		final File tmp = AwsPolly.generateEnglishAudio(AwsPolly.INSTRUCTOR, "This concludes this audio exercise.");
		final List<String> cmd = new ArrayList<>();
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
		final AudioData data = new AudioData();
		AudioDataFile answerData = new AudioDataFile(newPhrase);
		data.addAnswerFile(answerData);
		answerData.duration = getDuration(newPhrase);
		return data;
	}

	public static enum ExcerciseSet {
		ANIMALS, BOUND_PRONOUNS, BRAGGING_HUNTERS, CED, OSIYO_THEN_WHAT, CLL1, WWACC, WALC1
	}

	/**
	 * @author muksihs
	 *
	 */
	public static class TtsVoice {
		public TtsVoice() {
		}

		public TtsVoice(String id) {
			this(id, SexualGender.NOT_SPECIFIED);
		}

		public TtsVoice(String id, SexualGender sex) {
			this.id = id;
			this.sex = sex;
		}

		String id;
		SexualGender sex;

		@Override
		public String toString() {
			return "TtsVoice [id=" + id + ", sex=" + sex + "]";
		}

	}
}
