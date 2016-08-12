package com.che.SpringZeroConfig;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.che.cache.redis.MyRedisCacheManager;

import redis.clients.jedis.JedisPoolConfig;

/**
 * spring 零配置的配置类(xml情况下无需理会此类)
 * @author panguixiang
 *
 */
@Configuration
@EnableCaching
@ComponentScan("com.che")
@PropertySource("classpath:/redis.properties")
public class SpringConfig {

    private @Value("${redis.host}") String redisHost;
    private @Value("${redis.port}") Integer redisPort;
    private @Value("${redis.password}") String password;
    private @Value("${redis.maxIdle}") Integer maxIdle;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        JedisConnectionFactory factory = new JedisConnectionFactory();
        factory.setHostName(redisHost);
        factory.setPort(redisPort);
        factory.setUsePool(true);
        if(StringUtils.isNotBlank(password)) {
        	factory.setPassword(password);
        }
        JedisPoolConfig config = new JedisPoolConfig();
        if(maxIdle!=null) {
        	config.setMaxIdle(maxIdle);
        }
        factory.setPoolConfig(config);
        return factory;
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        return redisTemplate;
    }

    @Bean
    CacheManager cacheManager() {
        return new MyRedisCacheManager(redisTemplate());
    }
}