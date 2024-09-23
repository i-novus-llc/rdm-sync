package ru.i_novus.ms.rdm.sync.rdm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.fnsi.MockFnsiSourceLoaderService;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;

import java.net.URISyntaxException;

public class TestRdmConfig {

    public static final String EK002 = "EK002";
    public static final String XML_EK002 = "XMLEK002";
    public static final String EK003 = "EK003";
    public static final String XML_EK003 = "XMLEK003";

    public static final String SERVICE_URL = "http://mock.rdm.api";

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @Autowired
    private SyncSourceDao syncSourceDao;

    @Autowired
    private RdmLoggingService loggingService;

    public TestRdmConfig() {
        // Nothing to do.
    }

    @Bean
    @Primary
    public RestClient.Builder rdmRestClientBuilder() {
        return RestClient.builder().baseUrl(SERVICE_URL);
    }

    @Bean
    public SourceLoaderService mockRdmSourceLoaderService() {
        return new MockRdmSourceLoaderService(SERVICE_URL, syncSourceDao);
    }

    @Bean
    public SyncSourceServiceFactory mockRdmSyncSourceServiceFactory() throws URISyntaxException {

        final RestClient.Builder builder = rdmRestClientBuilder();

        final MockRestServiceServer mockServer = MockRestServiceServer
                .bindTo(builder)
                .ignoreExpectOrder(true)
                .build();

        mockRdm(mockServer, EK002);

        return new MockRdmSyncSourceServiceFactory(builder);
    }

    private void mockRdm(MockRestServiceServer mockServer, String refBookCode) {

    }

    //@PostConstruct
    //public void  init() {
    //    objectMapper.addMixIn(RowValue.class, TestRowValueMixin.class);
    //}

