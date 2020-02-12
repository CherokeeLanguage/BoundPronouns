package com.cherokeelessons.bp;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;

public class IOSLauncher extends IOSApplication.Delegate {
	
    @Override
    protected IOSApplication createApplication() {
    	try {
			System.out.println("BoundPronouns#platform");
			System.out.println("BoundPronouns#services");
			System.out.println("BoundPronouns#config");
			IOSApplicationConfiguration config = new IOSApplicationConfiguration();
			config.allowIpod = true;
			config.orientationLandscape = true;
			config.orientationPortrait = false;
			config.displayScaleLargeScreenIfNonRetina = 1.0f;
			config.displayScaleLargeScreenIfRetina = 1.0f;
			config.displayScaleSmallScreenIfNonRetina = 1.0f;
			config.displayScaleSmallScreenIfRetina = 1.0f;
			System.out.println("BoundPronouns#IOSApplication");
			IOSApplication app = new IOSApplication(new BoundPronouns(), config);
			System.out.println("BoundPronouns#IOSApplication#return");
			return app;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}