package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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
			if (process.exitValue() != 0) {
				System.err.println("FATAL: Bad exit value from:\n   " + cmd.toString());
				System.out.println();
				IOUtils.copy(process.getInputStream(), System.out);
				System.out.println();
				IOUtils.copy(process.getErrorStream(), System.err);
				System.out.println();
				throw new RuntimeException("FATAL: Bad exit value from " + cmd.toString());
			}
			process.destroy();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void generateWav(final String voice, final int speed, final File wavFile, final String text) throws IOException {
		File cacheDir = new File("espeak-ng/cache");
		cacheDir.mkdirs();
		File cachedFile = new File(cacheDir, (voice==null?"":voice.trim())+"_"+ wavFile.getName());
		
		wavFile.getParentFile().mkdirs();
		
		if (cachedFile.canRead()) {
			FileUtils.copyFile(cachedFile, wavFile);
			return;
		}
		
		final List<String> cmd = new ArrayList<>();

		cmd.add(espeakNg.getAbsolutePath());
		cmd.add("-z"); // trim trailing silence off of audio
		if (speed>0) {
			cmd.add("-s");
			cmd.add(""+speed);
		}
		cmd.add("-w");
		cmd.add(cachedFile.getAbsolutePath());
		if (voice != null && !voice.trim().isEmpty()) {
			cmd.add("-v");
			cmd.add(voice);
		}
		cmd.add(text);
		executeCmd(cmd);

		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(cachedFile.getAbsolutePath());
		executeCmd(cmd);
		
		FileUtils.copyFile(cachedFile, wavFile);
	}
}
