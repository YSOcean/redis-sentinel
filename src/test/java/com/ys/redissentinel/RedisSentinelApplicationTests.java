package com.ys.redissentinel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.jedis.JedisSentinelConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Set;

@SpringBootTest
class RedisSentinelApplicationTests {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 设置String类型的数据
     */
    public Object setSetString(String key,String value)  {
        return stringRedisTemplate.opsForSet().add(key,value);
    }

    /**
     * 获取String类型的数据
     */
    public Object getSetString(String key) {
        return stringRedisTemplate.opsForSet().pop(key);
    }

    //第一种方法
    @Test
    void testReidsMethod() {
        //设置值
        setSetString("hello","world");
        System.out.println(getSetString("hello"));
    }

    //第二种方法
    public static void main(String[] args) {
        //1.设置sentinel 各个节点集合
        Set<String> sentinelSet = new HashSet<>();
        sentinelSet.add("192.168.14.101:26379");
        sentinelSet.add("192.168.14.102:26380");
        sentinelSet.add("192.168.14.103:26381");

        //2.设置jedispool 连接池配置文件
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);
        config.setMaxWaitMillis(1000);

        //3.设置mastername,sentinelNode集合,配置文件,Redis登录密码
        JedisSentinelPool jedisSentinelPool = new JedisSentinelPool("mymaster",sentinelSet,config,"123");
        Jedis jedis = null;
        try {
            jedis = jedisSentinelPool.getResource();
            //获取Redis中key=hello的值
            String value = jedis.get("hello");
            System.out.println(value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
    }
}
