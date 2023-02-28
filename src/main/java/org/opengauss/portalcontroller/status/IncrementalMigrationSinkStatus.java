package org.opengauss.portalcontroller.status;

public class IncrementalMigrationSinkStatus {
    private int createCount;
    private int failCount;
    private int replayedCount;
    private int rest;
    private int speed;
    private int successCount;
    private long timestamp;

    public int getCreateCount() {
        return createCount;
    }

    public void setCreateCount(int createCount) {
        this.createCount = createCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public int getReplayedCount() {
        return replayedCount;
    }

    public void setReplayedCount(int replayedCount) {
        this.replayedCount = replayedCount;
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

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public IncrementalMigrationSinkStatus(int createCount, int failCount, int replayedCount, int rest, int speed, int successCount, long timestamp) {
        this.createCount = createCount;
        this.failCount = failCount;
        this.replayedCount = replayedCount;
        this.rest = rest;
        this.speed = speed;
        this.successCount = successCount;
        this.timestamp = timestamp;
    }
}
