package org.opengauss.portalcontroller.status;

public class IncrementalMigrationStatus {
    private int status;
    private int count;
    private int sourceSpeed;
    private int sinkSpeed;
    private int rest;
    private String msg;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getSourceSpeed() {
        return sourceSpeed;
    }

    public void setSourceSpeed(int sourceSpeed) {
        this.sourceSpeed = sourceSpeed;
    }

    public int getSinkSpeed() {
        return sinkSpeed;
    }

    public void setSinkSpeed(int sinkSpeed) {
        this.sinkSpeed = sinkSpeed;
    }

    public int getRest() {
        return rest;
    }

    public void setRest(int rest) {
        this.rest = rest;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public IncrementalMigrationStatus(int status, int count, int sourceSpeed, int sinkSpeed, int rest, String msg) {
        this.status = status;
        this.count = count;
        this.sourceSpeed = sourceSpeed;
        this.sinkSpeed = sinkSpeed;
        this.rest = rest;
        this.msg = msg;
    }

    public IncrementalMigrationStatus(int status, int count, int sourceSpeed, int sinkSpeed, int rest) {
        this.status = status;
        this.count = count;
        this.sourceSpeed = sourceSpeed;
        this.sinkSpeed = sinkSpeed;
        this.rest = rest;
    }
}
