package com.almasb.maze;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

@SuppressWarnings("deprecation")
public class Zombie extends CharacterControl {

    private Node node;
    private AnimChannel channel;
    private AnimControl control;

    private Vector3f walkDir = new Vector3f();
    private float walkTime = 0, walkNextTime = 5;

    private Quaternion rotation = new Quaternion(0, 1, 0, 0);

    public Zombie(float x, float y, float z, Node node) {
        // defines physics box with       x   y   z
        super(new CapsuleCollisionShape(1.5f, 6f, 1), 0.05f);

        // basically no jumping
        setGravity(0);
        // place the zombie at x y z
        setPhysicsLocation(new Vector3f(x, y, z));

        this.node = node;
        control = node.getControl(AnimControl.class);
        channel = control.createChannel();
        channel.setAnim("Walk");
    }

    public Node getNode() {
        return node;
    }

    public void onUpdate(float tpf) {
        walkTime += tpf;

        node.setLocalTranslation(getPhysicsLocation());

        if (walkTime > walkNextTime) {
            walkDir.setX(FastMath.nextRandomFloat()*0.1f - 0.05f);
            walkDir.setY(0);
            walkDir.setZ(FastMath.nextRandomFloat()*0.1f - 0.05f);
            setWalkDirection(walkDir);

            float angle = walkDir.normalizeLocal().angleBetween(Vector3f.UNIT_Z);
            if (walkDir.getX() < 0) {
                angle *= -1;
            }

            rotation.fromAngleNormalAxis(angle, Vector3f.UNIT_Y);
            node.setLocalRotation(rotation);

            walkTime = 0;
        }
    }
}
