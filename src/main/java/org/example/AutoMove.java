package org.example;


import org.teavm.jso.JSBody;

public final class AutoMove {

    public static void main(String[] args) {
        FEN fen = new FEN(args[0]);
        int[] moveArray = new int[4];
        for (int i=0; i<4; i++) {
            moveArray[i] = args[1].charAt(i) - 48;
        }
        Move firstMove = new Move(fen, moveArray);
        Move bestMove = firstMove.findBestFuture(Integer.parseInt(args[2]));
        move(bestMove.getMoveCode());
    }

    @JSBody(params = { "moveCode" }, script = "window[\"move\"](moveCode)")
    public static native void move(String move);

}
