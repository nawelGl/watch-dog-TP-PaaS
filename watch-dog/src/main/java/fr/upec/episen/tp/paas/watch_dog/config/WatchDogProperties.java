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
    private String healthUrl;

    private List<InstanceConfig> instances;

    @Data
    public static class InstanceConfig {
        private String name;
        private String ip;
        private String sshUser;
        private int sshPort = 22;
        private String healthUrl;
    
    }
}