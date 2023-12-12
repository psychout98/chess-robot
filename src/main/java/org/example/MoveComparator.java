package org.example;

import java.util.Comparator;
import java.util.Objects;

public class MoveComparator implements Comparator<Move> {
    @Override
    public int compare(Move m1, Move m2) {
        if (Objects.equals(m1.getPosition(), m2.getPosition())) {
            return 0;
        }else if (m1.getTotalAdvantage() >= m2.getTotalAdvantage()) {
            return 1;
        } else {
            return -1;
        }
    }
}
