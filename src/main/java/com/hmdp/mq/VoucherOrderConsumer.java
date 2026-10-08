package com.hmdp.mq;

import com.hmdp.config.QueueConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class VoucherOrderConsumer {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(queues = QueueConfig.QUEUE_A)
    /*
    告诉 Spring：这个方法监听 queue-a 队列

    队列里有消息 → 自动调用这个方法

    不用你手动去拉消息
    * */

    public void handleVoucherOrder(VoucherOrder order) {
        log.info("收到秒杀订单消息：{}", order);
        try {
            voucherOrderService.createVoucherOrder(order);
        } catch (Exception e) {
            log.error("处理秒杀订单失败，订单ID：{}", order.getId(), e);
        }
    }
}