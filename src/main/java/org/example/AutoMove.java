package org.example;


import org.teavm.jso.JSBody;

public final class AutoMove extends Thread {


    private final Move firstMove;
    private Move bestMove;
    private int level;

    public AutoMove(String[] args) {
        FEN fen = new FEN(args[0]);
        int[] moveArray = new int[4];
        for (int i=0; i<4; i++) {
            moveArray[i] = args[1].charAt(i) - 48;
        }
        firstMove = new Move(args[2].charAt(0), moveArray, fen);
        level = Integer.parseInt(args[3]);
        bestMove = firstMove.findBestFuture(0);
    }

    public static void main(String[] args) throws InterruptedException {
        AutoMove main = new AutoMove(args);
        long level = Integer.parseInt(args[3]);
        main.start();
        main.join(level * 1000);
        main.interrupt();
        move(main.bestMove.getMoveCode());
    }

    @Override
    public void run() {
        for (int i=1; i<=level; i++) {
            bestMove = firstMove.findBestFuture(i);
        }
    }

    @JSBody(params = { "moveCode" }, script = "window[\"move\"](moveCode)")
    public static native void move(String move);

}
