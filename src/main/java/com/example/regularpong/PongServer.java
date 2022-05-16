package com.example.regularpong;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PongServer {
    private ServerSocket serverSock;
    private final int maxPlayers = 2;
    private int numPlayers;

    private volatile Socket p1Sock;
    private volatile Socket p2Sock;
    private ReadClient p1ReadRunnable;
    private ReadClient p2ReadRunnable;
    private WriteClient p1WriteRunnable;
    private WriteClient p2WriteRunnable;
    private volatile boolean p1Ready = false; // status of each player, received and sent to other player when ready
    private volatile boolean p2Ready = false;

    private double p1YPos;
    private double p2YPos;

    public PongServer() {
        System.out.println("PONG SERVER STARTED");
        numPlayers = 0;

        try {
            serverSock = new ServerSocket(6666);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IOException from PongServer Constructor.");
        }
    }

    public void acceptConnections() {
        try {
            System.out.println("Waiting for connections...");

            while (numPlayers < maxPlayers) {
                Socket sock = serverSock.accept();
                DataInputStream in = new DataInputStream(sock.getInputStream());
                DataOutputStream out = new DataOutputStream(sock.getOutputStream());

                numPlayers++;
                out.writeInt(numPlayers);
                System.out.println("Player " + numPlayers + " has connected.");

                if (numPlayers == 1) {
                    this.p1Sock = sock;
                    this.p1ReadRunnable = new ReadClient(numPlayers, in);
                    this.p1WriteRunnable = new WriteClient(numPlayers, out);
                    Thread tRead = new Thread(p1ReadRunnable);
                    tRead.start();

                } else {
                    this.p2Sock = sock;
                    this.p2ReadRunnable = new ReadClient(numPlayers, in);
                    this.p2WriteRunnable = new WriteClient(numPlayers, out);
                    Thread tRead = new Thread(p2ReadRunnable);
                    tRead.start();
                }
            }

            System.out.println("No longer accepting connections.");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IOException from acceptConnections.");
        }
    }

    // runnable that reads ready status and mouse position from a player
    private class ReadClient implements Runnable {
        private int playerID;
        private DataInputStream in;

        public ReadClient(int playerID, DataInputStream in) {
            this.playerID = playerID;
            this.in = in;
            System.out.println("RC Player " + this.playerID + " Runnable created");
        }

        @Override
        public void run() {
            try {
                while (p2Sock == null) {} // delay run until player 2 connects

                // Attempted to receive when each player clicks on their client to ready up
                while (true) {
                    if (this.playerID == 1 && !p1Ready) {
                        if (p2Sock.isConnected()) {
                            p1Ready = this.in.readBoolean();
                            System.out.println("Server received Player 1 ready: " + p1Ready);
                            Thread tWrite = new Thread(p1WriteRunnable);
                            tWrite.start();
                        }
                    } else if (this.playerID == 2 && !p2Ready) {
                        if (p1Sock.isConnected()) {
                            p2Ready = this.in.readBoolean();
                            System.out.println("Server received Player 2 ready: " + p2Ready);
                            Thread tWrite = new Thread(p2WriteRunnable);
                            tWrite.start();

                        }
                    }
                    break;
                }

                while (true) {
                    if (this.playerID == 1) {
                        p1YPos = this.in.readDouble();
                    } else {
                        p2YPos = this.in.readDouble();
                        //.out.println(p2YPos);
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException from ReadClient Runnable");
            }
        }
    }

    // runnable that writes player status and mouse position to player
    private class WriteClient implements Runnable {
        private int playerID;
        private DataOutputStream out;

        public WriteClient(int playerID, DataOutputStream out) {
            this.playerID = playerID;
            this.out = out;
            System.out.println("WC Player " + this.playerID + " Runnable created");
        }

        @Override
        public void run() {
            try {
                // Attempted to send each player when the other player is ready
                while (!p1Ready || !p2Ready) {} //delay until both players are ready
                if (this.playerID == 1) {
                    System.out.println("Server sent Player 1 p2Ready");
                    this.out.writeBoolean(p2Ready);
                    this.out.flush();
                } else if (this.playerID == 2) {
                    System.out.println("Server sent Player 2 p1Ready");
                    this.out.writeBoolean(p1Ready);
                    this.out.flush();
                }

                while (true) {
                    if (this.playerID == 1) {
                        out.writeDouble(p2YPos);
                    } else {
                        out.writeDouble(p1YPos);
                    }
                    out.flush();

                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        System.out.println("InterruptedException in WriteClient Runnable");
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException from WriteClient Runnable");
            }
        }
    }

    public static void main(String[] args) {
        PongServer ps = new PongServer();
        ps.acceptConnections();
    }
}
