package com.cherokeelessons.bp.android;

import android.content.Intent;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.cherokeelessons.bp.BoundPronouns;
import com.cherokeelessons.play.GameServices;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		Platform.application=this;
		BoundPronouns.services = new GameServices(BoundPronouns.CredentialsFolder, new Platform());
		BoundPronouns game = new BoundPronouns();
		initialize(game, config);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}	
}
