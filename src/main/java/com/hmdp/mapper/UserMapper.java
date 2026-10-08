package com.hmdp.mapper;

import com.hmdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {
    /*                                          ↑ 泛型，告诉 MP 这个 Mapper 操作哪张表
    因为 BaseMapper<User> 这个父接口里已经预定义好了一大堆通用方法，你继承它，
    就自动有了这些能力。MyBatis-Plus 帮你把 SQL 都生成好了。
     */

}
