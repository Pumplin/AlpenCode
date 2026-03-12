package org.ruoyi.system.judge.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 判题队列配置
 * @author 32846
 */
@Configuration
public class RabbitMQConfig {

    public static final String JUDGE_QUEUE = "oj.judge.queue";
    public static final String JUDGE_EXCHANGE = "oj.judge.exchange";
    public static final String JUDGE_ROUTING_KEY = "oj.judge";

    @Bean
    public Queue judgeQueue() {
        return QueueBuilder.durable(JUDGE_QUEUE).build();
    }

    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange(JUDGE_EXCHANGE, true, false);
    }

    @Bean
    public Binding judgeBinding(Queue judgeQueue, DirectExchange judgeExchange) {
        return BindingBuilder.bind(judgeQueue).to(judgeExchange).with(JUDGE_ROUTING_KEY);
    }
}
