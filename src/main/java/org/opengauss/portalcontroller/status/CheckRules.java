package org.opengauss.portalcontroller.status;

/**
 * The type Check rules.
 */
public class CheckRules {
    /**
     * The Name.
     */
    String name;
    /**
     * The Text.
     */
    String text;
    /**
     * The Attribute.
     */
    String attribute;

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
     * Gets text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets text.
     *
     * @param text the text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets attribute.
     *
     * @return the attribute
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     * Sets attribute.
     *
     * @param attribute the attribute
     */
    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    /**
     * Instantiates a new Check rules.
     *
     * @param name      the name
     * @param text      the text
     * @param attribute the attribute
     */
    public CheckRules(String name, String text, String attribute) {
        this.name = name;
        this.text = text;
        this.attribute = attribute;
    }

    /**
     * Instantiates a new Check rules.
     *
     * @param name the name
     * @param text the text
     */
    public CheckRules(String name, String text) {
        this.name = name;
        this.text = text;
    }
}
