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

public class CherokeeTTS {

//	private String checkpoint = "cherokee5c_loss-140-0.117"; // "cherokee5b_loss-300-0.119";
	private String checkpoint = "cherokee5b_loss-300-0.119";
	
	private final File ttsBin;
	private boolean griffinLim = true;
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

	public void generateWav(final String voice, final File wavFile, final String text)
			throws IOException {
		generateWav("chr", voice, wavFile, text);
	}
	
	public void generateWav(final String lang, final String voice, final File wavFile, final String text)
			throws IOException {
		String _text = Normalizer.normalize(text, Normalizer.Form.NFD);
		if (isSpaceWrap()) {
			_text = " "+_text.trim()+" ";
		}
		final File cacheDir = new File("CherokeeTTS/cache");
		cacheDir.mkdirs();
		String cached_name = _text.replaceAll("(?i)[^a-z0-9]", "")+(griffinLim?"_gl":"")+"_"+checkpoint+"_"+DigestUtils.sha512Hex(_text);
		final File cachedFile = new File(cacheDir, (lang==null?"":lang.trim()) +"_" + (voice == null ? "" : voice.trim()) + "_" + cached_name + ".wav");

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
