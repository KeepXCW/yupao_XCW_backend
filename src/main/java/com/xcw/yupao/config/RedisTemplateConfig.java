package com.xcw.yupao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 配置序列化工具
 */
@Configuration
public class RedisTemplateConfig {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建RedisTemplate<String, Object>的一个实例，其中String是键的类型，Object是值的类型
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置Redis连接工厂，用于创建和管理Redis连接
        redisTemplate.setConnectionFactory(connectionFactory);


        //配置序列化器
        // 设置键的序列化器为字符串序列化器，因为Redis的键必须是字符串类型
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        /*一般不对value进行序列化，value只能存入String类型，不能存进LocalDateTime、LocalDate、LocalTime等时间类型
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());*/

        return redisTemplate;
    }
}