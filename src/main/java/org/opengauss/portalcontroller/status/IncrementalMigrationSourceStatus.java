package org.opengauss.portalcontroller.status;

public class IncrementalMigrationSourceStatus {
    private int analysisCount;
    private int createCount;
    private int pollCount;
    private int rest;
    private int speed;
    private long timestamp;

    public int getAnalysisCount() {
        return analysisCount;
    }

    public void setAnalysisCount(int analysisCount) {
        this.analysisCount = analysisCount;
    }

    public int getCreateCount() {
        return createCount;
    }

    public void setCreateCount(int createCount) {
        this.createCount = createCount;
    }

    public int getPollCount() {
        return pollCount;
    }

    public void setPollCount(int pollCount) {
        this.pollCount = pollCount;
    }

    public int getRest() {
        return rest;
    }

    public void setRest(int rest) {
        this.rest = rest;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public IncrementalMigrationSourceStatus(int analysisCount, int createCount, int pollCount, int rest, int speed, long timestamp) {
        this.analysisCount = analysisCount;
        this.createCount = createCount;
        this.pollCount = pollCount;
        this.rest = rest;
        this.speed = speed;
        this.timestamp = timestamp;
    }
}
