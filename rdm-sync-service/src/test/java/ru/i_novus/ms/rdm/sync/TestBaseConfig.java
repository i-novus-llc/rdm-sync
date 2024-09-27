package ru.i_novus.ms.rdm.sync;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;

import java.time.LocalDate;

public abstract class TestBaseConfig {

    @Autowired
    protected RdmSyncDao rdmSyncDao;

    @Autowired
    protected SyncSourceDao syncSourceDao;

    @Autowired
    protected RdmLoggingService loggingService;

    /**
     * Преобразование значения в формат для подстановки в uri.
     *
     * @param value значение
     * @return Значение в формате для uri
     */
    protected static String valueToUri(String value) {
        return UriComponentsBuilder.fromPath((value)).toUriString();
    }

    /**
     * Проверка отсутствия загруженных версий справочника.
     *
     * @param code код справочника
     * @return Функция проверки
     */
    protected RequestMatcher checkNoneLoaded(String code) {
        return clientHttpRequest -> Assert.assertTrue(isNoneLoaded(code));
    }

    private boolean isNoneLoaded(String code) {
        return loggingService.getList(LocalDate.now(), code).isEmpty();
    }

    /**
     * Проверка наличия загруженной версии справочника.
     *
     * @param code    код справочника
     * @param version проверяемая версия
     * @return Функция проверки
     */
    protected RequestMatcher checkGivenLoaded(String code, String version) {
        return clientHttpRequest -> Assert.assertTrue(isGivenLoaded(code, version));
    }

    private boolean isGivenLoaded(String code, String version) {
        return loggingService.getList(LocalDate.now(), code).stream()
                .anyMatch(log -> log.getNewVersion().equals(version));
    }

}
