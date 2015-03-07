package com.cherokeelessons.bp.ios;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSPropertyList;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.cherokeelessons.bp.BoundPronouns;
import com.cherokeelessons.play.GameServices;

public class IOSLauncher extends IOSApplication.Delegate {
	Platform platform;
    @Override
    protected IOSApplication createApplication() {
    		platform = new Platform();
		BoundPronouns.services=new GameServices(platform);
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.allowIpod=true;
        config.orientationLandscape=true;
        config.orientationPortrait=false;
        return new IOSApplication(new BoundPronouns(), config);
    }
    
    @Override
    public boolean openURL(UIApplication application, NSURL url,
    		String sourceApplication, NSPropertyList annotation) {
    		platform.webview(url);
    		return true;
//    		return super.openURL(application, url, sourceApplication, annotation);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}