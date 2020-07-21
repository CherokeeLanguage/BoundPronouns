package com.cherokeelessons.bp.audiogen;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.cherokeelessons.deck.Card;
import com.cherokeelessons.deck.CardStats;
import com.cherokeelessons.deck.CardUtils;

public class SessionUtil {
	public static final int FULLY_LEARNED_BOX = 10;
	public static final int PROFICIENT_BOX = 5;

	public static final int JUST_LEARNED_BOX = 1;

	private static Comparator<AudioCard> byShowTimeChunks = (o1, o2) -> {
		final CardStats s1 = o1.getCardStats();
		final CardStats s2 = o2.getCardStats();
		long diff = s1.getShowAgainDelay_ms() - s2.getShowAgainDelay_ms();
		if (diff < 0) {
			diff = -diff;
		}
		if (diff < 60l * 1000l) {
			return 0;
		}
		return s1.getShowAgainDelay_ms() > s2.getShowAgainDelay_ms() ? 1 : -1;
	};

	private final Map<String, AtomicInteger> vCounts;

	private final Map<String, AtomicInteger> pCounts;

	private final Set<String> nodupes = new HashSet<>();

	/**
	 * currently being looped through for display
	 */
	private final AudioDeck current_active = new AudioDeck();

	/**
	 * holding area for cards that have just been displayed or are not scheduled yet
	 * for display
	 */
	private final AudioDeck current_discards = new AudioDeck();

	/**
	 * holding area for cards that should not be shown any more this session
	 */
	private final AudioDeck current_done = new AudioDeck();

	/**
	 * holding area for cards that are "due" but deck size says don't show yet
	 */
	private final AudioDeck current_due = new AudioDeck();
	private final AudioDeck mainDeck;

	private final int showCount;

	/**
	 * time since last "shuffle"
	 */
	private float sinceLastNextCard_elapsed = 0f;

	public SessionUtil(final AudioDeck mainDeck, final int showsPerChallenge) {
		this.mainDeck = mainDeck;
		this.pCounts = new HashMap<>();
		this.vCounts = new HashMap<>();
		this.showCount = showsPerChallenge;
	}

	/**
	 * add this many cards to the Current Active Deck first from the current Active
	 * Deck then from the master Deck set
	 *
	 * @param needed
	 * @param active
	 */
	public void addCards(int needed, final AudioDeck active) {
		int startingSize = active.getCards().size();
		/**
		 * look for previous cards to load first, if their delay time is up
		 */
		Iterator<AudioCard> ipending = current_due.getCards().iterator();
		while (needed > 0 && ipending.hasNext()) {
			final AudioCard next = ipending.next();
			final CardStats cardStats = next.getCardStats();
			if (cardStats.getLeitnerBox() >= FULLY_LEARNED_BOX) {
				continue;
			}
			if (cardStats.getShowAgainDelay_ms() > 0) {
				continue;
			}
			next.resetTriesRemaining(showCount);
			active.getCards().add(next);
			needed--;
			ipending.remove();
		}
		System.out.println("Added " + (active.getCards().size() - startingSize) + " previous cards.");

		if (needed <= 0) {
			return;
		}

		/**
		 * not enough already seen cards, add new never seen cards
		 */
		startingSize = active.getCards().size();
		final Iterator<AudioCard> ideck = mainDeck.getCards().iterator();
		while (needed > 0 && ideck.hasNext()) {
			final AudioCard next = ideck.next();
			final String unique_id = next.id();
			if (nodupes.contains(unique_id)) {
				continue;
			}
			final AudioCard activeCard = new AudioCard();
			final CardStats cardStats = activeCard.getCardStats();
			cardStats.setLeitnerBox(0);
			cardStats.setCorrect(true);
			cardStats.setNewCard(true);
			cardStats.setShowAgainDelay_ms(CardUtils.getNextInterval(0));
			activeCard.resetTriesRemaining(showCount);

			int pSkill;
			if (!pCounts.containsKey(activeCard.getData().getBoundPronoun())) {
				pSkill = 0;
			} else {
				pSkill = pCounts.get(activeCard.getData().getBoundPronoun()).get();
			}
			int vSkill;
			if (!vCounts.containsKey(activeCard.getData().getVerbStem())) {
				vSkill = 0;
			} else {
				vSkill = vCounts.get(activeCard.getData().getVerbStem()).get();
			}

			System.out.println("Skill levels: "+activeCard.getData().getBoundPronoun()+"="+pSkill+", "+activeCard.getData().getVerbStem()+"="+vSkill);

			/*
			 * If student has been using both the stem and the pronoun in other challenges
			 * above the threshold, assume student can figure out the phrase without
			 * introducing it as a new card.
			 */
//			if (pSkill > 3 && vSkill > 2) {
//				/*
//				 * Should be proficiently skilled at this level, only try the new card once, and don't
//				 * introduce card as "new"
//				 */
//				activeCard.tries_remaining = 1;
//				activeCard.newCard = false;
//				System.out.println("Proficient mode active for: "+activeCard.pgroup+" "+activeCard.vgroup);
//			} else if (pSkill > 2 && vSkill > 2) {
//				/*
//				 * Should have some skill at this level, reduce tries per new card, and don't
//				 * introduce card as "new"
//				 */
//				if (activeCard.tries_remaining > 1) {
//					activeCard.tries_remaining -= 1;
//				}
//				activeCard.newCard = false;
//				System.out.println("Skilled mode active for: "+activeCard.pgroup+" "+activeCard.vgroup);
//			}

			active.getCards().add(activeCard);
			needed--;
			nodupes.add(unique_id);
		}
		System.out.println("Added " + (active.getCards().size() - startingSize) + " new cards.");

		if (needed <= 0) {
			return;
		}

		/**
		 * yikes! They processed ALL the cards!
		 */
		ipending = current_due.getCards().iterator();
		while (needed > 0 && ipending.hasNext()) {
			final AudioCard next = ipending.next();
			final CardStats cardStats = next.getCardStats();
			if (cardStats.getLeitnerBox() < FULLY_LEARNED_BOX) {
				continue;
			}
			next.resetTriesRemaining(showCount);
			active.getCards().add(next);
			needed--;
			ipending.remove();
		}
		ipending = current_due.getCards().iterator();
		while (needed > 0 && ipending.hasNext()) {
			final AudioCard next = ipending.next();
			next.resetTriesRemaining(showCount);
			active.getCards().add(next);
			needed--;
			ipending.remove();
		}

	}

