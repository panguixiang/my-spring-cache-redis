package com.che.service.impl;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.che.service.TestService;

@Service
public class TestServiceImpl implements TestService{

	@Override
	@Cacheable(value="xiaozi",expireTime=30)
	public String doworkService(String name) {
		System.out.println("-------TestService--------进入数据库查询-----------");
		return "22222222222222valueaonima";
	}
	
}
