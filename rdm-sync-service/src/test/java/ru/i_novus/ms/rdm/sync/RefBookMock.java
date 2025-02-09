package ru.i_novus.ms.rdm.sync;

import org.junit.jupiter.api.Assertions;
import org.mockserver.client.MockServerClient;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.MediaType;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class RefBookMock {

    private final MockServerClient client;

    /**
     * нужно использовать loggingService который будет внедрен в тест после поднятия контекста, а инициализация происходит до контекста
     */
    private final Supplier<RdmLoggingService> loggingServiceHolder;

    public static RefBookMock instanceOf(MockServerClient client, Supplier<RdmLoggingService> loggingServiceHolder) {
        return new RefBookMock(client, loggingServiceHolder);
    }

    private RefBookMock(MockServerClient client, Supplier<RdmLoggingService> loggingServiceHolder) {
        this.client = client;
        this.loggingServiceHolder = loggingServiceHolder;
    }

    public RdmRefBook rdm(String code) {
        return new RdmRefBook(code);
    }

    public FnsiRefBook fnsi(String oid) {
        return new FnsiRefBook(oid);
    }

    private boolean isNoneLoaded(String code) {
        return loggingServiceHolder.get() == null || loggingServiceHolder.get().getList(LocalDate.now(), code).isEmpty();
    }

    private boolean isGivenLoaded(String code, String version) {
        return loggingServiceHolder.get() != null && loggingServiceHolder.get().getList(LocalDate.now(), code).stream()
                .anyMatch(log -> log.getNewVersion().equals(version));
    }

    private String getFileContent(String filePath) {
        try {
            InputStream resourceAsStream = RefBookMock.class.getResourceAsStream(filePath);
            if (resourceAsStream == null) {
                throw new FileNotFoundException(filePath);
            }
            return new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Assertions.fail(e);
            return "unused";
        }
    }

    public class FnsiRefBook {
        private final String oid;

        private final List<SearchRefBook> searchRefBooksList = new ArrayList<>();
        private final List<GetVersions> versionsList = new ArrayList<>();
        private final List<GetPassport> passportList = new ArrayList<>();
        private final List<GetData> dataList = new ArrayList<>();
        private final List<Compare> compareList = new ArrayList<>();

        private FnsiRefBook(String oid) {
            this.oid = oid;
        }

        public FnsiRefBook withSearchRefBook(String version, String fileName) {
            this.searchRefBooksList.add(new SearchRefBook(version, fileName));
            return this;
        }

        public FnsiRefBook withGetVersions(String fileName) {
            this.versionsList.add(new GetVersions(fileName));
            return this;
        }

        public FnsiRefBook withGetPassport(String version, String fileName) {
            this.passportList.add(new GetPassport(version, fileName));
            return this;
        }


        public FnsiRefBook withGetData(String version, Map<Integer, String> pagesOfFileData) {
            this.dataList.add(new GetData(version, pagesOfFileData));
            return this;
        }

        public FnsiRefBook withCompare(LocalDateTime fromDate, LocalDateTime toDate, Map<Integer, String> pagesOfFileData) {
            this.compareList.add(new Compare(fromDate, toDate, pagesOfFileData));
            return this;
        }

        public void mock() {
            if (!searchRefBooksList.isEmpty()) {
                searchRefBooksList.sort(Comparator.comparing(SearchRefBook::version));
                ExpectationResponseCallback callback = httpRequest -> {
                    String response = null;
                    if (isNoneLoaded(oid)) {
                        response = getFileContent("/fnsi_responses/" + searchRefBooksList.get(0).fileName());
                    } else {
                        for (int i = 0; i < searchRefBooksList.size() - 1; i++) {
                            // теперь грузим след версию в хронологическом порядке, учитывая что в загруженных может быть только одна версия справочника всегда
                            if (isGivenLoaded(oid, searchRefBooksList.get(i).version()) && !isGivenLoaded(oid, searchRefBooksList.get(i + 1).version())) {
                                response = getFileContent("/fnsi_responses/" + searchRefBooksList.get(i + 1).fileName());
                                break;
                            }
                        }

                    }
                    return response()
                            .withStatusCode(response != null ? 200 : 404)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(response);
                };
                mockSearchRefBook(callback);
            }
            versionsList.forEach(this::mockGetVersions);
            passportList.forEach(this::mockGetPassport);
            mockFnsiGetData();
            compareList.forEach(this::mockCompare);
        }

        private void mockCompare(Compare compare) {

            final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            final String uri = "/fnsi/rest/compare";
            compare.pagesOfData().forEach((page, file) -> {
                final Map<String, List<String>> params = Map.of(
                        "userKey", List.of("test"),
                        "identifier", List.of(oid),
                        "date1", List.of("" + dtf.format(compare.fromDate())),
                        "date2", List.of("" + dtf.format(compare.toDate())),
                        "page", List.of("" + page),
                        "size", List.of("200")
                );
                client.when(
                        request()
                                .withMethod("GET")
                                .withPath(uri)
                                .withQueryStringParameters(params)
                ).respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(getFileContent("/fnsi_responses/" + file))
                );
            });
        }

        private void mockFnsiGetData() {
            if (dataList.isEmpty()) {
                return;
            }
            dataList.sort(Comparator.comparing(GetData::version));
            ExpectationResponseCallback callback = httpRequest -> {
                String response = null;
                Integer page = Integer.valueOf(httpRequest.getFirstQueryStringParameter("page"));


                if (isNoneLoaded(oid) && dataList.get(0).pagesOfFileData().containsKey(page)) {
                    response = getFileContent("/fnsi_responses/" + dataList.get(0).pagesOfFileData().get(page));
                } else {
                    for (int i = 0; i < dataList.size() - 1; i++) {
                        if (isGivenLoaded(oid, dataList.get(i).version()) && !isGivenLoaded(oid, dataList.get(i + 1).version()) && dataList.get(i + 1).pagesOfFileData().containsKey(page)) {
                            response = getFileContent("/fnsi_responses/" + dataList.get(i + 1).pagesOfFileData().get(page));
                        }
                    }
                }
                return response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(response == null ? getFileContent("/fnsi_responses/data_empty_page.json") : response);
            };
            final String uri = "/fnsi/rest/data";
            client.when(
                    request()
                            .withMethod("GET")
                            .withPath(uri)
                            .withQueryStringParameter("identifier", oid)
            ).respond(callback);
        }

        private void mockGetPassport(GetPassport passport) {
            client.when(
                            request()
                                    .withMethod("GET")
                                    .withPath("/fnsi/rest/passport")
                                    .withQueryStringParameter("identifier", oid)
                                    .withQueryStringParameter("version", passport.version())
                    )
                    .respond(
                            response()
                                    .withStatusCode(200)
                                    .withContentType(MediaType.APPLICATION_JSON)
                                    .withBody(getFileContent("/fnsi_responses/" + passport.fileName()))
                    );
        }


        private void mockGetVersions(GetVersions versions) {
            final Map<String, List<String>> params = Map.of(
                    "userKey", List.of("test"),
                    "identifier", List.of(oid),
                    "page", List.of("1"),
                    "size", List.of("200")
            );

            client.when(
                            request()
                                    .withMethod("GET")
                                    .withPath("/fnsi/rest/versions")
                                    .withQueryStringParameters(params)
                    )
                    .respond(
                            response()
                                    .withStatusCode(200)
                                    .withContentType(MediaType.APPLICATION_JSON)
                                    .withBody(getFileContent("/fnsi_responses/" + versions.fileName()))
                    );

        }

        private void mockSearchRefBook(ExpectationResponseCallback callback) {

            final Map<String, List<String>> params = Map.of(
                    "userKey", List.of("test"),
                    "identifier", List.of(oid),
                    "page", List.of("1"),
                    "size", List.of("200")
            );
            client.when(
                    request()
                            .withMethod("GET")
                            .withPath("/fnsi/rest/searchDictionary")
                            .withQueryStringParameters(params)

            ).respond(callback);
        }

        private record SearchRefBook(String version, String fileName) {
        }

        private record GetVersions(String fileName) {
        }

        private record GetPassport(String version, String fileName) {
        }

        private record GetData(String version, Map<Integer, String> pagesOfFileData) {
        }

        private record Compare(LocalDateTime fromDate, LocalDateTime toDate, Map<Integer, String> pagesOfData) {
        }
    }

    public class RdmRefBook {
        private final String code;

        private final List<VersionData> versionsData = new ArrayList<>();
        private final List<DataDiff> dataDiffs = new ArrayList<>();
        private final List<StructureDiff> structureDiffs = new ArrayList<>();
        private final List<GetByCodeAndVersion> getByCodeAndVersionList = new ArrayList<>();
        private String versionsResponse;

        private RdmRefBook(String code) {
            this.code = code;
        }

        public RdmRefBook withData(Integer versionId, Map<Integer, String> pagesOfFileData) {
            versionsData.add(new VersionData(versionId, pagesOfFileData));
            return this;
        }

        public RdmRefBook withDataDiff(Integer oldVersionId, Integer newVersionId, Map<Integer, String> pagesOfFileDiff) {
            dataDiffs.add(new DataDiff(oldVersionId, newVersionId, pagesOfFileDiff));
            return this;
        }

        public RdmRefBook withStructureDiff(Integer oldVersionId, Integer newVersionId, String fileName) {
            structureDiffs.add(new StructureDiff(oldVersionId, newVersionId, fileName));
            return this;
        }

        /**
         * Эммулирует запросы /version/{version}/refbook/{code} и /version/refBook/{code}/last.
         * Для /rdm/version/refBook/{code}/last учитывает последовательность версий в зависимости от того что было
         * загруженно в таблицу loaded_version для этого справочника, т.е в ответе будет следующая версия после той
         * которая есть в  loaded_version. Поэтому важно чтобы более раняя версия имела меньший номер версии.
         * @param version номер версии
         * @param fileName файл с примером ответа
         * @return this
         */
        public RdmRefBook withGetByCodeAndVersion(String version, String fileName) {
            getByCodeAndVersionList.add(new GetByCodeAndVersion(code, version, fileName));
            return this;
        }

        /**
         * Эммулирует запросы /version/versions
         * @param fileName файл с примером ответа
         * @return this
         */
        public RdmRefBook withVersions(String fileName) {
            this.versionsResponse = fileName;
            return this;
        }

        public void mock() {
            mockRdmRefBookLastVersion();
            versionsData.forEach(versionData -> mockRdmData(versionData.versionId(), versionData.pagesOfFileData()));
            structureDiffs.forEach(
                    structureDiff -> mockRdmStructureDiff(structureDiff.oldVersionId(), structureDiff.newVersionId(), structureDiff.fileName())
            );
            dataDiffs.forEach(dataDiff -> mockRdmDataDiff(dataDiff.oldVersionId(), dataDiff.newVersionId(), dataDiff.pagesOfFileDiff()));
            getByCodeAndVersionList.forEach(this::mockGetRdmByCodeAndVersion);
            if (versionsResponse != null) {
                mockVersions();
            }
        }

        private void mockVersions() {
            final String uri = "/rdm/version/versions";
            client
                    .when(request().withMethod("GET").withPath(uri).withQueryStringParameter("refBookCode"))
                    .respond(
                            response()
                                    .withStatusCode(200)
                                    .withContentType(MediaType.APPLICATION_JSON)
                                    .withBody(getFileContent("/rdm_responses/" + versionsResponse))
                    );


        }

        private void mockGetRdmByCodeAndVersion(GetByCodeAndVersion codeAndVersion) {

            final String uri = "/rdm/version/" + codeAndVersion.version() + "/refbook/" + codeAndVersion.refBookCode();
            client.when(
                    request()
                            .withMethod("GET")
                            .withPath(uri)
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(getFileContent("/rdm_responses/" + codeAndVersion.fileName()))
            );
        }

        private void mockRdmDataDiff(Integer oldVersionId, Integer newVersionId, Map<Integer, String> pagesOfData) {

            final String uri = "/rdm/compare/data";
            pagesOfData.forEach((page, file) -> {
                final Map<String, List<String>> params = Map.of(
                        "oldVersionId", List.of("" + oldVersionId),
                        "newVersionId", List.of("" + newVersionId),
                        "page", List.of("" + page),
                        "size", List.of("200")
                );
                client.when(
                        request()
                                .withMethod("GET")
                                .withPath(uri)
                                .withQueryStringParameters(params)
                ).respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(getFileContent("/rdm_responses/" + file))
                );
            });

            // Мок для всех остальных запросов с параметром page
            client.when(
                    request()
                            .withMethod("GET")
                            .withPath(uri)
                            .withQueryStringParameters(
                                    Map.of(
                                            "oldVersionId", List.of("" + oldVersionId),
                                            "newVersionId", List.of("" + newVersionId),
                                            "page", List.of(".+"), // Любое значение page
                                            "size", List.of("200")
                                    )
                            )
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(getFileContent("/rdm_responses/empty_data_diff.json"))
            );
        }


        private void mockRdmStructureDiff(Integer versionId1, Integer versionId2, String fileName) {
            final String uri = "/rdm/compare/structures/" + versionId1 + "-" + versionId2;
            client.when(
                    request()
                            .withMethod("GET")
                            .withPath(uri)
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(getFileContent("/rdm_responses/" + fileName))
            );
        }

        private void mockRdmRefBookLastVersion() {
            if (getByCodeAndVersionList.isEmpty()) {
                return;
            }
            getByCodeAndVersionList.sort(Comparator.comparing(v -> v.version));
            ExpectationResponseCallback versionCallback = httpRequest -> {
                String response = null;
                if (isNoneLoaded(code)) {
                    response = getFileContent("/rdm_responses/" + getByCodeAndVersionList.get(0).fileName());
                } else {
                    for (int i = 0; i < getByCodeAndVersionList.size() - 1; i++) {
                        if (isGivenLoaded(code, getByCodeAndVersionList.get(i).version()) && !isGivenLoaded(code, getByCodeAndVersionList.get(i + 1).version())) {
                            response = getFileContent("/rdm_responses/" + getByCodeAndVersionList.get(i + 1).fileName());
                        }
                    }
                }

                return response()
                        .withStatusCode(response != null ? 200 : 404)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(response);


            };

            client.when(
                    request()
                            .withMethod("GET")
                            .withPath("/rdm/version/refBook/" + code + "/last")

            ).respond(versionCallback);
        }

        private void mockRdmData(Integer versionId, Map<Integer, String> pagesOfData) {
            final String uri = "/rdm/version/" + versionId + "/data";
            pagesOfData.forEach((page, file) -> {
                final Map<String, List<String>> params = Map.of(
                        "page", List.of("" + page),
                        "size", List.of("200")
                );
                client.when(
                        request()
                                .withMethod("GET")
                                .withPath(uri)
                                .withQueryStringParameters(params)
                ).respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(getFileContent("/rdm_responses/" + file))
                );
            });

            // Мок для всех остальных запросов с параметром page
            client.when(
                    request()
                            .withMethod("GET")
                            .withPath(uri)
                            .withQueryStringParameters(
                                    Map.of(
                                            "page", List.of(".+"), // Любое значение page
                                            "size", List.of("200") // size обязателен
                                    )
                            )
            ).respond(
                    response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(getFileContent("/rdm_responses/empty_data.json"))
            );
        }


    }

    private record VersionData(Integer versionId, Map<Integer, String> pagesOfFileData) {
    }

    private record DataDiff(Integer oldVersionId, Integer newVersionId, Map<Integer, String> pagesOfFileDiff) {
    }

    private record StructureDiff(Integer oldVersionId, Integer newVersionId, String fileName) {
    }

    private record GetByCodeAndVersion(String refBookCode, String version, String fileName) {
    }

}
