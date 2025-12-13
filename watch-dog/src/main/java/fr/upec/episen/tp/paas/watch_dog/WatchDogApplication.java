package fr.upec.episen.tp.paas.watch_dog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties
@SpringBootApplication
public class WatchDogApplication {
    public static void main(String[] args) {
        SpringApplication.run(WatchDogApplication.class, args);
    }
}