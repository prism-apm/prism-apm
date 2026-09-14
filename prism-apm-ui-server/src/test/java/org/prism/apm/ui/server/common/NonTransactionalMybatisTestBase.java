package org.prism.apm.ui.server.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED) // Disable transactions for all tests
public class NonTransactionalMybatisTestBase extends MybatisTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected void refreshContinuousAggregate(){
        jdbcTemplate.execute("CALL refresh_continuous_aggregate('service_call_minute', null, null)");
    }

}
