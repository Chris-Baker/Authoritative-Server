package com.test.kt;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
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
	private Simulation previousFrame;
	private Array<SimulationSnapshot> unverifiedUpdates;
	private Array<SimulationSnapshot> verifiedUpdates;
	private SimulationSnapshot verifiedUpdate;

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
        previousFrame = new Simulation();
		unverifiedUpdates = new Array<SimulationSnapshot>();
		verifiedUpdates = new Array<SimulationSnapshot>();

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
			timeRequest.timestamp = TimeUtils.nanoTime();
			client.sendUDP(timeRequest);

			client.addListener(new Listener() {
				public void received (Connection connection, Object object) {
					if (object instanceof TimeResponseMessage) {
						TimeResponseMessage response = (TimeResponseMessage)object;
						ping = (TimeUtils.nanoTime() - response.clientSentTime);
						serverTimeAdjustment = (response.timestamp - (ping)) - response.clientSentTime; // should this be ping / 2?
						System.out.println("Ping: " + TimeUtils.nanosToMillis((ping)));
						System.out.println("Client Time: " + TimeUtils.nanosToMillis(TimeUtils.nanoTime()));
						System.out.println("Server Time: " + TimeUtils.nanosToMillis((response.timestamp - (ping / 2))));
						System.out.println("Difference: " + TimeUtils.nanosToMillis(serverTimeAdjustment));
					}
					else if (object instanceof SyncSimulationResponseMessage) {
						SyncSimulationResponseMessage response = (SyncSimulationResponseMessage)object;
						simulation.px = response.x;
						simulation.py = response.y;
					}
					else if (object instanceof TextMessage) {
						TextMessage response = (TextMessage)object;
						System.out.println(response.message);
					}
					else if (object instanceof PhysicsBodyMessage) {
                        PhysicsBodyMessage response = (PhysicsBodyMessage)object;
                        SimulationSnapshot verifiedUpdate = new SimulationSnapshot();
						verifiedUpdate.timestamp = response.timestamp;
						verifiedUpdate.px = response.x;
						verifiedUpdate.py = response.y;
						verifiedUpdates.add(verifiedUpdate);
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

        // update our previous frame simulation so we can calculate the deltas of everything
        previousFrame.px = simulation.px;
        previousFrame.py = simulation.py;

        // update the simulation locally based on player input
		if (this.moveLeft) {
			simulation.px -= 1;
		}
		if (this.moveRight) {
			simulation.px += 1;
		}

		// store our update as an unverified update
		SimulationSnapshot updateDelta = new SimulationSnapshot();
		updateDelta.timestamp = TimeUtils.nanoTime();
		updateDelta.px = simulation.px - previousFrame.px;
		updateDelta.py = simulation.py - previousFrame.py;

		if (updateDelta.px != 0) {
			unverifiedUpdates.add(updateDelta);
		}

		// insert verified updates and reapply unverified updates
		if (verifiedUpdates.size > 0) {
			// get the timestamp of the verified update
			verifiedUpdate = verifiedUpdates.pop();
			verifiedUpdates.clear();

			int index = 0;
			while (index < unverifiedUpdates.size) {
				SimulationSnapshot unverifiedUpdate = unverifiedUpdates.get(index);

				if (unverifiedUpdate.timestamp <= (verifiedUpdate.timestamp) - ping + serverTimeAdjustment) {
					unverifiedUpdates.removeIndex(index);
				}
				else {
					verifiedUpdate.px += unverifiedUpdate.px;
					verifiedUpdate.py += unverifiedUpdate.py;
					index += 1;
				}
			}

			// apply that to the simulation for rendering
			simulation.px = verifiedUpdate.px;
			simulation.py = verifiedUpdate.py;

		}

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
		if (verifiedUpdate != null) {
			shapeRenderer.setColor(0, 1, 0, 1);
			shapeRenderer.rect(verifiedUpdate.px, verifiedUpdate.py, 32, 32);
		}
		shapeRenderer.end();
	}

	@Override
	public void dispose () {

	}
}
