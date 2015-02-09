package com.almasb.maze;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.almasb.maze.MazeGenerator.MazeCell;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.TangentBinormalGenerator;

public class App extends SimpleApplication {

    private BulletAppState physicsState;
    private Player player;

    private SpotLight flashlight;

    private Node coinsNode;
    private Spatial theTree;

    private List<Zombie> enemies = new ArrayList<Zombie>();

    private String message = "";
    private BitmapText textCoins, textMessage;

    private float footstepsTime = 0, footstepsNextTime = 1;
    private Vector3f footstepsPosition = new Vector3f();
    private AudioNode audioFootsteps;

    @Override
    public void simpleInitApp() {
        initInput();
        initLight();
        initPhysics();

        // 10 x 10 = 100 square blocks
        int mazeSize = 10;
        // half wall size, so 6*2 = 12
        int wallSize = 6;
        /*
         * maze center is then ((mazeSize / 2) * wallSize * 2, 0, (mazeSize / 2) * wallSize * 2 + wallSize)
         * mazeSize / 2 can be replaced by any arbitrary cell value [0..mazeSize-1]
         * to obtain the center point in cell
         */

        initFloor(mazeSize, wallSize);
        initMaze(mazeSize, wallSize);
        initPlayer(mazeSize, wallSize);
        initObjects(mazeSize, wallSize);
        initEnemies(mazeSize, wallSize);

        initAudio();
        initGUI();
    }

