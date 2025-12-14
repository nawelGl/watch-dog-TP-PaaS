package fr.upec.episen.tp.paas.watch_dog.scheduler;

import fr.upec.episen.tp.paas.watch_dog.config.WatchDogProperties;
import fr.upec.episen.tp.paas.watch_dog.model.InstanceState;
import fr.upec.episen.tp.paas.watch_dog.service.HttpHealthProbeService;
import fr.upec.episen.tp.paas.watch_dog.service.SshProbeService;
import fr.upec.episen.tp.paas.watch_dog.service.SshCommandService;
import fr.upec.episen.tp.paas.watch_dog.util.Ansi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchdogScheduler {

    private final WatchDogProperties props;
    private final SshProbeService sshProbeService;
    private final HttpHealthProbeService httpHealthProbeService;
    private final SshCommandService sshCommandService;

    private final Map<String, InstanceState> states = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${watchdog.interval-ms}")
    public void checkInstances() {

        var instances = props.getInstances();
        if (instances == null || instances.isEmpty()) {
            log.warn("{}", Ansi.yellow("[WATCHDOG] No instances configured"));
            return;
        }

        for (var inst : instances) {

            InstanceState state = states.computeIfAbsent(inst.getName(), k -> new InstanceState());

            /* =======================
             * 1) CHECK VM (SSH)
             * ======================= */
            boolean vmUp = sshProbeService.isVmUp(inst, props.getTimeoutMs(), props.getRetries());

            // INIT log (premier passage)
            if (state.getLastVmUp() == null) {
                log.info("[WATCHDOG] {} ({}@{}) : INIT monitoring",
                        inst.getName(), inst.getSshUser(), inst.getIp());
            }

            // Transition VM UP -> DOWN
            if (!vmUp) {
                if (state.getLastVmUp() == null || Boolean.TRUE.equals(state.getLastVmUp())) {
                    log.error("[WATCHDOG] {} ({}@{}) : VM={} (UP -> DOWN)",
                            inst.getName(), inst.getSshUser(), inst.getIp(), Ansi.darkred("DOWN"));
                }
                state.setLastVmUp(false);

                // si infra down, on reset les fails service (ça n’a plus de sens)
                state.resetFailures();
                continue;
            }

            // Transition VM DOWN -> UP
            if (Boolean.FALSE.equals(state.getLastVmUp())) {
                log.info("[WATCHDOG] {} ({}@{}) : VM={} (DOWN -> UP)",
                        inst.getName(), inst.getSshUser(), inst.getIp(), Ansi.green("UP"));
            }
            state.setLastVmUp(true);

            /* =======================
             * 2) CHECK SERVICE (HTTP)
             * ======================= */
            boolean serviceUp = httpHealthProbeService.isServiceUp(inst, props.getTimeoutMs(), props.getRetries());

            // Transition SERVICE DOWN -> UP
            if (serviceUp) {
                if (Boolean.FALSE.equals(state.getLastServiceUp())) {
                    log.info("[WATCHDOG] {} ({}) : SERVICE={} (DOWN -> UP)",
                            inst.getName(), inst.getIp(), Ansi.green("UP"));
                }

                // si tout est ok ET qu’on vient de changer, on peut log “global”
                if (state.getLastServiceUp() == null || Boolean.FALSE.equals(state.getLastServiceUp())) {
                    log.info("[WATCHDOG] {} ({}) : VM={} | SERVICE={}",
                            inst.getName(), inst.getIp(), Ansi.green("UP"), Ansi.green("UP"));
                }

                state.setLastServiceUp(true);
                state.resetFailures();
                continue;
            }

            // Transition SERVICE UP -> DOWN
            if (state.getLastServiceUp() == null || Boolean.TRUE.equals(state.getLastServiceUp())) {
                log.warn("[WATCHDOG] {} ({}) : SERVICE={} (UP -> DOWN)",
                        inst.getName(), inst.getIp(), Ansi.red("DOWN"));
            }
            state.setLastServiceUp(false);

            /* =======================
             * 3) SERVICE DOWN → RETRIES
             * ======================= */
            int failures = state.incrementAndGetFailures();
            int threshold = Math.max(1, props.getRestartThreshold());

            // On log les retries en WARN uniquement si c’est utile pour la démo
            log.warn("[WATCHDOG] {} ({}) : VM={} | SERVICE={} (retry {}/{})",
                    inst.getName(),
                    inst.getIp(),
                    Ansi.green("UP"),
                    Ansi.yellow("DOWN"),
                    failures,
                    threshold);

            /* =======================
             * 4) RESTART SI SEUIL ATTEINT
             * ======================= */
            if (failures >= threshold) {

                log.warn("[WATCHDOG] {} ({}) : ACTION={}",
                        inst.getName(), inst.getIp(), Ansi.blue("RESTART via SSH"));

                var result = sshCommandService.run(inst, inst.getRestartCommand(),props.getRestartTimeoutMs());

                if (!result.ok()) {
                    log.error("[WATCHDOG] {} ({}) : RESTART={} -> {}",
                            inst.getName(), inst.getIp(), Ansi.red("FAILED"), result.message());
                    state.resetFailures();
                    continue;
                }

                /* =======================
                 * 5) WAIT & RECHECK HEALTH
                 * ======================= */
                var pr = props.getPostRestart();

                try {
                    Thread.sleep(Math.max(0, pr.getInitialWaitMs()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                long deadline = System.currentTimeMillis() + Math.max(1000, pr.getMaxWaitMs());
                boolean serviceBack = false;

                while (System.currentTimeMillis() < deadline) {

                    // 1 tentative par poll (sinon ça prend trop longtemps)
                    serviceBack = httpHealthProbeService.isServiceUp(inst, props.getTimeoutMs(), 1);

                    if (serviceBack) break;

                    try {
                        Thread.sleep(Math.max(200, pr.getPollIntervalMs()));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                if (serviceBack) {
                    log.info("[WATCHDOG] {} ({}) : SERVICE={}",
                            inst.getName(), inst.getIp(), Ansi.green("RECOVERED"));

                    // important : on remet lastServiceUp à true car on vient de le constater
                    state.setLastServiceUp(true);

                } else {
                    log.error("[WATCHDOG] {} ({}) : SERVICE={}",
                            inst.getName(), inst.getIp(), Ansi.red("STILL DOWN"));
                }

                state.resetFailures();
            }
        }
    }
}