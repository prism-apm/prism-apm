package org.prism.apm.common.repository;

import org.prism.apm.common.MybatisTestBase;
import org.prism.apm.common.model.entity.FieldContentReplacerRuleEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author john
 * @date 2025-09-11 14:20
 */
public class FieldContentReplacerRuleMapperTest extends MybatisTestBase {

    @Autowired
    private FieldContentReplacerRuleMapper mapper;

    private static class RuleBuilder {
        public FieldContentReplacerRuleEntity build(short uniqueifier, boolean enabled) {
            return FieldContentReplacerRuleEntity.builder()
                    .serviceName("service-" + uniqueifier)
                    .component("component-" + uniqueifier)
                    .spanKind(uniqueifier)
                    .fieldName("field-" + uniqueifier)
                    .matchType("matchType-" + uniqueifier)
                    .searchContent("searchContent-" + uniqueifier)
                    .replacement("replacement-" + uniqueifier)
                    .enabled(enabled)
                    .build();
        }
    }

    @Test
    public void testInsert_And_SelectByPrimaryKey() {
        // Arrange
        FieldContentReplacerRuleEntity newEntity = new RuleBuilder().build((short) 1, true);
        mapper.insert(newEntity);

        // Act
        FieldContentReplacerRuleEntity selectedEntity = mapper.selectByPrimaryKey(newEntity.getId());

        // Assert
        assertNotNull(selectedEntity);
        assertEquals(newEntity.getId(), selectedEntity.getId());
        assertEquals(newEntity.getServiceName(), selectedEntity.getServiceName());
        assertEquals(newEntity.getComponent(), selectedEntity.getComponent());
        assertEquals(newEntity.getFieldName(), selectedEntity.getFieldName());
        assertEquals(newEntity.getSpanKind(), selectedEntity.getSpanKind());
        assertEquals(newEntity.getMatchType(), selectedEntity.getMatchType());
        assertEquals(newEntity.getSearchContent(), selectedEntity.getSearchContent());
        assertEquals(newEntity.getReplacement(), selectedEntity.getReplacement());
        assertEquals(newEntity.getEnabled(), selectedEntity.getEnabled());
    }

    @Test
    public void testSelectByAll_WithSingleCondition() {
        // Arrange
        RuleBuilder builder = new RuleBuilder();
        FieldContentReplacerRuleEntity entity1 = builder.build((short) 1, true);
        FieldContentReplacerRuleEntity entity2 = builder.build((short) 2, true);
        mapper.insert(entity1);
        mapper.insert(entity2);

        // Act
        FieldContentReplacerRuleEntity queryEntity = new FieldContentReplacerRuleEntity();
        queryEntity.setServiceName(entity1.getServiceName());
        List<FieldContentReplacerRuleEntity> results = mapper.selectByAll(queryEntity);

        // Assert
        assertEquals(1, results.size());
        assertEquals(entity1.getServiceName(), results.get(0).getServiceName());
    }

    @Test
    public void testUpdateByPrimaryKeySelective_WithPartialFields() {
        // Arrange
        RuleBuilder builder = new RuleBuilder();
        FieldContentReplacerRuleEntity originalEntity = builder.build((short) 1, true);
        mapper.insert(originalEntity);

        // Create a different entity to source new values from, ensuring they are not hardcoded.
        FieldContentReplacerRuleEntity sourceForUpdate = builder.build((short) 2, false);

        // Act
        FieldContentReplacerRuleEntity updateEntity = FieldContentReplacerRuleEntity.builder()
                .id(originalEntity.getId())
                .fieldName(sourceForUpdate.getFieldName()) // Value from another generated entity
                .enabled(sourceForUpdate.getEnabled())     // Value from another generated entity
                .build();
        mapper.updateByPrimaryKeySelective(updateEntity);

        // Assert
        FieldContentReplacerRuleEntity updatedEntity = mapper.selectByPrimaryKey(originalEntity.getId());
        assertNotNull(updatedEntity);
        // Assert that fields were updated correctly
        assertEquals(updateEntity.getFieldName(), updatedEntity.getFieldName());
        assertEquals(updateEntity.getEnabled(), updatedEntity.getEnabled());
        // Assert that other fields were NOT updated
        assertEquals(originalEntity.getServiceName(), updatedEntity.getServiceName());
        assertEquals(originalEntity.getComponent(), updatedEntity.getComponent());
        assertEquals(originalEntity.getSpanKind(), updatedEntity.getSpanKind());
        assertEquals(originalEntity.getMatchType(), updatedEntity.getMatchType());
        assertEquals(originalEntity.getSearchContent(), updatedEntity.getSearchContent());
        assertEquals(originalEntity.getReplacement(), updatedEntity.getReplacement());
        assertNotEquals(originalEntity.getFieldName(), updatedEntity.getFieldName());
    }

