package com.eshop.inventory.request;

/**
 * 请求接口
 *
 * @author zhanzhan
 */
public interface Request {

    void process();

    Integer getProductId();

    boolean isForceRefresh();

}
