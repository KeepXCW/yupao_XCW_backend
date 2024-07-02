package com.xcw.yupao.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.yupao.config.RedissonConfig;
import com.xcw.yupao.model.domain.User;
import com.xcw.yupao.service.UserService;


import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务 (spring Schedule spring默认整合)
 * 缓存预热
 *
 * 使用分布式锁来确保任务在分布式环境下不会被多个实例同时执行。
 *      *
 *      * redisson分布式锁：只有抢到锁的服务器才能执行业务逻辑
 *      * 对于这种缓存的写操作，如果多个请求同时到达，
 *      * 有可能会出现缓存击穿（即多个请求同时查询数据库并写入缓存）的情况。
 *      * 为了避免这种情况，通常会考虑加锁机制。这里用redisson分布式锁
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    /**
     *这里的重点用户展示只设置userID=1的用户
     * 后期Vip用户，经常访问的用户的都可以是重点用户，所以下面用的foreach来通过遍历用户的userId(redisKey)
     * 来存入redis的value（缓存）
     */
    private List<Long> mainList = Arrays.asList(1L);


    /**
     * 指定 cron 表达式或者执行频率  cron生成网站https://cron.ciding.cc/
     *
     * 第一个字段（秒）：0，表示在每分钟的第 0 秒执行任务。
     * 第二个字段（分钟）：24，表示在每小时的第 24 分钟执行任务。
     * 第三个字段（小时）：18，表示在每天的 18 点（即下午 6 点）执行任务。
     * 第四个字段（日期）：*，表示每个月的每一天。
     * 第五个字段（月份）：*，表示每年的每一个月。
     * 第六个字段（星期几）：*，表示星期的每一天。
     */
    @Scheduled(cron = "0 24 18 * * *")
    public void doCacheRecommendUser(){

        //调用配置类redissonClient方法获取配置
        RedissonConfig redissonConfig = new RedissonConfig();
        RedissonClient redissonClient = redissonConfig.redissonClient();
        //获取锁实例，锁的名字为 "yupao:precachejob:docache:lock"
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");

        try {
            // 尝试获取锁，等待时间为0，就是预热操作每天只用执行一次，当第一个服务器抢到后，第二个服务器在执行不会等待，会立即返回
            // 表示立即返回；租期为-1，表示锁提前过期进程未执行完，会续时
            //利用的redis的nx（SET if Not eXists）特性，保证只有一个锁写入，详细看笔记
            //key=锁的name value=线程id或者服务器id
            if (lock.tryLock(0, -1, TimeUnit.MICROSECONDS)){

                //对重点用户进行预热查询
                for (Long userId:mainList) {
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    //mybatisPlus分页查询
                    Page<User> userPage = userService.page(new Page<>(1, 20), queryWrapper);
                    //用JAVA字符串格式化，创建了一个用于缓存的键，格式为yupao:user:recommend:<用户ID>
                    String redisKey = String.format("yupao:user:recommend:%s",userId);

                    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();

                    //写缓存
                    try {
                        opsForValue.set(redisKey, userPage,30000, TimeUnit.MICROSECONDS);
                    } catch (Exception e) {
                        log.error("redis set key error",e);
                    }
                }
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
                //释放锁：判断是否时自己的锁，防止释放别人的锁，
            //并且为了防止刚判断完锁失效了，别的服务器又抢到了锁，然后删除的时别的锁，此时没有锁，第三台服务器又抢到一个锁
            //需要用原子操作
            //会判断当前锁是不是当前 线程添加的
            if (lock.isHeldByCurrentThread()){

                System.out.println("unlock: "+Thread.currentThread().getId());
                //释放锁
                lock.unlock();
            }
        }


    }

}
