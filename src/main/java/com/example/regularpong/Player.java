package com.example.regularpong;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Player {
    private double xPos, yPos;
    private final int playerHeight = 100, playerWidth = 15;
    private volatile int score;
    private volatile boolean ready = false;

    public Player(double x, double y) {
        this.xPos = x;
        this.yPos = y;
    }

    public void drawPlayer(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.fillRect(xPos, yPos, playerWidth, playerHeight);
    }

    public void moveY (double n) {
        this.yPos += n;
    }

    public void setyPos (double n) {
        this.yPos = n;
    }

    public double getxPos() {
        return xPos;
    }

    public double getyPos() {
        return yPos;
    }

    public void addPoint() {
        this.score++;
    }

    public int getScore() {
        return this.score;
    }

    public boolean isReady() {
        return this.ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
