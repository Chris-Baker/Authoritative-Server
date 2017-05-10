package com.test.kt;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.test.kt.messages.CharacterControllerMessage;
import com.test.kt.messages.PhysicsBodyMessage;
import com.test.kt.messages.TextMessage;

import java.io.IOException;

public class ClientTest extends ApplicationAdapter {

    private Client client;
    private Simulation simulation;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean jump = false;

	@Override
	public void create () {

        simulation = new Simulation();

		try {
			client = new Client();

			Kryo kryo = client.getKryo();
			kryo.register(TextMessage.class);
            kryo.register(CharacterControllerMessage.class);
            kryo.register(PhysicsBodyMessage.class);

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
					else if (object instanceof PhysicsBodyMessage) {
                        PhysicsBodyMessage response = (PhysicsBodyMessage)object;
                        simulation.px = response.x;
                        simulation.py = response.y;
                    }
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render () {

        this.moveLeft = Gdx.input.isKeyPressed(Keys.A);
        this.moveRight = Gdx.input.isKeyPressed(Keys.D);
        this.jump = Gdx.input.isKeyPressed(Keys.SPACE);

        CharacterControllerMessage request = new CharacterControllerMessage();
        request.timestamp = TimeUtils.nanoTime();
        request.moveLeft = this.moveLeft;
        request.moveRight = this.moveRight;
        request.jump = this.jump;

        client.sendUDP(request);

        System.out.println(simulation.px + ", " + simulation.py);
	}
	
	@Override
	public void dispose () {

	}
}
