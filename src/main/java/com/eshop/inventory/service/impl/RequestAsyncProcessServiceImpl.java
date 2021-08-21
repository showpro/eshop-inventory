package com.eshop.inventory.service.impl;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.springframework.stereotype.Service;

import com.eshop.inventory.request.ProductInventoryCacheRefreshRequest;
import com.eshop.inventory.request.ProductInventoryDBUpdateRequest;
import com.eshop.inventory.request.Request;
import com.eshop.inventory.request.RequestQueue;
import com.eshop.inventory.service.RequestAsyncProcessService;

/**
 * 请求异步处理的service实现
 *
 * @author zhanzhan
 */
@Service("requestAsyncProcessService")
public class RequestAsyncProcessServiceImpl implements RequestAsyncProcessService {

    @Override
    public void process(Request request) {
        try {
            /**
             * 优化点：
             * 如果一个读请求过来，发现前面已经有一个写请求和一个读请求了，那么这个读请求就不需要压入队列中了
             *
             * 因为那个写请求肯定会更新数据库，然后那个读请求肯定会从数据库中读取最新数据，然后刷新到缓存中，
             * 自己只要hang一会儿就可以从缓存中读到数据了
             */
/*             // 先做读请求的去重
            // 思考：（放在这个地方，多个请求并发的情况下可能会产生bug,比如标识位更新，最好先进队列后，然后在queue.take()之后，去判断要不要往下走去数据库查）
           RequestQueue requestQueue = RequestQueue.getInstance();
            Map<Integer, Boolean> flagMap = requestQueue.getFlagMap();

            if (request instanceof ProductInventoryDBUpdateRequest) {
                // 如果是一个更新数据库的请求(写请求)，那么就将那个productId对应的标识设置为true
                flagMap.put(request.getProductId(), true);
            } else if (request instanceof ProductInventoryCacheRefreshRequest) {
                // 如果是缓存刷新的请求（读请求）
                Boolean flag = flagMap.get(request.getProductId());

                // 如果flag是null,请求第一次过来，之前没有写请求
                if (flag == null) {
                    flagMap.put(request.getProductId(), false);
                }

                // 如果是缓存刷新的请求，那么就判断，如果标识不为空，而且是true，就说明之前有一个这个商品的数据库更新请求
                if (flag != null && flag) {
                    // 还是会把请求塞到后面的内存队列中去
                    flagMap.put(request.getProductId(), false);
                }

                // 如果是缓存刷新的请求，而且发现标识不为空，但是标识是false
                // 说明前面已经有一个数据库更新请求+一个缓存刷新请求了，大家想一想
                if (flag != null && !flag) {
                    // 对于这种读请求，直接就过滤掉，不要放到后面的内存队列里面去了
                    return;
                }
            }*/

            // 做请求的路由，根据每个请求的商品id，路由到对应的内存队列中去。
            // 思考：这个地方大量并发请求过来，有没有并发安全问题呢？
            // 所有的并发请求最终都到这里了，ArrayBlockingQueue是多线程并发安全的，所以不存在
            ArrayBlockingQueue<Request> queue = getRoutingQueue(request.getProductId());
            // 将请求放入对应的内存队列中，完成路由操作
            //queue.add(request);//add: 内部实际上获取的offer方法，当Queue已经满了时，抛出一个异常。不会阻塞。
            queue.put(request);//put:当Queue已经满了时，会进入等待，只要不被中断，就会插入数据到队列中。会阻塞，可以响应中断。
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据请求的商品id获取路由到的内存队列
     *
     * @param productId 商品id
     * @return 内存队列
     */
    private ArrayBlockingQueue<Request> getRoutingQueue(Integer productId) {
        RequestQueue requestQueue = RequestQueue.getInstance();

        // 1、先获取productId的hash值
        String key = String.valueOf(productId);
        int h;
        int hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);

        // 2、对hash值取模，将hash值路由到指定的内存队列中，比如内存队列大小为8
        // 用内存队列的数量对hash值取模之后，结果一定是在0~7之间
        // 所以任何一个商品id都会被固定路由到同样的一个内存队列中去的
        int index = (requestQueue.queueSize() - 1) & hash;

        System.out.println("===========日志===========: 路由内存队列，商品id=" + productId + ", 队列索引=" + index);

        // 取出路由的内存队列并返回
        return requestQueue.getQueue(index);
    }

}
