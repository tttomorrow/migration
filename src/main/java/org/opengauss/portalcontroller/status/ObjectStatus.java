package org.opengauss.portalcontroller.status;

/**
 * The type Object status.
 */
public class ObjectStatus {
    private String name;
    private int status;

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
     * Instantiates a new Object status.
     *
     * @param name   the name
     * @param status the status
     */
    public ObjectStatus(String name, int status) {
        this.name = name;
        this.status = status;
    }
}
