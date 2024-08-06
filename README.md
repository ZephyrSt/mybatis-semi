### 简单的mybatis脚手架

在mybatis的基础上进行通用功能的提取。

##### 功能：
+ 通用Mapper
+ 自定义通用Mapper
+ 主键生成策略
+ 敏感字段加解密
+ 逻辑删除标识
+ 启用/禁用标识
+ 通用查询接口
##### 快速开始

Maven引用

```xml
<dependency>
<groupId>top.zephyrs</groupId>
<artifactId>mybatis-semi-spring-boot-starter</artifactId>
<version>Latest Version</version>
</dependency>
```
在Bean中使用注解标识表名与主键
```java
@Data
// Table 注解标识表名
@Table("ur_user")
public class User{

    // Primary 主键标识主键，目前只支持单一字段主键
    @Primary
    private Long userId;
    // Column注解标识字段，一般可省略。 默认使用驼峰转下划线的方式对应字段名称
    @Column(value = "user_name" )
    private String username;
    private String password;
}
```
Mapper接口继承BaseMapper
```java

@Mapper
@Repository
public interface UserMapper extends BaseMapper<User> {
}
```


###### 通过构建 SqlSessionFactory 使用：
```java

@Configuration
public class MybatisConfiguration {

    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        //使用 SemiMybatisConfiguration 代替 org.apache.ibatis.session.Configuration.
        //此类继承 Configuration,并扩充了属性，因此同 org.apache.ibatis.session.Configuration 的使用方式相同, 这里省略
        SemiMybatisConfiguration configuration = new SemiMybatisConfiguration();
        //初始化主键生成策略
        configuration.setKeyCreators();
        //这里设置自定义的主键策略
        configuration.setKeyCreator(IdType.CUSTOM, new MyKeyCreator());
        //设置全局配置
        configuration.setGlobalConfig(new GlobalConfig());
        configuration.addMapper(UserMapper.class);

        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setConfiguration(configuration);
        return sqlSessionFactoryBean.getObject();
    }

}
```

##### 主键生成
目前Semi 中集成了以下六种配置：
DEFAULT(默认方式, 使用全局默认配置),
NONE(不使用主键策略), 
AUTO(使用数据库的自动生成)
UUID,
SNOWFLAKE(雪花算法)
CUSTOM(自定义)
可以通过 Primary 注解指定主键生成策略
```
@Primary(idType = IdType.AUTO)
private Long userId;
```
可以通过GlobalConfig 指定默认的全局生成策略。
通过指定 mybatis-semi.global-config.key-generate.custom-key-creator 可以指定 custom 对应的自定义策略

```java
import java.util.UUID;

public class MyKeyCreator implements KeyCreator<String> {

    @Override
    public synchronized String nextId() {
        return UUID.randomUUID().toString().replaceAll("-","");
    }
}
```
```yaml
mybatis-semi:
  global-config:
    key-generate:
      #雪花算法的机器编号，默认0
      work-id: 1
      # 默认的主键生成策略 默认：snowflake
      default-id-type: custom
      # 自定义的主键生成策略
      custom-key-creator: com.example.MyKeyCreator
```
若不使用spring-boot-starter, 也可以在Configuration中替换指定的策略
```
configuration.setKeyCreator(IdType.CUSTOM, new MyKeyCreator());
```

##### 敏感字段加密存储
通过注解 @Sensitive 可以标识字段需要加密存储
使用此功能需要指定加密策略的实现类，如：
```java
@Table("ur_user")
public class User{

    @Primary
    private Long userId;
    @Sensitive(Md5Sensitive.class)
    private String password;
}
```
也可以通过指定全局默认策略来实现：
```
    @Sensitive
    private String password;
```
```yaml
mybatis-semi:
  global-config:
    sensitive:
      # 默认是否解密，如设置为true, 则查询时字段属性自动解密
      default-decrypt: false
      # 如果不揭密，是否用null值来替换加密后的值
      use-null-on-not-decrypt: true
      # 默认的加解密方式
      default-impl: com.example.Md5Sensitive
```
如果设置默认不解密（default-decrypt=false），可以通过 @SensitiveDecrypt 注解标识指定的查询方法解密字段
```java
public interface UserMapper extends BaseMapper<User> {
    
    // SensitiveDecrypt 注解表示此方法查询的结果集中  @Sensitive 注解标识的字段会解密
    @SensitiveDecrypt
    @Select("select * from ur_user where user_id = #{userId}")
    User selectOneDecrypt(@Param("userId") Long userId);

}
```
注：参数的类型必须包含 <b>@Sensitive</b> 注解标识的字段才会加解密

