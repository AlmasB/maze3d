package com.almasb.maze;

import com.jme3.system.AppSettings;

public class MazeMain {
    public static void main(String[] args) {
        App game = new App();

        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setFrameRate(60);
        settings.setBitsPerPixel(32);
        settings.setTitle("The Maze v" + AppProperties.getVersion() + " by AlmasB");
        settings.setVSync(true);

        game.setSettings(settings);
        game.setShowSettings(false);
        game.start();
    }
}
