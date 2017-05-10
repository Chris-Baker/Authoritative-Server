package com.test.kt;

import com.badlogic.gdx.ApplicationAdapter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.test.kt.messages.TextMessage;

import java.io.IOException;

public class ServerTest extends ApplicationAdapter {
	
	@Override
	public void create () {

		try {
			Server server = new Server();
			Kryo kryo = server.getKryo();
			kryo.register(TextMessage.class);
			server.start();
			server.bind(54555, 54777);

			server.addListener(new Listener() {
				public void received (Connection connection, Object object) {

					if (object instanceof TextMessage) {
						TextMessage request = (TextMessage)object;
						System.out.println(request.message);

						TextMessage response = new TextMessage();
						response.message = "Thanks";
						connection.sendTCP(response);
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
