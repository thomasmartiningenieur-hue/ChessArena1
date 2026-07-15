package com.chessarena.util;

import com.github.bhlangonijr.chesslib.Board;

public final class ChessUtils {

    private ChessUtils() {
        // classe utilitaire, pas d'instanciation
    }

    public static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static boolean isValidFen(String fen) {
        try {
            Board board = new Board();
            board.loadFromFen(fen);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isWhiteToMove(String fen) {
        return fen.split(" ")[1].equals("w");
    }
}
