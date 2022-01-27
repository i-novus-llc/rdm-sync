package ru.i_novus.ms.rdm.sync;

import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:/xml_mapping/application-test.properties")
public class XmlMappingRdmSyncServiceUseCaseTest extends RdmSyncServiceUseCaseTest {

}
