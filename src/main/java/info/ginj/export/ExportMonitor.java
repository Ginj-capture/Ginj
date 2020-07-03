package info.ginj.export;

public interface ExportMonitor {
    /**
     * Indicates a progress.
     * For example, progress 0 to 9 is "preparing", and 10 to 90 is "uploading" and 91 to 100 is "finishing".
     * @param state a custom "state" string
     * @param progress the progress, a number between 0 and 100
     * @param currentSizeBytes the number of bytes already processed
     * @param totalSizeBytes the total number of bytes to process
     */
    void log(String state, int progress, long currentSizeBytes, long totalSizeBytes);

    /**
     * Indicates a progress.
     * For example, progress 0 to 9 is "preparing", and 10 to 90 is "uploading" and 91 to 100 is "finishing".
     * @param state a custom "state" string
     * @param progress the progress, a number between 0 and 100
     * @param sizeProgress a custom "size progress" string
     */
    void log(String state, int progress, String sizeProgress);

    /**
     * Indicates a progress.
     * For example, progress 0 to 9 is "preparing", and 10 to 90 is "uploading" and 91 to 100 is "finishing".
     * @param state a custom "state" string
     * @param progress the progress, a number between 0 and 100
     */
    void log(String state, int progress);

    /**
     * Update the state without changing the progress
     * @param state a custom "state" string
     */
    void log(String state);

    /**
     * Indicates that the export completed successfully.
     * @param state a custom "state" string
     */
    void complete(String state);

    /**
     * Indicates that the export failed.
     */
    void failed(String state);

    
}
