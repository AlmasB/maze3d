package com.almasb.maze;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.SpotLight;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class FlashlightControl extends AbstractControl {

    private SpotLight flashlight;
    private boolean flashlightOn = false;
    private int batteryLife = 100;
    private float batteryTime = 0, batteryNextTime = 10;

    private Camera camera;

    public FlashlightControl(SpotLight light, Camera camera) {
        this.camera = camera;
        flashlight = light;
        InputManager inputManager = App.getInstance().getInputManager();
        inputManager.addMapping("Flashlight", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("Flashlight") && isPressed) {
                    if (flashlightOn) {
                        flashlightOn = false;
                        spatial.removeLight(flashlight);
                    }
                    else {
                        if (batteryLife > 0) {
                            flashlightOn = true;
                            spatial.addLight(flashlight);
                        }
                    }
                }
            }
        }, "Flashlight");
    }

    @Override
    protected void controlUpdate(float tpf) {
        flashlight.setDirection(camera.getDirection());
        flashlight.setPosition(camera.getLocation());

        batteryTime += tpf;
        if (batteryTime > batteryNextTime) {
            if (flashlightOn) {
                batteryLife--;
            }
            else {
                if (batteryLife < 100)
                    batteryLife++;
            }
            batteryTime = 0;
            batteryNextTime = FastMath.nextRandomFloat() * batteryLife*0.1f;
        }

        if (batteryLife == 0) {
            flashlightOn = false;
            spatial.removeLight(flashlight);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public int getBatteryLife() {
        return batteryLife;
    }
}
