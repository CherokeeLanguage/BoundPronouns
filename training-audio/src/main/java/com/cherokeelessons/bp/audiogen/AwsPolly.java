package com.cherokeelessons.bp.audiogen;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.Engine;
import com.amazonaws.services.polly.model.LanguageCode;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.TextType;
import com.amazonaws.services.polly.model.VoiceId;

public class AwsPolly {

	public static final VoiceId INSTRUCTOR = VoiceId.Matthew;
	public static final VoiceId PRESENTER_MALE_1 = VoiceId.Joey;
	public static final VoiceId PRESENTER_FEMALE_1 = VoiceId.Kendra;

	public static File generateEnglishAudio(final VoiceId voice, final String text) throws IOException {
		final File cacheDir = new File("aws/polly-cache");
		cacheDir.mkdirs();
		final String filename = AudioGenUtil.asEnglishFilename("en-us-" + voice.name() + "-" + text) + ".mp3";

		final File outputFile = new File(cacheDir, filename);

		if (outputFile.canRead()) {
			return outputFile;
		}

		final AmazonPollyClientBuilder standard = AmazonPollyClientBuilder.standard();
		final AmazonPolly client = standard.build();

		final SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest()
				.withOutputFormat(OutputFormat.Mp3).withSampleRate("22050").withEngine(Engine.Neural)
				.withLanguageCode(LanguageCode.EnUS).withTextType(TextType.Text).withVoiceId(voice).withText(text);
		FileUtils.copyToFile(client.synthesizeSpeech(synthesizeSpeechRequest).getAudioStream(), outputFile);
		return outputFile;
	}

	public AwsPolly() {
	}
}
