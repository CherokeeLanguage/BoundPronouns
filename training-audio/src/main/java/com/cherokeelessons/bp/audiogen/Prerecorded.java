package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
		final File byVoicesFolder = new File("Prerecorded/by-voice");
		byVoicesFolder.mkdirs();
		File voiceFolder = new File(byVoicesFolder,voice.trim());
		if (voiceFolder.exists()) {
			String cachedName = voice.trim()+"-"+text.replaceAll("(?i)[^a-z0-9 ]", "").replace(" ", "_");
			File cachedFile = new File(voiceFolder, cachedName + ".wav");
			if (cachedFile.canRead()) {
				wavFile.getParentFile().mkdirs();
				FileUtils.copyFile(cachedFile, wavFile);
				return;
			}
			missing.add(voiceFolder.getName()+"/"+cachedName);
			Set<String> fallbacks = new TreeSet<>();
			fallbacks.addAll(Arrays.asList("299-en-f", "311-en-m", "318-en-f", "334-en-m", "339-en-f", "345-en-m", "360-en-m"));
			fallbacks.remove(voice.trim());
			for (String fallbackVoice: fallbacks) {
				voiceFolder = new File(byVoicesFolder, fallbackVoice);
				cachedName = fallbackVoice.trim()+"-"+text.replaceAll("(?i)[^a-z0-9 ]", "").replace(" ", "_");
				cachedFile = new File(voiceFolder, cachedName + ".wav");
				if (cachedFile.canRead()) {
					wavFile.getParentFile().mkdirs();
					FileUtils.copyFile(cachedFile, wavFile);
					return;
				}
				missing.add(voiceFolder.getName()+"/"+cachedName);
			}
			
			Collections.reverse(missing);
			System.err.println("Did not find any of the following audio files\n"+missing.toString()+"\n");
			throw new IllegalStateException("Missing source audio file "+cachedName);
		}
		
		
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
		System.err.println("Did not find any of the following audio files\n"+missing.toString()+"\nin the directory\n"+cacheDir.getAbsolutePath());
		throw new IllegalStateException("Missing source audio file");
	}
}
