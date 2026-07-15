package com.chessarena.controller;

import com.chessarena.dto.GameResponse;
import com.chessarena.dto.MoveRequest;
import com.chessarena.service.ChessGameService;
import com.chessarena.service.StockfishService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Tag(name = "Parties", description = "Création, consultation et gestion des parties d'échecs")
public class ChessGameController {

    private final ChessGameService chessGameService;
    private final StockfishService stockfishService;

    @PostMapping
    public ResponseEntity<GameResponse> createGame(@RequestParam Long whitePlayerId,
                                                     @RequestParam Long blackPlayerId) {
        GameResponse response = chessGameService.createGame(whitePlayerId, blackPlayerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable Long gameId) {
        return ResponseEntity.ok(chessGameService.getGame(gameId));
    }

    @PostMapping("/{gameId}/moves")
    public ResponseEntity<GameResponse> playMove(@PathVariable Long gameId,
                                                   @Valid @RequestBody MoveRequest request) {
        GameResponse response = chessGameService.applyMove(gameId, request.getUci());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameId}/best-move")
    public ResponseEntity<String> suggestBestMove(@PathVariable Long gameId,
                                                    @RequestParam(defaultValue = "12") int depth) {
        GameResponse game = chessGameService.getGame(gameId);
        String bestMove = stockfishService.getBestMove(game.getFen(), depth);
        return ResponseEntity.ok(bestMove);
    }
}
