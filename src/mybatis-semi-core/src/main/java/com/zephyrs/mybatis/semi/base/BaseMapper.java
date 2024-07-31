package com.zephyrs.mybatis.semi.base;

import com.zephyrs.mybatis.semi.base.quey.Query;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

public interface BaseMapper<T> extends IMapper<T> {

    /**
     * 新增, 当属性值为null时，则使用数据库默认值
     *
     * @param domain 实体对象
     * @return 修改数据的数量
     */
    int insert(T domain);

    /**
     * 根据主键修改，当属性值为null时，则不修改该属性
     *
     * @param domain 实体对象
     * @return 修改数据的数量
     */
    int updateById(T domain);

    /**
     * 删除,
     *
     * @param id 主键ID
     * @return 修改数据的数量
     */
    int deleteById(@Param("id") Serializable id);

    /**
     * 启用
     *
     * @param id 主键
     * @return 修改数据的数量
     */
    int enable(@Param("id") Serializable id);

    /**
     * 禁用
     *
     * @param id 主键
     * @return 修改数据的数量
     */
    int disable(@Param("id") Serializable id);

    /**
     * 切换 启用/禁用
     *
     * @param id 主键
     * @return 修改数据的数量
     */
    int toggleEnable(@Param("id") Serializable id);

    /**
     * 根据主键查询
     *
     * @param id 主键
     * @return 查询到的数据
     */
    T selectById(@Param("id") Serializable id);

    /**
     * 查询全部
     * @return 查询到的数据
     */
    List<T> selectAll();

    /**
     * 条件查询
     *
     * @param query 查询条件
     * @return 查询到的数据
     */
    List<T> selectByQuery(@Param("query") Query query);

}
