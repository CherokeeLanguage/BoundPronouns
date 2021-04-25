package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;

public interface TTS {

	void setGriffinLim(boolean griffinLim);

	default void generateWav(final String voice, final File wavFile, final String text)
			throws IOException {
		generateWav("chr", voice, wavFile, text);
	}

	void generateWav(String language, String voice, File wavFile, String text) throws IOException;

}
