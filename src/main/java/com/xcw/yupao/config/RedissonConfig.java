package com.xcw.yupao.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
/*
  用于将外部配置文件中的属性绑定到当前类的字段上。
  这里的prefix = "spring.redis"意味着它会查找以spring.redis为前缀的属性，
  例如spring.redis.host和spring.redis.port。
 */
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private String port;

    @Bean
    public RedissonClient redissonClient(){
        //1、创建配置
        Config config = new Config();
        //动态获取redis地址而不写死
        String redisAddress = String.format("redis://%s:%s", host, port);
        // 配置Redisson客户端使用单个Redis服务器。setAddress方法设置了Redis服务器的地址
        config.useSingleServer().setAddress(redisAddress).setDatabase(3);

        //2.创建实例
        RedissonClient redisson = Redisson.create(config);

        return redisson;


    }
}
