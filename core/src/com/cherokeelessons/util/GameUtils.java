package com.cherokeelessons.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.cherokeelessons.cards.ActiveCard;
import com.cherokeelessons.cards.Answer;
import com.cherokeelessons.cards.Card;
import com.cherokeelessons.cards.Deck;
import com.cherokeelessons.cards.Answer.AnswerList;

public class GameUtils {
	
	private static final int MAX_ANSWERS = 4;
	
	private static final int maxCorrect = 4;

	/**
	 * cost array, horizontally
	 */
	private static int lv_d[] = new int[1024];

	/**
	 * 'previous' cost array, horizontally
	 */
	private static int lv_p[] = new int[1024];

	/**
	 * <p>
	 * Taken from StringUtils.class and reconfigured to use pre-allocated arrays to
	 * prevent GC issues on Android.
	 * </p>
	 * <p>
	 * Find the Levenshtein distance between two Strings if it's less than or equal
	 * to a given threshold.
	 * </p>
	 *
	 * <p>
	 * This is the number of changes needed to change one String into another, where
	 * each change is a single character modification (deletion, insertion or
	 * substitution).
	 * </p>
	 *
	 * <p>
	 * This implementation follows from Algorithms on Strings, Trees and Sequences
	 * by Dan Gusfield and Chas Emerick's implementation of the Levenshtein distance
	 * algorithm from
	 * <a href="http://www.merriampark.com/ld.htm" >http://www.merriampark.com/
	 * ld.htm</a>
	 * </p>
	 *
	 * <pre>
	 * StringUtils.getLevenshteinDistance(null, *, *)             = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance(*, null, *)             = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance(*, *, -1)               = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance("","", 0)               = 0
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 8)       = 7
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 7)       = 7
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 6))      = -1
	 * StringUtils.getLevenshteinDistance("elephant", "hippo", 7) = 7
	 * StringUtils.getLevenshteinDistance("elephant", "hippo", 6) = -1
	 * StringUtils.getLevenshteinDistance("hippo", "elephant", 7) = 7
	 * StringUtils.getLevenshteinDistance("hippo", "elephant", 6) = -1
	 * </pre>
	 *
	 * @param s         the first String, must not be null
	 * @param t         the second String, must not be null
	 * @param threshold the target threshold, must not be negative
	 * @return result distance, or {@code -1} if the distance would be greater than
	 *         the threshold
	 */

