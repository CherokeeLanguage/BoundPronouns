package com.cherokeelessons.bp;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;

public class IOSLauncher extends IOSApplication.Delegate {

    @Override
    protected IOSApplication createApplication() {
    	try {
			System.out.println("#config");
			IOSApplicationConfiguration config = new IOSApplicationConfiguration();
			config.allowIpod = true;
			config.orientationLandscape = true;
			config.orientationPortrait = false;
			config.preventScreenDimming = true;
			System.out.println("#IOSApplication");
			return new IOSApplication(new BoundPronouns(), config);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }

    public static void main(String[] argv) {
		System.out.println("BoundPronouns#main");
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}