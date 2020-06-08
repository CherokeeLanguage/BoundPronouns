package com.cherokeelessons.cards;

import java.util.ArrayList;
import java.util.List;

public class Answer {
	public static class AnswerList {
		public List<Answer> list = new ArrayList<>(8);

		public AnswerList() {
		}

		public AnswerList(final AnswerList answerSetsFor) {
			for (final Answer answer : answerSetsFor.list) {
				list.add(new Answer(answer));
			}
		}

		public int correctCount() {
			int c = 0;
			for (final Answer a : list) {
				if (a.correct) {
					c++;
				}
			}
			return c;
		}
	}

	public int distance = 0;

	public boolean correct = false;

	public String answer = "";

	public Answer() {
	}

	public Answer(final Answer answer2) {
		this.answer = answer2.answer;
		this.correct = answer2.correct;
		this.distance = answer2.distance;
	}

	public Answer(final boolean correct, final String answer, final int distance) {
		super();
		this.correct = correct;
		this.answer = answer;
		this.distance = distance;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("AnswerSet [distance=").append(distance).append(", correct=").append(correct).append(", ");
		if (answer != null) {
			builder.append("answer=").append(answer);
		}
		builder.append("]");
		return builder.toString();
	}
}