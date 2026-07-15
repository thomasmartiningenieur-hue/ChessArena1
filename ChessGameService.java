package com.chessarena.service;

import com.chessarena.dto.GameResponse;
import com.chessarena.exception.GameNotFoundException;
import com.chessarena.exception.PlayerNotFoundException;
import com.chessarena.model.ChessGame;
import com.chessarena.model.Move;
import com.chessarena.model.Player;
import com.chessarena.repository.ChessGameRepository;
import com.chessarena.repository.MoveRepository;
import com.chessarena.repository.PlayerRepository;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import com.github.bhlangonijr.chesslib.move.MoveList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChessGameService {

    private final ChessGameRepository chessGameRepository;
    private final MoveRepository moveRepository;
    private final PlayerRepository playerRepository;

    @Transactional
    public GameResponse createGame(Long whitePlayerId, Long blackPlayerId) {
        Player white = playerRepository.findById(whitePlayerId)
                .orElseThrow(() -> new PlayerNotFoundException(whitePlayerId));
        Player black = playerRepository.findById(blackPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException(blackPlayerId));

        ChessGame game = ChessGame.builder()
                .whitePlayer(white)
                .blackPlayer(black)
                .status(ChessGame.GameStatus.IN_PROGRESS)
                .build();

        chessGameRepository.save(game);
        return toResponse(game);
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(Long gameId) {
        ChessGame game = chessGameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        return toResponse(game);
    }

    /**
     * Applique un coup (notation UCI, ex "e2e4") sur une partie existante,
     * en validant sa légalité avec chesslib avant de le persister.
     */
    @Transactional
    public GameResponse applyMove(Long gameId, String uci) {
        ChessGame game = chessGameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (game.getStatus() != ChessGame.GameStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Cette partie n'est plus en cours");
        }

        Board board = new Board();
        board.loadFromFen(game.getFen());

        com.github.bhlangonijr.chesslib.move.Move chessMove = parseUciMove(board, uci);

        if (!isLegal(board, chessMove)) {
            throw new IllegalArgumentException("Coup illégal : " + uci);
        }

        board.doMove(chessMove);

        int moveNumber = game.getMoves().size() + 1;
        Move move = Move.builder()
                .game(game)
                .moveNumber(moveNumber)
                .uci(uci)
                .san(chessMove.toString())
                .fenAfter(board.getFen())
                .build();

        game.getMoves().add(move);
        game.setFen(board.getFen());
        moveRepository.save(move);

        applyGameEndConditions(game, board);
        chessGameRepository.save(game);

        return toResponse(game);
    }

    private void applyGameEndConditions(ChessGame game, Board board) {
        if (board.isMated()) {
            game.setStatus(ChessGame.GameStatus.FINISHED);
            game.setResult(board.getSideToMove().toString().equals("WHITE")
                    ? ChessGame.GameResult.BLACK_WINS
                    : ChessGame.GameResult.WHITE_WINS);
            game.setFinishedAt(Instant.now());
        } else if (board.isDraw() || board.isStaleMate()) {
            game.setStatus(ChessGame.GameStatus.FINISHED);
            game.setResult(ChessGame.GameResult.DRAW);
            game.setFinishedAt(Instant.now());
        }
    }

    private boolean isLegal(Board board, com.github.bhlangonijr.chesslib.move.Move move) {
        MoveList legalMoves = MoveGenerator.generateLegalMoves(board);
        return legalMoves.contains(move);
    }

    private com.github.bhlangonijr.chesslib.move.Move parseUciMove(Board board, String uci) {
        try {
            return new com.github.bhlangonijr.chesslib.move.Move(uci, board.getSideToMove());
        } catch (Exception e) {
            throw new IllegalArgumentException("Format de coup invalide (attendu UCI, ex: e2e4) : " + uci);
        }
    }

    private GameResponse toResponse(ChessGame game) {
        List<String> movesUci = game.getMoves().stream()
                .map(Move::getUci)
                .collect(Collectors.toList());

        return GameResponse.builder()
                .id(game.getId())
                .whitePlayerId(game.getWhitePlayer().getId())
                .whitePlayerUsername(game.getWhitePlayer().getUsername())
                .blackPlayerId(game.getBlackPlayer().getId())
                .blackPlayerUsername(game.getBlackPlayer().getUsername())
                .fen(game.getFen())
                .status(game.getStatus())
                .result(game.getResult())
                .movesUci(movesUci)
                .createdAt(game.getCreatedAt())
                .build();
    }
}
