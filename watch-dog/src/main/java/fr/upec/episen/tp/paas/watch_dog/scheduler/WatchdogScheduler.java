package fr.upec.episen.tp.paas.watch_dog.scheduler;

import fr.upec.episen.tp.paas.watch_dog.config.WatchDogProperties;
import fr.upec.episen.tp.paas.watch_dog.service.HttpHealthProbeService;
import fr.upec.episen.tp.paas.watch_dog.service.SshProbeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchdogScheduler {

    private final WatchDogProperties props;
    private final SshProbeService sshProbeService;
    private final HttpHealthProbeService httpHealthProbeService;

    @Scheduled(fixedDelayString = "${watchdog.interval-ms}")
    public void checkInstances() {

        var instances = props.getInstances();
        if (instances == null || instances.isEmpty()) {
            log.warn("[WATCHDOG] No instances configured");
            return;
        }

        for (var inst : instances) {

            boolean vmUp = sshProbeService.isVmUp(inst, props.getTimeoutMs(), props.getRetries());

            if (!vmUp) {
                log.error("[WATCHDOG] {} ({}) : VM=DOWN (SSH failed)", inst.getName(), inst.getIp());
                continue;
            }

            boolean serviceUp = httpHealthProbeService.isServiceUp(inst, props.getTimeoutMs(), props.getRetries());

            if (serviceUp) {
                log.info("[WATCHDOG] {} ({}) : VM=UP | SERVICE=UP", inst.getName(), inst.getIp());
            } else {
                log.warn("[WATCHDOG] {} ({}) : VM=UP | SERVICE=DOWN", inst.getName(), inst.getIp());
            }
        }
    }
}