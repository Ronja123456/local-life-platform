
package com.hmdp.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    // 交换机名
    public static final String X_EXCHANGE = "x-exchange";
    // 队列名
    public static final String QUEUE_A = "queue-a";
    // 绑定键
    public static final String QUEUE_A_BINDING_KEY = "queue-a-key";

    // ===== 声明交换机 =====
    @Bean
    public DirectExchange xExchange() {
        return new DirectExchange(X_EXCHANGE);
    }

    // ===== 声明队列 =====
    @Bean
    public Queue queueA() {
        return QueueBuilder.durable(QUEUE_A).build();
    }

    // ===== 绑定：队列 → 交换机 =====
    @Bean
    public Binding bindingQueueA() {
        return BindingBuilder.bind(queueA())
                .to(xExchange())
                .with(QUEUE_A_BINDING_KEY);
    }
}