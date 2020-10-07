package com.dulianbin.mini.springmvc.demo.controller;


import com.dulianbin.mini.springmvc.demo.service.IUserService;
import com.dulianbin.mini.springmvc.mvcframework.annotations.DulianbinAutowired;
import com.dulianbin.mini.springmvc.mvcframework.annotations.DulianbinController;
import com.dulianbin.mini.springmvc.mvcframework.annotations.DulianbinRequestMapping;
import com.dulianbin.mini.springmvc.mvcframework.annotations.DulianbinRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@DulianbinController
@DulianbinRequestMapping("/demo")
public class DemoController {

    @DulianbinAutowired
    private IUserService userService;

    @DulianbinRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @DulianbinRequestParam("id") String id){
        String result = userService.getName(id);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @DulianbinRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @DulianbinRequestParam("a") Integer a, @DulianbinRequestParam("b") Integer b){
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @DulianbinRequestMapping("/remove")
    public void remove(HttpServletRequest req,HttpServletResponse resp,
                       @DulianbinRequestParam("id") Integer id){
        try {
            resp.getWriter().write("remove："+id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
