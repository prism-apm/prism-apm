package org.prism.apm.parser.repository;

import org.prism.apm.parser.MybatisTestBase;
import org.prism.apm.parser.enums.SpanKindEnum;
import org.prism.apm.parser.model.entity.ServiceCallEntity;

import org.prism.apm.parser.utils.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author john
 * @date 2024-08-26 16:53
 */
public class ServiceCallMapperTest extends MybatisTestBase {

    @Autowired
    private ServiceCallMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanServiceCallTable() {
        jdbcTemplate.update("DELETE FROM service_call");
    }

    @Test
    public void testBatchInsert() {
        // Create two different test records
        List<ServiceCallEntity> entities = createServiceCallEntities();

        // Insert data
        int result = mapper.batchInsert(entities);

        // Verify the inserted record count
        assertEquals(entities.size(), result);

        // Query the database and verify the result
        for (ServiceCallEntity entity : entities) {
            ServiceCallEntity dbEntity = mapper.selectByPrimaryKey(entity.getTime(), entity.getSpanId());
            assertNotNull(dbEntity);
            assertEquals(entity.getTime(), dbEntity.getTime());
            assertEquals(entity.getServiceName(), dbEntity.getServiceName());
            assertEquals(entity.getHostName(), dbEntity.getHostName());
            assertEquals(entity.getActionName(), dbEntity.getActionName());
            assertEquals(entity.getTargetNameId(), dbEntity.getTargetNameId());
            assertEquals(entity.getSystemType(), dbEntity.getSystemType());
            assertEquals(entity.getRemoteAddr(), dbEntity.getRemoteAddr());
            assertEquals(entity.getComponent(), dbEntity.getComponent());
            assertEquals(entity.getSpanKind(), dbEntity.getSpanKind());
            assertEquals(entity.getCallerTime(), dbEntity.getCallerTime());
            assertEquals(entity.getCallerServiceName(), dbEntity.getCallerServiceName());
            assertEquals(entity.getCallerHostName(), dbEntity.getCallerHostName());
            assertEquals(entity.getCallerActionName(), dbEntity.getCallerActionName());
            assertEquals(entity.getCallerTargetNameId(), dbEntity.getCallerTargetNameId());
            assertEquals(entity.getCallerSystemType(), dbEntity.getCallerSystemType());
            assertEquals(entity.getCallerRemoteAddr(), dbEntity.getCallerRemoteAddr());
            assertEquals(entity.getCallerComponent(), dbEntity.getCallerComponent());
            assertEquals(entity.getCallerSpanKind(), dbEntity.getCallerSpanKind());
            assertEquals(entity.getCnt(), dbEntity.getCnt());
            assertEquals(entity.getDuration(), dbEntity.getDuration());
            assertEquals(entity.getError(), dbEntity.getError());
        }
    }

    @Test
    public void testDeleteDataInRange() {

        // Insert test data
        List<ServiceCallEntity> entities = createServiceCallEntities();
        int insertResult = mapper.batchInsert(entities);
        assertEquals(entities.size(), insertResult);

        OffsetDateTime startTime = OffsetDateTime.parse("2023-10-08T00:00:00.000000Z");
        OffsetDateTime endTime = OffsetDateTime.parse("2023-10-09T23:59:59.999999Z");

        // Execute delete operation
        int deleteResult = mapper.deleteDataInRange(startTime, endTime);
        assertEquals(2, deleteResult); // Verify that two records were deleted

        // Verify remaining records
        List<ServiceCallEntity> remainingEntities = mapper.selectAll(); // assume there is a method to fetch all records
        assertEquals(1, remainingEntities.size()); // there should be only one record left
        assertEquals("service3", remainingEntities.get(0).getServiceName()); // Verify the remaining record is service3
    }

