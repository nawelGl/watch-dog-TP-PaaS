package fr.upec.episen.tp.paas.watch_dog.service;

import fr.upec.episen.tp.paas.watch_dog.config.WatchDogProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class HttpHealthProbeService {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public boolean isServiceUp(WatchDogProperties.InstanceConfig inst,
                               long timeoutMs,
                               int retries) {

        if (inst.getHealthUrl() == null || inst.getHealthUrl().isBlank()) {
            log.warn("[HTTP] {} healthUrl not configured", inst.getName());
            return false;
        }

        for (int attempt = 1; attempt <= retries; attempt++) {
            if (tryOnce(inst.getHealthUrl(), timeoutMs)) {
                return true;
            }
            log.warn("[HTTP] {} attempt {}/{} failed", inst.getName(), attempt, retries);
        }

        return false;
    }

    private boolean tryOnce(String healthUrl, long timeoutMs) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            // Cas 1: endpoint simple /health => HTTP 200 suffit
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String body = resp.body() == null ? "" : resp.body();
                // Cas 2: actuator => status "UP"
                if (body.isBlank()) return true;
                return body.contains("\"status\"") ? body.contains("\"UP\"") : true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }
}