##### 自定义通用Mapper
通过 <b>InjectProcessor</b> 可以配置自定义的通用方法
```java

public class MyInjectProcessor extends InjectProcessor {

    @Override
    public void loadMethods() {
        
        this.addInject("insert", new Insert());
        this.addInject("updateById", new UpdateById());
        this.addInject("deleteById", new DeleteById());

        this.addInject("queryAll", new SelectAll());
        this.addInject("selectOne", new SelectById());

        this.addInject(new Enable());
        this.addInject(new Disable());
    }
}
```
```
// 这里注册通用方法处理器
configuration.setInjectProcessor(new MyInjectProcessor());
```
自定义一个Mapper接口
```java
public interface MyBaseMapper<T> extends IMapper<T> {

    int insert(T domain);

    int updateById(T domain);

    int deleteById(@Param("id") Serializable id);

    T selectOne(@Param("id") Serializable id);

    List<T> queryAll();

    int enable(@Param("id") Serializable id);

    int disable(@Param("id") Serializable id);
}
```
可以通过继承 AbstractInjectMethod 类来自定义需要实现的方法，如：
SelectById的实现
```java
public class SelectById extends AbstractInjectMethod {
    @Override
    public String getId() {
        return "selectById";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.SELECT;
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {
        ColumnInfo primary = tableInfo.getPkColumn();
        if(primary == null) {
            return null;
        }
        StringBuilder columnScript = new StringBuilder();
        for (ColumnInfo column : tableInfo.getColumns()) {
            if(column.isSelect()) {
                columnScript.append(column.getColumnName()).append(", ");
            }
        }
        String columns = columnScript.substring(0, columnScript.length()-2);
        String INSERT_TMPL = "select %s from %s where %s";
        return String.format(INSERT_TMPL,
                columns,
                tableInfo.getTableName(),
                primary.getColumnName() + "=#{id}");
    }
}
```

##### 逻辑删除
可以通过 @LogicDelete 注解实现逻辑删除功能
BaseMapper.deleteById、selectAll、selectByQuery  方法的实现已经进行了判断，具体参考：DeleteById, SelectAll、SelectByQuery

也可以通过全局设置,配置通用的删除字段，当Bean中存在同名字段时，默认其为逻辑删除字段
```yaml
mybatis-semi:
  global-config:
    logic-delete:
      # 表示删除的列名
      column: deleted
      # 已删除的标识
      deleted-value: true
      # 未删除的标识
      exists-value: false
```

##### 启用/禁用

可以通过 @Enable 注解实现启用/禁用功能
BaseMapper.enable、disable、toggleEnable 方法的实现已经进行了判断，具体参考：Enable、Disable、ToggleEnable

也可以通过全局设置配置通用的删除字段，当Bean中存在同名字段时，默认其为逻辑删除字段
```yaml
mybatis-semi:
  global-config:
    enable:
      # 启用/禁用的字段
      column: enabled
      # 启用的值
      enabled-value: true
      # 禁用的值
      disabled-value: false
```
##### 通用查询接口
BaseMapper 仅仅集成了一个通用的查询接口 selectByQuery，为了避免 类似 QueryWrapper 造成的层级混乱问题，
这里Query定义为一个接口，且只有通过Bean继承Query并通过查询注解才能使用(增加使用条件查询的复杂度,使其使用比直接写Mapper方法麻烦)
如：
```java
@Data
public class UserQuery implements Query {

    //没有查询注解的字段，默认使用 @Equal
    private String username;
    @Equal
    private String phone;
    @Like
    private String email;
    @Between
    private List<Date> createTime;
}
```
```
UserQuery query = new UserQuery();
userMapper.selectByQuery(query);
```

##### 分页查询

mybatis-semi-spring-boot-starter 已经集成 PageHelper, 请参考 [PageHelper文档](https://github.com/pagehelper/Mybatis-PageHelper)

