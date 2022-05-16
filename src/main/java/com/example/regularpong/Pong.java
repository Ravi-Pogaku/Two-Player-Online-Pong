package com.example.regularpong;

import javafx.application.Application;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.concurrent.Task;

import java.io.*;
import java.net.*;

/**
 * Regular Game of Pong, Template for P2P Pong game
 */

public class Pong extends Application {
    private static final int width = 700;        // width of canvas
    private static final int height = 500;       // height of canvas
    private static final int playerWidth = 15;
    private static final int playerHeight = 100;
    private static final double ballR = 15;      // radius of ball
    private int ballYSpeed = 1;                  // used for translating the ball vertically
    private int ballXSpeed = 1;                  // used for translating the ball horizontally
    private int maxSpeed = 6;
    private Player player;
    private Player opponent;
    private double ballYPos = (double) height / 2;        // ball starting position, center of canvas
    private double ballXPos = (double) width / 2;
    private int playerID;
    DataInputStream in;
    DataOutputStream out;
    private ReadServer rsRunnable;
    private WriteServer wsRunnable;

    private Socket clientSock;

    private void connectToServer() {
        try {
            clientSock = new Socket("99.226.202.88", 6666);
            in = new DataInputStream(clientSock.getInputStream());      // from server
            out = new DataOutputStream(clientSock.getOutputStream());  // to server

            rsRunnable = new ReadServer(in);
            wsRunnable = new WriteServer(out);

            playerID = in.readInt();
            System.out.println("You are player # " + playerID);
            if (playerID == 1) {
                System.out.println("Wait for player #2 to connect...");
            }
        } catch (ConnectException e) {
            System.out.println("ConnectException from connectToServer");
        } catch (IOException e) {
            System.out.println("IOException from connectToServer");
        }
    }

    /**
     * Runnable that will read data stream of ready status and mouse coordinates of opposing player from the server
     */
    private class ReadServer implements Runnable {
        private DataInputStream in;

        public ReadServer(DataInputStream in) {
            this.in = in;
            System.out.println("RS Runnable created");
        }

        @Override
        public void run() {
            try {
                while (!opponent.isReady()) { // read for enemy ready
                    opponent.setReady(in.readBoolean());
                }

                while (true) {
                    opponent.setyPos(in.readDouble());
                }
            } catch (IOException e) {
                System.out.println("IOException from ReadServer Runnable");
            } finally {
            }
        }
    }

    /**
     * Runnable that will write this player's mouse coordinates to the server
     */
    private class WriteServer implements Runnable {
        private DataOutputStream out;

        public WriteServer(DataOutputStream out) {
            this.out = out;
            System.out.println("WS Runnable created");
        }

        @Override
        public void run() {
            try {
                // writes to server when player is ready
                while (!player.isReady()) {
                } // delay until player is ready
                out.writeBoolean(player.isReady()); // write player ready to server
                out.flush();

                Thread tReadMouse = new Thread(rsRunnable);
                tReadMouse.start();

                // and then infinite while loop to continuously send player position to server
                while (true) {
                    this.out.writeDouble(player.getyPos());
                    this.out.flush();
                    try {
                        Thread.sleep(25); // sleep thread for a couple milliseconds so the server isnt overwhelmed
                    } catch (InterruptedException e) {
                        System.out.println("InterruptedException from WriteServer Runnable");
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException from WriteServer Runnable");
            }
        }
    }

    /**
     * Checks for winner at the end of the timeline, need to figure out how to sync both clients.
     *
     * @param gc
     */
    public void checkWinner(GraphicsContext gc, Timeline tl) {
        if (playerID == 1) {
            if (player.getScore() > opponent.getScore()) {     // player 1 wins
                gc.strokeText("You Win!", width / 2, height / 2);
            } else {
                    gc.strokeText("You Lose.", width / 2, height / 2);
            }
        } else {
            if (player.getScore() > opponent.getScore()) {     // player 1 wins
                gc.strokeText("You Win!", width / 2, height / 2);
            } else {
                gc.strokeText("You Lose.", width / 2, height / 2);
            }
        }

        tl.pause();
    }

    /**
     * creates the window
     *
     * @param stage
     */
    @Override
    public void start(Stage stage) {
        this.connectToServer();
        this.createPlayers();
        Thread tWriteMouse = new Thread(wsRunnable);
        tWriteMouse.start();

        stage.setTitle("PONG PLAYER #" + playerID);
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(10), e -> run(gc)));
        tl.setCycleCount(Timeline.INDEFINITE); // 1 minute

        if (player != null) {
            canvas.setOnMouseMoved(e -> {
                player.setyPos(e.getY());
            });
        }

