package ru.i_novus.ms.rdm.sync.init.mapping;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.xml.XmlMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.xml.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.init.RdmSyncInitializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static ru.i_novus.ms.rdm.sync.init.RdmSyncInitUtils.buildTableNameWithSchema;

@Component
public class XmlMappingSourceService implements MappingSourceService {

    private static final Logger logger = LoggerFactory.getLogger(XmlMappingSourceService.class);

    @Value("${rdm-sync.rdm-mapping.xml.path:/rdm-mapping.xml}")
    private String rdmMappingXmlPath;

    @Value("${rdm-sync.auto-create.schema:rdm}")
    private String defaultSchema;

    @Value("${rdm-sync.auto-create.ignore-case:true}")
    private Boolean caseIgnore;

    public void setRdmMappingXmlPath(String rdmMappingXmlPath) {
        this.rdmMappingXmlPath = rdmMappingXmlPath;
    }

    @Override
    public List<SyncMapping> getMappings() {
        try (InputStream io = RdmSyncInitializer.class.getResourceAsStream(rdmMappingXmlPath)) {

            if (io == null) {
                logger.info("rdm-mapping.xml not found, xml mapping loader skipped");
                return Collections.emptyList();
            }

            Unmarshaller jaxbUnmarshaller = XmlMapping.JAXB_CONTEXT.createUnmarshaller();
            XmlMapping mapping = (XmlMapping) jaxbUnmarshaller.unmarshal(io);
            normalizeSysTable(mapping);

            return toVersionMappingList(mapping.getRefbooks());

        } catch (IOException | JAXBException e) {
            logger.error("xml mapping load error ", e);
            throw new RdmSyncException(e);
        }
    }

    private List<SyncMapping> toVersionMappingList(List<XmlMappingRefBook> refBooks) {
        return refBooks.stream()
                .map(XmlMappingRefBook::convertToSyncMapping)
                .collect(Collectors.toList());
    }

    private void normalizeSysTable(XmlMapping mapping) {
        mapping.getRefbooks().forEach(v -> {
            String tableName = buildTableNameWithSchema(v.getCode(), v.getSysTable(), defaultSchema, TRUE.equals(caseIgnore));
            v.setSysTable(tableName);
        });
    }

}
