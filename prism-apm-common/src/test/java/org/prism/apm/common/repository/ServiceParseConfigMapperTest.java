package org.prism.apm.common.repository;

import org.prism.apm.common.MybatisTestBase;
import org.prism.apm.common.model.entity.ServiceParseConfigEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author john
 * @date 2024-10-17 14:20
 */
public class ServiceParseConfigMapperTest extends MybatisTestBase {

    @Autowired
    private ServiceParseConfigMapper mapper;

    @Test
    public void testSelectByPage_withMultipleDataAndPagination() {
        // Insert test data
        insertTestEntity(1, "service1", true, false);
        insertTestEntity(2, "service2", true, false);
        insertTestEntity(3, "service3", true, false);

        // Test paginated query
        ServiceParseConfigEntity queryEntity = new ServiceParseConfigEntity();
        queryEntity.setParsed(true);

        List<ServiceParseConfigEntity> resultPage1 = mapper.selectByPage(queryEntity, 2, 0);
        List<ServiceParseConfigEntity> resultPage2 = mapper.selectByPage(queryEntity, 2, 2);

        // Verify paginated data
        assertEquals(2, resultPage1.size());
        assertEquals(1, resultPage2.size());

        // Verify field contents
        assertEquals("service1", resultPage1.get(0).getServiceName());
        assertTrue(resultPage1.get(0).getParsed());
        assertTrue(resultPage2.get(0).getServiceName().equals("service3"));
    }

    @Test
    public void testSelectByPage_withNoMatchingConditions() {
        // Insert test data
        insertTestEntity(1, "service1", true, true);
        insertTestEntity(2, "service2", false, true);

        // Create a query with no matching conditions
        ServiceParseConfigEntity queryEntity = new ServiceParseConfigEntity();
        queryEntity.setServiceName("non-existing-service");

        List<ServiceParseConfigEntity> result = mapper.selectByPage(queryEntity, 10, 0);

        // Verify there are no results
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSelectByPage_withAllConditions() {
        // Insert test data
        insertTestEntity(1, "service1", true, true);
        insertTestEntity(2, "service2", false, true);

        // Create a query with all conditions
        ServiceParseConfigEntity queryEntity = new ServiceParseConfigEntity();
        queryEntity.setServiceName("service1");
        queryEntity.setParsed(true);
        queryEntity.setEnabled(true);

        List<ServiceParseConfigEntity> result = mapper.selectByPage(queryEntity, 10, 0);

        // Verify results
        assertEquals(1, result.size());
        assertEquals("service1", result.get(0).getServiceName());
    }

    @Test
    public void testSelectByPage_withNullEntity() {
        // Insert test data
        insertTestEntity(1, "service1", true, true);
        insertTestEntity(2, "service2", false, true);
        insertTestEntity(3, "service3", true, false);

        // Execute a paginated query when the entity parameter is null
        List<ServiceParseConfigEntity> resultPage = mapper.selectByPage(null, 2, 0);

        // Verify returned results
        assertEquals(2, resultPage.size()); // page size is limited to 2 records

        // Verify field contents
        assertEquals("service1", resultPage.get(0).getServiceName());
        assertEquals("service2", resultPage.get(1).getServiceName());
    }

    private void insertTestEntity(int id, String serviceName, boolean parsed, boolean enabled) {
        ServiceParseConfigEntity entity = ServiceParseConfigEntity.builder()
                .id(id)
                .serviceName(serviceName)
                .parsed(parsed)
                .enabled(enabled)
                .build();
        mapper.insert(entity);
    }


}