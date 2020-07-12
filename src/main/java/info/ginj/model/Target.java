package info.ginj.model;

import info.ginj.export.Exporter;

/**
 * One Target is a specific instance that can be linked to an export button
 * It is linked to an exporter on creation
 */
public class Target {
    private String displayName;
    private Exporter exporter;
    private ExportSettings settings;
    private Account account;

    /**
     * A target should always have at least an ID and an Exporter, but this no-arg constructor is used for deserialization
     */
    public Target() {
    }

    public Target(Exporter exporter, Account account) {
        this.exporter = exporter;
        this.account = account;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Exporter getExporter() {
        return exporter;
    }

    public void setExporter(Exporter exporter) {
        this.exporter = exporter;
    }

    public ExportSettings getSettings() {
        return settings;
    }

    public void setSettings(ExportSettings settings) {
        this.settings = settings;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
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
