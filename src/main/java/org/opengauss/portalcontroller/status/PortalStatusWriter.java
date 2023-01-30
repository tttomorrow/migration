package org.opengauss.portalcontroller.status;

/**
 * The type Portal status writer.
 */
public class PortalStatusWriter {
    private int status;
    private long timestamp;

    /**
     * Gets status.
     *
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets timestamp.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets timestamp.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Instantiates a new Portal status writer.
     *
     * @param status    the status
     * @param timestamp the timestamp
     */
    public PortalStatusWriter(int status, long timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }
}
