package ru.i_novus.ms.rdm.sync.service.updater;

import javax.annotation.Nullable;

public interface RefBookUpdater {

    void update(String refCode, @Nullable String version);

}
