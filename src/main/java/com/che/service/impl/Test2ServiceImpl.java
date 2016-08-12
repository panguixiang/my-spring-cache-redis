package com.che.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.che.service.Test2Service;
import com.che.service.TestService;

/**
 * 测试spring-cache-redis缓存，加过期时间
 * @author panguixiang
 */
@Service
public class Test2ServiceImpl implements Test2Service {

	@Autowired
	private TestService testService;
	
	/**
	 * 测试嵌套service情况下 cache是否起作用 （起作用）
	 */
	@Override
	public String serviTwo(String name) {
		return testService.doworkService(name);
	}
	
	/**
	 * 测试单service情况下，cache是否其作用 （起作用）
	 */
	@Override
	@Cacheable(value="onlyMySelf",expireTime=30)
	public String onlyMySelf() {
		System.out.println("-------Test2Service2--------进入数据库查询-----------");
		return "---onlyMySelf----";
	}

	/**
	 * 测试 同一类中 方法A 调用 方法B，cache在B方法上时，cache不起作用（注意）
	 */
	@Override
	public String cacheExpire() {
		// TODO Auto-generated method stub
		return onlyMySelf();
	}
}
