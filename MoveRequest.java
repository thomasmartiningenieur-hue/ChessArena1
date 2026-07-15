package com.chessarena.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {

    @NotBlank(message = "L'id de la partie est requis")
    private Long gameId;

    // Coup en notation UCI, ex: "e2e4", "e7e8q" (promotion)
    @NotBlank(message = "Le coup est requis (notation UCI, ex: e2e4)")
    private String uci;
}
