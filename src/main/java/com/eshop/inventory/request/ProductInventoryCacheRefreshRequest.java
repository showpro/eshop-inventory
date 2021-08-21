package com.eshop.inventory.request;

import com.eshop.inventory.model.ProductInventory;
import com.eshop.inventory.service.ProductInventoryService;

/**
 * 重新加载商品库存的缓存
 *
 * @author zhanzhan
 */
public class ProductInventoryCacheRefreshRequest implements Request {

    /**
     * 商品id
     */
    private Integer productId;
    /**
     * 商品库存Service
     */
    private ProductInventoryService productInventoryService;
    /**
     * 是否强制刷新缓存
     */
    private boolean forceRefresh;

    public ProductInventoryCacheRefreshRequest(Integer productId, ProductInventoryService productInventoryService, boolean forceRefresh) {
        this.productId = productId;
        this.productInventoryService = productInventoryService;
        this.forceRefresh = false;
    }

    @Override
    public void process() {
        // 1、从数据库中查询最新的商品库存数量
        ProductInventory productInventory = productInventoryService.findProductInventory(productId);
        System.out.println("===========日志===========: 已查询到商品最新的库存数量，商品id=" + productId + ", 商品库存数量="
            + productInventory.getInventoryCnt());
        // 2、将最新的商品库存数量，刷新到redis缓存中去
        productInventoryService.setProductInventoryCache(productInventory);
    }

    @Override
    public Integer getProductId() {
        return productId;
    }

    @Override
    public boolean isForceRefresh() {
        return forceRefresh;
    }
}
