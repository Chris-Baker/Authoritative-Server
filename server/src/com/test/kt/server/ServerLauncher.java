package com.test.kt.server;

import static org.mockito.Mockito.mock;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.test.kt.ServerTest;

public class ServerLauncher {
	public static void main (String[] arg) {
		Gdx.gl = mock(GL20.class);
		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		new HeadlessApplication(new ServerTest(), config);
	}
}
