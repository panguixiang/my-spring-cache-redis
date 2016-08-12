# my-spring-cache-redis
实现了spring-cache-redis的整合支持xml配置和零配置两种情况(可以注解cache失效时长)

spring-cache 默认提供的redis 无法实现对每个key灵活的失效时间设置，
此demo自定义注解，修改 spring-cache源码的方式来实现了key失效时间的设置。
并且支持两种方式：
1.spring零配置
2.xml情况下
