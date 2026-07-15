package com.chessarena.dto;

import com.chessarena.model.ChessGame;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {

    private Long id;
    private Long whitePlayerId;
    private String whitePlayerUsername;
    private Long blackPlayerId;
    private String blackPlayerUsername;
    private String fen;
    private ChessGame.GameStatus status;
    private ChessGame.GameResult result;
    private List<String> movesUci;
    private Instant createdAt;
}
