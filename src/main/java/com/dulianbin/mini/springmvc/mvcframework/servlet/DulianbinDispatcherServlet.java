package com.dulianbin.mini.springmvc.mvcframework.servlet;

import com.dulianbin.mini.springmvc.mvcframework.annotations.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DulianbinDispatcherServlet extends HttpServlet {

    //存储application.properties配置信息
    private Properties contextConfig=new Properties();

    //存储所有扫描到的类
    private List<String> classNames = new ArrayList<String>();

    private Map<String,Object> ioc=new HashMap<String,Object>();

    //private Map<String,Method> handlerMapping = new HashMap<String,Method>();

    //保存所有的Url和方法的映射关系
    private List<Handler> handlerMapping = new ArrayList<Handler>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            //开始匹配到对应的方方法
            System.out.println("到这里了:"+req.getPathInfo());
            doDispatch(req,resp);
        }catch(Exception e){
            //如果匹配过程出现异常，将异常信息打印出去
            resp.getWriter().write("500 Exception,Details:\r\n" + Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]", "").replaceAll(",\\s", "\r\n"));
        }
    }



    /**
     * 匹配URL
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    private void doDispatch(HttpServletRequest req,HttpServletResponse resp) throws Exception{

        try{
            Handler handler = getHandler(req);

            if(handler == null){
                //如果没有匹配上，返回404错误
                resp.getWriter().write("404 Not Found");
                return;
            }


            //获取方法的参数列表
            Class<?> [] paramTypes = handler.method.getParameterTypes();

            //保存所有需要自动赋值的参数值
            Object [] paramValues = new Object[paramTypes.length];


            Map<String,String[]> params = req.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");

                //如果找到匹配的对象，则开始填充参数值
                if(!handler.paramIndexMapping.containsKey(param.getKey())){continue;}
                int index = handler.paramIndexMapping.get(param.getKey());
                paramValues[index] = convert(paramTypes[index],value);
            }


            //设置方法中的request和response对象
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;

            handler.method.invoke(handler.controller, paramValues);

        }catch(Exception e){
            throw e;
        }
    }

    private Handler getHandler(HttpServletRequest req) throws Exception{
        if(handlerMapping.isEmpty()){ return null; }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        System.out.println("真实请求的url:"+url);
        for (Handler handler : handlerMapping) {
            try{
                Matcher matcher = handler.pattern.matcher(url);
                //如果没有匹配上继续下一个匹配
                if(!matcher.matches()){ continue; }

                return handler;
            }catch(Exception e){
                throw e;
            }
        }
        return null;
    }

    //url传过来的参数都是String类型的，HTTP是基于字符串协议
    //只需要把String转换为任意类型就好
    private Object convert(Class<?> type,String value){
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        //如果还有double或者其他类型，继续加if
        //这时候，我们应该想到策略模式了
        return value;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //扫描文件
        doScanner(contextConfig.getProperty("scanPackage"));

        //3、初始化所有相关的类的实例，并且放入到IOC容器之中
        doInstance();

        //4、完成依赖注入
        doAutowired();

        //5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("My Spring MVC framework is init success!");
    }

    private void initHandlerMapping() {
        if(ioc.isEmpty()){return ;}
        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clazz=entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(DulianbinController.class)){continue ;}

            String baseUrl="";
            if(clazz.isAnnotationPresent(DulianbinRequestMapping.class)){
                baseUrl=clazz.getAnnotation(DulianbinRequestMapping.class).value();
            }

            //获取Method的url配置
            Method[] methods = clazz.getMethods();
            for(Method method: methods){
                if(!method.isAnnotationPresent(DulianbinRequestMapping.class)){continue;}
                DulianbinRequestMapping requestMapping=method.getAnnotation(DulianbinRequestMapping.class);
                String regex = ("/" + baseUrl + requestMapping.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                handlerMapping.add(new Handler(pattern,entry.getValue(),method));
                System.out.println("mapping " + regex + "," + method);
            }

        }
    }

    private void doAutowired() {
        if(ioc.isEmpty()){return ;}
        for (Map.Entry<String,Object> entry: ioc.entrySet()){
            //拿到实例对象中的所有属性
            Field[] fields=entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if(!field.isAnnotationPresent(DulianbinAutowired.class)){continue;}
                DulianbinAutowired autowired=field.getAnnotation(DulianbinAutowired.class);
                String beanName=autowired.value().trim();
                if("".equals(beanName)){
                    beanName=field.getType().getName();
                }
                //不管你愿不愿意，强吻
                field.setAccessible(true); //设置私有属性的访问权限,强制设置可访问权限
                try{
                    System.out.println("注入的字段:"+field.getName()+",beanName？"+beanName);
                    System.out.println("接收注入的类对象？"+entry.getValue()+",注入的值:"+ioc.get(beanName));
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch(Exception e){
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void doInstance() {
        if(classNames.isEmpty()){return ;}
        try{
            for (String clazzName:classNames){
                Class<?> clazz=Class.forName(clazzName);
                if(clazz.isAnnotationPresent(DulianbinController.class)){
                    Object o=clazz.newInstance();
                    String beanName=toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,o);
                }else if(clazz.isAnnotationPresent(DulianbinService.class)){
                    DulianbinService  service=clazz.getAnnotation(DulianbinService.class);
                    //2、自定义命名
                    String beanName=toLowerFirstCase(clazz.getSimpleName());
                    if(!"".equals(service.value())){
                        beanName = service.value();
                    }
                    Object o=clazz.newInstance();
                    ioc.put(beanName,o);
                    for (Class i:clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The beanName is exists!!");
                        }
                        ioc.put(i.getName(),o);
                    }
                }else{
                    continue;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        chars[0] += 32;
        return  String.valueOf(chars);
    }


    private void doLoadConfig(String contextConfigLocation) {

        InputStream fis = null;
        try{
            fis=this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
            contextConfig.load(fis);
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(fis !=null){
                    fis.close();
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url=this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classpath=new File(url.getFile());
        for (File file : classpath.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                if(!file.getName().endsWith(".class")){continue;}
                classNames.add(scanPackage+"."+file.getName().replace(".class",""));
            }
        }
    }

    /**
     * Handler记录Controller中的RequestMapping和Method的对应关系
     * 内部类
     */
    private class Handler{

        protected Object controller;	//保存方法对应的实例
        protected Method method;		//保存映射的方法
        protected Pattern pattern;
        protected Map<String,Integer> paramIndexMapping;	//参数顺序

        /**
         * 构造一个Handler基本的参数
         * @param controller
         * @param method
         */
        protected Handler(Pattern pattern,Object controller,Method method){
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;

            paramIndexMapping = new HashMap<String,Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method){

            //提取方法中加了注解的参数
            Annotation[] [] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length ; i ++) {
                for(Annotation a : pa[i]){
                    if(a instanceof DulianbinRequestParam){
                        String paramName = ((DulianbinRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            //提取方法中的request和response参数
            Class<?> [] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length ; i ++) {
                Class<?> type = paramsTypes[i];
                if(type == HttpServletRequest.class ||
                        type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);
                }
            }
        }
    }
}
