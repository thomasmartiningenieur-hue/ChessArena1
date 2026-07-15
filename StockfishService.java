package com.chessarena.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gère un unique processus Stockfish et communique avec lui via le protocole UCI.
 * Le binaire doit être installé sur le système (voir Dockerfile) — jamais commité dans le repo.
 */
@Slf4j
@Service
public class StockfishService {

    @Value("${app.stockfish.path:/usr/games/stockfish}")
    private String stockfishPath;

    private Process engineProcess;
    private BufferedReader reader;
    private Writer writer;
    private final ReentrantLock lock = new ReentrantLock();

    public synchronized void start() {
        if (engineProcess != null && engineProcess.isAlive()) {
            return;
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(stockfishPath);
            builder.redirectErrorStream(true);
            engineProcess = builder.start();
            reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
            writer = new OutputStreamWriter(engineProcess.getOutputStream());

            sendCommand("uci");
            waitFor("uciok");
            sendCommand("isready");
            waitFor("readyok");
            log.info("Moteur Stockfish démarré depuis {}", stockfishPath);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de démarrer Stockfish : " + e.getMessage(), e);
        }
    }

    /**
     * Demande à Stockfish le meilleur coup pour une position FEN donnée.
     *
     * @param fen           position actuelle
     * @param depth         profondeur de recherche (ex: 10-15 pour un niveau raisonnable)
     * @return le meilleur coup en notation UCI, ex "e2e4"
     */
    public String getBestMove(String fen, int depth) {
        lock.lock();
        try {
            ensureStarted();
            sendCommand("position fen " + fen);
            sendCommand("go depth " + depth);

            String line;
            String bestMove = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    bestMove = line.split(" ")[1];
                    break;
                }
            }
            return bestMove;
        } catch (IOException e) {
            throw new IllegalStateException("Erreur de communication avec Stockfish : " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Renvoie une évaluation de la position (en centipawns du point de vue des blancs),
     * utile pour de l'analyse post-partie plutôt que du jeu en temps réel.
     */
    public int evaluatePosition(String fen, int depth) {
        lock.lock();
        try {
            ensureStarted();
            sendCommand("position fen " + fen);
            sendCommand("go depth " + depth);

            String line;
            int lastCp = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("score cp")) {
                    String[] parts = line.split("score cp");
                    lastCp = Integer.parseInt(parts[1].trim().split(" ")[0]);
                }
                if (line.startsWith("bestmove")) {
                    break;
                }
            }
            return lastCp;
        } catch (IOException e) {
            throw new IllegalStateException("Erreur de communication avec Stockfish : " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private void ensureStarted() {
        if (engineProcess == null || !engineProcess.isAlive()) {
            start();
        }
    }

    private void sendCommand(String command) throws IOException {
        writer.write(command + "\n");
        writer.flush();
    }

    private void waitFor(String expected) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().equals(expected)) {
                return;
            }
        }
    }

    @PreDestroy
    public void stop() {
        if (engineProcess != null && engineProcess.isAlive()) {
            try {
                sendCommand("quit");
            } catch (IOException ignored) {
                // le process va être détruit de toute façon
            }
            engineProcess.destroy();
        }
    }
}
