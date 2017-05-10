package com.test.kt;

import com.badlogic.gdx.ApplicationAdapter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.test.kt.messages.CharacterControllerMessage;
import com.test.kt.messages.PhysicsBodyMessage;
import com.test.kt.messages.TextMessage;

import java.io.IOException;

public class ServerTest extends ApplicationAdapter {

	private Simulation simulation;

	@Override
	public void create () {

		simulation = new Simulation();

		try {
			Server server = new Server();

			Kryo kryo = server.getKryo();
			kryo.register(TextMessage.class);
			kryo.register(CharacterControllerMessage.class);
			kryo.register(PhysicsBodyMessage.class);

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
					else if (object instanceof CharacterControllerMessage) {
						CharacterControllerMessage request = (CharacterControllerMessage)object;

						if (request.moveLeft) {
							simulation.px -= 1;
						}

						if (request.moveRight) {
							simulation.px += 1;
						}

						PhysicsBodyMessage response = new PhysicsBodyMessage();
						response.x = simulation.px;
						response.y = simulation.py;
						connection.sendUDP(response);
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
