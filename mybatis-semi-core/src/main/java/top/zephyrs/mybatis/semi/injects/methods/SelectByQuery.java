package top.zephyrs.mybatis.semi.injects.methods;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.annotations.query.*;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.MetadataHelper;
import top.zephyrs.mybatis.semi.metadata.TableInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public class SelectByQuery extends AbstractInjectMethod {

    public static final String EMPTY_STR = "";

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
                                     Class<?> mapperClass, Class<?> beanClass, Method method,
                                     Class<?> parameterTypeClass,
                                     LanguageDriver languageDriver) {
        TableInfo tableInfo = MetadataHelper.getTableInfo(configuration.getGlobalConfig(), beanClass);
        return (Object parameterObject) -> {
            Class<?> queryType;
            if (parameterObject instanceof Map) {
                Object param = ((Map<?, ?>) parameterObject).get("query");
                queryType = param.getClass();
            } else {
                queryType = parameterObject.getClass();
            }
            // 查询的列
            StringBuilder columnScript = new StringBuilder();
            for (ColumnInfo column : tableInfo.getColumns()) {
                if (column.isSelect()) {
                    columnScript.append(column.getColumnName()).append(", ");
                }
            }
            String columns = columnScript.substring(0, columnScript.length() - 2);

            // 查询的条件
            StringBuilder whereScript = new StringBuilder("<where>");
            Field[] fields = queryType.getDeclaredFields();
            for (Field field : fields) {
                StringBuilder fieldWhere = new StringBuilder();
                fieldWhere.append(between(field, tableInfo));
                fieldWhere.append(greaterThan(field, tableInfo));
                fieldWhere.append(greaterThanOrEqual(field, tableInfo));
                fieldWhere.append(lessThan(field, tableInfo));
                fieldWhere.append(lessThanOrEqual(field, tableInfo));
                fieldWhere.append(like(field, tableInfo));
                fieldWhere.append(likeLeft(field, tableInfo));
                fieldWhere.append(likeRight(field, tableInfo));
                fieldWhere.append(in(field, tableInfo));
                // 如果以上注解都没，则是equal
                if (fieldWhere.toString().trim().isEmpty()) {
                    fieldWhere.append(equal(field, tableInfo));
                }
                whereScript.append(fieldWhere);
            }
            // 逻辑删除的要过滤
            if (tableInfo.isLogical()) {
                whereScript
                        .append(" AND ")
                        .append(tableInfo.getLogicalColumn().getColumnName())
                        .append("=")
                        .append(tableInfo.getNoDeletedValue());
            }
            whereScript.append("</where>");

            String sqlScript;

            String INSERT_TMPL = "<script>select %s from %s %s</script>";
            sqlScript = String.format(INSERT_TMPL, columns, tableInfo.getTableName(), whereScript);
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlScript, parameterTypeClass);
            return sqlSource.getBoundSql(parameterObject);
        };
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {
        return EMPTY_STR;
    }


    private String equal(Field field, TableInfo tableInfo) {
        Equal equal = field.getAnnotation(Equal.class);
        if(equal == null) {
            return EMPTY_STR;
        }
        ColumnInfo column = this.parseSelectColumn(tableInfo, field, equal.value());
        if (column == null) {
            return EMPTY_STR;
        }
        String fieldName = field.getName();
        if (field.getType() == String.class) {
            String script = "<if test=\"query.%s != null and query.%s != ''\">AND %s=#{query.%s}</if>";
            return String.format(script, fieldName, fieldName, column.getColumnName(), fieldName);
        } else {
            String script = "<if test=\"query.%s != null\">AND %s=#{query.%s}</if>";
            return String.format(script, fieldName, column.getColumnName(), fieldName);
        }
    }

    private String between(Field field, TableInfo tableInfo) {
        Between between = field.getAnnotation(Between.class);
        if (between == null || !(Collection.class.isAssignableFrom(field.getType()))) {
            return EMPTY_STR;
        }
        ColumnInfo column = this.parseSelectColumn(tableInfo, field, between.value());
        if (column == null) {
            return EMPTY_STR;
        }
        String fieldName = field.getName();
        String script = "<if test=\"query.%s != null and query.%s.size()>1\">AND %s between #{query.%s[0]} and #{query.%s[1]}</if>";
        return String.format(script, fieldName, fieldName, column.getColumnName(), fieldName, fieldName);
    }

    private String in(Field field, TableInfo tableInfo) {
        In in = field.getAnnotation(In.class);
        if (in == null || !field.getType().isAssignableFrom(Collection.class)) {
            return EMPTY_STR;
        }
        ColumnInfo column = this.parseSelectColumn(tableInfo, field, in.value());
        if (column == null) {
            return EMPTY_STR;
        }
        String script = "<if test=\"query.%s != null and query.%s.size()>0\">" +
                "AND %s IN " +
                "<foreach collection=\"query." + field.getName() + "\" item=\"item\" index=\"index\" open=\"(\" close=\")\" separator=\",\">" +
                "#{item}</foreach>" +
                "</if>";
        return String.format(script, field.getName(), field.getName(), column.getColumnName());
    }


    private String like(Field field, TableInfo tableInfo) {
        Like annotation = field.getAnnotation(Like.class);
        return annotation == null ? EMPTY_STR : parseLike(field, tableInfo, annotation.value(), "full");
    }

    private String likeLeft(Field field, TableInfo tableInfo) {
        LikeLeft annotation = field.getAnnotation(LikeLeft.class);
        return annotation == null ? EMPTY_STR : parseLike(field, tableInfo, annotation.value(), "left");
    }

    private String likeRight(Field field, TableInfo tableInfo) {
        LikeRight annotation = field.getAnnotation(LikeRight.class);
        return annotation == null ? EMPTY_STR : parseLike(field, tableInfo, annotation.value(), "right");
    }

    private String parseLike(Field field, TableInfo tableInfo, String annotaionValue, String type) {
        ColumnInfo column = this.parseSelectColumn(tableInfo, field, annotaionValue);
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
        return String.format(script, fieldName, fieldName, column.getColumnName(), like);
    }

    private String greaterThan(Field field, TableInfo tableInfo) {
        GreaterThan annotation = field.getAnnotation(GreaterThan.class);
        return annotation == null ? EMPTY_STR : this.parseGreaterOrLess(field, tableInfo, annotation.value(), "&gt;");
    }

    private String greaterThanOrEqual(Field field, TableInfo tableInfo) {
        GreaterThanOrEqual annotation = field.getAnnotation(GreaterThanOrEqual.class);
        return annotation == null ? EMPTY_STR : this.parseGreaterOrLess(field, tableInfo, annotation.value(), "&gt;=");
    }

    private String lessThan(Field field, TableInfo tableInfo) {
        LessThan annotation = field.getAnnotation(LessThan.class);
        return annotation == null ? EMPTY_STR : this.parseGreaterOrLess(field, tableInfo, annotation.value(), "&lt;");
    }

    private String lessThanOrEqual(Field field, TableInfo tableInfo) {
        LessThanOrEqual annotation = field.getAnnotation(LessThanOrEqual.class);
        return annotation == null ? EMPTY_STR : this.parseGreaterOrLess(field, tableInfo, annotation.value(), "&lt;=");
    }

    private String parseGreaterOrLess(Field field, TableInfo tableInfo, String annotaionValue, String symbol) {
        ColumnInfo column = this.parseSelectColumn(tableInfo, field, annotaionValue);
        if (column == null) {
            return EMPTY_STR;
        }
        String fieldName = field.getName();
        String script = "<if test=\"query.%s != null\">AND %s %s #{query.%s}</if>";
        return String.format(script, fieldName, column.getColumnName(), symbol, fieldName);
    }

    private ColumnInfo parseSelectColumn(TableInfo tableInfo, Field field, String annotaionValue) {
        String bindColumn = field.getName();
        if(annotaionValue != null && !annotaionValue.isEmpty()) {
            bindColumn = annotaionValue;
        }
        return MetadataHelper.getColumnByFieldName(tableInfo, bindColumn);
    }

}
