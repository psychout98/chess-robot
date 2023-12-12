package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TreeView extends JFrame implements ActionListener {

    HashMap<String, JButton> buttons = new HashMap<>();
    JPanel centerPanel = new JPanel();
    JPanel northPanel = new JPanel();
    JButton backButton = new JButton("root");
    Move move;
    Stack<Move> lastMove;
    Map<String, Move> futures;
    JLabel treeSize = new JLabel();
    int depth = 1;
    JButton run = new JButton("Run at depth 1");
    JButton flip = new JButton("board");
    boolean flipped = false;


    public TreeView(Move move) {
        super("tree");
        this.move = move;
        lastMove = new Stack<>();
        setSize(1000, 800);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        flip.addActionListener(this);
        centerPanel.setLayout(new GridLayout(8, 8));
        run.addActionListener(this);
        northPanel.add(run);
        backButton.addActionListener(this);
        northPanel.add(backButton);
        northPanel.add(treeSize);
        northPanel.add(flip);
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(northPanel, BorderLayout.NORTH);
        treeSize.setText("Nodes: " + move.sumFutures());
        displayFutures();
    }

    public static void main(String[] args) {
        FEN fen = new FEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        int[] moveArray = {6, 4, 4, 4};
        Move firstMove = new Move('P', moveArray, fen);
        firstMove.generateFutures();
        new TreeView(firstMove);
    }

    public void displayBoard() {
        centerPanel.removeAll();
        centerPanel.add(new JLabel(move.getFenString()));
        SwingUtilities.updateComponentTreeUI(this);
    }

    public void displayFutures() {
        if (move != null) {
            treeSize.setText("Nodes: " + move.sumFutures());
            setTitle(move.getMoveString() + "(" + String.format("%s, %s, %s, %s",
                    move.getTotalAdvantage(),
                    move.getMaterialAdvantage(),
                    move.getPositionAdvantage(),
                    move.getStrategicAdvantage()) + ") " +
                    (move.isWhite() ? "black to move" : "white to move"));
            if (!lastMove.isEmpty()) {
                backButton.setText("go back to " + lastMove.lastElement().getMoveString());
            } else {
                backButton.setText("root");
            }
            centerPanel.removeAll();
            buttons = new HashMap<>();
            futures = move.getFutures().stream().collect(Collectors.toMap(Move::getMoveCode, Function.identity()));
            for (Move future : move.getFutures()) {
                JButton futureButton = new JButton(future.getMoveString() + " (" + String.format("%s", future.getTotalAdvantage()) + ")");
                if (!future.getFutures().isEmpty()) {
                    futureButton.addActionListener(this);
                }
                buttons.put(future.getMoveCode(), futureButton);
                centerPanel.add(futureButton);
            }
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (String key : buttons.keySet()) {
            if (e.getSource().equals(buttons.get(key))) {
                lastMove.push(move);
                move = futures.get(key);
                displayFutures();
            }
        }
        if (e.getSource().equals(backButton) && !lastMove.isEmpty()) {
            move = lastMove.pop();
            displayFutures();
        }
        if (e.getSource().equals(run)) {
            move.findBestFuture(depth);
            depth++;
            run.setText("Run at depth " + depth);
            displayFutures();
        }
        if (e.getSource().equals(flip)) {
            if (flipped) {
                flip.setText("board");
                displayFutures();
            } else {
                flip.setText("tree");
                displayBoard();
            }
            flipped = !flipped;
        }
    }
}
