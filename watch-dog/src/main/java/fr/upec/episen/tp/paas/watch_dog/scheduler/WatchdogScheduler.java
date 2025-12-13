package fr.upec.episen.tp.paas.watch_dog.scheduler;

import fr.upec.episen.tp.paas.watch_dog.config.WatchDogProperties;
import fr.upec.episen.tp.paas.watch_dog.service.SshProbeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/** Rôle : orchestrer le cycle de surveillance toutes les X millisecondes.
 * 
 * Il fait :
	•	toutes les interval-ms
	•	pour chaque instance :
	•	appelle sshProbeService.isVmUp(...)
	•	log [UP] ou [DOWN]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchdogScheduler {

    private final WatchDogProperties props;
    private final SshProbeService sshProbeService;

    @Scheduled(fixedDelayString = "${watchdog.interval-ms}")
    public void checkInstances() {

        for (var inst : props.getInstances()) {

            boolean up = sshProbeService.isVmUp(
                    inst,
                    props.getTimeoutMs(),
                    props.getRetries()
            );

            if (up) {
                log.info("[WATCHDOG] {} ({}) : VM = UP",
                        inst.getName(), inst.getIp());
            } else {
                log.error("[WATCHDOG] {} ({}) : VM = DOWN",
                        inst.getName(), inst.getIp());
            }
        }
    }
}