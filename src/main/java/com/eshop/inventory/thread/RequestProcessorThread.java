package com.eshop.inventory.thread;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

import com.eshop.inventory.request.ProductInventoryCacheRefreshRequest;
import com.eshop.inventory.request.ProductInventoryDBUpdateRequest;
import com.eshop.inventory.request.Request;
import com.eshop.inventory.request.RequestQueue;

/**
 * 执行请求的工作线程
 *
 * @author zhanzhan
 */
public class RequestProcessorThread implements Callable<Boolean> {

    /**
     * 工作线程自己监控的内存队列
     */
    private ArrayBlockingQueue<Request> queue;

    public RequestProcessorThread(ArrayBlockingQueue<Request> queue) {
        this.queue = queue;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            //后台不断的从queue中消费请求
            while (true) {
                // 为什么是ArrayBlockingQueue呢
                // Blocking就是说明，如果队列满了，或者是空的，那么都会在执行操作的时候，阻塞住
                Request request = queue.take();

                boolean forceRefresh = request.isForceRefresh();
                if (!forceRefresh) {
                    //读请求去重最好放到这里,从queue拿出来直接操作
                    // 先做读请求的去重
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
                            System.out.println("===========日志===========: 工作线程从队列取出的读请求去重，不入内存队列，商品id=" + request.getProductId());
                            return true;
                        }
                    }
                }

                System.out.println("------>>>>>日志: 工作线程从队列取出请求并开始处理请求，商品id=" + request.getProductId());
                // 执行这个request操作
                request.process();

                // 假如说，执行完了一个读请求之后，假设数据已经刷新到redis中了
                // 但是后面可能redis中的数据会因为内存满了，被自动清理掉了
                // 被清理掉了以后，然后后面又来了一个读请求，此时如果进来，发现标志位是false，就不会去执行这个刷新操作了
                // 所以在执行完这个读请求之后，实际上这个标志位是停留在false的。

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
