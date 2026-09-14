package org.prism.apm.ui.server.common;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface QueryPlanExplainMapper {

    @Select("EXPLAIN ANALYZE ${sql}")
    List<Map<String, String>> explain(@Param("sql")String sql);

}
