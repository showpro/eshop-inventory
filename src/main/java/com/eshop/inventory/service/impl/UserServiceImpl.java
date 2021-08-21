package com.eshop.inventory.service.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.eshop.inventory.dao.RedisDAO;
import com.eshop.inventory.mapper.UserMapper;
import com.eshop.inventory.model.User;
import com.eshop.inventory.service.UserService;

/**
 * @author zhanzhan
 * @date 2021/5/21 22:42
 */
@Service("userService")
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisDAO redisDAO;

    @Override
    public User findUserInfo() {
        return userMapper.findUserInfo();
    }

    @Override
    public User getCachedUserInfo() {
        redisDAO.set("cached_user_lisi", "{\"name\": \"李四\", \"age\":28}");

        String userJSON = redisDAO.get("cached_user_lisi");
        JSONObject userJSONObject = JSONObject.parseObject(userJSON);

        User user = new User();
        user.setName(userJSONObject.getString("name"));
        user.setAge(userJSONObject.getInteger("age"));

        return user;
    }
}
