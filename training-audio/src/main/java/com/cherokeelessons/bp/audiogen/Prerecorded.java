package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class Prerecorded implements TTS {
	@Override
	public void setGriffinLim(boolean griffinLim) {
		// ignored
	}

	@Override
	public void generateWav(String language, String voice, File wavFile, String _text) throws IOException {
		String text = Normalizer.normalize(_text, Form.NFD).toLowerCase().trim();
		List<String> missing = new ArrayList<>();
		final File cacheDir = new File("Prerecorded/cache");
		cacheDir.mkdirs();
		String cached_name = text.replaceAll("(?i)[^a-z0-9 :]", "") //
				.replaceAll("(?i)(.)(:)", "$1$1").replace(" ", "_") //
				+ (voice == null ? "" : "_" + voice.trim()) //
				+ (language == null ? "" : "_" + language.trim());
		File cachedFile = new File(cacheDir, cached_name + ".wav");

		if (cachedFile.canRead()) {
			wavFile.getParentFile().mkdirs();
			FileUtils.copyFile(cachedFile, wavFile);
			return;
		}
		missing.add(cached_name);
		
		cached_name = text.replaceAll("(?i)[^a-z0-9 :]", "") //
				.replaceAll("(?i)(.)(:)", "$1$1").replace(" ", "_") //
				+ (voice == null ? "" : "_" + voice.trim());
		cachedFile = new File(cacheDir, cached_name + ".wav");

		if (cachedFile.canRead()) {
			wavFile.getParentFile().mkdirs();
			FileUtils.copyFile(cachedFile, wavFile);
			return;
		}
		missing.add(cached_name);
		
		cached_name = text.replaceAll("(?i)[^a-z0-9 :]", "") //
				.replaceAll("(?i)(.)(:)", "$1$1").replace(" ", "_") //
				+ (language == null ? "" : "_" + language.trim());
		cachedFile = new File(cacheDir, cached_name + ".wav");
		if (cachedFile.canRead()) {
			wavFile.getParentFile().mkdirs();
			FileUtils.copyFile(cachedFile, wavFile);
			return;
		}
		missing.add(cached_name);
		
		cached_name = text.replaceAll("(?i)[^a-z0-9 :]", "") //
				.replaceAll("(?i)(.)(:)", "$1$1").replace(" ", "_");
		cachedFile = new File(cacheDir, cached_name + ".wav");
		if (cachedFile.canRead()) {
			wavFile.getParentFile().mkdirs();
			FileUtils.copyFile(cachedFile, wavFile);
			return;
		}
		missing.add(cached_name);
		Collections.reverse(missing);
		System.err.println("Did not any of the following audio files\n"+missing.toString()+"\nin the directory\n"+cacheDir.getAbsolutePath());
		throw new IllegalStateException("Missing source audio file");
	}
}
