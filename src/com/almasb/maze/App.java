package com.almasb.maze;

import java.util.Random;

import com.almasb.maze.MazeGenerator.MazeCell;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
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
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.TangentBinormalGenerator;

public class App extends SimpleApplication {

    public static final boolean DEBUG = AppProperties.isDebugMode();

    private static final App instance = new App();

    public static App getInstance() {
        return instance;
    }

    private App() {}

    private BulletAppState physicsState;

    private Node coinsNode;
    private Spatial theTree;

    private String message = "";
    private BitmapText textCoins, textMessage, textBattery, textHitPoints,
                        textBullets;

    private Node player;
    private Spatial gun;

    private PointLight gunLight;
    private long gunLightTime = 0;

    @Override
    public void simpleInitApp() {
        if (DEBUG) {
            flyCam.setMoveSpeed(100);
        }
        else {
            flyCam.setZoomSpeed(0);
        }

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

        //initParticles(mazeSize, wallSize);

        initAudio();
        initGUI();
    }

    private AudioNode gunShot;

    private void initInput() {
        inputManager.addMapping("PickUpCoin", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("ActivateTree", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

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

                if (name.equals("Shoot") && isPressed) {
                    boolean shot = gun.getControl(GunControl.class).shoot();
                    if (shot) {
                        gunShot.playInstance();

                        gunLight.setPosition(gun.getLocalTranslation());

                        rootNode.addLight(gunLight);
                        gunLightTime = System.nanoTime();
                    }
                }
            }
        }, "PickUpCoin", "ActivateTree", "Shoot");

        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Reload", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if ("Reload".equals(name) && isPressed) {
                    gun.getControl(GunControl.class).reload();
                    return;
                }

                player.setUserData(name, isPressed);
            }
        }, "Left", "Right", "Forward", "Back", "Reload");
    }

    private void initLight() {
        AmbientLight globalLight = new AmbientLight();
        globalLight.setColor(ColorRGBA.White.mult(DEBUG ? 1 : 0.05f));
        rootNode.addLight(globalLight);

        SpotLight flashlight = new SpotLight();
        flashlight.setColor(ColorRGBA.White.mult(1.5f));
        flashlight.setSpotRange(55);
        flashlight.setSpotInnerAngle(5 * FastMath.DEG_TO_RAD);
        flashlight.setSpotOuterAngle(15 * FastMath.DEG_TO_RAD);

        rootNode.addControl(new FlashlightControl(flashlight, cam));

        gunLight = new PointLight();
        gunLight.setColor(ColorRGBA.Yellow.clone());
        gunLight.setRadius(30);
    }

    private void initPhysics() {
        physicsState = new BulletAppState();
        stateManager.attach(physicsState);

        physicsState.getPhysicsSpace().addCollisionListener(event -> {
            /*
             * Note: we would explicitly check as follows if we didn't have
             * references to objects or we were interested in actual objects.
             * There is no need to do this here because we have player reference
             * and we don't do anything with the zombie reference
             */
            Node zombie = null, player = null;

            if ("Zombie".equals(event.getNodeA().getName())) {
                zombie = (Node) event.getNodeA();
            }

            if ("Zombie".equals(event.getNodeB().getName())) {
                zombie = (Node) event.getNodeB();
            }

            if ("Player".equals(event.getNodeA().getName())) {
                player = (Node) event.getNodeA();
            }

            if ("Player".equals(event.getNodeB().getName())) {
                player = (Node) event.getNodeB();
            }

            if (zombie != null && player != null) {
                player.getControl(PlayerControl.class).takeHit();
            }
        });
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
        wallGeo.setShadowMode(ShadowMode.Receive);


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
        // set collision group of maze to same as the floor
        // means collision between floor and maze won't be checked
        // while collision between maze and other objects will be
        mazePhysicsBody.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        mazePhysicsBody.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);
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

        // register maze for collision
        CollisionShape floorShape = CollisionShapeFactory.createMeshShape(floorNode);
        RigidBodyControl floorPhysicsBody = new RigidBodyControl(floorShape, 0);
        // set collision group of floor to same as the maze
        // means collision between floor and maze won't be checked
        // while collision between floor and other objects will be
        floorPhysicsBody.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        floorPhysicsBody.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);
        floorNode.addControl(floorPhysicsBody);

        physicsState.getPhysicsSpace().add(floorPhysicsBody);

        // add floor for render
        rootNode.attachChild(floorNode);
    }

    private void initPlayer(int mazeSize, int wallSize) {
        player = new Node("Player");
        player.setUserData("Left", false);
        player.setUserData("Right", false);
        player.setUserData("Forward", false);
        player.setUserData("Back", false);
        player.setLocalTranslation(mazeSize * wallSize, 0, mazeSize * wallSize + wallSize);

        player.addControl(new PlayerControl());

        physicsState.getPhysicsSpace().add(player);
        rootNode.attachChild(player);

        initGun();
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
        theTree.setLocalTranslation((mazeSize/2 - 1) * wallSize * 2, 0, mazeSize * wallSize + wallSize);
        theTree.setShadowMode(ShadowMode.CastAndReceive);
    }

    private void initGun() {
        gun = assetManager.loadModel("Models/Gun/gun.obj");
        gun.setLocalScale(0.01f);

        AmbientLight light = new AmbientLight();
        light.setColor(ColorRGBA.White.clone());
        gun.addLight(light);

        gun.addControl(new GunControl(player));

        rootNode.attachChild(gun);
    }

    private void initEnemies(int mazeSize, int wallSize) {
        Node zombieModel = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");

        // adjust model for collisions
        zombieModel.move(0, 2.5f, 0);
        zombieModel.setLocalScale(0.5f);
        zombieModel.setShadowMode(ShadowMode.CastAndReceive);

        // 4 enemies in 4 corners
        for (int i = 0; i < 4; i++) {
            Spatial zombieModelCopy = zombieModel.clone();
            // set animation to loop in walk
            AnimControl animControl = zombieModelCopy.getControl(AnimControl.class);
            AnimChannel channel = animControl.createChannel();
            channel.setAnim("Walk");

            Node zombie = new Node("Zombie");
            zombie.attachChild(zombieModelCopy);
            ZombieControl control = new ZombieControl();
            zombie.addControl(control);

            int x = (i < 2) ? 0 : mazeSize - 1;
            int z = (i % 2 == 0) ? 0 : mazeSize - 1;

            // place our node where we want
            control.warp(new Vector3f(x * wallSize * 2, 0f, z * wallSize * 2 + wallSize));

            physicsState.getPhysicsSpace().add(zombie);
            rootNode.attachChild(zombie);
        }
    }

    private void initParticles(int mazeSize, int wallSize) {
        ParticleEmitter fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        fire.setMaterial(mat);
        fire.setImagesX(2); // because we have 2 by 2 texture image
        fire.setImagesY(2);
        fire.setEndColor(ColorRGBA.Red);
        fire.setStartColor(ColorRGBA.Yellow);
        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fire.setStartSize(1.5f);
        fire.setEndSize(0.1f);
        fire.setGravity(0, 0, 0);
        fire.setLowLife(1f);
        fire.setHighLife(3f);
        fire.getParticleInfluencer().setVelocityVariation(0.3f);

        fire.setLocalTranslation(mazeSize * wallSize, 1f, mazeSize * wallSize + wallSize);
        rootNode.attachChild(fire);

        // fire lighting
        PointLight fireLight = new PointLight();
        fireLight.setColor(ColorRGBA.Yellow.mult(1.3f));
        fireLight.setRadius(wallSize*2 - 2);
        fireLight.setPosition(fire.getLocalTranslation());
        rootNode.addLight(fireLight);

        // shadows from the lighting above
        PointLightShadowRenderer shadowRenderer = new PointLightShadowRenderer(assetManager, 512);
        shadowRenderer.setLight(fireLight);
        shadowRenderer.setShadowIntensity(0.5f);
        shadowRenderer.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        viewPort.addProcessor(shadowRenderer);
    }

    private void initAudio() {
        audioRenderer.setEnvironment(Environment.Cavern);

        AudioNode bgm = new AudioNode(assetManager, "Sound/Environment/bgm.ogg");
        bgm.setPositional(false);
        bgm.setLooping(true);
        bgm.setVolume(0.3f);
        bgm.play();

        AudioNode audioFootsteps = new AudioNode(assetManager, "Sound/Effects/Foot_steps.ogg");
        audioFootsteps.addControl(new RandomSoundControl());
        // we attach it to root so that control is registered
        rootNode.attachChild(audioFootsteps);

        gunShot = new AudioNode(assetManager, "Sound/Effects/shot.ogg");
        gunShot.setPositional(false);
        rootNode.attachChild(gunShot);
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

        textBattery = new BitmapText(guiFont, false);
        textBattery.setSize(guiFont.getCharSet().getRenderedSize());
        textBattery.setLocalTranslation(50, 650 - textBattery.getLineHeight(), 0);
        guiNode.attachChild(textBattery);

        textHitPoints = new BitmapText(guiFont, false);
        textHitPoints.setSize(guiFont.getCharSet().getRenderedSize());
        textHitPoints.setLocalTranslation(50, 600 - textHitPoints.getLineHeight(), 0);
        guiNode.attachChild(textHitPoints);

        textBullets = new BitmapText(guiFont, false);
        textBullets.setSize(guiFont.getCharSet().getRenderedSize());
        textBullets.setLocalTranslation(50, 550 - textBullets.getLineHeight(), 0);
        guiNode.attachChild(textBullets);
    }

    private void updateGUI() {
        textCoins.setText("Coins Left: " + coinsNode.getQuantity());
        textBattery.setText("Flashlight Battery: " + rootNode.getControl(FlashlightControl.class).getBatteryLife() + "%");
        textMessage.setText(message);
        textHitPoints.setText("Hit Points: " + player.getControl(PlayerControl.class).getHitPoints());
        textBullets.setText("Bullets: " + gun.getControl(GunControl.class).getNumBullets());
    }

    @Override
    public void simpleUpdate(float tpf) {
        updateGUI();

        if (System.nanoTime() - gunLightTime >= tpf * 1000000000) {
            rootNode.removeLight(gunLight);
        }
    }
}
