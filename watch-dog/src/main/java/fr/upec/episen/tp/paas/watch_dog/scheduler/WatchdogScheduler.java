package fr.upec.episen.tp.paas.watch_dog.scheduler;

import fr.upec.episen.tp.paas.watch_dog.config.WatchDogProperties;
import fr.upec.episen.tp.paas.watch_dog.model.InstanceState;
import fr.upec.episen.tp.paas.watch_dog.service.HttpHealthProbeService;
import fr.upec.episen.tp.paas.watch_dog.service.SshProbeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import fr.upec.episen.tp.paas.watch_dog.service.SshCommandService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchdogScheduler {

    private final WatchDogProperties props;
    private final SshProbeService sshProbeService;
    private final HttpHealthProbeService httpHealthProbeService;
    private final Map<String, InstanceState> states = new ConcurrentHashMap<>();
    private final SshCommandService sshCommandService;
    

    @Scheduled(fixedDelayString = "${watchdog.interval-ms}")
    public void checkInstances() {

        var instances = props.getInstances();
        if (instances == null || instances.isEmpty()) {
            log.warn("[WATCHDOG] No instances configured");
            return;
        }

        for (var inst : instances) {

            InstanceState state = states.computeIfAbsent(inst.getName(), k -> new InstanceState());

            boolean vmUp = sshProbeService.isVmUp(inst, props.getTimeoutMs(), props.getRetries());

            if (!vmUp) {
                state.reset(); // important: on ne garde pas un échec service si la VM est down
            log.error("[WATCHDOG] {} ({}@{}) : VM=DOWN (SSH failed)", inst.getName(), inst.getSshUser(), inst.getIp());                continue;
            }

            boolean serviceUp = httpHealthProbeService.isServiceUp(inst, props.getTimeoutMs(), props.getRetries());

            if (serviceUp) {
                state.reset(); // service OK => on reset le compteur d'échecs consécutifs
                log.info("[WATCHDOG] {} ({}) : VM=UP | SERVICE=UP", inst.getName(), inst.getIp());
                continue;
            }

            // service DOWN
            int failures = state.incrementAndGet();
            int threshold = Math.max(1, props.getRestartThreshold()); // soit 1 soit le nombre d'essai de properties si il est plus grand
            log.warn("[WATCHDOG] {} ({}) : VM=UP | SERVICE=DOWN (fail {}/{})",
                    inst.getName(), inst.getIp(), failures, threshold);

            if (failures >= threshold) {
                log.error("[WATCHDOG] {} ({}) : SERVICE DOWN {} times -> restarting via SSH",
                        inst.getName(), inst.getIp(), threshold);

                var result = sshCommandService.run(inst, inst.getRestartCommand(), props.getTimeoutMs());

                if (result.ok()) {
                    log.info("[WATCHDOG] {} ({}) : restart command OK ({}ms) stdout='{}'",
                            inst.getName(), inst.getIp(), result.durationMs(), result.stdout());
                } else {
                    log.error("[WATCHDOG] {} ({}) : restart command FAILED -> {}",
                            inst.getName(), inst.getIp(), result.message());
                    // Même si restart échoue, on reset pour éviter boucle agressive
                    state.reset();
                    continue;
                }

                // petit délai pour laisser le service redémarrer
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // re-check HTTP après relance
                boolean serviceBack = httpHealthProbeService.isServiceUp(inst, props.getTimeoutMs(), props.getRetries());

                if (serviceBack) {
                    log.info("[WATCHDOG] {} ({}) : SERVICE RECOVERED after restart",
                            inst.getName(), inst.getIp());
                } else {
                    log.error("[WATCHDOG] {} ({}) : SERVICE STILL DOWN after restart",
                            inst.getName(), inst.getIp());
                }

                state.reset();
            }
        }
    }
}