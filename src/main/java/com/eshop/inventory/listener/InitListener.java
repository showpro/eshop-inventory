package com.eshop.inventory.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.eshop.inventory.thread.RequestProcessorThreadPool;

/**
 * 系统初始化监听器
 *
 * @author zhanzhan
 */
public class InitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("==========系统开始启动，初始化工作线程池和内存队列==========");
        // 初始化工作线程池和内存队列
        RequestProcessorThreadPool.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
