package com.xcw.yupao.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;

    @Test
    void test() {

        System.out.println("集合");
        // list，数据存在本地 JVM 内存中
        List<String> list = new ArrayList<>();
        list.add("shier");
        System.out.println("list:" + list.get(0));

        // list.remove(0);

        System.out.println("------------------------------------------------------------------");

        System.out.println("redis");
        // 数据存在 redis 的内存中
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("shier");
        System.out.println("rlist:" + rList.get(0));
         //rList.remove(0);

        // map
        Map<String, Integer> map = new HashMap<>();
        map.put("shier", 10);
        map.get("shier");

        RMap<Object, Object> map1 = redissonClient.getMap("test-map");
    }

    /**
     * 续时，防止锁提前过期（看门狗）
     *
     * 注意锁的存在时间要设置为-1（开启开门狗），默认锁的过期时间是30秒，通过sleep实现
     * 运行，通过quickredis观察，可以发现  每 10 秒续期一次（补到 30 秒）
     * **踩坑处：不要用debug启动，会被认为是宕机**
     */
    @Test
    void testWatchDog(){
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");
        try{
            //只有一个线程能获取到锁
            if (lock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
                Thread.sleep(300000);
                System.out.println("getLock: "+Thread.currentThread().getId());
            }
        }catch(InterruptedException e){
            System.out.println(e.getMessage());
        }finally {
            //只能释放自己的锁
            if (lock.isHeldByCurrentThread()){
                System.out.println("unlock: "+Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
