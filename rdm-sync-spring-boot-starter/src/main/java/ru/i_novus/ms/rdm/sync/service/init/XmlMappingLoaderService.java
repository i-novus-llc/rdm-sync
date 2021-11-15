package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMapping;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

@Component
class XmlMappingLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(XmlMappingLoaderService.class);

    @Value("${rdm_sync.rdm-mapping.xml.path:/rdm-mapping.xml}")
    private String rdmMappingXmlPath;

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @Autowired
    private ClusterLockService lockService;

    @Transactional
    public void load() {

        try (InputStream io = RdmSyncInitializer.class.getResourceAsStream(rdmMappingXmlPath)) {
            if (io == null) {
                logger.info("rdm-mapping.xml not found, xml mapping loader skipped");
                return;
            }

            Unmarshaller jaxbUnmarshaller = XmlMapping.JAXB_CONTEXT.createUnmarshaller();
            XmlMapping mapping = (XmlMapping) jaxbUnmarshaller.unmarshal(io);
            load(mapping);

        } catch (IOException | JAXBException e) {
            logger.error("xml mapping load error ", e);
            throw new RdmException(e);
        }
    }

    private void load(XmlMapping mapping) {

        if (lockService.tryLock()) {
            try  {
                logger.info("loading ...");
                mapping.getRefbooks().forEach(this::load);
                logger.info("xml mapping was loaded");

            } finally {
                logger.info("Lock successfully released.");
            }
        }
    }

    private void load(XmlMappingRefBook xmlMappingRefBook) {

        if (xmlMappingRefBook.getMappingVersion() > rdmSyncDao.getLastVersion(xmlMappingRefBook.getCode())) {
            logger.info("load {}", xmlMappingRefBook.getCode());
            VersionMapping versionMapping = rdmSyncDao.getVersionMapping(xmlMappingRefBook.getCode(), "CURRENT");
            if(versionMapping == null) {
                rdmSyncDao.insertVersionMapping(xmlMappingRefBook.convertToVersionMapping());
                versionMapping = rdmSyncDao.getVersionMapping(xmlMappingRefBook.getCode(), "CURRENT");
            } else {
                rdmSyncDao.updateVersionMapping(xmlMappingRefBook.convertToVersionMapping());
            }
            rdmSyncDao.insertFieldMapping(versionMapping.getMappingId(), xmlMappingRefBook.getFields().stream().map(XmlMappingField::convertToFieldMapping).collect(Collectors.toList()));
            logger.info("mapping for code {} was loaded", xmlMappingRefBook.getCode());

        } else {
            logger.info("mapping for {} not changed", xmlMappingRefBook.getCode());
        }
    }
}
