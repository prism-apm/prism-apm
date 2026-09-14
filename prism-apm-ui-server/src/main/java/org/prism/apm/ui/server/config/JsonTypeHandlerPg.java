package org.prism.apm.ui.server.config;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(String.class)
public class JsonTypeHandlerPg extends BaseTypeHandler<String> {

    /**
     * Use PGobject utility class provided by PostgreSQL
     */
    private final PGobject JSON_OBJECT = new PGobject();
    public static final String JSON_TYPE = "json";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String param, JdbcType jdbcType) throws SQLException {
        JSON_OBJECT.setType(JSON_TYPE);
        JSON_OBJECT.setValue(param);
        ps.setObject(i, JSON_OBJECT);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String sqlJson = rs.getString(columnName);
        if (StringUtils.isNotBlank(sqlJson)) {
            return sqlJson;
        }
        return null;
    }

    //Get nullable result by column index
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String sqlJson = rs.getString(columnIndex);
        if (StringUtils.isNotBlank(sqlJson)) {
            return sqlJson;
        }
        return null;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String sqlJson = cs.getString(columnIndex);
        if (StringUtils.isNotBlank(sqlJson)) {
            return sqlJson;
        }
        return null;
    }
}
