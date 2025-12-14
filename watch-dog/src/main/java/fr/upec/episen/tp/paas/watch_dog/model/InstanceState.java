package fr.upec.episen.tp.paas.watch_dog.model;

public class InstanceState {

    // Compteur d'échecs service consécutifs (pour déclencher le restart)
    private int consecutiveServiceFailures = 0;

    // Mémoire du dernier état connu (pour logguer les transitions)
    private Boolean lastVmUp = null;       // null = jamais évalué
    private Boolean lastServiceUp = null;  // null = jamais évalué

    public void resetFailures() {
        consecutiveServiceFailures = 0;
    }

    public int incrementAndGetFailures() {
        return ++consecutiveServiceFailures;
    }

    public int getFailures() {
        return consecutiveServiceFailures;
    }

    public Boolean getLastVmUp() {
        return lastVmUp;
    }

    public void setLastVmUp(Boolean lastVmUp) {
        this.lastVmUp = lastVmUp;
    }

    public Boolean getLastServiceUp() {
        return lastServiceUp;
    }

    public void setLastServiceUp(Boolean lastServiceUp) {
        this.lastServiceUp = lastServiceUp;
    }
}