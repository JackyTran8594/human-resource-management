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
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

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


    @RabbitListener(queues = "${spring.rabbitmq.queue-human}")
    public void receivedMessageFromGateway(
            @Header(value = AmqpHeaders.REPLY_TO, required = false) String senderId,
            @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String correlationId,
            @Header(value = "typeRequest", required = false) String type,
            String username) {

        try {

            UserDTO userDTO = new UserDTO();
            if (senderId != null && correlationId != null) {

                if (type.equals(TypeRequestEnum.INSERT.getName())) {
                    // creating if user isn't exist in db
                    logger.warn("User not found with username ----> create in db",
                            username);
                    userDTO.setStatus("ACTIVE");
                    userDTO.setUsername(username);
                    userDTO = userService.save(userDTO);
                    rabbitTemplate.convertAndSend(senderId, objectMapper.writeValueAsString(userDTO),
                            message -> {
                                MessageProperties properties = message.getMessageProperties();
                                properties.setCorrelationId(correlationId);
                                return message;
                            });

                }
                if (type.equals(TypeRequestEnum.VIEW.getName())) {
                    // creating if user isn't exist in db
                    userDTO = userService.findByUsername(username);
                    if (!DataUtils.isNullOrEmpty(userDTO)) {
                        if (!"ACTIVE".equalsIgnoreCase(userDTO.getStatus())) {
                            throw new UsernameNotFoundException("User not found with username: ");
                        }
                        logger.info("-----VIEW:" + username);
                        rabbitTemplate.convertAndSend(senderId, objectMapper.writeValueAsString(userDTO),
                            message -> {
                                MessageProperties properties = message.getMessageProperties();
                                properties.setCorrelationId(correlationId);
                                return message;
                            });
                    }
                }
               
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

    }

    // @RabbitListener(queues = "${spring.rabbitmq.queue-human}")
    // public void receiveMessageTest(
    //         @Header(value = AmqpHeaders.REPLY_TO, required = false) String senderId,
    //         @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String correlationId,
    //         String requestMessage) {
    //     logger.info("HUMAN SERVICE Received is.. " + requestMessage);

    //     if (senderId != null && correlationId != null) {
    //         String success = "Success";
    //         rabbitTemplate.convertAndSend(senderId, success, message -> {
    //             MessageProperties properties = message.getMessageProperties();
    //             properties.setCorrelationId(correlationId);
    //             return message;
    //         });
    //     }

    // }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar rabbitListenerEndpointRegistrar) {

    }

}