    @Test
    public void testDeleteDataInRange_WithBoundaryConditions() {
        // ---- 1. Define a clear time range for deletion: [startTime, endTime) ----
        OffsetDateTime startTime = OffsetDateTime.parse("2023-10-09T00:00:00.000000Z");
        OffsetDateTime endTime = OffsetDateTime.parse("2023-10-10T00:00:00.000000Z");

        // ---- 2. Obtain a valid entity template ----
        // Get a valid entity with all required fields populated from an existing helper method.
        // This ensures that all database constraints (e.g., NOT NULL) are met.
        ServiceCallEntity baseEntity = createServiceCallEntities().get(0);

        // ---- 3. Prepare a series of test data covering the boundary conditions ----
        // This assumes the entity class has the @Builder(toBuilder = true) annotation.
        List<ServiceCallEntity> boundaryEntities = Arrays.asList(
                // Scenario 1: Should NOT be deleted (time = startTime - 1 microsecond)
                baseEntity.toBuilder()
                        .time(startTime.minusNanos(1000)) // 1000 nanoseconds = 1 microsecond
                        .spanId("span_before_start")
                        .build(),

                // Scenario 2: Should be deleted (time = startTime)
                baseEntity.toBuilder()
                        .time(startTime)
                        .spanId("span_on_start")
                        .build(),

                // Scenario 3: Should be deleted (time = startTime + 1 microsecond)
                baseEntity.toBuilder()
                        .time(startTime.plusNanos(1000))
                        .spanId("span_after_start")
                        .build(),

                // Scenario 4: Should be deleted (time = endTime - 1 microsecond)
                baseEntity.toBuilder()
                        .time(endTime.minusNanos(1000))
                        .spanId("span_before_end")
                        .build(),

                // Scenario 5: Should NOT be deleted (time = endTime)
                baseEntity.toBuilder()
                        .time(endTime)
                        .spanId("span_on_end")
                        .build(),

                // Scenario 6: Should NOT be deleted (time = endTime + 1 microsecond)
                baseEntity.toBuilder()
                        .time(endTime.plusNanos(1000))
                        .spanId("span_after_end")
                        .build()
        );

        // ---- 4. Insert all test data into the database ----
        int insertResult = mapper.batchInsert(boundaryEntities);
        assertEquals(boundaryEntities.size(), insertResult, "Should successfully insert all boundary test data");

        // ---- 5. Execute the delete operation ----
        int deleteResult = mapper.deleteDataInRange(startTime, endTime);

        // ---- 6. Verify the results ----
        // Expected to delete 3 records: span_on_start, span_after_start, and span_before_end.
        assertEquals(3, deleteResult, "Should delete exactly 3 records within the time range");

        // Query the remaining data from the database.
        List<ServiceCallEntity> remainingEntities = mapper.selectAll();
        assertEquals(3, remainingEntities.size(), "There should be exactly 3 records left in the database");

        // Further verification to ensure the correct records remain.
        Map<String, ServiceCallEntity> remainingMap = new HashMap<>();
        for (ServiceCallEntity entity : remainingEntities) {
            remainingMap.put(entity.getSpanId(), entity);
        }

        assertTrue(remainingMap.containsKey("span_before_start"), "Record with time < startTime should be kept");
        assertTrue(remainingMap.containsKey("span_on_end"), "Record with time = endTime should be kept");
        assertTrue(remainingMap.containsKey("span_after_end"), "Record with time > endTime should be kept");

        assertFalse(remainingMap.containsKey("span_on_start"), "Record with time = startTime should have been deleted");
        assertFalse(remainingMap.containsKey("span_after_start"), "Record with time > startTime (in range) should have been deleted");
        assertFalse(remainingMap.containsKey("span_before_end"), "Record with time < endTime (in range) should have been deleted");
    }


