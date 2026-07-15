package com.chessarena.websocket;

import com.chessarena.dto.GameResponse;
import com.chessarena.dto.MoveRequest;
import com.chessarena.service.ChessGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Reçoit les coups joués via STOMP sur /app/games/{gameId}/move
 * et diffuse le nouvel état de la partie à tous les abonnés de /topic/games/{gameId}.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChessSocketController {

    private final ChessGameService chessGameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/games/{gameId}/move")
    public void handleMove(@DestinationVariable Long gameId, @Payload MoveRequest moveRequest) {
        try {
            GameResponse updatedGame = chessGameService.applyMove(gameId, moveRequest.getUci());
            messagingTemplate.convertAndSend("/topic/games/" + gameId, updatedGame);
        } catch (IllegalArgumentException e) {
            // On notifie uniquement l'auteur du coup invalide, pas les autres abonnés
            messagingTemplate.convertAndSend("/topic/games/" + gameId + "/errors", e.getMessage());
            log.warn("Coup invalide reçu pour la partie {} : {}", gameId, e.getMessage());
        }
    }
}
