package org.springframework.cloud.stream.binder.rocketmq.support;

import java.util.List;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQConsumerProperties;
import org.springframework.integration.endpoint.MessageProducerSupport;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RocketMQMessageListener extends MessageProducerSupport {
	private ObjectMapper mapper;
	private RocketMQResourceManager resourceManager;
	private String topic;
	private String consumerGroup;
	private ExtendedConsumerProperties<RocketMQConsumerProperties> extendedConsumerProperties;
	private DefaultMQPushConsumer consumer;

	protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	public RocketMQMessageListener(RocketMQResourceManager resourceManager, String topic, String consumerGroup,
			ExtendedConsumerProperties<RocketMQConsumerProperties> extendedConsumerProperties) {
		this.resourceManager = resourceManager;
		this.mapper = new ObjectMapper();
		this.topic = topic;
		this.extendedConsumerProperties = extendedConsumerProperties;
		this.consumerGroup = consumerGroup;
	}

	@Override
	protected void doStart() {
		String nameSrvConnectionString = this.resourceManager.getConfigurationProperties().getNameSrvConnectionString();
		String tags = this.extendedConsumerProperties.getExtension().getTags();
		logger.info("[consumer]nameSrvConnectionString:{},consumerGroup:{},topic:{},tags:{}", nameSrvConnectionString,
				consumerGroup, topic, tags);
		consumer = new DefaultMQPushConsumer(consumerGroup);
		consumer.setNamesrvAddr(nameSrvConnectionString);
		consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
		consumer.setMessageModel(MessageModel.BROADCASTING);
		try {
			consumer.subscribe(this.topic, tags);
			consumer.registerMessageListener(new MessageListenerConcurrently() {

				@Override
				public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
						ConsumeConcurrentlyContext context) {
					logger.info(Thread.currentThread().getName() + " Receive New Messages[size={}]: {}", msgs.size(),
							msgs);
					for (MessageExt messageExt : msgs) {
						byte[] body = messageExt.getBody();
						String payload = null;
						try {
//							MessageValues mv = EmbeddedHeaderUtils.extractHeaders(body);
//							byte[] rawPayloadNoHeaders = (byte[]) mv.getPayload();
							payload = new String(body, "UTF-8");
							logger.info(payload);
							logger.info(getOutputChannel().toString());
							sendMessage(getMessageBuilderFactory().withPayload(body).build());

						} catch (Exception e) {
							e.printStackTrace();
							payload = null;
						}
					}
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
			});

			consumer.start();
			logger.info("Consumer Started.%n");
		} catch (MQClientException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doStop() {
		if (this.consumer != null) {
			// this.consumer.shutdown();
			logger.info("Consumer shutdown.%n");
		}
	}
}