    @Test
    public void testDeleteByPrimaryKey_AfterInsert() {
        // Arrange
        RuleBuilder builder = new RuleBuilder();
        FieldContentReplacerRuleEntity entityToDelete = builder.build((short) 1, true);
        FieldContentReplacerRuleEntity entityToKeep = builder.build((short) 2, false);
        mapper.insert(entityToDelete);
        mapper.insert(entityToKeep);

        // Pre-Assert: Ensure both entities are in the database before deletion
        assertNotNull(mapper.selectByPrimaryKey(entityToDelete.getId()));
        assertNotNull(mapper.selectByPrimaryKey(entityToKeep.getId()));

        // Act
        int deletedRows = mapper.deleteByPrimaryKey(entityToDelete.getId());

        // Assert
        assertEquals(1, deletedRows);
        assertNull(mapper.selectByPrimaryKey(entityToDelete.getId()), "The deleted entity should not be found.");
        assertNotNull(mapper.selectByPrimaryKey(entityToKeep.getId()), "The other entity should still exist.");
    }

    @Test
    public void testSelectByAll_WithMultipleConditions() {
        // Arrange
        RuleBuilder builder = new RuleBuilder();
        FieldContentReplacerRuleEntity entity1 = builder.build((short) 1, true);
        FieldContentReplacerRuleEntity entity2 = builder.build((short) 2, false);
        FieldContentReplacerRuleEntity entity3 = builder.build((short) 1, true); // Same service name as entity1
        mapper.insert(entity1);
        mapper.insert(entity2);
        mapper.insert(entity3);

        // Act
        FieldContentReplacerRuleEntity queryEntity = new FieldContentReplacerRuleEntity();
        queryEntity.setServiceName(entity1.getServiceName());
        queryEntity.setEnabled(true);
        List<FieldContentReplacerRuleEntity> results = mapper.selectByAll(queryEntity);

        // Assert
        assertEquals(2, results.size());
        for (FieldContentReplacerRuleEntity result : results) {
            assertEquals(entity1.getServiceName(), result.getServiceName());
            assertTrue(result.getEnabled());
        }
    }

