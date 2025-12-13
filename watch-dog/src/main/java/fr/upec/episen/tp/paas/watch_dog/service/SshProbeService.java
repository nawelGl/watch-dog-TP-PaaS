package fr.upec.episen.tp.paas.watch_dog.service;

import fr.upec.episen.tp.paas.watch_dog.config.WatchDogProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SshProbeService {

    public boolean isVmUp(WatchDogProperties.InstanceConfig inst,
                          long timeoutMs,
                          int retries) {

        for (int attempt = 1; attempt <= retries; attempt++) {
            if (tryOnce(inst, timeoutMs)) {
                return true;
            }
            log.warn("[SSH] {} attempt {}/{} failed",
                    inst.getName(), attempt, retries);
        }
        return false;
    }

    private boolean tryOnce(WatchDogProperties.InstanceConfig inst,
                            long timeoutMs) {

        String target = inst.getSshUser() + "@" + inst.getIp();

        ProcessBuilder pb = new ProcessBuilder(
                "ssh",
                "-p", String.valueOf(inst.getSshPort()),
                "-o", "BatchMode=yes",
                "-o", "StrictHostKeyChecking=no",
                "-o", "ConnectTimeout=" + (timeoutMs / 1000),
                target,
                "echo", "ok"
        );

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            return false;
        }
    }
}