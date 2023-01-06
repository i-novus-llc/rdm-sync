package ru.i_novus.ms.rdm.sync.service.downloader;

public class DownloadResult {

    private final String tableName;

    private final DownloadResultType type;

    public DownloadResult(String tableName, DownloadResultType type) {
        this.tableName = tableName;
        this.type = type;
    }

    public String getTableName() {
        return tableName;
    }

    public DownloadResultType getType() {
        return type;
    }
}
