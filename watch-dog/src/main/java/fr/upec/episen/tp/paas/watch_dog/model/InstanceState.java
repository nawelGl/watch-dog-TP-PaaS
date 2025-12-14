package fr.upec.episen.tp.paas.watch_dog.model;

public class InstanceState {
    private int consecutiveServiceFailures = 0;

    public void reset() {
        consecutiveServiceFailures = 0;
    }

    public int incrementAndGet() {
        return ++consecutiveServiceFailures;
    }
}