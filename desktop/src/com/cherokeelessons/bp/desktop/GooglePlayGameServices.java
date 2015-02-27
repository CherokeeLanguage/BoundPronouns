package com.cherokeelessons.bp.desktop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.cherokeelessons.bp.BoundPronouns;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.games.Games;
import com.google.api.services.games.Games.Leaderboards;

public class GooglePlayGameServices extends Thread {
	private final File DATA_STORE_DIR;
	private Credential credential;
	private FileDataStoreFactory dataStoreFactory;
	private NetHttpTransport httpTransport;
	private final File LEADERS_DIR;
	private final FileHandle p0;
	private static final JacksonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	public GooglePlayGameServices() {
		String path0 = "BoundPronouns/GooglePlayGameServices/";
		p0 = Gdx.files.external(path0);
		p0.mkdirs();
		DATA_STORE_DIR = p0.child("datastore").file();
		LEADERS_DIR=p0.child("leaderboards").file();
	}

	@Override
	public void run() {
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			credential = authorize();
			getLeaderboards();
		} catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
	private void getLeaderboards() throws IOException{
		Games g = new Games(httpTransport, JSON_FACTORY, credential);
		Leaderboards.List leaders = g.leaderboards().list();
		LEADERS_DIR.mkdirs();
		
		OutputStream outputStream=new FileOutputStream(new File(LEADERS_DIR, "leaders.json"));
		leaders.executeAndDownloadTo(outputStream);
		Gdx.app.log("getLeaderBoard", p0.child("leaderboards").child("leaders.json").readString("UTF-8"));
	}

	private Credential authorize() throws IOException {
		// load client secrets
		/*
		 * The JSON is a direct download from
		 * https://console.developers.google.com/ for
		 * "Client ID for native application"
		 */
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JSON_FACTORY,
				new InputStreamReader(BoundPronouns.class
						.getResourceAsStream("/client_secrets.json")));

		ArrayList<String> scopes = new ArrayList<String>();
		scopes.add("https://www.googleapis.com/auth/games");

		GoogleAuthorizationCodeFlow.Builder builder = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, JSON_FACTORY, clientSecrets, scopes);
		builder.setScopes(scopes);
		GoogleAuthorizationCodeFlow flow = builder.setAccessType("offline")
				.setDataStoreFactory(dataStoreFactory).build();

		// authorize
		Credential authorize=null;
		try {
			authorize = new AuthorizationCodeInstalledApp(flow,
					new LocalServerReceiver()).authorize("user");
			authorize.refreshToken();
		} catch (TokenResponseException e) {
			e.printStackTrace();
			if (authorize!=null) {
				Gdx.app.log(this.getClass().getName(), "Bad Tokens. Resetting and requesting new ...");
				flow.getCredentialDataStore().clear();
				return authorize();
			}
		} catch (Exception e) {
			Gdx.app.log(this.getClass().getName(), e.getClass().getName());
		}
		
		return authorize;
	}
}
