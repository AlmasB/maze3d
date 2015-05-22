package com.almasb.maze;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

public class GunControl extends AbstractControl {

    private Node player;

    private int numBullets = 10;

    public GunControl(Node player) {
        this.player = player;
    }

    public int getNumBullets() {
        return numBullets;
    }

    public boolean shoot() {
        numBullets--;
        // TODO: impl
        return true;
    }

    @Override
    protected void controlUpdate(float tpf) {
        // place gun where player is but a bit higher in Y direction
        spatial.setLocalTranslation(player.getLocalTranslation().add(0, 3, 0));

        // rotate gun using player rotation
        spatial.setLocalRotation(player.getLocalRotation());

        // further rotate gun to place it at an angle
        spatial.rotate(0, FastMath.DEG_TO_RAD * (180 + 15), 0);

        // move gun slightly before the player
        spatial.move(player.getControl(PlayerControl.class).getViewDirection().mult(0.7f));

        // move gun slightly to the right
        spatial.move(player.getControl(PlayerControl.class).getViewDirection().cross(new Vector3f(0, 1, 0)).mult(0.7f));
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
