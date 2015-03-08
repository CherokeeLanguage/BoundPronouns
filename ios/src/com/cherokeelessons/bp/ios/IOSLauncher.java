package com.cherokeelessons.bp.ios;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.Gdx;
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
        config.displayScaleLargeScreenIfNonRetina=1.0f;
        config.displayScaleLargeScreenIfRetina=1.0f;
        config.displayScaleSmallScreenIfNonRetina=1.0f;
        config.displayScaleSmallScreenIfRetina=1.0f;
        return new IOSApplication(new BoundPronouns(), config);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
    
    @Override
    public void didBecomeActive(UIApplication application) {
    		Gdx.app.log("", "didBecomeActive");
    		super.didBecomeActive(application);
    }
}