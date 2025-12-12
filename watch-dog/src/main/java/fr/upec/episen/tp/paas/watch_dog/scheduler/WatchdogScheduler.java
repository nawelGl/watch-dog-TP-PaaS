package fr.upec.episen.tp.paas.watch_dog.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WatchdogScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WatchdogScheduler.class);

    @Scheduled(fixedDelayString = "${watchdog.interval-ms}")
    public void check() {
        logger.info("Watchdog scheduler on");
    }
}
