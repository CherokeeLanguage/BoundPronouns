package com.cherokeelessons.bp.desktop;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.cherokeelessons.bp.BoundPronouns;

public class DesktopLauncher {
	public static void main (String[] arg) {
		
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = (80*gd.getDisplayMode().getWidth())/100;
		int height = (85*gd.getDisplayMode().getHeight())/100;
		
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.allowSoftwareMode=true;
		config.forceExit=true;
		config.height=height;
		config.width=width;
		config.addIcon("icons/icon-128.png", FileType.Internal);
		config.addIcon("icons/icon-32.png", FileType.Internal);
		config.addIcon("icons/icon-16.png", FileType.Internal);
		LwjglApplication app = new LwjglApplication(new BoundPronouns(), config);
		
		app.getGraphics().getWidth();
	}
	
	
}