    private void initInput() {
        inputManager.addMapping("PickUpCoin", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("ActivateTree", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("PickUpCoin") && isPressed) {
                    CollisionResults results = new CollisionResults();
                    Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                    ray.setLimit(10);

                    /*
                     * Note: we should technically check collision
                     * with all collidables in the world like walls
                     * and get closest collision and check if it's a "coin"
                     * but for simple pre-alpha demo this is sufficient
                     */
                    coinsNode.collideWith(ray, results);

                    if (results.size() > 0) {
                        CollisionResult closest = results.getClosestCollision();
                        Geometry coin = closest.getGeometry();

                        coinsNode.detachChild(coin);
                        message = "Picked up a coin!";

                        if (coinsNode.getQuantity() == 0) {
                            rootNode.attachChild(theTree);
                            message = "The teleportation tree has spawned in the center of the maze";
                        }
                    }
                }

                if (name.equals("ActivateTree") && isPressed) {
                    if (coinsNode.getQuantity() == 0) {
                        CollisionResults results = new CollisionResults();
                        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                        ray.setLimit(10);

                        theTree.collideWith(ray, results);

                        if (results.size() > 0) {
                            message = "You have completed the demo!";
                        }
                    }
                }
            }
        }, "PickUpCoin", "ActivateTree");
    }

    private void initLight() {
        AmbientLight globalLight = new AmbientLight();
        globalLight.setColor(ColorRGBA.White.mult(0.05f));
        rootNode.addLight(globalLight);

        flashlight = new SpotLight();
        flashlight.setColor(ColorRGBA.White.mult(1.5f));
        flashlight.setSpotRange(55);
        flashlight.setSpotInnerAngle(5 * FastMath.DEG_TO_RAD);
        flashlight.setSpotOuterAngle(15 * FastMath.DEG_TO_RAD);
        rootNode.addLight(flashlight);
    }

    private void initPhysics() {
        physicsState = new BulletAppState();
        stateManager.attach(physicsState);
    }

    private void initMaze(int mazeSize, int wallSize) {
        MazeCell[][] maze = new MazeGenerator(mazeSize, mazeSize).getMaze();

        Node mazeNode = new Node("Maze");

        // build wall geometry
        Box wallMesh = new Box(wallSize, 6f, 0.5f);
        Geometry wallGeo = new Geometry("Wall", wallMesh);
        TangentBinormalGenerator.generate(wallMesh);           // to use normal maps

        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        wallMat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/wall.jpg"));
        wallMat.setTexture("NormalMap", assetManager.loadTexture("Textures/wall_normals.jpg"));
        wallMat.setBoolean("UseMaterialColors", true);
        wallMat.setColor("Diffuse", ColorRGBA.White);
        wallMat.setColor("Specular", ColorRGBA.White);
        wallMat.setColor("Ambient", ColorRGBA.White);
        wallMat.setFloat("Shininess", 32);  // [0,128]

        wallGeo.setMaterial(wallMat);
        wallGeo.setShadowMode(ShadowMode.CastAndReceive);


        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                MazeCell cell = maze[j][i];
                if (cell.leftWall) {
                    Geometry wall = wallGeo.clone();
                    wall.rotate(0, 90 * FastMath.DEG_TO_RAD, 0);
                    wall.setLocalTranslation(j*wallSize*2 - wallSize, 6f,  i*wallSize*2 + wallSize);

                    mazeNode.attachChild(wall);
                }

                if (cell.topWall) {
                    Geometry wall = wallGeo.clone();
                    wall.setLocalTranslation(j*wallSize*2, 6f, i*wallSize*2);

                    mazeNode.attachChild(wall);
                }

                // right side
                if (j == mazeSize - 1) {
                    Geometry wall = wallGeo.clone();
                    wall.rotate(0, 90 * FastMath.DEG_TO_RAD, 0);
                    wall.setLocalTranslation(j*wallSize*2 + wallSize, 6f,  i*wallSize*2 + wallSize);

                    mazeNode.attachChild(wall);
                }
            }
        }

        // bottom of the maze
        for (int j = 0; j < mazeSize; j++) {
            Geometry wall = wallGeo.clone();
            wall.setLocalTranslation(j*wallSize*2, 6f, mazeSize*wallSize*2);

            mazeNode.attachChild(wall);
        }

        // register maze for collision
        CollisionShape mazeShape = CollisionShapeFactory.createMeshShape(mazeNode);
        RigidBodyControl mazePhysicsBody = new RigidBodyControl(mazeShape, 0);
        mazeNode.addControl(mazePhysicsBody);

        physicsState.getPhysicsSpace().add(mazePhysicsBody);

        // add maze for render
        rootNode.attachChild(mazeNode);
    }

    private void initFloor(int mazeSize, int wallSize) {
        Material mat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        mat.getTextureParam("DiffuseMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat.getTextureParam("NormalMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.White);
        mat.setColor("Ambient", ColorRGBA.White);
        mat.setColor("Specular", ColorRGBA.White);
        mat.setFloat("Shininess", 8);

        Node floorNode = new Node("Floor");

        Box floor = new Box(20, 1f, 20);
        TangentBinormalGenerator.generate(floor);
        floor.scaleTextureCoordinates(new Vector2f(10, 10));
        Geometry floorGeom = new Geometry("Tile", floor);
        floorGeom.setMaterial(mat);
        floorGeom.setShadowMode(ShadowMode.Receive);

        for (int i = 0; i < wallSize * 2 * mazeSize / 40 + 1; i++) {
            for (int j = 0; j < wallSize * 2 * mazeSize / 40 + 1; j++) {
                Spatial tile = floorGeom.clone();
                tile.setLocalTranslation(20*j*2, -1, 20*i*2);

                floorNode.attachChild(tile);
            }
        }

        rootNode.attachChild(floorNode);
    }

    private void initPlayer(int mazeSize, int wallSize) {
        player = new Player(mazeSize * wallSize, 3, mazeSize * wallSize + wallSize, inputManager, cam);
        physicsState.getPhysicsSpace().add(player);

        flyCam.setMoveSpeed(100);
    }

    private void initObjects(int mazeSize, int wallSize) {
        coinsNode = new Node("Coins");

        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            Sphere coinShape = new Sphere(10, 10, 0.2f);
            Geometry coinGeo = new Geometry("Coin", coinShape);
            coinGeo.setLocalTranslation(random.nextInt(mazeSize) * wallSize * 2, 0.2f, random.nextInt(mazeSize) * wallSize * 2 + wallSize);
            coinGeo.setShadowMode(ShadowMode.CastAndReceive);

            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", ColorRGBA.Yellow);
            mat.setColor("Ambient", ColorRGBA.Yellow);
            mat.setColor("Specular", ColorRGBA.White);
            mat.setFloat("Shininess", 8);
            coinGeo.setMaterial(mat);

            coinsNode.attachChild(coinGeo);
        }
        rootNode.attachChild(coinsNode);

        theTree = assetManager.loadModel("Models/Tree/Tree.mesh.j3o");
        // center of the maze
        theTree.setLocalTranslation(mazeSize * wallSize, 0, mazeSize * wallSize + wallSize);
        theTree.setShadowMode(ShadowMode.CastAndReceive);
    }

    private void initEnemies(int mazeSize, int wallSize) {
        Node enemyModel = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        enemyModel.setLocalScale(0.5f);
        Zombie zombie = new Zombie(mazeSize * wallSize, 2.4f, mazeSize * wallSize + wallSize, enemyModel);

        physicsState.getPhysicsSpace().add(zombie);
        rootNode.attachChild(enemyModel);

        enemies.add(zombie);
    }

    private void initAudio() {
        audioRenderer.setEnvironment(Environment.Cavern);

        AudioNode bgm = new AudioNode(assetManager, "Sound/Environment/bgm.ogg");
        bgm.setPositional(false);
        bgm.setLooping(true);
        bgm.setVolume(0.3f);
        bgm.play();

        audioFootsteps = new AudioNode(assetManager, "Sound/Effects/Foot_steps.ogg");
    }

    private void initGUI() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        textCoins = new BitmapText(guiFont, false);
        textCoins.setSize(guiFont.getCharSet().getRenderedSize());
        textCoins.setLocalTranslation(50, 700 - textCoins.getLineHeight(), 0);
        guiNode.attachChild(textCoins);

        textMessage = new BitmapText(guiFont, false);
        textMessage.setSize(guiFont.getCharSet().getRenderedSize());
        textMessage.setLocalTranslation(640 - 150, textMessage.getLineHeight(), 0);
        guiNode.attachChild(textMessage);
    }

    private void updateGUI() {
        textCoins.setText("Coins Left: " + coinsNode.getQuantity());
        textMessage.setText(message);
    }

    private void updateAudio(float tpf) {
        footstepsTime += tpf;
        if (footstepsTime > footstepsNextTime) {
            footstepsPosition.setX(FastMath.nextRandomFloat());
            footstepsPosition.setY(FastMath.nextRandomFloat());
            footstepsPosition.setZ(FastMath.nextRandomFloat());
            footstepsPosition.multLocal(100, 2, 100);
            footstepsPosition.subtractLocal(50, 1, 50);

            audioFootsteps.setLocalTranslation(footstepsPosition);
            audioFootsteps.playInstance();
            footstepsTime = 0;
            footstepsNextTime = FastMath.nextRandomFloat() * 20 + 0.5f;
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        player.onUpdate();
        enemies.forEach(enemy -> enemy.onUpdate(tpf));

        flashlight.setDirection(cam.getDirection());
        flashlight.setPosition(cam.getLocation());

        updateGUI();
        updateAudio(tpf);
    }
}
