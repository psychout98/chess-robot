package org.example;


import lombok.*;
import org.example.exception.InvalidMoveException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Builder
@Getter
public class Board {
    
    private FEN fen;
    private boolean check;
    private boolean checkmate;
    private boolean stalemate;
    private Map<String, Move> moves;
    private Move lastMove;
    @Builder.Default
    private boolean shallow = false;
    private final static Boolean[][] kingMoves = {{false,true,true,true,false},{true,true,false,true,true},{false,true,true,true,false}};
    private final static Boolean[][] whitePawnMoves = {{false,true,false},{true,true,true},{false,false,false}};
    private final static Boolean[][] blackPawnMoves = {{false,false,false},{true,true,true},{false,true,false}};
    private final static Boolean[][] knightMoves = {{false,true,false,true,false},{true,false,false,false,true},{false,false,false,false,false},{true,false,false,false,true},{false,true,false,true,false}};
    private final static Boolean[][] bishopMoves = new Boolean[15][15];
    private final static Boolean[][] rookMoves = new Boolean[15][15];
    private final static Boolean[][] queenMoves = new Boolean[15][15];
    private final static HashMap<Character, Boolean[][]> moveMaps = new HashMap<>();
    private final static HashMap<Character, int[]> pieceLocations = new HashMap<>();
    static {
        pieceLocations.put('k', new int[]{1,2});
        pieceLocations.put('K', new int[]{1,2});
        pieceLocations.put('p', new int[]{0,1});
        pieceLocations.put('P', new int[]{2,1});
        pieceLocations.put('n', new int[]{2,2});
        pieceLocations.put('N', new int[]{2,2});
        pieceLocations.put('b', new int[]{7,7});
        pieceLocations.put('B', new int[]{7,7});
        pieceLocations.put('r', new int[]{7,7});
        pieceLocations.put('R', new int[]{7,7});
        pieceLocations.put('q', new int[]{7,7});
        pieceLocations.put('Q', new int[]{7,7});
        for (int i=0; i<15; i++) {
            for (int j=0; j<15; j++) {
                rookMoves[i][j] = false;
                bishopMoves[i][j] = false;
                queenMoves[i][j] = false;
            }
        }
        for (int i=0;i<15;i++) {
            for (int j=0;j<15;j++) {
                if (i == 7 && j != 7) {
                    rookMoves[i][j] = true;
                    queenMoves[i][j] = true;
                }
                if (j == 7 && i != 7) {
                    rookMoves[i][j] = true;
                    queenMoves[i][j] = true;
                }
                if (i == j && i != 7) {
                    bishopMoves[i][j] = true;
                    queenMoves[i][j] = true;
                }
                if (i == 14 - j && i != 7) {
                    bishopMoves[i][j] = true;
                    queenMoves[i][j] = true;
                }
            }
        }
        moveMaps.put('k', kingMoves);
        moveMaps.put('K', kingMoves);
        moveMaps.put('p', blackPawnMoves);
        moveMaps.put('P', whitePawnMoves);
        moveMaps.put('n', knightMoves);
        moveMaps.put('N', knightMoves);
        moveMaps.put('b', bishopMoves);
        moveMaps.put('B', bishopMoves);
        moveMaps.put('r', rookMoves);
        moveMaps.put('R', rookMoves);
        moveMaps.put('q', queenMoves);
        moveMaps.put('Q', queenMoves);
    }

    public static int[] spaceToSpace(String space) {
        return new int[]{56 - space.charAt(1), (char) (space.charAt(0) - 97)};
    }

    public static String spaceToSpace(int[] space) {
        return String.format("%s%s", (char) (space[1] + 97), (char) (56 - space[0]));
    }

    public void update() {
        moves = new HashMap<>();
        addMoves();
    }

    private void addMoves() {
        char[][] boardKey = fen.getBoardKey();
        for (int i=0;i<8;i++) {
            for (int j=0;j<8;j++) {
                if (boardKey[i][j] != 'x') {
                    Boolean[][] moveMap = moveMaps.get(boardKey[i][j]);
                    for (int k = 0; k < moveMap.length; k++) {
                        for (int l = 0; l < moveMap[k].length; l++) {
                            if (moveMap[k][l]) {
                                int[] location = pieceLocations.get(boardKey[i][j]);
                                int row = i + (k - location[0]);
                                int col = j + (l - location[1]);
                                if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                                    int[] moveArray = {i, j, row, col};
                                    Move move = new Move(boardKey[i][j], moveArray, fen);
                                    moves.put(move.getMoveCode(), move);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!shallow) {
            moves.values().stream().filter(Move::isValid).filter(Move::isMyMove).forEach(Move::generateFutures);
        }
    }

    public static char[][] copyBoardKey(char[][] boardKey) {
        char[][] copy = new char[8][8];
        for (int i=0;i<8;i++) {
            System.arraycopy(boardKey[i], 0, copy[i], 0, 8);
        }
        return copy;
    }

    private int[] moveCodeToMove(String moveCode) {
        int[] move = new int[4];
        for (int i=0; i<4; i++) {
            move[i] = moveCode.charAt(i) - '0';
        }
        return move;
    }

    public boolean checkCheck(boolean white) {
        return moves.values().stream().filter(move -> move.isWhite() != white).filter(Move::isValid)
                .anyMatch(move -> Objects.equals(fen.getBoardKey()[move.getEndRow()][move.getEndCol()], (white ? 'K' : 'k')));
    }

    public void move(String moveCode) {
        if (moveCode.length() == 4) {
            if (!Arrays.stream(moveCodeToMove(moveCode)).allMatch(i -> i < 8 && i >= 0)) {
                throw new InvalidMoveException(String.format("Unable to parse move code %s", moveCode));
            }
        } else {
            throw new InvalidMoveException(String.format("Move code has incorrect format %s", moveCode));
        }
        Move move = moves.get(moveCode);
        if (move != null && move.isValid() && move.isMyMove()) {
            lastMove = move;
            fen = new FEN(move.getFenString());
            update();
        } else {
            throw new InvalidMoveException("Invalid move: " + moveCode);
        }
    }
}
