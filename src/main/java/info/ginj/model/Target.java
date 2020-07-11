package info.ginj.model;

import info.ginj.export.GinjExporter;

import java.util.HashMap;
import java.util.Map;

/**
 * One Target is a specific instance that can be linked to an export button
 * It is linked to an exporter on creation
 */
public class Target {
    private GinjExporter exporter;
    private Account account;
    private String displayName;
    // TODO options should be a GinjExporterSettings object (or subclass thereof)
    private Map<String,String> options = new HashMap<>();

    /**
     * A target should always have at least an ID and an Exporter, but this no-arg constructor is used for deserialization
     */
    public Target() {
    }

    public Target(GinjExporter exporter, Account account) {
        this.exporter = exporter;
        this.account = account;
    }

    public GinjExporter getExporter() {
        return exporter;
    }

    public void setExporter(GinjExporter exporter) {
        this.exporter = exporter;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    // TODO migrate those options to specific TargetOptions objects ?

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public String toString() {
        String text;
        if (displayName != null) {
            text = displayName;
        }
        else {
            text = exporter.toString();
            if (account != null && account.toString() != null) {
                text += " (" + account.toString() + ")";
            }
        }
        return text;
    }
}
