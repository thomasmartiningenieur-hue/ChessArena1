import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';

import { ChessBoardComponent } from '../../components/chess-board/chess-board.component';
import { MoveHistoryComponent, HistoryEntry } from '../../components/move-history/move-history.component';
import { ChessService } from '../../services/chess.service';
import { WebsocketService } from '../../services/websocket.service';
import { AuthService } from '../../services/auth.service';
import { ChessGame } from '../../models/game.model';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule, ChessBoardComponent, MoveHistoryComponent],
  template: `
    @if (game) {
      <div class="game-page">
        <app-chess-board [fen]="game.fen" (moveMade)="onMoveMade($event)"></app-chess-board>
        <app-move-history [moves]="history"></app-move-history>
      </div>
      <p class="status">Statut : {{ game.status }} — Resultat : {{ game.result }}</p>
    }
  `,
  styles: [`
    .game-page { display: flex; gap: 2rem; justify-content: center; margin-top: 2rem; }
    .status { text-align: center; margin-top: 1rem; }
  `]
})
export class GameComponent implements OnInit, OnDestroy {
  game: ChessGame | null = null;
  history: HistoryEntry[] = [];
  gameId!: number;
  private wsSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private chessService: ChessService,
    private wsService: WebsocketService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.gameId = Number(this.route.snapshot.paramMap.get('id'));

    this.chessService.getGame(this.gameId).subscribe((game) => (this.game = game));

    this.wsService.connect().then(() => {
      this.wsSubscription = this.wsService.watchGame(this.gameId).subscribe((update) => {
        if (this.game) {
          this.game.fen = update.fen;
          this.game.status = update.status;
          this.game.result = update.result;
        }
        if (update.lastMoveSan) {
          this.history.push({ moveNumber: this.history.length + 1, san: update.lastMoveSan });
        }
      });
    });
  }

  onMoveMade(uci: string): void {
    const username = this.authService.currentUsername();
    if (username) {
      this.wsService.sendMove(this.gameId, uci, username);
    }
  }

  ngOnDestroy(): void {
    this.wsSubscription?.unsubscribe();
    this.wsService.disconnect();
  }
}