    //@Bean
    //public RefBookService refBookService() throws IOException {
    //    RefBookService refBookService = mock(RefBookService.class);
    //    RefBook ek002Ver1 = getRefBook("/EK002_version1.json");
    //    RefBook ek002Ver2 = getRefBook("/EK002_version2.json");
    //    RefBook xmlek002Ver1 = getRefBook("/EK002_version1.json", EK002, XML_EK002);
    //    RefBook xmlek002Ver2 = getRefBook("/EK002_version2.json", EK002, XML_EK002);
    //
    //    RefBook ek003Ver3_0 = getRefBook("/rdm_responses/EK003_version_3.0.json");
    //    RefBook ek003Ver3_1 = getRefBook("/rdm_responses/EK003_version_3.1.json");
    //    RefBook xmlek003Ver3_0 = getRefBook("/rdm_responses/EK003_version_3.0.json", EK003, XML_EK003);
    //    RefBook xmlek003Ver3_1 = getRefBook("/rdm_responses/EK003_version_3.1.json", EK003, XML_EK003);
    //
    //    when(refBookService.search(argThat(refBookCriteria -> refBookCriteria!=null && Set.of(EK002, XML_EK002).contains(refBookCriteria.getCodeExact()))))
    //            .thenAnswer((Answer<Page<RefBook>>) invocationOnMock -> {
    //                RefBookCriteria refBookCriteria = invocationOnMock.getArgument(0, RefBookCriteria.class);
    //                if (refBookCriteria.getPageNumber() >= 1) {
    //                    return new RestPage<>(Collections.emptyList());
    //                }
    //                boolean ver1IsLoaded = versionIsLoaded(refBookCriteria.getCodeExact(), "1");
    //                if(!ver1IsLoaded) {
    //                    return new RestPage<>(Collections.singletonList(refBookCriteria.getCodeExact().equals(EK002) ? ek002Ver1 : xmlek002Ver1));
    //                } else
    //                    return new RestPage<>(Collections.singletonList(refBookCriteria.getCodeExact().equals(EK002) ? ek002Ver2 : xmlek002Ver2));
    //            });
    //    when(refBookService.search(argThat(refBookCriteria -> Set.of(EK003, XML_EK003).contains(refBookCriteria.getCodeExact()))))
    //            .thenAnswer((Answer<Page<RefBook>>) invocationOnMock -> {
    //                RefBookCriteria refBookCriteria = invocationOnMock.getArgument(0, RefBookCriteria.class);
    //                if (refBookCriteria.getPageNumber() >= 1) {
    //                    return new RestPage<>(Collections.emptyList());
    //                }
    //                boolean ver3_0IsLoaded = versionIsLoaded(refBookCriteria.getCodeExact(), "3.0");
    //                if(!ver3_0IsLoaded  ) {
    //                    return new RestPage<>(Collections.singletonList(refBookCriteria.getCodeExact().equals(EK003) ? ek003Ver3_0 : xmlek003Ver3_0));
    //                } else
    //                    return new RestPage<>(Collections.singletonList(refBookCriteria.getCodeExact().equals(EK003) ? ek003Ver3_1 : xmlek003Ver3_1));
    //
    //            });
    //    return refBookService;
    //}
    //
    //@Bean
    //public VersionRestService versionService() throws IOException {
    //    RefBookRowValue[] ek002v1Rows = getVersionRows("/EK002-data_version1.json");
    //    RefBookRowValue[] ek002v2Rows = getVersionRows("/EK002-data_version2.json");
    //    RefBookRowValue[] ek003v3_0Rows = getVersionRows("/rdm_responses/EK003_data_version_3.0.json");
    //    RefBookRowValue[] ek003v3_1Rows = getVersionRows("/rdm_responses/EK003_data_version_3.1.json");
    //
    //    VersionRestService versionService = mock(VersionRestService.class);
    //
    //    RefBookVersion ek002Version1 = getRefBookVersion("/EK002_version1.json");
    //    RefBookVersion ek002Version2 = getRefBookVersion("/EK002_version2.json");
    //    RefBookVersion xmlEk002Version2 = getRefBookVersion("/EK002_version2.json");
    //    xmlEk002Version2.setCode(XML_EK002);
    //    RefBookVersion xmlEk002Version1 = getRefBookVersion("/EK002_version1.json");
    //    xmlEk002Version1.setCode(XML_EK002);
    //    when(versionService.getVersion(eq("1"), eq(EK002))).thenReturn(ek002Version1);
    //    when(versionService.getVersion(eq("2"), eq(EK002))).thenReturn(ek002Version2);
    //    when(versionService.getVersion(eq("1"), eq(XML_EK002))).thenReturn(xmlEk002Version1);
    //    when(versionService.getVersion(eq("2"), eq(XML_EK002))).thenReturn(xmlEk002Version2);
    //    RefBookVersion ek003Version3_0 = getRefBookVersion("/rdm_responses/EK003_version_3.0.json");
    //    RefBookVersion ek003Version3_1 = getRefBookVersion("/rdm_responses/EK003_version_3.1.json");
    //    RefBookVersion xmlEk003Version3_0 = getRefBookVersion("/rdm_responses/EK003_version_3.0.json");
    //    xmlEk003Version3_0.setCode(XML_EK003);
    //    RefBookVersion xmlEk003Version3_1 = getRefBookVersion("/rdm_responses/EK003_version_3.1.json");
    //    xmlEk003Version3_1.setCode(XML_EK003);
    //    when(versionService.getVersion(eq("3.0"), eq(EK003))).thenReturn(ek003Version3_0);
    //    when(versionService.getVersion(eq("3.1"), eq(EK003))).thenReturn(ek003Version3_1);
    //    when(versionService.getVersion(eq("3.0"), eq(XML_EK003))).thenReturn(xmlEk003Version3_0);
    //    when(versionService.getVersion(eq("3.1"), eq(XML_EK003))).thenReturn(xmlEk003Version3_1);
    //    when(versionService.search(eq(ek002Version1.getId()), any(SearchDataCriteria.class))).thenAnswer(
    //            (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
    //                SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
    //                if (searchDataCriteria.getPageNumber() >= 1) {
    //                    return new RestPage<>(Collections.emptyList());
    //                }
    //                 if (searchDataCriteria.getPageNumber() == 0 )
    //                    return new RestPage<>(Arrays.asList(ek002v1Rows));
    //
    //                return new RestPage<>(Collections.emptyList());
    //            });
    //
    //    when(versionService.search(eq(ek002Version2.getId()), any(SearchDataCriteria.class))).thenAnswer(
    //            (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
    //                SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
    //                if (searchDataCriteria.getPageNumber() >= 1) {
    //                    return new RestPage<>(Collections.emptyList());
    //                }
    //                if (searchDataCriteria.getPageNumber() == 0)
    //                    return new RestPage<>(Arrays.asList(ek002v2Rows));
    //
    //                return new RestPage<>(Collections.emptyList());
    //            });
    //
    //
    //
    //    when(versionService.search(eq(ek003Version3_0.getId()), any(SearchDataCriteria.class))).thenAnswer(
    //            (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
    //                SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
    //                if (searchDataCriteria.getPageNumber() >= 1) {
    //                    return new RestPage<>(Collections.emptyList());
    //                }
    //                if (searchDataCriteria.getPageNumber() == 0)
    //                    return new RestPage<>(Arrays.asList(ek003v3_0Rows));
    //
    //                return new RestPage<>(Collections.emptyList());
    //            });
    //
    //    when(versionService.search(eq(ek003Version3_1.getId()), any(SearchDataCriteria.class))).thenAnswer(
    //            (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
    //                SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
    //                if (searchDataCriteria.getPageNumber() >= 1) {
    //                    return new RestPage<>(Collections.emptyList());
    //                }
    //                if (searchDataCriteria.getPageNumber() == 0)
    //                    return new RestPage<>(Arrays.asList(ek003v3_1Rows));
    //
    //                return new RestPage<>(Collections.emptyList());
    //            });
    //
    //
    //
    //
    //    return versionService;
    //}
    //
    //@Bean
    //public CompareService compareService(){
    //    CompareService compareService = mock(CompareService.class);
    //
    //    when(compareService.compareData(any(CompareDataCriteria.class))).thenAnswer(
    //            (Answer<RefBookDataDiff>) invocationOnMock -> {
    //                CompareDataCriteria criteria = invocationOnMock.getArgument(0, CompareDataCriteria.class);
    //                if(criteria.getPageNumber() == 0 && Integer.valueOf(199).equals(criteria.getOldVersionId()) && Integer.valueOf(286).equals(criteria.getNewVersionId())) {
    //                    return getRefBookDataDiff("/EK002_diff_v1_v2.json");
    //                } else if (criteria.getPageNumber() == 0 && Integer.valueOf(206).equals(criteria.getOldVersionId()) && Integer.valueOf(293).equals(criteria.getNewVersionId())) {
    //                    return getRefBookDataDiff("/rdm_responses/EK003_diff_v3.0_v3.1.json");
    //                }
    //                return new RefBookDataDiff();
    //
    //            });
    //
    //    when(compareService.compareStructures(anyInt(), anyInt())).thenReturn(new StructureDiff());
    //
    //    return compareService;
    //
    //}
    //
    //private RefBookDataDiff getRefBookDataDiff(String path) throws IOException {
    //    return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8"), RefBookDataDiff.class);
    //}

    //private RefBook getRefBook(String path) throws IOException {
    //    return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8"), RefBook.class);
    //}
    //
    //private RefBookVersion getRefBookVersion(String path) throws IOException {
    //    return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8"), RefBookVersion.class);
    //}
    //
    //private RefBook getRefBook(String path, String replacedCode, String code) throws IOException {
    //    String version = IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8").replaceAll(replacedCode, code);
    //    return objectMapper.readValue(version, RefBook.class);
    //}
    //
    //private RefBookRowValue[] getVersionRows(String path) throws IOException {
    //    return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8"), RefBookRowValue[].class);
    //}

    //private String encode(String value) {
    //    return UriComponentsBuilder.fromPath((value)).toUriString();
    //}

}
