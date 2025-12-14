package fr.upec.episen.tp.paas.watch_dog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "watchdog")
public class WatchDogProperties {

    private long intervalMs;
    private long timeoutMs;
    private int retries;
    private int restartThreshold = 2;
    private PostRestart postRestart = new PostRestart();
    private long restartTimeoutMs = 8000;

    

    private List<InstanceConfig> instances;

    @Data
    public static class InstanceConfig {
        private String name;
        private String ip;
        private String sshUser;
        private int sshPort = 22;
        private long restartTimeoutMs = 8000;
        private String healthUrl;
        private String restartCommand;
    
    }

    @Data
    public static class PostRestart {
        private long initialWaitMs = 2000;
        private long maxWaitMs = 15000;
        private long pollIntervalMs = 2000;
    }

}