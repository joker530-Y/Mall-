package com.macro.mall.security.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring工具类
 * Created by macro on 2020/3/3.
 */
@Component
public class SpringUtil implements ApplicationContextAware {

    /** Spring应用上下文 */
    private static ApplicationContext applicationContext;

    /**
     * 获取Spring应用上下文
     * @return ApplicationContext实例
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 设置Spring应用上下文（Spring容器初始化时自动调用）
     * @param applicationContext 应用上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringUtil.applicationContext == null) {
            SpringUtil.applicationContext = applicationContext;
        }
    }

    /**
     * 通过Bean名称获取Bean
     * @param name Bean名称
     * @return Bean实例
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过Bean类型获取Bean
     * @param clazz Bean类型
     * @param <T> 泛型
     * @return Bean实例
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过Bean名称和类型获取Bean
     * @param name Bean名称
     * @param clazz Bean类型
     * @param <T> 泛型
     * @return Bean实例
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

}
