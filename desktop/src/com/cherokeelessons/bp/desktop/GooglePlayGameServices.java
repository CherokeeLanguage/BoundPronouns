package com.cherokeelessons.bp.desktop;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.bp.BoundPronouns;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

public class GooglePlayGameServices extends Thread {
	private File DATA_STORE_DIR;
	private Credential credential;
	private FileDataStoreFactory dataStoreFactory;
	private NetHttpTransport httpTransport;
	
	public GooglePlayGameServices() {
		String path0 = "BoundPronouns/GooglePlayGameServices/";
		FileHandle p0 = Gdx.files.external(path0);
		p0.mkdirs();
		DATA_STORE_DIR=p0.child("datastore").file();
	}

	@Override
	public void run() {
		try {
			httpTransport = GoogleNetHttpTransport
					.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			credential = authorize();

		} catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
	private Credential authorize() {
		  // load client secrets
//		  GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JacksonFactory.class,
//		      new InputStreamReader(BoundPronouns.class.getResourceAsStream("/client_secrets.json")));
		  // set up authorization code flow
//		  GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//		      httpTransport, JacksonFactory.class, clientSecrets,
//		      Collections.singleton(googlep PlusScopes.PLUS_ME)).setDataStoreFactory(
//		      dataStoreFactory).build();
		  // authorize
//		  return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
		  return null;
		}
}
