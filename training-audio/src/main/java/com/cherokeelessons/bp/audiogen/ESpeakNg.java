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

	public ESpeakNg(String espeakNgLocation) {
		espeakNg = new File(espeakNgLocation);
	}

	public ESpeakNg(File espeakNgLocation) {
		espeakNg = espeakNgLocation;
	}

	public void generateWav(String voice, File wavFile, String text) {
		wavFile.getParentFile().mkdirs();
		List<String> cmd = new ArrayList<>();

		cmd.add(espeakNg.getAbsolutePath());
		cmd.add("-w");
		cmd.add(wavFile.getAbsolutePath());
		if (voice != null && !voice.trim().isEmpty()) {
			cmd.add("-v");
			cmd.add(voice);
		}
		cmd.add(text);

		ProcessBuilder b = new ProcessBuilder(cmd);
		Process process;
		try {
			process = b.start();
			process.waitFor();
			process.destroy();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