	public static synchronized int getLevenshteinDistance(CharSequence s, CharSequence t, final int threshold) {
		if (s == null || t == null) {
			throw new IllegalArgumentException("Strings must not be null");
		}
		if (threshold < 0) {
			throw new IllegalArgumentException("Threshold must not be negative");
		}

		/*
		 * This implementation only computes the distance if it's less than or equal to
		 * the threshold value, returning -1 if it's greater. The advantage is
		 * performance: unbounded distance is O(nm), but a bound of k allows us to
		 * reduce it to O(km) time by only computing a diagonal stripe of width 2k + 1
		 * of the cost table. It is also possible to use this to compute the unbounded
		 * Levenshtein distance by starting the threshold at 1 and doubling each time
		 * until the distance is found; this is O(dm), where d is the distance.
		 *
		 * One subtlety comes from needing to ignore entries on the border of our stripe
		 * eg. p[] = |#|#|#|* d[] = *|#|#|#| We must ignore the entry to the left of the
		 * leftmost member We must ignore the entry above the rightmost member
		 *
		 * Another subtlety comes from our stripe running off the matrix if the strings
		 * aren't of the same size. Since string s is always swapped to be the shorter
		 * of the two, the stripe will always run off to the upper right instead of the
		 * lower left of the matrix.
		 *
		 * As a concrete example, suppose s is of length 5, t is of length 7, and our
		 * threshold is 1. In this case we're going to walk a stripe of length 3. The
		 * matrix would look like so:
		 *
		 * 1 2 3 4 5 1 |#|#| | | | 2 |#|#|#| | | 3 | |#|#|#| | 4 | | |#|#|#| 5 | | |
		 * |#|#| 6 | | | | |#| 7 | | | | | |
		 *
		 * Note how the stripe leads off the table as there is no possible way to turn a
		 * string of length 5 into one of length 7 in edit distance of 1.
		 *
		 * Additionally, this implementation decreases memory usage by using two
		 * single-dimensional arrays and swapping them back and forth instead of
		 * allocating an entire n by m matrix. This requires a few minor changes, such
		 * as immediately returning when it's detected that the stripe has run off the
		 * matrix and initially filling the arrays with large values so that entries we
		 * don't compute are ignored.
		 *
		 * See Algorithms on Strings, Trees and Sequences by Dan Gusfield for some
		 * discussion.
		 */

		int n = s.length(); // length of s
		int m = t.length(); // length of t

		// if one string is empty, the edit distance is necessarily the length
		// of the other
		if (n == 0) {
			return m <= threshold ? m : -1;
		} else if (m == 0) {
			return n <= threshold ? n : -1;
		}

		if (n > m) {
			// swap the two strings to consume less memory
			final CharSequence tmp = s;
			s = t;
			t = tmp;
			n = m;
			m = t.length();
		}

		// int lv_p[] = new int[n + 1]; // 'previous' cost array, horizontally
		// int lv_d[] = new int[n + 1]; // cost array, horizontally
		if (lv_p.length < n + 1) {
			lv_p = new int[n + 1];
		}
		if (lv_d.length < n + 1) {
			lv_d = new int[n + 1];
		}
		int _d[]; // placeholder to assist in swapping p and d

		// fill in starting table values
		final int boundary = Math.min(n, threshold) + 1;
		for (int i = 0; i < boundary; i++) {
			lv_p[i] = i;
		}
		// these fills ensure that the value above the rightmost entry of our
		// stripe will be ignored in following loop iterations
		Arrays.fill(lv_p, boundary, lv_p.length, Integer.MAX_VALUE);
		Arrays.fill(lv_d, Integer.MAX_VALUE);

		// iterates through t
		for (int j = 1; j <= m; j++) {
			final char t_j = t.charAt(j - 1); // jth character of t
			lv_d[0] = j;

			// compute stripe indices, constrain to array size
			final int min = Math.max(1, j - threshold);
			final int max = Math.min(n, j + threshold);

			// the stripe may lead off of the table if s and t are of different
			// sizes
			if (min > max) {
				return -1;
			}

			// ignore entry left of leftmost
			if (min > 1) {
				lv_d[min - 1] = Integer.MAX_VALUE;
			}

			// iterates through [min, max] in s
			for (int i = min; i <= max; i++) {
				if (s.charAt(i - 1) == t_j) {
					// diagonally left and up
					lv_d[i] = lv_p[i - 1];
				} else {
					// 1 + minimum of cell to the left, to the top, diagonally
					// left and up
					lv_d[i] = 1 + Math.min(Math.min(lv_d[i - 1], lv_p[i]), lv_p[i - 1]);
				}
			}

			// copy current distance counts to 'previous row' distance counts
			_d = lv_p;
			lv_p = lv_d;
			lv_d = _d;
		}

		// if p[n] is greater than the threshold, there's no guarantee on it
		// being the correct
		// distance
		if (lv_p[n] <= threshold) {
			return lv_p[n];
		}
		return -1;
	}

	/**
	 * <p>
	 * Taken from StringUtils.class and reconfigured to use pre-allocated arrays to
	 * preven GC issues on Android.
	 * </p>
	 * <p>
	 * Find the Levenshtein distance between two Strings if it's less than or equal
	 * to a given threshold.
	 * </p>
	 *
	 * <p>
	 * This is the number of changes needed to change one String into another, where
	 * each change is a single character modification (deletion, insertion or
	 * substitution).
	 * </p>
	 *
	 * <p>
	 * This implementation follows from Algorithms on Strings, Trees and Sequences
	 * by Dan Gusfield and Chas Emerick's implementation of the Levenshtein distance
	 * algorithm from
	 * <a href="http://www.merriampark.com/ld.htm" >http://www.merriampark.com/
	 * ld.htm</a>
	 * </p>
	 *
	 * <pre>
	 * StringUtils.getLevenshteinDistance(null, *, *)             = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance(*, null, *)             = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance(*, *, -1)               = IllegalArgumentException
	 * StringUtils.getLevenshteinDistance("","", 0)               = 0
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 8)       = 7
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 7)       = 7
	 * StringUtils.getLevenshteinDistance("aaapppp", "", 6))      = -1
	 * StringUtils.getLevenshteinDistance("elephant", "hippo", 7) = 7
	 * StringUtils.getLevenshteinDistance("elephant", "hippo", 6) = -1
	 * StringUtils.getLevenshteinDistance("hippo", "elephant", 7) = 7
	 * StringUtils.getLevenshteinDistance("hippo", "elephant", 6) = -1
	 * </pre>
	 *
	 * @param s         the first String, must not be null
	 * @param t         the second String, must not be null
	 * @param threshold the target threshold, must not be negative
	 * @return result distance, or {@code -1} if the distance would be greater than
	 *         the threshold
	 */
	public static synchronized int getLevenshteinDistanceIgnoreCase(final String s, final String t,
			final int threshold) {
		return GameUtils.getLevenshteinDistance(s != null ? s.toLowerCase() : "", t != null ? t.toLowerCase() : "",
				threshold);
	}

	protected GameUtils() {
		//
	}
	
