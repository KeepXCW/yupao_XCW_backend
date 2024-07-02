package com.xcw.yupao.service;

import com.xcw.yupao.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class RedisTest {
    @Resource
    //redis中data.redis包里自带的操控redis增删改查的类(如果使用的不是springboot则需要自定义一个这样的类)
    private RedisTemplate redisTemplate;

    @Test
    void test(){
        /*opsForValue()方法提供了一个便捷的方式来获取操作Redis字符串值的接口实例
        opsForValue()方法提供的是操作Redis中字符串类型值（value）的接口实例,里面有增删改查等方法*/
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //增set
        valueOperations.set("xString","dog");
        valueOperations.set("cInt",1);
        valueOperations.set("wDouble",2.0);

        User user = new User();
        user.setId(1L);
        user.setUsername("cw");
        valueOperations.set("xcw",user);

        //查get
        Object result = valueOperations.get("xString");
        Assertions.assertTrue("dog".equals((String)result));

        result = valueOperations.get("cInt");
        Assertions.assertTrue(1==((Integer)result));

        result = valueOperations.get("wDouble");
        Assertions.assertTrue(2.0==((Double)result));

        System.out.println(valueOperations.get("xcw"));
//        valueOperations.set("yupiString","dog");

        //删delete
//        redisTemplate.delete("yupiDouble");
//        redisTemplate.delete("yupiString");
//        redisTemplate.delete("yupiInt");
//        redisTemplate.delete("yupiUser");


        /*ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("yupiString", "dog");
        valueOperations.set("yupiInt", 1);
        valueOperations.set("yupiDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("yupi");
        valueOperations.set("yupiUser", user);

        // 查
        Object yupi = valueOperations.get("yupiString");
        Assertions.assertTrue("dog".equals((String) yupi));
        yupi = valueOperations.get("yupiInt");
        Assertions.assertTrue(1 == (Integer) yupi);
        yupi = valueOperations.get("yupiDouble");
        Assertions.assertTrue(2.0 == (Double) yupi);
        System.out.println(valueOperations.get("yupiUser"));
        valueOperations.set("yupiString", "dog");
        redisTemplate.delete("yupiString");*/




    }
}