package com.dulianbin.mini.springmvc.demo.service.impl;

import com.dulianbin.mini.springmvc.demo.service.IUserService;
import com.dulianbin.mini.springmvc.mvcframework.annotations.DulianbinService;

@DulianbinService
public class UserServiceImpl implements IUserService {
    @Override
    public String getName(String id) {
        return "My name is "+id;
    }
}
