package com.almasb.maze;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class ZombieControl extends BetterCharacterControl {

    private Vector3f walkDir = new Vector3f();
    private float walkTime = 0, walkNextTime = 5;

    public ZombieControl() {
        super(1.5f, 6f, 1f);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        walkTime += tpf;

        if (walkTime > walkNextTime) {
            walkDir.setX(FastMath.nextRandomFloat() - 0.5f);
            walkDir.setY(0);
            walkDir.setZ(FastMath.nextRandomFloat() - 0.5f);
            walkDir.multLocal(5);

            setWalkDirection(walkDir);
            setViewDirection(walkDir);

            walkTime = 0;
        }
    }
}
