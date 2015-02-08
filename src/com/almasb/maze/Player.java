package com.almasb.maze;

import java.util.HashMap;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

@SuppressWarnings("deprecation")
public class Player extends CharacterControl implements ActionListener {

    private HashMap<String, Boolean> mappings = new HashMap<String, Boolean>();

    private Camera camera;

    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    private Vector3f walkDir = new Vector3f();

    public Player(float x, float y, float z, InputManager inputManager, Camera camera) {
        // defines physics box with       x   y   z
        super(new CapsuleCollisionShape(1.5f, 6f, 1), 0.05f);

        // basically no jumping
        setGravity(0);
        // place the player at x y z
        setPhysicsLocation(new Vector3f(x, y, z));

        // set up input
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Forward");
        inputManager.addListener(this, "Back");

        this.camera = camera;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        mappings.put(name, isPressed);
    }

    public boolean isActive(String mappingName) {
        return mappings.getOrDefault(mappingName, false);
    }

    public void onUpdate() {
        camDir.set(camera.getDirection()).multLocal(0.2f);
        camLeft.set(camera.getLeft()).multLocal(0.1f);
        walkDir.set(0, 0, 0);

        if (isActive("Left")) {
            walkDir.addLocal(camLeft);
        }

        if (isActive("Right")) {
            walkDir.addLocal(camLeft.negate());
        }

        if (isActive("Forward")) {
            walkDir.addLocal(camDir);
        }

        if (isActive("Back")) {
            walkDir.addLocal(camDir.negate());
        }

        // disable walking up/down
        walkDir.setY(0);
        setWalkDirection(walkDir);
        camera.setLocation(getPhysicsLocation());
    }
}
