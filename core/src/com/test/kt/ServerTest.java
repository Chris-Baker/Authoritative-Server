package com.test.kt;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.test.kt.messages.CharacterControllerMessage;
import com.test.kt.messages.PhysicsBodyMessage;
import com.test.kt.messages.TextMessage;

import java.io.IOException;

public class ServerTest extends ApplicationAdapter {

	private static final float NETWORK_UPDATE_RATE = 1 / 10.0f;
	private float accum = 0;

	private Simulation simulation;

	private Server server;

	@Override
	public void create () {

		simulation = new Simulation();

		try {
			server = new Server();

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

						// Instead of responding right away we will send a snapshot at intervals
//						PhysicsBodyMessage response = new PhysicsBodyMessage();
//						response.x = simulation.px;
//						response.y = simulation.py;
//						connection.sendUDP(response);
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render () {

		accum += Gdx.graphics.getDeltaTime();

		// iterate over all entities and get the physics components

		// we should have the previous snapshot already from last update
		// we can see if there are new objects then we must send an update for them
		// we can compare the properties of existing ones and if thee are changes we also include them in the update

		// send snapshot object
		if (accum >= NETWORK_UPDATE_RATE) {
			accum = 0;
			PhysicsBodyMessage response = new PhysicsBodyMessage();
			response.x = simulation.px;
			response.y = simulation.py;
			server.sendToAllUDP(response);
		}

	}
	
	@Override
	public void dispose () {

	}
}
