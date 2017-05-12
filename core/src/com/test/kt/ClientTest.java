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
import com.test.kt.messages.*;

import java.io.IOException;

public class ClientTest extends ApplicationAdapter {

	// network
    private Client client;
	long ping;
	long serverTimeAdjustment;

    // simulation
    private Simulation simulation;
	private Simulation localSimulation;

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
		localSimulation = new Simulation();

		try {
			client = new Client();

			Kryo kryo = client.getKryo();
			kryo.register(TextMessage.class);
            kryo.register(CharacterControllerMessage.class);
			kryo.register(PhysicsBodyMessage.class);
			kryo.register(SyncSimulationRequestMessage.class);
			kryo.register(SyncSimulationResponseMessage.class);
			kryo.register(TimeRequestMessage.class);
			kryo.register(TimeResponseMessage.class);

			client.start();
			client.connect(5000, "localhost", 54555, 54777);

			SyncSimulationRequestMessage syncRequest = new SyncSimulationRequestMessage();
			client.sendTCP(syncRequest);

			TimeRequestMessage timeRequest = new TimeRequestMessage();
			timeRequest.timestamp = TimeUtils.millis();
			client.sendUDP(timeRequest);

			client.addListener(new Listener() {
				public void received (Connection connection, Object object) {
					if (object instanceof TimeResponseMessage) {
						TimeResponseMessage response = (TimeResponseMessage)object;
						ping = (TimeUtils.millis() - response.clientSentTime);
						serverTimeAdjustment = response.timestamp - TimeUtils.millis() - ping;
						System.out.println("Ping: " + ping);
						System.out.println("Client Time: " + TimeUtils.millis());
						System.out.println("Server Time: " + (TimeUtils.millis() + serverTimeAdjustment));
						System.out.println("Difference: " + serverTimeAdjustment);
					}
					else if (object instanceof SyncSimulationResponseMessage) {
						SyncSimulationResponseMessage response = (SyncSimulationResponseMessage)object;
						localSimulation.px = response.x;
						localSimulation.py = response.y;
						simulation.px = response.x;
						simulation.py = response.y;
					}
					else if (object instanceof TextMessage) {
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

        // update the simulation locally
		if (this.moveLeft) {
			localSimulation.px -= 1;
		}

		if (this.moveRight) {
			localSimulation.px += 1;
		}

        // create network request
        CharacterControllerMessage request = new CharacterControllerMessage();
        request.timestamp = TimeUtils.millis();
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
		shapeRenderer.rect(localSimulation.px, localSimulation.py, 32, 32);
		shapeRenderer.setColor(0, 1, 0, 1);
		shapeRenderer.rect(simulation.px, simulation.py, 32, 32);
		shapeRenderer.end();
	}

	@Override
	public void dispose () {

	}
}