    private List<ServiceCallEntity> createServiceCallEntities() {
        ServiceCallEntity entity1 = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-08T00:00:00.123456Z"))
                .traceId("traceId1")
                .spanId("spanId1")
                .serviceName("service1")
                .hostName("host1")
                .actionName("action1")
                .targetName("target1")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .systemType("actionName1")
                .remoteAddr("remoteAddr1")
                .component("component1")
                .spanKind(SpanKindEnum.SERVER.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)))
                .callerSpanId("callerSpanId1")
                .callerServiceName("callerService1")
                .callerHostName("callerHost1")
                .callerActionName("callerAction1")
                .callerTargetName("callerTargetName1")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
                .callerSystemType("callerSystemType1")
                .callerRemoteAddr("callerRemoteAddr1")
                .callerComponent("callerComponent1")
                .callerSpanKind(SpanKindEnum.CLIENT.getValue())
                .cnt(1L)
                .duration(1000L)
                .error(0L)
                .createTime(OffsetDateTime.parse("2023-10-08T00:00:01.123456Z"))
                .updateTime(OffsetDateTime.parse("2023-10-08T00:00:02.123456Z"))
                .build();

        ServiceCallEntity entity2 = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-09T00:00:00.654321Z"))
                .traceId("traceId2")
                .spanId("spanId2")
                .serviceName("service2")
                .hostName("host2")
                .actionName("action2")
                .targetName("target2")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .systemType("actionName2")
                .remoteAddr("remoteAddr2")
                .component("component2")
                .spanKind(SpanKindEnum.CLIENT.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(3)))
                .callerSpanId("callerSpanId2")
                .callerServiceName("callerService2")
                .callerHostName("callerHost2")
                .callerActionName("callerAction2")
                .callerTargetName("callerTargetName2")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000012"))
                .callerSystemType("callerSystemType2")
                .callerRemoteAddr("callerRemoteAddr2")
                .callerComponent("callerComponent2")
                .callerSpanKind(SpanKindEnum.SERVER.getValue())
                .cnt(2L)
                .duration(2000L)
                .error(1L)
                .createTime(OffsetDateTime.parse("2023-10-09T00:00:01.654321Z"))
                .updateTime(OffsetDateTime.parse("2023-10-09T00:00:02.654321Z"))
                .build();

        ServiceCallEntity entity3 = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-10T00:00:00.987654Z"))
                .traceId("traceId3")
                .spanId("spanId3")
                .serviceName("service3")
                .hostName("host3")
                .actionName("action3")
                .targetName("target3")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .systemType("actionName3")
                .remoteAddr("remoteAddr3")
                .component("component3")
                .spanKind(SpanKindEnum.CLIENT.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5)))
                .callerSpanId("callerSpanId3")
                .callerServiceName("callerService3")
                .callerHostName("callerHost3")
                .callerActionName("callerAction3")
                .callerTargetName("callerTargetName3")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000013"))
                .callerSystemType("callerSystemType3")
                .callerRemoteAddr("callerRemoteAddr3")
                .callerComponent("callerComponent3")
                .callerSpanKind(SpanKindEnum.SERVER.getValue())
                .cnt(3L)
                .duration(3000L)
                .error(1L)
                .createTime(OffsetDateTime.parse("2023-10-10T00:00:01.987654Z"))
                .updateTime(OffsetDateTime.parse("2023-10-10T00:00:02.987654Z"))
                .build();

        return Arrays.asList(entity1, entity2, entity3);
    }

    // case1: directly insert two records with upsert and verify all fields
    @Test
    public void testUpsert_withNoConflict() {
        List<ServiceCallEntity> entities = createServiceCallEntitiesForUpsert();

        // Execute UPSERT
        int result = mapper.batchUpsert(entities);

        // Verify UPSERT inserted two records successfully
        assertEquals(entities.size(), result);

        // Verify all fields were inserted correctly
        for (ServiceCallEntity entity : entities) {
            ServiceCallEntity dbEntity = mapper.selectByPrimaryKey(entity.getTime(), entity.getSpanId());
            assertEntityFields(entity, dbEntity);
        }

    }

    // case2: insert two records, then UPSERT to update them and verify updated fields
    @Test
    public void testUpsert_withAllConflict() {
        List<ServiceCallEntity> originalEntities = createServiceCallEntitiesForUpsert();
        Map<OffsetDateTime, ServiceCallEntity> originalEntityMap = new HashMap<>();
        for (ServiceCallEntity originalEntity : originalEntities) {
            originalEntityMap.put(originalEntity.getTime(), originalEntity);
        }

        // Mock batch insert
        mapper.batchInsert(originalEntities);

        // Mock batch UPSERT
        List<ServiceCallEntity> updatedEntities = createUpdatedServiceCallEntities();

        // Execute UPSERT update
        int upsertResult = mapper.batchUpsert(updatedEntities);
        assertEquals(updatedEntities.size(), upsertResult);

        // Verify all fields were updated correctly
        for (ServiceCallEntity updatedEntity : updatedEntities) {
            ServiceCallEntity dbEntity = mapper.selectByPrimaryKey(updatedEntity.getTime(), updatedEntity.getSpanId());
            assertEntityFieldsForUpsert(updatedEntity, dbEntity, originalEntityMap.get(updatedEntity.getTime()));
        }
    }

    // case3: insert two records, then UPSERT one update and one insert, verifying the final count and updates
    @Test
    public void testUpsert_WithPartialConflict() {
        List<ServiceCallEntity> originalEntities = createServiceCallEntitiesForUpsert();
        Map<OffsetDateTime, ServiceCallEntity> originalEntityMap = new HashMap<>();
        for (ServiceCallEntity originalEntity : originalEntities) {
            originalEntityMap.put(originalEntity.getTime(), originalEntity);
        }


        // Mock batch insert
        mapper.batchInsert(originalEntities);

        // Mock batch UPSERT with one conflicting row and one non-conflicting row
        List<ServiceCallEntity> partialUpdatedEntities = createPartialConflictServiceCallEntities();

        // Execute UPSERT
        int upsertResult = mapper.batchUpsert(partialUpdatedEntities);
        assertEquals(partialUpdatedEntities.size(), upsertResult);

        // Verify that there are three records
        List<ServiceCallEntity> allEntities = mapper.selectAll();
        assertEquals(3, allEntities.size());

        // Verify updated and newly inserted data
        for (ServiceCallEntity entity : allEntities) {
            if (entity.getServiceName().equals("service1")) {
                assertEntityFieldsForUpsert(partialUpdatedEntities.get(0), entity
                        , originalEntityMap.get(entity.getTime())); // Verify update
            } else if (entity.getServiceName().equals("service2")) {
                assertEntityFields(originalEntities.get(1), entity); // Verify unchanged record
            } else {
                assertEntityFields(partialUpdatedEntities.get(1), entity); // Verify newly inserted record
            }
        }
    }


    // case1: directly insert two records with batchUpsertDoNothing and verify all fields
    @Test
    public void testUpsertDoNothing_withNoConflict() {
        List<ServiceCallEntity> entities = createServiceCallEntities();

        // Execute UPSERT DO NOTHING
        int result = mapper.batchUpsertDoNothing(entities);

        // Verify UPSERT DO NOTHING inserted two records successfully.
        assertEquals(entities.size(), result);

        // Verify all fields were inserted correctly
        for (ServiceCallEntity entity : entities) {
            ServiceCallEntity dbEntity = mapper.selectByPrimaryKey(entity.getTime(), entity.getSpanId());
            assertEntityFields(entity, dbEntity);
        }

    }

    // case2: insert two records, then UPSERT DO NOTHING and verify no updates
    @Test
    public void testUpsertDoNothing_withAllConflict() {
        List<ServiceCallEntity> originalEntities = createServiceCallEntitiesForUpsert();

        mapper.batchInsert(originalEntities);


        // Execute UPSERT DO NOTHING
        int upsertResult = mapper.batchUpsertDoNothing(originalEntities);
        assertEquals(0, upsertResult); // no updated rows

        // Verify inserted data was not updated
        for (ServiceCallEntity entity : originalEntities) {
            ServiceCallEntity dbEntity = mapper.selectByPrimaryKey(entity.getTime(), entity.getSpanId());
            assertEntityFields(entity, dbEntity);
        }

        // Verify the record count is still two
        List<ServiceCallEntity> allEntities = mapper.selectAll();
        assertEquals(2, allEntities.size());
    }

    // case3: insert two records, then run UPSERT and verify no-update behavior
    @Test
    public void testUpsertDoNothing_withPartialConflict() {
        List<ServiceCallEntity> originalEntities = createServiceCallEntitiesForUpsert();

        mapper.batchInsert(originalEntities);

        List<ServiceCallEntity> partialEntities = createPartialConflictServiceCallEntities();

        // Execute UPSERT DO NOTHING
        int upsertResult = mapper.batchUpsertDoNothing(partialEntities);
        assertEquals(1, upsertResult); // insert one new record

        // Verify that there are three records
        List<ServiceCallEntity> allEntities = mapper.selectAll();
        assertEquals(3, allEntities.size());

        // Verify conflicting data was not updated
        ServiceCallEntity conflictedEntity = mapper.selectByPrimaryKey(originalEntities.get(0).getTime(), originalEntities.get(0).getSpanId());
        assertEntityFields(originalEntities.get(0), conflictedEntity);

        // Verify new data was inserted correctly
        ServiceCallEntity newEntity = mapper.selectByPrimaryKey(partialEntities.get(1).getTime(), partialEntities.get(1).getSpanId());
        assertEntityFields(partialEntities.get(1), newEntity);
    }

    // Helper method for verifying all fields
    private void assertEntityFields(ServiceCallEntity expected, ServiceCallEntity actual) {
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime());
        assertEquals(expected.getSpanId(), actual.getSpanId());
        assertEquals(expected.getServiceName(), actual.getServiceName());
        assertEquals(expected.getHostName(), actual.getHostName());
        assertEquals(expected.getActionName(), actual.getActionName());
        assertEquals(expected.getTargetNameId(), actual.getTargetNameId());
        assertEquals(expected.getSystemType(), actual.getSystemType());
        assertEquals(expected.getRemoteAddr(), actual.getRemoteAddr());
        assertEquals(expected.getComponent(), actual.getComponent());
        assertEquals(expected.getSpanKind(), actual.getSpanKind());
        assertEquals(expected.getCallerTime(), actual.getCallerTime());
        assertEquals(expected.getCallerSpanId(), actual.getCallerSpanId());
        assertEquals(expected.getCallerServiceName(), actual.getCallerServiceName());
        assertEquals(expected.getCallerHostName(), actual.getCallerHostName());
        assertEquals(expected.getCallerActionName(), actual.getCallerActionName());
        assertEquals(expected.getCallerTargetNameId(), actual.getCallerTargetNameId());
        assertEquals(expected.getCallerSystemType(), actual.getCallerSystemType());
        assertEquals(expected.getCallerRemoteAddr(), actual.getCallerRemoteAddr());
        assertEquals(expected.getCallerComponent(), actual.getCallerComponent());
        assertEquals(expected.getCallerSpanKind(), actual.getCallerSpanKind());
        assertEquals(expected.getCnt(), actual.getCnt());
        assertEquals(expected.getDuration(), actual.getDuration());
        assertEquals(expected.getError(), actual.getError());
        assertEquals(expected.getCreateTime(), actual.getCreateTime());
        assertEquals(expected.getUpdateTime(), actual.getUpdateTime());
    }

    private void assertEntityFieldsForUpsert(ServiceCallEntity expected,
            ServiceCallEntity actual,
            ServiceCallEntity original) {
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime());
        assertEquals(expected.getSpanId(), actual.getSpanId());
        assertEquals(original.getServiceName(), actual.getServiceName()); // serviceName is not updated
        assertEquals(expected.getHostName(), actual.getHostName());
        assertEquals(expected.getActionName(), actual.getActionName());
        assertEquals(expected.getTargetNameId(), actual.getTargetNameId());
        assertEquals(expected.getSystemType(), actual.getSystemType());
        assertEquals(expected.getRemoteAddr(), actual.getRemoteAddr());
        assertEquals(expected.getComponent(), actual.getComponent());
        assertEquals(expected.getSpanKind(), actual.getSpanKind());
        assertEquals(expected.getCallerTime(), actual.getCallerTime());
        assertEquals(expected.getCallerSpanId(), actual.getCallerSpanId());
        assertEquals(expected.getCallerServiceName(), actual.getCallerServiceName());
        assertEquals(expected.getCallerHostName(), actual.getCallerHostName());
        assertEquals(expected.getCallerActionName(), actual.getCallerActionName());
        assertEquals(expected.getCallerTargetNameId(), actual.getCallerTargetNameId());
        assertEquals(expected.getCallerSystemType(), actual.getCallerSystemType());
        assertEquals(expected.getCallerRemoteAddr(), actual.getCallerRemoteAddr());
        assertEquals(expected.getCallerComponent(), actual.getCallerComponent());
        assertEquals(expected.getCallerSpanKind(), actual.getCallerSpanKind());
        assertEquals(expected.getCnt(), actual.getCnt());
        assertEquals(expected.getDuration(), actual.getDuration());
        assertEquals(expected.getError(), actual.getError());
        assertEquals(original.getCreateTime(), actual.getCreateTime()); // createTime is not updated
        assertEquals(expected.getUpdateTime(), actual.getUpdateTime());
    }

    // Create mock ServiceCallEntity instances
    private List<ServiceCallEntity> createServiceCallEntitiesForUpsert() {
        // Two initial records
        ServiceCallEntity entity1 = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-08T00:00:00.111222Z"))
                .traceId("traceId1")
                .spanId("spanId1")
                .serviceName("service1")
                .hostName("host1")
                .actionName("action1")
                .targetName("target1")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .systemType("systemType1")
                .remoteAddr("remoteAddr1")
                .component("component1")
                .spanKind(SpanKindEnum.SERVER.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)))
                .callerSpanId("callerSpanId1")
                .callerServiceName("callerService1")
                .callerHostName("callerHost1")
                .callerActionName("callerAction1")
                .callerTargetName("callerTargetName1")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
                .callerSystemType("callerSystemType1")
                .callerRemoteAddr("callerRemoteAddr1")
                .callerComponent("callerComponent1")
                .callerSpanKind(SpanKindEnum.CLIENT.getValue())
                .cnt(1L)
                .duration(1000L)
                .error(0L)
                .createTime(OffsetDateTime.parse("2023-10-08T00:00:01.111222Z"))
                .updateTime(OffsetDateTime.parse("2023-10-08T00:00:02.111222Z"))
                .build();

        ServiceCallEntity entity2 = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-09T00:00:00.333444Z"))
                .traceId("traceId2")
                .spanId("spanId2")
                .serviceName("service2")
                .hostName("host2")
                .actionName("action2")
                .targetName("target2")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .systemType("systemType2")
                .remoteAddr("remoteAddr2")
                .component("component2")
                .spanKind(SpanKindEnum.CLIENT.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(3)))
                .callerSpanId("callerSpanId2")
                .callerServiceName("callerService2")
                .callerHostName("callerHost2")
                .callerActionName("callerAction2")
                .callerTargetName("callerTargetName2")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000012"))
                .callerSystemType("callerSystemType2")
                .callerRemoteAddr("callerRemoteAddr2")
                .callerComponent("callerComponent2")
                .callerSpanKind(SpanKindEnum.SERVER.getValue())
                .cnt(2L)
                .duration(2000L)
                .error(1L)
                .createTime(OffsetDateTime.parse("2023-10-09T00:00:01.333444Z"))
                .updateTime(OffsetDateTime.parse("2023-10-09T00:00:02.333444Z"))
                .build();

        return Arrays.asList(entity1, entity2);
    }

    // Create updated data
    private List<ServiceCallEntity> createUpdatedServiceCallEntities() {
        ServiceCallEntity updatedEntity1 = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-08T00:00:00.111222Z")) // primary key, unchanged
                .traceId("traceId1")
                .spanId("spanId1")
                .serviceName("updatedService1")
                .hostName("updatedHost1")
                .actionName("updatedAction1")
                .targetName("updatedTarget1")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .systemType("updatedSystemType1")
                .remoteAddr("updatedRemoteAddr1")
                .component("updatedComponent1")
                .spanKind(SpanKindEnum.CLIENT.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5)))
                .callerSpanId("updatedCallerSpanId1")
                .callerServiceName("updatedCallerService1")
                .callerHostName("updatedCallerHost1")
                .callerActionName("updatedCallerAction1")
                .callerTargetName("updatedCallerTargetName1")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000013"))
                .callerSystemType("updatedCallerSystemType1")
                .callerRemoteAddr("updatedCallerRemoteAddr1")
                .callerComponent("updatedCallerComponent1")
                .callerSpanKind(SpanKindEnum.CLIENT.getValue())
                .cnt(11L)
                .duration(3000L)
                .error(1L)
                .createTime(OffsetDateTime.parse("2023-10-08T00:00:11.555666Z"))
                .updateTime(OffsetDateTime.parse("2023-10-08T00:00:12.555666Z"))
                .build();

        ServiceCallEntity updatedEntity2 = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-09T00:00:00.333444Z")) // primary key, unchanged
                .traceId("traceId2")
                .spanId("spanId2")
                .serviceName("updateService2")
                .hostName("updatedHost2")
                .actionName("updatedAction2")
                .targetName("updatedTarget2")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000004"))
                .systemType("updatedSystemType2")
                .remoteAddr("updatedRemoteAddr2")
                .component("updatedComponent2")
                .spanKind(SpanKindEnum.SERVER.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10)))
                .callerSpanId("updateSpanId2")
                .callerServiceName("updatedCallerService2")
                .callerHostName("updatedCallerHost2")
                .callerActionName("updatedCallerAction2")
                .callerTargetName("updatedCallerTargetName2")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000014"))
                .callerSystemType("updatedCallerSystemType2")
                .callerRemoteAddr("updatedCallerRemoteAddr2")
                .callerComponent("updatedCallerComponent2")
                .callerSpanKind(SpanKindEnum.SERVER.getValue())
                .cnt(22L)
                .duration(5000L)
                .error(0L)
                .createTime(OffsetDateTime.parse("2023-10-09T00:00:21.777888Z"))
                .updateTime(OffsetDateTime.parse("2023-10-09T00:00:22.777888Z"))
                .build();

        return Arrays.asList(updatedEntity1, updatedEntity2);
    }

    // Create partially conflicting data
    private List<ServiceCallEntity> createPartialConflictServiceCallEntities() {
        ServiceCallEntity updatedEntity1 = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-08T00:00:00.111222Z")) // conflicts with the first record in originalEntities
                .traceId("traceId1")
                .spanId("spanId1")
                .serviceName("newService1")
                .hostName("newHost1")
                .actionName("newAction1")
                .targetName("newTarget1")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000005"))
                .systemType("newSystemType1")
                .remoteAddr("newRemoteAddr1")
                .component("newComponent1")
                .spanKind(SpanKindEnum.CLIENT.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(7)))
                .callerSpanId("newSpanId1")
                .callerServiceName("newCallerService1")
                .callerHostName("newCallerHost1")
                .callerActionName("newCallerAction1")
                .callerTargetName("newCallerTargetName1")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000015"))
                .callerSystemType("newCallerSystemType1")
                .callerRemoteAddr("newCallerRemoteAddr1")
                .callerComponent("newCallerComponent1")
                .callerSpanKind(SpanKindEnum.SERVER.getValue())
                .cnt(1L)
                .duration(4000L)
                .error(1L)
                .createTime(OffsetDateTime.parse("2023-10-08T00:00:11.121212Z"))
                .updateTime(OffsetDateTime.parse("2023-10-08T00:00:12.121212Z"))
                .build();

        ServiceCallEntity newEntity = ServiceCallEntity.builder()
                .time(OffsetDateTime.parse("2023-10-10T00:00:00.999888Z")) // new record, no conflict
                .traceId("traceId3")
                .spanId("spanId3")
                .serviceName("service3")
                .hostName("host3")
                .actionName("action3")
                .targetName("target3")
                .targetNameId(UUID.fromString("00000000-0000-0000-0000-000000000006"))
                .systemType("systemType3")
                .remoteAddr("remoteAddr3")
                .component("component3")
                .spanKind(SpanKindEnum.SERVER.getValue())
                .callerTime(CommonUtil.withStandardPrecision(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(15)))
                .callerSpanId("callerSpanId3")
                .callerServiceName("callerService3")
                .callerHostName("callerHost3")
                .callerActionName("callerAction3")
                .callerTargetName("callerTargetName3")
                .callerTargetNameId(UUID.fromString("00000000-0000-0000-0000-000000000016"))
                .callerSystemType("callerSystemType3")
                .callerRemoteAddr("callerRemoteAddr3")
                .callerComponent("callerComponent3")
                .callerSpanKind(SpanKindEnum.CLIENT.getValue())
                .cnt(3L)
                .duration(6000L)
                .error(0L)
                .createTime(OffsetDateTime.parse("2023-10-10T00:00:31.999888Z"))
                .updateTime(OffsetDateTime.parse("2023-10-10T00:00:32.999888Z"))
                .build();

        return Arrays.asList(updatedEntity1, newEntity);
    }
}
