package info.ginj.online;

import info.ginj.export.ExportMonitor;

public abstract class AbstractOnlineService implements OnlineService {
    private ExportMonitor exportMonitor;

    @Override
    public void setExportMonitor(ExportMonitor exportMonitor) {
        this.exportMonitor = exportMonitor;
    }

    protected void logProgress(String state, int value) {
        if (exportMonitor != null) exportMonitor.log(state, value);
    }

    protected void logProgress(String state) {
        if (exportMonitor != null) exportMonitor.log(state);
    }
}