	/**
	 * Sort answers by edit distance so the list can be trimmed to size easily. The
	 * sort only considers edit distance and does not factor in actual String values
	 * - this is intentional.
	 */
	private static final Comparator<Answer> BY_EDIT_DISTANCE = new Comparator<Answer>() {
		@Override
		public int compare(final Answer o1, final Answer o2) {
			if (o1.correct != o2.correct && o1.correct) {
				return -1;
			}
			if (o2.correct) {
				return 1;
			}
			if (o1.distance < o2.distance) {
				return -1;
			}
			if (o1.distance > o2.distance) {
				return 1;
			}
			return 0;
		}
	};

	public static AnswerList getAnswerSetsBySimilarChallenges(final ActiveCard active, final Card card, final Deck deck) {
		final Random rand = new Random();
		final AnswerList answers = new AnswerList();
		final String challenge = card.challenge.get(1);
		/**
		 * contains copies of used answers, vgroups, and pgroups to prevent duplicates
		 */
		final Set<String> already = new HashSet<>(16);
		already.add(card.pgroup);
		already.add(card.vgroup);
		already.addAll(card.answer);
		already.add(challenge);

		/*
		 * for temporary manipulation of list data so we don't mess with master copies
		 * in cards, etc.
		 */
		final List<String> tmp_correct = new ArrayList<>(16);
		tmp_correct.clear();
		tmp_correct.addAll(card.answer);

		/**
		 * sort answers from least known to most known
		 */
		Collections.sort(tmp_correct, sortLeastKnownFirst(active));
		
		/**
		 * Add exactly one answer. Least known preferred.
		 */
		final String answer = tmp_correct.get(0);
		answers.list.add(new Answer(true, answer, 0));

		/**
		 * look for "similar" looking challenges
		 */
		final Deck tmp = new Deck(deck);
		scanDeck: for (int distance = 5; distance < 100; distance +=5) {
			Collections.shuffle(tmp.cards);
			for (final Card wrongCard : tmp.cards) {
				/*
				 * make sure we keep bare pronouns with bare pronouns and vice-versa
				 */
				if (StringUtils.isBlank(card.vgroup) != StringUtils.isBlank(wrongCard.vgroup)) {
					continue;
				}
				if (!StringUtils.isBlank(card.vgroup)) {
					/*
					 * make sure we have unique pronouns for each wrong conjugated answer
					 */
					if (already.contains(wrongCard.pgroup)) {
						continue;
					}
					/*
					 * keep verbs unique as well
					 */
					if (already.contains(wrongCard.vgroup)) {
						continue;
					}
				}
				final String wrongChallenge = wrongCard.challenge.get(1);
				/*
				 * If we've already picked a wrong answer from this card, skip it and try the next card
				 */
				if (already.contains(wrongChallenge)) {
					continue;
				}
				final int threshold = distance;
				final int editDistance = GameUtils.getLevenshteinDistanceIgnoreCase(challenge, wrongChallenge, threshold);
				/*
				 * if edit distance isn't close enough, skip it and try the next card				 * 
				 */
				if (editDistance < 1) {
					continue;
				}
				/*
				 * select a random wrong answer
				 */
				final String wrongAnswer = wrongCard.answer.get(rand.nextInt(wrongCard.answer.size()));
				/*
				 * if the wrong answer has already been used, skip it and try the next card				 * 
				 */
				if (already.contains(wrongAnswer)) {
					continue;
				}
				/*
				 * Add the new wrong challenge, the pgroup, vgroup and
				 * wrong answer to already used for duplicate checking
				 */
				already.add(wrongChallenge);
				if (!StringUtils.isBlank(wrongCard.vgroup)) {
					already.add(wrongCard.vgroup);
				}
				if (!StringUtils.isBlank(wrongCard.pgroup)) {
					already.add(wrongCard.pgroup);
				}
				already.add(wrongAnswer);

				/*
				 * Add the wrong answer to the list of challenges for the active card
				 */
				answers.list.add(new Answer(false, wrongAnswer, editDistance));
				if (answers.list.size() >= MAX_ANSWERS) {
					break scanDeck;
				}
			}
		}
		Collections.sort(answers.list, BY_EDIT_DISTANCE);
		if (answers.list.size() > MAX_ANSWERS) {
			answers.list.subList(MAX_ANSWERS, answers.list.size()).clear();
		}
		Collections.shuffle(answers.list);
		return answers;
	}

	private static Comparator<String> sortLeastKnownFirst(final ActiveCard active) {
		return new Comparator<String>() {
			@Override
			public int compare(final String o1, final String o2) {
				final int i1 = active.getCorrectInARowFor(o1);
				final int i2 = active.getCorrectInARowFor(o2);
				if (i1 < i2) {
					return -1;
				}
				if (i1 > i2) {
					return 1;
				}
				return o1.compareTo(o2);
			}
		};
	}
}
