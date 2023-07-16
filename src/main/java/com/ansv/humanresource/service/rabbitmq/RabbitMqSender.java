package com.ansv.humanresource.service.rabbitmq;

import com.ansv.humanresource.dto.response.UserDTO;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class RabbitMqSender {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.exchange:#{null}}")
    private String exchange;

    @Value("${spring.rabbitmq.routingkey:#{null}}")
    private String routingkey;
    

    @Value("${spring.rabbitmq.routingkey-human:#{null}}")
    private String routingkeyHuman;

    public void sender(UserDTO user) {
        rabbitTemplate.convertAndSend(exchange, routingkeyHuman, user);
    }

    public void senderToGateway(UserDTO user) {
        rabbitTemplate.convertAndSend(exchange, routingkey, user);
    }

}
