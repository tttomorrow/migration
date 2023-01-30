package org.opengauss.portalcontroller.status;

public class CheckRules {
    String name;
    String text;
    String attribute;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public CheckRules(String name, String text, String attribute) {
        this.name = name;
        this.text = text;
        this.attribute = attribute;
    }

    public CheckRules(String name, String text) {
        this.name = name;
        this.text = text;
    }
}
