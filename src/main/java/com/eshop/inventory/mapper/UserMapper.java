package com.eshop.inventory.mapper;

import com.eshop.inventory.model.User;

/**
 * 测试用户的Mapper接口
 */
public interface UserMapper {

    /**
     * 查询测试用户的信息
     *
     * @return
     */
    public User findUserInfo();

}
