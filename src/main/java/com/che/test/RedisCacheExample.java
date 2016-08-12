package com.che.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.che.SpringZeroConfig.SpringConfig;
import com.che.service.Test2Service;
/**
 * spring mvc 零配置情况下测试spring-cache-redis
 * @author panguixiang
 *
 */
public class RedisCacheExample {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfig.class);
		Test2Service musicService = ctx.getBean(Test2Service.class);
		String result = musicService.serviTwo("zero config");
		System.out.println(result);
	}
}
