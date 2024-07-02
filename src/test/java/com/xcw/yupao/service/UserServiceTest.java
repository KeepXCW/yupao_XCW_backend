package com.xcw.yupao.service;

import com.xcw.yupao.model.domain.User;
import com.xcw.yupao.mapper.UserMapper;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
class UserServiceTest {

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserService userService;

//    @Test
//    public void testSearchUsersByTags() {
//        List<String> tagNameList = Arrays.asList("java");
//        List<User> userList = userService.searchUserByTags(tagNameList);
//        Assert.assertNotNull(userList);
//    }

    @Test
    public void testSearchUsersByTags() {
        List<String> tagNameList = Arrays.asList("java","python");
        List<User> userList = userService.searchUserByTags(tagNameList);
        for (User user : userList) {
            System.out.println(user);
        }
    }

    @Test
    public void testSelectAll(){
        System.out.println("111执行-----------------------------");
        List<User> list = userMapper.selectList(null);
        for (User i:list) {
            System.out.println(i);
        }
    }



}