        Runnable rCheckWinner = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (player.getScore() == 10 || opponent.getScore() == 10) {
                        System.out.println("Someone won");
                        checkWinner(gc, tl);
                        break;
                    }
                }
            }
        };

        Thread tCheckWinner = new Thread(rCheckWinner);
        tCheckWinner.setDaemon(true);
        tCheckWinner.start();

        stage.setOnCloseRequest(e -> {
            stage.close();
            try {
                clientSock.close();
            } catch (NullPointerException ex) {
                System.out.println("NullPointerException in setOnCloseRequest");
            } catch (IOException ex) {
                System.out.println("IOException in setOnCloseRequest");
            }
        });

        canvas.setOnMouseClicked(e -> {
            this.player.setReady(true);
            System.out.println("player " + playerID + " is ready.");
        });

        StackPane sp = new StackPane(canvas);
        Scene scene = new Scene(sp);
        stage.setScene(scene);

        stage.show();
        tl.play();
    }

    public void createPlayers() {
        if (playerID == 1) {  // player 1 is on the left
            player = new Player(0, height / 2);
            opponent = new Player(width - playerWidth, height / 2);
        } else {  // player 2 is on the right
            player = new Player(width - playerWidth, height / 2);
            opponent = new Player(0, height / 2);
        }
    }

    /**
     * Helper function that handles the ball's speed and direction when hit by a player
     */
    public void hitBall() {
        if (playerID == 1) {
            if ((ballXPos <= playerWidth && ballYPos <= player.getyPos() + playerHeight && ballYPos >= player.getyPos()) ||
                    (ballXPos >= width - 2 * playerWidth && ballYPos <= opponent.getyPos() + playerHeight && ballYPos >= opponent.getyPos())) {

                ballXSpeed += 1 * Math.signum(ballXSpeed); // Math.signum returns -1 if negative, and 1 if positive so ball is
                ballYSpeed += 1 * Math.signum(ballYSpeed); // sped up properly regardless of current ball speed.
                ballXSpeed *= -1;
            }
        } else {
            if ((ballXPos <= playerWidth && ballYPos <= opponent.getyPos() + playerHeight && ballYPos >= opponent.getyPos()) ||
                    (ballXPos >= width - 2 * playerWidth && ballYPos <= player.getyPos() + playerHeight && ballYPos >= player.getyPos())) {

                ballXSpeed += Math.signum(ballXSpeed); // signum returns -1 if negative, and 1 if positive so ball is
                ballYSpeed += Math.signum(ballYSpeed); // sped up properly regardless of current ball speed.
                ballXSpeed *= -1;
            }
        }
    }

    /**
     * Function that runs the game
     *
     * @param gc - used for animating players and ball
     */
    public void run(GraphicsContext gc) {
        // background
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        // any text will be white
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(20));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);


        if (player.isReady() && opponent.isReady()) {


            // Task to iterate score and reset ball position
            Task<Void> iterateScore = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if (ballXPos < 0 || ballXPos > width) {
                        if (ballXPos < 0) {
                            opponent.addPoint();
                        } else {
                            player.addPoint();
                        }

                        ballXPos = (double) width / 2;
                        ballYPos = (double) height / 2;
                        ballXSpeed = 1;
                        ballYSpeed = 1;
                    }

                    return null;
                }
            };

            // Javafx runs of its own thread so a Threaded Task is needed to make changes to UI elements in real time
            Thread tScore = new Thread(iterateScore);
            tScore.setDaemon(true);
            tScore.start();

            // draw scores
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(player.getScore() + " : " + opponent.getScore(), width / 2, 50);

            // draw players
            player.drawPlayer(gc);
            opponent.drawPlayer(gc);


            // draw ball
            gc.fillOval(ballXPos, ballYPos, ballR, ballR);

            // ball movement
            ballXPos += ballXSpeed;
            ballYPos += ballYSpeed;

            // if ball touches top or bottom of screen, then ricochet.
            if (ballYPos <= 0 || ballYPos >= height - ballR) {
                ballYSpeed *= -1;
            }

            // if player hits the ball, ricochet the ball back in other direction at a 90-degree angle
            if (ballXSpeed <= maxSpeed) {
                hitBall();
            }
        } else {
            if (!player.isReady()) {
                gc.setStroke(Color.WHITE);
                gc.strokeText("Click to ready up.", width / 2, height / 2);
            } else {
                gc.setStroke(Color.WHITE);
                gc.strokeText("Waiting for opponent to ready up.", width / 2, height / 2);
            }
        }
    }

    public static void main(String[] args) {
        Pong client = new Pong();
        launch();
    }
}