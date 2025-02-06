package top.zephyrs.mybatis.semi.injects.methods;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.annotations.query.*;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.MetaHelper;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectByQuery extends AbstractInjectMethod {


    @Override
    public String getId() {
        return "selectByQuery";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.SELECT;
    }

    @Override
    public SqlSource createSqlSource(SemiMybatisConfiguration configuration,
                                     MetaInfo metaInfo,
                                     Method method,
                                     Class<?> parameterTypeClass,
                                     LanguageDriver languageDriver) {
        return (Object parameterObject) -> {
            // 查询的列
            Set<String> needSelectColumns = new HashSet<>();
            for (ColumnInfo column : metaInfo.getColumns()) {
                if (column.isSelect()) {
                    needSelectColumns.add(column.getColumnName());
                }
            }

            String columns = String.join(",", needSelectColumns);

            String whereScript = getWhereScript(parameterObject, metaInfo);

            String sqlScript;

            String SQL_TMPL = "<script>select %s from %s %s</script>";
            sqlScript = String.format(SQL_TMPL, columns, metaInfo.getTableName(), whereScript);
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlScript, parameterTypeClass);
            return sqlSource.getBoundSql(parameterObject);
        };
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration, MetaInfo metaInfo) {
        return EMPTY_STR;
    }

    protected String getWhereScript(Object parameterObject, MetaInfo metaInfo) {
        // 查询的条件
        StringBuilder whereScript = new StringBuilder("<where>");
        //查询参数类型
        Class<?> queryType;
        if (parameterObject instanceof Map) {
            Object param = ((Map<?, ?>) parameterObject).get("query");
            queryType = param.getClass();
        } else {
            queryType = parameterObject.getClass();
        }
        if(Map.class.isAssignableFrom(queryType)) {
            Map<String, ?> param = (Map<String, ?>) ((Map<?, ?>) parameterObject).get("query");
            for(String key: param.keySet()) {
                whereScript.append(equal(key, param.get(key), metaInfo));
            }
        }else {
            Field[] fields = queryType.getDeclaredFields();
            for (Field field : fields) {
                StringBuilder fieldWhere = new StringBuilder();
                fieldWhere.append(between(field, metaInfo));
                fieldWhere.append(greaterThan(field, metaInfo));
                fieldWhere.append(greaterThanOrEqual(field, metaInfo));
                fieldWhere.append(lessThan(field, metaInfo));
                fieldWhere.append(lessThanOrEqual(field, metaInfo));
                fieldWhere.append(like(field, metaInfo));
                fieldWhere.append(likeLeft(field, metaInfo));
                fieldWhere.append(likeRight(field, metaInfo));
                fieldWhere.append(in(field, metaInfo));
                // 如果以上注解都没，则是equal
                if (fieldWhere.toString().trim().isEmpty()) {
                    fieldWhere.append(equal(field, metaInfo));
                }
                whereScript.append(fieldWhere);
            }
        }

        // 逻辑删除的要过滤
        if (metaInfo.isLogical()) {
            whereScript
                    .append(" AND ")
                    .append(metaInfo.getLogicalColumn().getColumnName())
                    .append("=")
                    .append(metaInfo.getNoDeletedValue());
        }
        whereScript.append("</where>");
        return whereScript.toString();
    }

    private String equal(String fieldName, Object fieldValue, MetaInfo metaInfo) {
        if(fieldValue == null) {
            return EMPTY_STR;
        }
        String column = this.parseSelectColumn(metaInfo, fieldName);
        if (column == null) {
            return EMPTY_STR;
        }
        if (fieldValue.getClass() == String.class) {
            String script = "<if test=\"query.%s != null and query.%s != ''\">AND %s=#{query.%s}</if>";
            return String.format(script, fieldName, fieldName, column, fieldName);
        } else {
            String script = "<if test=\"query.%s != null\">AND %s=#{query.%s}</if>";
            return String.format(script, fieldName, column, fieldName);
        }
    }

    private String equal(Field field, MetaInfo metaInfo) {
        Equal equal = field.getAnnotation(Equal.class);
        String column;
        if(equal == null) {
            column = this.parseSelectColumn(metaInfo, field, EMPTY_STR, null);
        }else {
            column = this.parseSelectColumn(metaInfo, field, equal.value(), equal.column());
        }
        if (column == null) {
            return EMPTY_STR;
        }
        String fieldName = field.getName();
        if (field.getType() == String.class) {
            String script = "<if test=\"query.%s != null and query.%s != ''\">AND %s=#{query.%s}</if>";
            return String.format(script, fieldName, fieldName, column, fieldName);
        } else {
            String script = "<if test=\"query.%s != null\">AND %s=#{query.%s}</if>";
            return String.format(script, fieldName, column, fieldName);
        }
    }

    private String between(Field field, MetaInfo metaInfo) {
        Between between = field.getAnnotation(Between.class);
        if (between == null || !(Collection.class.isAssignableFrom(field.getType()))) {
            return EMPTY_STR;
        }
        String column = this.parseSelectColumn(metaInfo, field, between.value(), between.column());
        if (column == null) {
            return EMPTY_STR;
        }
        String fieldName = field.getName();
        String script = "<if test=\"query.%s != null and query.%s.size()>1\">AND %s between #{query.%s[0]} and #{query.%s[1]}</if>";
        return String.format(script, fieldName, fieldName, column, fieldName, fieldName);
    }

    private String in(Field field, MetaInfo metaInfo) {
        In in = field.getAnnotation(In.class);
        if (in == null || !Collection.class.isAssignableFrom(field.getType())) {
            return EMPTY_STR;
        }
        String column = this.parseSelectColumn(metaInfo, field, in.value(), in.column());
        if (column == null) {
            return EMPTY_STR;
        }
        String script = "<if test=\"query.%s != null and query.%s.size()>0\">" +
                "AND %s IN " +
                "<foreach collection=\"query." + field.getName() + "\" item=\"item\" index=\"index\" open=\"(\" close=\")\" separator=\",\">" +
                "#{item}</foreach>" +
                "</if>";
        return String.format(script, field.getName(), field.getName(), column);
    }


    private String like(Field field, MetaInfo metaInfo) {
        Like annotation = field.getAnnotation(Like.class);
        return annotation == null ? EMPTY_STR : parseLike(field, metaInfo, annotation.value(), annotation.column(), "full");
    }

    private String likeLeft(Field field, MetaInfo metaInfo) {
        LikeLeft annotation = field.getAnnotation(LikeLeft.class);
        return annotation == null ? EMPTY_STR : parseLike(field, metaInfo, annotation.value(), annotation.column(), "left");
    }

    private String likeRight(Field field, MetaInfo metaInfo) {
        LikeRight annotation = field.getAnnotation(LikeRight.class);
        return annotation == null ? EMPTY_STR : parseLike(field, metaInfo, annotation.value(), annotation.column(), "right");
    }

    private String parseLike(Field field, MetaInfo metaInfo, String annotaionValue, String columnName, String type) {
        String column = this.parseSelectColumn(metaInfo, field, annotaionValue, columnName);
        if (column == null) {
            return EMPTY_STR;
        }
        String fieldName = field.getName();
        String like;
        if ("left".equals(type)) {
            like = "concat('%',#{query." + fieldName + "})";
        } else if ("right".equals(type)) {
            like = "concat(#{query." + fieldName + "},'%')";
        } else {
            like = "concat('%',#{query." + fieldName + "},'%')";
        }
        String script = "<if test=\"query.%s != null and query.%s != ''\">AND %s LIKE %s</if>";
        return String.format(script, fieldName, fieldName, column, like);
    }

    private String greaterThan(Field field, MetaInfo metaInfo) {
        GreaterThan annotation = field.getAnnotation(GreaterThan.class);
        return annotation == null ? EMPTY_STR : this.parseGreaterOrLess(field, metaInfo, annotation.value(), annotation.column(), "&gt;");
    }

    private String greaterThanOrEqual(Field field, MetaInfo metaInfo) {
        GreaterThanOrEqual annotation = field.getAnnotation(GreaterThanOrEqual.class);
        return annotation == null ? EMPTY_STR : this.parseGreaterOrLess(field, metaInfo, annotation.value(), annotation.column(), "&gt;=");
    }

    private String lessThan(Field field, MetaInfo metaInfo) {
        LessThan annotation = field.getAnnotation(LessThan.class);
        return annotation == null ? EMPTY_STR : this.parseGreaterOrLess(field, metaInfo, annotation.value(), annotation.column(), "&lt;");
    }

    private String lessThanOrEqual(Field field, MetaInfo metaInfo) {
        LessThanOrEqual annotation = field.getAnnotation(LessThanOrEqual.class);
        return annotation == null ? EMPTY_STR : this.parseGreaterOrLess(field, metaInfo, annotation.value(), annotation.column(), "&lt;=");
    }

    private String parseGreaterOrLess(Field field, MetaInfo metaInfo, String annotaionValue, String columnName, String symbol) {
        String column = this.parseSelectColumn(metaInfo, field, annotaionValue, columnName);
        if (column == null) {
            return EMPTY_STR;
        }
        String fieldName = field.getName();
        String script = "<if test=\"query.%s != null\">AND %s %s #{query.%s}</if>";
        return String.format(script, fieldName, column, symbol, fieldName);
    }

    private String parseSelectColumn(MetaInfo metaInfo, Field field, String fieldName, String columnNme) {
        //默认的查询字段为注解标识的字段
        if(columnNme != null && !columnNme.isEmpty()) {
            return columnNme;
        }
        String bindColumn = field.getName();
        if(fieldName != null && !fieldName.isEmpty()) {
            ColumnInfo columnInfo = MetaHelper.getColumnByFieldName(metaInfo, bindColumn);
            if(columnInfo != null) {
                return columnInfo.getColumnName();
            }
        }
        return null;
    }


    private String parseSelectColumn(MetaInfo metaInfo, String fieldName) {
        ColumnInfo columnInfo = MetaHelper.getColumnByFieldName(metaInfo, fieldName);
        if(columnInfo == null) {
            return null;
        }
        return columnInfo.getColumnName();
    }

}
