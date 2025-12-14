package fr.upec.episen.tp.paas.watch_dog.service;

import fr.upec.episen.tp.paas.watch_dog.config.WatchDogProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Service responsable de l’exécution de commandes à distance via SSH.
 * Utilisé par le WatchDog pour relancer un service (ex: docker restart)
 * sur une VM distante.
 */
@Slf4j
@Service
public class SshCommandService {

    /**
     * Exécute une commande distante via SSH sur une instance donnée.
     *
     * @param inst       instance cible (user, ip, port)
     * @param command    commande à exécuter (ex: docker restart ...)
     * @param timeoutMs  timeout maximum de l’exécution
     * @return résultat structuré de l’exécution
     */
    public CommandResult run(WatchDogProperties.InstanceConfig inst, String command, long timeoutMs) {

        // Vérification de la présence de la commande
        // Évite de lancer une commande SSH vide ou invalide
        if (command == null || command.isBlank()) {
            return CommandResult.failure("restartCommand is empty");
        }

        // Construction de la cible SSH : user@ip
        // Exemple : vm-operations-core@172.31.249.170
        String target = inst.getSshUser() + "@" + inst.getIp();

        // Construction de la commande SSH via ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder(
                "ssh",
                "-p", String.valueOf(inst.getSshPort()),

                // Mode non interactif : aucune demande de mot de passe
                "-o", "BatchMode=yes",

                // Désactive la confirmation de clé SSH (nécessaire pour l’automatisation)
                "-o", "StrictHostKeyChecking=no",

                // Timeout de connexion SSH (en secondes, min = 1)
                "-o", "ConnectTimeout=" + Math.max(1, timeoutMs / 1000),

                // Cible SSH
                target,

                // Commande distante à exécuter
                command
        );

        try {
            // Début de mesure du temps d’exécution
            long start = System.currentTimeMillis();

            // Lancement du process SSH
            Process p = pb.start();

            // Attente de la fin de l’exécution avec timeout global
            boolean finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            // Si le process dépasse le timeout, on le force à s’arrêter
            if (!finished) {
                p.destroyForcibly();
                return CommandResult.failure("timeout after " + timeoutMs + "ms");
            }

            // Code de sortie de la commande
            int exit = p.exitValue();

            // Lecture de la sortie standard (stdout)
            String stdout = new String(
                    p.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).trim();

            // Lecture de la sortie d’erreur (stderr)
            String stderr = new String(
                    p.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).trim();

            // Calcul du temps total d’exécution
            long durationMs = System.currentTimeMillis() - start;

            // Code 0 = succès
            if (exit == 0) {
                return CommandResult.success(stdout, durationMs);
            }

            // Code différent de 0 = échec
            return CommandResult.failure(
                    "exit=" + exit + " stderr=" + stderr,
                    stdout,
                    durationMs
            );

        } catch (Exception e) {
            // Gestion des exceptions (SSH indisponible, IO, interruption, etc.)
            return CommandResult.failure(
                    "exception: " + e.getClass().getSimpleName() + " " + e.getMessage()
            );
        }
    }

    /**
     * Objet résultat représentant l’exécution d’une commande SSH.
     * Utilisé par le scheduler pour décider des actions suivantes.
     */
    public record CommandResult(
            boolean ok,        // succès ou échec
            String message,    // message de statut
            String stdout,     // sortie standard
            long durationMs   // durée d’exécution
    ) {

        // Résultat de succès
        public static CommandResult success(String stdout, long durationMs) {
            return new CommandResult(true, "OK", stdout, durationMs);
        }

        // Résultat d’échec simple
        public static CommandResult failure(String message) {
            return new CommandResult(false, message, "", 0);
        }

        // Résultat d’échec avec détails
        public static CommandResult failure(String message, String stdout, long durationMs) {
            return new CommandResult(false, message, stdout, durationMs);
        }
    }
}