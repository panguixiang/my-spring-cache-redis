package com.che.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.che.service.Test2Service;

/**
 * 测试spring-cache-redis 
 * @author panguixiang
 *
 */
@Controller
public class TestController {
	
	@Autowired
	Test2Service test2Service;
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/testRedis", method = RequestMethod.GET)
    public String testRedis(HttpServletRequest request,HttpServletResponse response) throws Exception {
		return  test2Service.serviTwo("are you ok?");
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/testRedis2", method = RequestMethod.GET)
    public String onlyMySelf(HttpServletRequest request,HttpServletResponse response) throws Exception {
		return  test2Service.onlyMySelf();
	}
	
	/**
	 * 此cache 不起作用 是AOP的原因，嵌套缓存不被允许
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/cacheExpire", method = RequestMethod.GET)
    public String cacheExpire(HttpServletRequest request,HttpServletResponse response) throws Exception {
		return  test2Service.cacheExpire();
	}

}
