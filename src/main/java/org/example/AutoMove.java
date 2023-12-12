package org.example;


import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

public final class AutoMove extends Thread {

    private static final HTMLDocument document = Window.current().getDocument();

    private final Move firstMove;
    private int depth;

    public AutoMove(String[] args) {
        FEN fen = new FEN(args[0]);
        int[] moveArray = new int[4];
        for (int i=0; i<4; i++) {
            moveArray[i] = args[1].charAt(i) - 48;
        }
        firstMove = new Move(args[2].charAt(0), moveArray, fen);
        depth = Integer.parseInt(args[3]);
    }

    public static void main(String[] args) throws InterruptedException {
        AutoMove main = new AutoMove(args);
        main.start();
        while (main.depth > 0) {
            Thread.sleep(10000);
            if (main.isAlive()) {
                main.interrupt();
                main.depth--;
                main.start();
            } else {
                main.depth = 0;
            }
        }
    }

    @Override
    public void run() {
        autoMove(firstMove, depth);
    }
    private static void autoMove(Move firstMove, int depth) {
        Move move = firstMove.findBestFuture(depth);
        System.out.println(move.getMoveString());
        move(move.getMoveCode());
    }

    @JSBody(params = { "moveCode" }, script = "window[\"move\"](moveCode)")
    public static native void move(String move);

//    public static void refresh(Move move) {
//        String root = String.format("%s (%s)", move.getMoveString(), move.getTotalAdvantage());
//        String[] branches = new String[move.getFutures().size()];
//        for (int i=0; i<branches.length; i++) {
//            Move future = move.getFutures().get(i);
//            branches[i] = String.format("%s (%s)", future.getMoveString(), future.getTotalAdvantage());
//        }
//        HTMLElement treeView = document.getElementById("tree-view");
//        StringBuilder elements = new StringBuilder();
//        elements.append("<div className=\"flex flex-row\">");
//        for (String branch : branches) {
//            elements.append(String.format("<div className=\"flex p-3\">%s</div>", branch));
//        }
//        elements.append("</div>");
//        treeView.setInnerHTML(root + elements);
//    }
}
