package com.test.kt;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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

	// network
    private Client client;

    // simulation
    private Simulation simulation;

    // player movement
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean jump = false;

    // render
	ShapeRenderer shapeRenderer;
	Camera camera;

	@Override
	public void create () {

		camera = new OrthographicCamera(640, 480);
		shapeRenderer = new ShapeRenderer();
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

		// get user input
        this.moveLeft = Gdx.input.isKeyPressed(Keys.A);
        this.moveRight = Gdx.input.isKeyPressed(Keys.D);
        this.jump = Gdx.input.isKeyPressed(Keys.SPACE);

        // create network request
        CharacterControllerMessage request = new CharacterControllerMessage();
        request.timestamp = TimeUtils.nanoTime();
        request.moveLeft = this.moveLeft;
        request.moveRight = this.moveRight;
        request.jump = this.jump;

        client.sendUDP(request);


        // render the world
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		camera.update();
		shapeRenderer.setProjectionMatrix(camera.combined);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(1, 1, 0, 1);
		shapeRenderer.rect(simulation.px, simulation.py, 32, 32);
		shapeRenderer.end();
	}

	@Override
	public void dispose () {

	}
}