    @Test
    public void testSelectByAll_WhenNoMatch() {
        // Arrange
        mapper.insert(new RuleBuilder().build((short) 1, true));

        // Act
        FieldContentReplacerRuleEntity queryEntity = new FieldContentReplacerRuleEntity();
        queryEntity.setServiceName("non-existent-service");
        List<FieldContentReplacerRuleEntity> results = mapper.selectByAll(queryEntity);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSelectByAll_WhenNoConditions() {
        // Arrange
        mapper.insert(new RuleBuilder().build((short) 1, true));
        mapper.insert(new RuleBuilder().build((short) 2, false));

        // Act
        List<FieldContentReplacerRuleEntity> results = mapper.selectByAll(new FieldContentReplacerRuleEntity());

        // Assert
        assertEquals(2, results.size());
    }

    @Test
    public void testInsertSelective_WithAllFields() {
        // Arrange
        FieldContentReplacerRuleEntity selectiveEntity = new RuleBuilder().build((short) 1, true);

        // Act
        mapper.insertSelective(selectiveEntity);

        // Assert
        FieldContentReplacerRuleEntity selectedEntity = mapper.selectByPrimaryKey(selectiveEntity.getId());
        assertNotNull(selectedEntity);
        assertEquals(selectiveEntity.getServiceName(), selectedEntity.getServiceName());
        assertEquals(selectiveEntity.getComponent(), selectedEntity.getComponent());
        assertEquals(selectiveEntity.getSpanKind(), selectedEntity.getSpanKind());
        assertEquals(selectiveEntity.getFieldName(), selectedEntity.getFieldName());
        assertEquals(selectiveEntity.getMatchType(), selectedEntity.getMatchType());
        assertEquals(selectiveEntity.getSearchContent(), selectedEntity.getSearchContent());
        assertEquals(selectiveEntity.getReplacement(), selectedEntity.getReplacement());
        assertEquals(selectiveEntity.getEnabled(), selectedEntity.getEnabled());
    }

    @Test
    public void testUpdateByPrimaryKey_WithAllFields() {
        // Arrange
        RuleBuilder builder = new RuleBuilder();
        FieldContentReplacerRuleEntity originalEntity = builder.build((short) 1, true);
        mapper.insert(originalEntity);

        // Act
        FieldContentReplacerRuleEntity updateEntity = builder.build((short) 2, false);
        updateEntity.setId(originalEntity.getId());
        mapper.updateByPrimaryKey(updateEntity);

        // Assert
        FieldContentReplacerRuleEntity updatedEntity = mapper.selectByPrimaryKey(originalEntity.getId());
        assertEquals(updateEntity.getServiceName(), updatedEntity.getServiceName());
        assertEquals(updateEntity.getComponent(), updatedEntity.getComponent());
        assertEquals(updateEntity.getSpanKind(), updatedEntity.getSpanKind());
        assertEquals(updateEntity.getFieldName(), updatedEntity.getFieldName());
        assertEquals(updateEntity.getMatchType(), updatedEntity.getMatchType());
        assertEquals(updateEntity.getSearchContent(), updatedEntity.getSearchContent());
        assertEquals(updateEntity.getReplacement(), updatedEntity.getReplacement());
        assertEquals(updateEntity.getEnabled(), updatedEntity.getEnabled());
    }

    

    @Test
    public void testSelectByAll_WithAllConditions() {
        // Arrange
        RuleBuilder builder = new RuleBuilder();
        FieldContentReplacerRuleEntity targetEntity = builder.build((short) 1, true);
        FieldContentReplacerRuleEntity distractorEntity = builder.build((short) 2, false);
        mapper.insert(targetEntity);
        mapper.insert(distractorEntity);

        // Act
        // Create a query object with all fields from the target entity
        FieldContentReplacerRuleEntity queryEntity = FieldContentReplacerRuleEntity.builder()
                .id(targetEntity.getId())
                .serviceName(targetEntity.getServiceName())
                .component(targetEntity.getComponent())
                .spanKind(targetEntity.getSpanKind())
                .fieldName(targetEntity.getFieldName())
                .matchType(targetEntity.getMatchType())
                .searchContent(targetEntity.getSearchContent())
                .replacement(targetEntity.getReplacement())
                .enabled(targetEntity.getEnabled())
                .build();

        List<FieldContentReplacerRuleEntity> results = mapper.selectByAll(queryEntity);

        // Assert
        assertEquals(1, results.size());
        FieldContentReplacerRuleEntity result = results.get(0);
        assertEquals(targetEntity.getId(), result.getId());
        assertEquals(targetEntity.getServiceName(), result.getServiceName());
        assertEquals(targetEntity.getComponent(), result.getComponent());
        assertEquals(targetEntity.getSpanKind(), result.getSpanKind());
        assertEquals(targetEntity.getFieldName(), result.getFieldName());
        assertEquals(targetEntity.getMatchType(), result.getMatchType());
        assertEquals(targetEntity.getSearchContent(), result.getSearchContent());
        assertEquals(targetEntity.getReplacement(), result.getReplacement());
        assertEquals(targetEntity.getEnabled(), result.getEnabled());
    }
}