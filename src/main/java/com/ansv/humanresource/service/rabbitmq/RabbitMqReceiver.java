package com.ansv.humanresource.service.rabbitmq;

import com.ansv.humanresource.constants.TypeRequestEnum;
import com.ansv.humanresource.dto.mapper.BaseMapper;
import com.ansv.humanresource.dto.response.UserDTO;
import com.ansv.humanresource.model.UserEntity;
import com.ansv.humanresource.repository.UserEntityRepository;
import com.ansv.humanresource.service.UserService;
import com.ansv.humanresource.util.DataUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMqReceiver implements RabbitListenerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqReceiver.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final BaseMapper<UserEntity, UserDTO> mapper = new BaseMapper<>(UserEntity.class, UserDTO.class);

    public RabbitMqReceiver() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Autowired
    private UserService userService;

    @Value("${spring.rabbitmq.exchange:#{null}}")
    private String exchange;

    @Value("${spring.rabbitmq.routingkey:#{null}}")
    private String routingkey;

    @Value("${spring.rabbitmq.routingkey-human:#{null}}")
    private String routingkeyHuman;

    public UserDTO userDTO = new UserDTO();

    public String username = "";

    // @RabbitListener(queues = "${spring.rabbitmq.queue}")
    // public void receivedMessage(UserDTO user) {
    // logger.info("User Details Received is.. " + user.getUsername());
    // userDTO = user;
    // }

    @RabbitListener(queues = "${spring.rabbitmq.queue-human}")
    public void receivedMessageFromGateway(UserDTO item, Message message) {

        try {


            MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {

                @Override
                public Message postProcessMessage(Message mess) throws AmqpException {
                    // TODO Auto-generated method stub
                    MessageProperties messageProperties =  mess.getMessageProperties();
                    messageProperties.setCorrelationId(message.getMessageProperties().getCorrelationId().toString());
                    return message;
                }
                
            };

            UserDTO user = new UserDTO();
            if (item.getTypeRequest().equals(TypeRequestEnum.INSERT.getName())) {
                // creating if user isn't exist in db
                log.warn("User not found with username ----> create in db",
                        item.getUsername());
                item.setStatus("ACTIVE");
                user = userService.save(item);
                rabbitTemplate.convertAndSend(exchange, routingkey, user, messagePostProcessor);

            }
            if (item.getTypeRequest().equals(TypeRequestEnum.VIEW.getName())) {
                // creating if user isn't exist in db
                user = userService.findByUsername(item);
                if (!DataUtils.isNullOrEmpty(user)) {
                    if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        throw new UsernameNotFoundException("User not found with username: ");
                    }

                    rabbitTemplate.convertAndSend(exchange, routingkey, user, messagePostProcessor);
                    // rabbitTemplate.convertSendAndReceive(exchange, routingkeyHuman, user);
                }
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

    }

    @RabbitListener(queues = "${spring.rabbitmq.queue}")
    public void receiveMessageTest(String json) {
        logger.info("HUMAN SERVICE Received is.. " + json);
    }

    // @RabbitListener(queues = "${spring.rabbitmq.queue-human}")
    // public UserDTO receivedMessageFromGateway(UserDTO item) {
    // try {
    // UserDTO user = new UserDTO();
    // if (item.getTypeRequest().equals(TypeRequestEnum.INSERT.getName())) {
    // // creating if user isn't exist in db
    // log.warn("User not found with username ----> create in db",
    // item.getUsername());
    // item.setStatus("ACTIVE");
    // user = userService.save(item);
    // // rabbitTemplate.convertAndSend(exchange, routingkey, user);
    // }
    // if (item.getTypeRequest().equals(TypeRequestEnum.VIEW.getName())) {
    // // creating if user isn't exist in db
    // user = userService.findByUsername(item);
    // if (!DataUtils.isNullOrEmpty(user)) {
    // if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
    // throw new UsernameNotFoundException("User not found with username: ");
    // }

    // // rabbitTemplate.convertAndSend(exchange, routingkey, user);
    // }
    // }

    // return user;

    // } catch (Exception ex) {
    // logger.error(ex.getMessage(), ex);
    // return null;
    // }

    // }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar rabbitListenerEndpointRegistrar) {

    }

}