	private Card getCardByPronounVstemCombo(final String pgroup, final String vgroup) {
//		for (final Card card : game.getCards().cards) {
//			if (!card.pgroup.equals(pgroup)) {
//				continue;
//			}
//			if (!card.vgroup.equals(vgroup)) {
//				continue;
//			}
//			return card;
//		}
		return null;
	}

	/**
	 * Calculates amount of ms needed to shift by to move deck to "0" point.
	 *
	 * @param current_pending
	 * @return
	 */
	public long getMinShiftTimeOf(final AudioDeck current_pending) {
		if (current_pending.getCards().size() == 0) {
			return 0;
		}
		long by = Long.MAX_VALUE;
		final Iterator<AudioCard> icard = current_pending.getCards().iterator();
		while (icard.hasNext()) {
			final AudioCard card = icard.next();
			final CardStats cardStats = card.getCardStats();
			if (cardStats.getTriesRemaining() < 1) {
				continue;
			}
			final long showAgainDelay_ms = cardStats.getShowAgainDelay_ms();
			if (by > showAgainDelay_ms) {
				by = showAgainDelay_ms;
			}
		}
		if (by == Long.MAX_VALUE) {
			by = 60l * 1000l;
		}
		return by;
	}

	public AudioCard getNextCard() {
		if (current_active.getCards().size() == 0) {
			updateTime(current_discards, (long) (sinceLastNextCard_elapsed * 1000f));
			sinceLastNextCard_elapsed = 0;
			Iterator<AudioCard> discards;
			discards = current_discards.getCards().iterator();
			/**
			 * remove from active session any cards with no tries left
			 */
			while (discards.hasNext()) {
				final AudioCard discard = discards.next();
				final CardStats cardStats = discard.getCardStats();
				if (cardStats.getTriesRemaining() > 0) {
					continue;
				}
				if (cardStats.isCorrect()) {
					cardStats.leitnerBoxInc();
					current_done.getCards().add(discard);
					cardStats.setShowAgainDelay_ms(cardStats.getShowAgainDelay_ms()
							+ CardUtils.getNextSessionInterval_ms(cardStats.getLeitnerBox()));
					discards.remove();
					System.out.println("Bumped Card: " + discard.getData().getChallenge());
					continue;
				}
				cardStats.leitnerBoxDec();
				current_done.getCards().add(discard);
				cardStats.setShowAgainDelay_ms(cardStats.getShowAgainDelay_ms()
						+ CardUtils.getNextSessionInterval_ms(cardStats.getLeitnerBox()));
				discards.remove();
				System.out.println("Retired Card: " + discard.getData().getChallenge());
				return getNextCard();
			}
			discards = current_discards.getCards().iterator();
			/**
			 * Find all cards in active session ready for display by time
			 */
			while (discards.hasNext()) {
				final AudioCard tmp = discards.next();
				final CardStats cardStats = tmp.getCardStats();
				if (cardStats.getShowAgainDelay_ms() > 0) {
					continue;
				}
				current_active.getCards().add(tmp);
				discards.remove();
			}
			if (current_active.getCards().size() == 0) {
				return null;
			}
			Collections.shuffle(current_active.getCards());
			Collections.sort(current_active.getCards(), byShowTimeChunks);
		}
		final AudioCard card = current_active.getCards().get(0);
		current_active.getCards().remove(0);
		current_discards.getCards().add(card);
		return card;
	}

	/**
	 * record all cards currently "in-play" so that when cards are retrieved from
	 * the master deck they are new cards
	 *
	 * @param activeDeck
	 */
	public void recordAlreadySeen(final AudioDeck activeDeck) {
		final Iterator<AudioCard> istat = activeDeck.getCards().iterator();
		while (istat.hasNext()) {
			final AudioCard next = istat.next();
			final String unique_id = next.id();
			nodupes.add(unique_id);
		}
	}

	private void reInsertCard(final AudioCard card) {
		if (current_active.getCards().size() < 2) {
			current_active.getCards().add(card);
		} else {
			current_active.getCards().add(1, card);
		}
		current_discards.getCards().remove(card);
	}

	/**
	 * time-shift all cards by time since last recorded run.
	 *
	 * @param currentDeck
	 * @param ms
	 */
	private void updateTime(final AudioDeck currentDeck, final long ms) {
		final Iterator<AudioCard> istat = currentDeck.getCards().iterator();
		while (istat.hasNext()) {
			final AudioCard next = istat.next();
			final CardStats cardStats = next.getCardStats();
			cardStats.setShowAgainDelay_ms(cardStats.getShowAgainDelay_ms() - ms);
		}
	}
}
