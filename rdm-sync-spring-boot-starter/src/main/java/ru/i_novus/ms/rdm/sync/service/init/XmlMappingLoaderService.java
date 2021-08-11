package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMapping;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;

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
            if (lockService.tryLock()) {
                try  {
                    logger.info("loading ...");
                    mapping.getRefbooks().forEach(this::load);
                    logger.info("xml mapping was loaded");
                } finally {
                    logger.info("Lock successfully released.");
                }
            }
        } catch (IOException | JAXBException e) {
            logger.error("xml mapping load error ", e);
            throw new RdmException(e);
        }
    }

    private void load(XmlMappingRefBook xmlMappingRefBook) {

        if (xmlMappingRefBook.getMappingVersion() > rdmSyncDao.getLastVersion(xmlMappingRefBook.getCode())) {
            logger.info("load {}", xmlMappingRefBook.getCode());
            rdmSyncDao.insertFieldMapping(xmlMappingRefBook.getCode(), xmlMappingRefBook.getFields());
            rdmSyncDao.upsertVersionMapping(xmlMappingRefBook);
            logger.info("mapping for code {} was loaded", xmlMappingRefBook.getCode());

        } else {
            logger.info("mapping for {} not changed", xmlMappingRefBook.getCode());
        }
    }
}
