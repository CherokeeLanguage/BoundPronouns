package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <a href=
 * "https://github.com/espeak-ng/espeak-ng/blob/master/src/espeak-ng.1.ronn">espeak-ng
 * github documentation</a>
 *
 * @author Michael Conrad
 */

public class ESpeakNg {

	private final File espeakNg;

	public ESpeakNg(final File espeakNgLocation) {
		espeakNg = espeakNgLocation;
	}

	public ESpeakNg(final String espeakNgLocation) {
		espeakNg = new File(espeakNgLocation);
	}

	private void executeCmd(final List<String> cmd) {
		final ProcessBuilder b = new ProcessBuilder(cmd);
		Process process;
		try {
			process = b.start();
			process.waitFor();
			process.destroy();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void generateWav(final String voice, final int speed, final File wavFile, final String text) {
		wavFile.getParentFile().mkdirs();
		final List<String> cmd = new ArrayList<>();

		cmd.add(espeakNg.getAbsolutePath());
		cmd.add("-z"); // trim trailing silence off of audio
		if (speed>0) {
			cmd.add("-s");
			cmd.add(""+speed);
		}
		cmd.add("-w");
		cmd.add(wavFile.getAbsolutePath());
		if (voice != null && !voice.trim().isEmpty()) {
			cmd.add("-v");
			cmd.add(voice);
		}
		cmd.add(text);
		executeCmd(cmd);

		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(wavFile.getAbsolutePath());
		executeCmd(cmd);
	}
}
