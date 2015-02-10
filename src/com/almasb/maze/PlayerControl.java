package com.almasb.maze;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class PlayerControl extends BetterCharacterControl {

    private Camera camera;
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    private Vector3f walkDir = new Vector3f();

    public PlayerControl() {
        super(1f, 4.5f, 8f);
        camera = App.getInstance().getCamera();
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        camLeft.set(camera.getLeft()).normalizeLocal().multLocal(10f);
        camDir.set(camera.getDirection()).normalizeLocal().multLocal(10f);

        walkDir.set(0, 0, 0);

        if ((boolean) spatial.getUserData("Left")) {
            walkDir.addLocal(camLeft);
        }

        if ((boolean) spatial.getUserData("Right")) {
            walkDir.addLocal(camLeft.negate());
        }

        if ((boolean) spatial.getUserData("Forward")) {
            walkDir.addLocal(camDir);
        }

        if ((boolean) spatial.getUserData("Back")) {
            walkDir.addLocal(camDir.negate());
        }

        // disable walking up/down
        walkDir.setY(0);
        // set constant speed
        walkDir.normalizeLocal().multLocal(10);
        setWalkDirection(walkDir);

        if (!App.DEBUG)
            camera.setLocation(spatial.getWorldTranslation().add(0, 4, 0).subtractLocal(camera.getDirection()));

        setViewDirection(camera.getDirection());
    }
}
