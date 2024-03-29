package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Component
class ClusterLockService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterLockService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean tryLock() {

        boolean acquired;

        try {
            jdbcTemplate.queryForObject("SELECT last_acquired FROM rdm_sync.cluster_lock FOR UPDATE NOWAIT", Timestamp.class);
            acquired = true;

        } catch (CannotAcquireLockException ex) {

            logger.warn("Cannot acquire lock.", ex);
            acquired = false;
        }

        if (acquired) {
            jdbcTemplate.update("UPDATE rdm_sync.cluster_lock SET last_acquired = (SELECT CURRENT_TIMESTAMP AT TIME ZONE 'UTC')");
            logger.info("Lock successfully acquired.");
        }

        return acquired;
    }
}
