package org.example;


import lombok.*;
import org.example.exception.InvalidMoveException;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Move {

    private final String moveCode;
    private String moveString;
    private boolean valid;
    private final boolean myMove;
    private final char key;
    private final boolean white;
    private final int startRow;
    private final int endRow;
    private final int startCol;
    private final int endCol;
    private String fenString;
    private final String position;
    private final FEN previousFen;
    private final char endKey;
    private boolean castleMove;
    private boolean enPassant;
    private boolean pushTwo;
    private final TreeSet<Move> futures;
    private int materialAdvantage;
    private int strategicAdvantage;
    private int positionAdvantage;
    private int totalAdvantage;
    private static final String queensAndRooksAndPawns = "qQrRpP";
    private static final String queensAndBishops = "qQbB";
    private static final String kingsAndKnights = "kKnN";
    private static final HashMap<String, String> castle = new HashMap<>();
    private static final int[] gradient = {0, 1, 2, 4, 4, 2, 1, 0};
    private static final HashMap<Character, Integer> pointValues = new HashMap<>();
    static {
        castle.put("0402", "q");
        castle.put("0406", "k");
        castle.put("7472", "Q");
        castle.put("7476", "K");
        pointValues.put('q', -2521);
        pointValues.put('Q', 2521);
        pointValues.put('r', -1270);
        pointValues.put('R', 1270);
        pointValues.put('b', -836);
        pointValues.put('B', 836);
        pointValues.put('n', -817);
        pointValues.put('N', 817);
        pointValues.put('p', -198);
        pointValues.put('P', 198);
        pointValues.put('k', -300);
        pointValues.put('K', 300);
        pointValues.put('x', 0);
    }
    private static final Comparator<Move> moveComparator = new MoveComparator();
    private static final Runtime rt = Runtime.getRuntime();

    public Move(final FEN previousFen, final int[] moveArray) {
        this.previousFen = previousFen;
        this.key = previousFen.getBoardKey()[moveArray[0]][moveArray[1]];
        white = Character.isUpperCase(key);
        myMove = white == previousFen.isWhiteToMove();
        enPassant = false;
        moveString = "";
        startRow = moveArray[0];
        startCol = moveArray[1];
        endRow = moveArray[2];
        endCol = moveArray[3];
        castleMove = false;
        pushTwo = false;
        moveCode = String.format("%s%s%s%s", startRow, startCol, endRow, endCol);
        boolean pawnMove = key == 'p' || key == 'P';
        char[][] boardKey = Board.copyBoardKey(previousFen.getBoardKey());
        endKey = boardKey[endRow][endCol];
        fenString = previousFen.getFen();
        boolean free = endKey == 'x';
        valid = !isObstructed(boardKey);
        moveString += pawnMove ? (startCol == endCol ? "" : (char) (startCol + 97)) : (white ? Character.toLowerCase(key) : key);
        if (pawnMove) {
            runPawnMove(boardKey, free, previousFen.getEnPassantTarget());
        } else if ((key == 'k' || key == 'K') && castle.get(moveCode) != null && previousFen.getCastles().contains(castle.get(moveCode))) {
            runCastle(boardKey);
        } else if ((key == 'k' || key == 'K') && Math.abs(endCol - startCol) == 2) {
            valid = false;
            runBasicMove(boardKey, free);
        } else {
            runBasicMove(boardKey, free);
        }
        fenString = FEN.updateFEN(previousFen, boardKey, key, endCol, pushTwo ? enPassantTarget() : "-");
        position = FEN.getBoardField(fenString);
        futures = new TreeSet<>(moveComparator);
        calculateMaterialAdvantage();
        calculatePositionAdvantage();
        strategicAdvantage = 0;
        calculateTotalAdvantage();
    }

    private String enPassantTarget() {
        return Board.spaceToSpace(new int[]{white ? startRow - 1 : startRow + 1, startCol});
    }

    private boolean isObstructed(char[][] boardKey) {
        boolean obstructed = false;
        boolean open = endKey == 'x' || (Character.isLowerCase(key) != Character.isLowerCase(endKey));
        if (kingsAndKnights.contains(String.valueOf(key))) {
            obstructed = !open;
        }
        if (queensAndBishops.contains(String.valueOf(key))) {
            obstructed = diagonalObstruction(boardKey) || !open;
        }
        if (queensAndRooksAndPawns.contains(String.valueOf(key))) {
            obstructed = obstructed || (straightObstruction(boardKey) || !open);
        }
        return obstructed;
    }

    private boolean diagonalObstruction(char[][] boardKey) {
        int vertical = endRow - startRow;
        int horizontal = endCol - startCol;
        if (Math.abs(vertical) != Math.abs(horizontal)) {
            return false;
        } else if (vertical > 0 && horizontal > 0) {
            for (int i=1; i<vertical; i++) {
                if (boardKey[startRow + i][startCol + i] != 'x') {
                    return true;
                }
            }
        } else if (vertical > 0 && horizontal < 0) {
            for (int i=1; i<vertical; i++) {
                if (boardKey[startRow + i][startCol - i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0 && horizontal > 0) {
            for (int i=-1; i>vertical; i--) {
                if (boardKey[startRow + i][startCol - i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0 && horizontal < 0) {
            for (int i=-1; i>vertical; i--) {
                if (boardKey[startRow + i][startCol + i] != 'x') {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean straightObstruction(char[][] boardKey) {
        int vertical = endRow - startRow;
        int horizontal = endCol - startCol;
        if (vertical != 0 && horizontal != 0) {
            return false;
        } else if (vertical == 0 && horizontal < 0) {
            for (int i=endCol + 1; i<startCol; i++) {
                if (boardKey[startRow][i] != 'x') {
                    return true;
                }
            }
        } else if (vertical == 0 && horizontal > 0){
            for (int i=startCol + 1; i<endCol; i++) {
                if (boardKey[startRow][i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0) {
            for (int i=endRow + 1; i<startRow; i++) {
                if (boardKey[i][startCol] != 'x') {
                    return true;
                }
            }
        } else {
            for (int i=startRow + 1; i<endRow; i++) {
                if (boardKey[i][startCol] != 'x') {
                    return true;
                }
            }
        }
        return false;
    }

    private void runBasicMove(char[][] boardKey, boolean free) {
        if (!free) {
            moveString += "x";
        }
        moveString += (char) (endCol + 97);
        moveString += 8 - endRow;
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
    }

    private void runPawnMove(char[][] boardKey, boolean free, int[] enPassantTarget) {
        if (Math.abs(endRow - startRow) == 2) {
            pushTwo = true;
            valid = valid && endKey == 'x' && (white ? startRow == 6 : startRow == 1);
            runBasicMove(boardKey, free);
        } else if (startCol != endCol) {
            if (isEnPassant(enPassantTarget)) {
                runEnPassant(boardKey);
            } else {
                valid = valid && endKey != 'x' && Character.isLowerCase(key) != Character.isLowerCase(endKey);
                if (endRow == (white ? 0 : 7)) {
                    runQueenPromotion(boardKey, free);
                } else {
                    runBasicMove(boardKey, free);
                }
            }
        } else {
            valid = valid && endKey == 'x';
            if (endRow == (white ? 0 : 7)) {
                runQueenPromotion(boardKey, free);
            } else {
                runBasicMove(boardKey, free);
            }
        }
    }

    private void runEnPassant(char[][] boardKey) {
        int targetRow = white ? endRow + 1 : endRow - 1;
        enPassant = true;
        moveString += "x";
        moveString += (char) (endCol + 97);
        moveString += (targetRow + 1);
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[targetRow][endCol] = 'x';
    }

    private void runQueenPromotion(char[][] boardKey, boolean free) {
        if (!free) {
            moveString += "x";
        }
        moveString += (char) (endCol + 97);
        moveString += 8 - endRow;
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = white ? 'Q' : 'q';
    }

    private void runCastle(char[][] boardKey) {
        for (int[] space : Castle.castleRoutes.get(moveCode)) {
            if (boardKey[space[0]][space[1]] != 'x') {
                valid = false;
                break;
            }
        }
        castleMove = true;
        moveString = Castle.castleMoveString.get(moveCode);
        int[] rookMove = Castle.castleRookMove.get(moveCode);
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[rookMove[0]][rookMove[1]] = 'x';
        boardKey[rookMove[2]][rookMove[3]] = white ? 'R' : 'r';
    }

    public boolean isEnPassant(int[] enPassantTarget) {
        if (enPassantTarget == null) {
            return false;
        } else {
            boolean attack = startCol != endCol;
            return attack && endRow == enPassantTarget[0] && endCol == enPassantTarget[1];
        }
    }

    public void generateFutures() {
        Board copyBoard = Board.builder()
                .fen(previousFen)
                .shallow(true)
                .build();
        copyBoard.update();
        try {
            copyBoard.move(moveCode);
            if (castleMove) {
                valid = valid && copyBoard.getMoves().values().stream()
                        .filter(future -> future.valid && future.myMove)
                        .noneMatch(future -> Arrays.stream(Castle.castleSpaces.get(moveCode))
                                .anyMatch(dest -> future.endRow == dest[0] && future.endCol == dest[1]));
            } else {
                valid = valid && !copyBoard.checkCheck(white);
            }
        } catch (InvalidMoveException e) {
            valid = false;
        }
        futures.addAll(copyBoard.getMoves().values().stream().filter(Move::isValid).collect(Collectors.toSet()));
        calculateStrategicAdvantage();
        futures.removeIf(future -> !future.myMove);
        calculateTotalAdvantage();
    }

    private void calculateStrategicAdvantage() {
        int attacks = futures.stream()
                .filter(Move::isValid)
                .mapToInt(Move::getPositionAdvantage).sum();
        int kingQueenFactor = castleMove ? 500 : key == 'k' || key == 'K' || key == 'q' || key == 'Q' ? -100 : 0;

        strategicAdvantage = ((white ? 1 : -1) * kingQueenFactor) + attacks;
    }

    private void calculatePositionAdvantage() {
        positionAdvantage = (white ? 1 : -1) * (gradient[endRow] + gradient[endCol]);
    }

    private void calculateMaterialAdvantage() {
        materialAdvantage = 0;
        String[] rows = fenString.split(" ")[0].split("/");
        for (String row : rows) {
            for (char key : row.toCharArray()) {
                materialAdvantage += calculatePoints(key);
            }
        }
    }

    private static int calculatePoints(char key) {
        return Objects.requireNonNullElse(pointValues.get(key), 0);
    }

    public void buildTree(int branchDepth, int maxDepth, HashMap<String, Move> positionMap) {
        if (branchDepth < maxDepth) {
            Iterator<Move> iterator = white ? futures.descendingIterator() : futures.iterator();
            TreeSet<Move> builtFutures = new TreeSet<>(moveComparator);
            iterator.forEachRemaining(future -> {
                Move mappedPosition = positionMap.get(future.position);
                if (mappedPosition == null) {
                    if (future.futures.isEmpty()) {
                        if (memoryUsage() < 0.9) {
                            future.generateFutures();
                        } else {
                            throw new OutOfMemoryError("Excessive memory usage");
                        }
                    }
                    if (future.valid && future.myMove && (builtFutures.isEmpty() || isViableMove(future, builtFutures))) {
                        future.buildTree(branchDepth + 1, maxDepth, positionMap);
                        builtFutures.add(future);
                    }
                } else {
                    builtFutures.add(mappedPosition);
                }
            });
            futures.clear();
            futures.addAll(builtFutures);
            try {
                totalAdvantage = white ? futures.last().totalAdvantage : futures.first().totalAdvantage;
            } catch (NoSuchElementException ignored) {
                totalAdvantage = white ? -100 : 100;
            }
        }
    }

    private static boolean isViableMove(Move future, TreeSet<Move> builtFutures) {
        if (future.white) {
            return future.totalAdvantage >= builtFutures.last().totalAdvantage;
        } else {
            return future.totalAdvantage <= builtFutures.first().totalAdvantage;
        }
    }

    public Move findBestFuture(int maxDepth) {
        HashMap<String, Move> positionMap = new HashMap<>();
        if (futures.isEmpty()) {
            generateFutures();
        }
        if (futures.size() == 1) {
            return futures.first();
        }
        buildTree(0, maxDepth, positionMap);
        if (futures.isEmpty()) {
            return null;
        } else {
            return white ? futures.first() : futures.last();
        }
    }

    public int sumFutures() {
        if (futures.isEmpty()) {
            return 0;
        } else {
            return futures.size() + futures.stream().map(Move::sumFutures).mapToInt(Integer::intValue).sum();
        }
    }

    public double memoryUsage() {
        long totalMemory = rt.totalMemory();
        long usedMemory = totalMemory - rt.freeMemory();
        return (double) usedMemory / totalMemory;
    }

    public void calculateTotalAdvantage() {
        totalAdvantage = materialAdvantage + strategicAdvantage;
    }

}
