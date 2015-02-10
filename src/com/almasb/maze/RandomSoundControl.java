package com.almasb.maze;

import com.jme3.audio.AudioNode;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public class RandomSoundControl extends AbstractControl {

    // to avoid constant casting we keep "AudioNode" type reference
    private AudioNode sound;
    private Vector3f position = new Vector3f();
    private float time = 0, nextTime = 20;

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        sound = (AudioNode) spatial;
    }

    @Override
    protected void controlUpdate(float tpf) {
        time += tpf;
        if (time > nextTime) {
            position.setX(FastMath.nextRandomFloat());
            position.setY(FastMath.nextRandomFloat());
            position.setZ(FastMath.nextRandomFloat());
            position.multLocal(100, 2, 100);
            position.subtractLocal(50, 1, 50);

            sound.setLocalTranslation(position);
            sound.playInstance();
            time = 0;
            nextTime = FastMath.nextRandomFloat() * 20 + 0.5f;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
