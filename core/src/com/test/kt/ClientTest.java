package com.test.kt;

import com.badlogic.gdx.ApplicationAdapter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.test.kt.messages.TextMessage;

import java.io.IOException;

public class ClientTest extends ApplicationAdapter {
	
	@Override
	public void create () {

		try {
			Client client = new Client();
			Kryo kryo = client.getKryo();
			kryo.register(TextMessage.class);
			client.start();
			client.connect(5000, "localhost", 54555, 54777);

			TextMessage request = new TextMessage();
			request.message = "Here is the request";
			client.sendTCP(request);

			client.addListener(new Listener() {
				public void received (Connection connection, Object object) {
					if (object instanceof TextMessage) {
						TextMessage response = (TextMessage)object;
						System.out.println(response.message);
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render () {

	}
	
	@Override
	public void dispose () {

	}
}
