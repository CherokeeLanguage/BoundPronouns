package com.cherokeelessons.cards;

public class AnswerSet {
	public AnswerSet() {
	}

	public AnswerSet(boolean correct, String answer, int distance) {
		super();
		this.correct = correct;
		this.answer = answer;
		this.distance = distance;
	}

	public int distance = 0;
	public boolean correct = false;
	public String answer = "";

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AnswerSet [distance=").append(distance)
				.append(", correct=").append(correct).append(", ");
		if (answer != null)
			builder.append("answer=").append(answer);
		builder.append("]");
		return builder.toString();
	}
}