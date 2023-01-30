package org.opengauss.portalcontroller.status;

import java.util.ArrayList;

/**
 * The type Full migration status.
 */
public class FullMigrationStatus {
    private ArrayList<TableStatus> table = new ArrayList<>();
   private ArrayList<ObjectStatus> view = new ArrayList<>();
    private ArrayList<ObjectStatus> function = new ArrayList<>();
    private ArrayList<ObjectStatus> trigger = new ArrayList<>();
    private ArrayList<ObjectStatus> procedure = new ArrayList<>();

    public ArrayList<TableStatus> getTable() {
        return table;
    }

    public void setTable(ArrayList<TableStatus> table) {
        this.table = table;
    }

    public ArrayList<ObjectStatus> getView() {
        return view;
    }

    public void setView(ArrayList<ObjectStatus> view) {
        this.view = view;
    }

    public ArrayList<ObjectStatus> getFunction() {
        return function;
    }

    public void setFunction(ArrayList<ObjectStatus> function) {
        this.function = function;
    }

    public ArrayList<ObjectStatus> getTrigger() {
        return trigger;
    }

    public void setTrigger(ArrayList<ObjectStatus> trigger) {
        this.trigger = trigger;
    }

    public ArrayList<ObjectStatus> getProcedure() {
        return procedure;
    }

    public void setProcedure(ArrayList<ObjectStatus> procedure) {
        this.procedure = procedure;
    }

    public FullMigrationStatus(ArrayList<TableStatus> table, ArrayList<ObjectStatus> view, ArrayList<ObjectStatus> function, ArrayList<ObjectStatus> trigger, ArrayList<ObjectStatus> procedure) {
        this.table = table;
        this.view = view;
        this.function = function;
        this.trigger = trigger;
        this.procedure = procedure;
    }
}
