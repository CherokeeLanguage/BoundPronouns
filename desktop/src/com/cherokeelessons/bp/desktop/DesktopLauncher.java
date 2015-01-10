package com.cherokeelessons.bp.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.cherokeelessons.bp.BoundPronouns;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.allowSoftwareMode=true;
		config.forceExit=true;
		config.height=720;
		config.width=1280;		
		new LwjglApplication(new BoundPronouns(), config);
	}
}
