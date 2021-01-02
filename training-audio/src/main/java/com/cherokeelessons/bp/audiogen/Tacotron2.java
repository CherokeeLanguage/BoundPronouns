package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * See <a href= "https://github.com/CherokeeLanguage/Cherokee-TTS">Cherokee-TTS</a>
 *
 * @author Michael Conrad
 */

public class Tacotron2 {

	private final File tacotronShellScript;
	
	public Tacotron2() {
		this(new File(SystemUtils.getUserHome(), "git/Cherokee-TTS/tts.sh"));
	}

	public Tacotron2(final File tacotronShellScript) {
		this.tacotronShellScript = tacotronShellScript;
	}

	public Tacotron2(final String tacotronShellScript) {
		this.tacotronShellScript = new File(tacotronShellScript);
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

	public void generateWav(final String voice, final File wavFile, final String text)
			throws IOException {
		final File cacheDir = new File("tacotron2/cache");
		cacheDir.mkdirs();
		final File cachedFile = new File(cacheDir, (voice == null ? "" : voice.trim()) + "_" + wavFile.getName());

		wavFile.getParentFile().mkdirs();

		if (cachedFile.canRead()) {
			FileUtils.copyFile(cachedFile, wavFile);
			return;
		}

		final List<String> cmd = new ArrayList<>();

		cmd.add(tacotronShellScript.getAbsolutePath());
		cmd.add(cachedFile.getAbsolutePath());
		cmd.add(text);
		executeCmd(cmd);

		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(cachedFile.getAbsolutePath());
		executeCmd(cmd);

		FileUtils.copyFile(cachedFile, wavFile);
	}
}
