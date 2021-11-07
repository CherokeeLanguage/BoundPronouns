package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * @author Michael Conrad
 */

public class CherokeeTTS implements TTS {
	
	private static final boolean NFC = true;
	private static final boolean GPU = false;

//	private String checkpoint = "2a-2021-05-01-epoch_300-loss_0.0740";C
	private String checkpoint = "5f-2021-11-04-epoch_300-loss_0.0748";
	
	private final File ttsBin;
	private boolean griffinLim = false;
	private boolean spaceWrap = false;
	
	public CherokeeTTS() {		
		this(System.getProperty("user.home")+"/git/Cherokee-TTS/tts-wrapper/tts.sh");
	}

	public CherokeeTTS(final File ttsBin) {
		this.ttsBin = ttsBin;
	}

	public CherokeeTTS(final String ttsBin) {
		this(new File(ttsBin));
	}

	private void executeCmd(final List<String> cmd) {
		final ProcessBuilder b = new ProcessBuilder(cmd);
		Process process;
		try {
			process = b.start();
			while (process.isAlive()) {
				IOUtils.copy(process.getInputStream(), System.out);
				Thread.sleep(100);
			}
			if (process.exitValue() != 0) {
				System.err.println();
				System.err.println("FATAL: Bad exit value from:\n   " + cmd.toString());
				System.err.println();
				IOUtils.copy(process.getInputStream(), System.out);
				IOUtils.copy(process.getErrorStream(), System.err);
				System.out.println();
				throw new RuntimeException("FATAL: Bad exit value from " + cmd.toString());
			}
			process.destroy();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void generateWav(final String lang, final String voice, final File wavFile, final String text)
			throws IOException {
		String _text = Normalizer.normalize(text, NFC ? Normalizer.Form.NFC : Normalizer.Form.NFD);
		if (isSpaceWrap()) {
			_text = " "+_text.trim()+" ";
		}
		final File cacheDir = new File("CherokeeTTS/cache-"+checkpoint);
		cacheDir.mkdirs();
		String cached_name = _text.replace(":", "\u02D0") .replace(" ", "-") .replaceAll("(?i)[^\\p{Script=Latin}\\p{Block=Combining_Diacritical_Marks}-\u02D0]", "_").replace(" ", "_") //
				+ (voice == null ? "" : "_" + voice.trim()) //
				+ (lang == null ? "" : "_" + lang.trim()) //
				+ (griffinLim ? "_gl" : "") //
//				+ "_" + checkpoint //
				+ "_" + DigestUtils.sha512Hex(_text);
		cached_name = cached_name.replaceAll("_+", "_");

		final File cachedFile = new File(cacheDir, cached_name + ".wav");

		wavFile.getParentFile().mkdirs();

		if (cachedFile.canRead()) {
			FileUtils.copyFile(cachedFile, wavFile);
			return;
		}

		final List<String> cmd = new ArrayList<>();

		cmd.add(ttsBin.getAbsolutePath());
		if (griffinLim) {
			cmd.add("--griffin_lim");
		}
		if (lang != null && !lang.trim().isEmpty()) {
			cmd.add("--lang");
			cmd.add(lang);
		}
		if (voice != null && !voice.trim().isEmpty()) {
			cmd.add("--voice");
			cmd.add(voice);
		}
		cmd.add("--wav");
		cmd.add(cachedFile.getAbsolutePath());
		cmd.add("--text");
		cmd.add(_text);
		
		cmd.add("--checkpoint");
		cmd.add(checkpoint);
		
		if (GPU) {
			cmd.add("--gpu");
		}
		
		executeCmd(cmd);

		cmd.clear();
		cmd.add("normalize-audio");
		cmd.add(cachedFile.getAbsolutePath());
		executeCmd(cmd);

		FileUtils.copyFile(cachedFile, wavFile);
	}

	public boolean isGriffinLim() {
		return griffinLim;
	}

	public void setGriffinLim(boolean griffinLim) {
		this.griffinLim = griffinLim;
	}

	public String getCheckpoint() {
		return checkpoint;
	}

	public void setCheckpoint(String checkpoint) {
		this.checkpoint = checkpoint;
	}

	public boolean isSpaceWrap() {
		return spaceWrap;
	}

	public void setSpaceWrap(boolean spaceWrap) {
		this.spaceWrap = spaceWrap;
	}
}
