package org.opengauss.portalcontroller.status;

/**
 * The type Table status.
 */
public class TableStatus {
    private String name;
    private int status;
    private double percent;

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

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
     * Gets percent.
     *
     * @return the percent
     */
    public double getPercent() {
        return percent;
    }

    /**
     * Sets percent.
     *
     * @param percent the percent
     */
    public void setPercent(double percent) {
        this.percent = percent;
    }

    /**
     * Instantiates a new Table status.
     *
     * @param name    the name
     * @param status  the status
     * @param percent the percent
     */
    public TableStatus(String name, int status, double percent) {
        this.name = name;
        this.status = status;
        this.percent = percent;
    }

    /**
     * Instantiates a new Table status.
     *
     * @param name   the name
     * @param status the status
     */
    public TableStatus(String name, int status) {
        this.name = name;
        this.status = status;
        this.percent = 1;
    }
}
