/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder.rocketmq;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.EmbeddedHeaderUtils;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.binder.MessageValues;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQConsumerProperties;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQExtendedBindingProperties;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQProducerProperties;
import org.springframework.cloud.stream.binder.rocketmq.provisioning.RocketMQTopicProvisioner;
import org.springframework.cloud.stream.binder.rocketmq.support.RocketMQMessageHandler;
import org.springframework.cloud.stream.binder.rocketmq.support.RocketMQMessageListener;
import org.springframework.cloud.stream.binder.rocketmq.support.RocketMQResourceManager;
import org.springframework.cloud.stream.binder.rocketmq.utils.CommonUtils;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link Binder} that uses RocketMQ as the underlying middleware.
 *
 * @author Xi Ning Wang
 *
 */
public class RocketMQMessageChannelBinder extends
		AbstractMessageChannelBinder<ExtendedConsumerProperties<RocketMQConsumerProperties>, ExtendedProducerProperties<RocketMQProducerProperties>, RocketMQTopicProvisioner>
		implements ExtendedPropertiesBinder<MessageChannel, RocketMQConsumerProperties, RocketMQProducerProperties> {
	private static final Logger logger = LoggerFactory.getLogger(RocketMQResourceManager.class);

	public static final String X_ORIGINAL_TOPIC = "x-original-topic";

	public static final String X_EXCEPTION_MESSAGE = "x-exception-message";

	public static final String X_EXCEPTION_STACKTRACE = "x-exception-stacktrace";

	private final RocketMQBinderConfigurationProperties configurationProperties;

	private final Map<String, TopicInformation> topicsInUse = new HashMap<>();

	private RocketMQResourceManager resourceManager;

	private RocketMQExtendedBindingProperties extendedBindingProperties = new RocketMQExtendedBindingProperties();

	public RocketMQMessageChannelBinder(RocketMQBinderConfigurationProperties configurationProperties,
			RocketMQTopicProvisioner provisioningProvider) {
		super(false, headersToMap(configurationProperties), provisioningProvider);
		this.configurationProperties = configurationProperties;

		RocketMQResourceManager rocketmqResourceManager = new RocketMQResourceManager(configurationProperties);
		this.resourceManager = rocketmqResourceManager;
	}

	private static String[] headersToMap(RocketMQBinderConfigurationProperties configurationProperties) {
		String[] headersToMap;
		if (ObjectUtils.isEmpty(configurationProperties.getHeaders())) {
			headersToMap = BinderHeaders.STANDARD_HEADERS;
		} else {
			String[] combinedHeadersToMap = Arrays.copyOfRange(BinderHeaders.STANDARD_HEADERS, 0,
					BinderHeaders.STANDARD_HEADERS.length + configurationProperties.getHeaders().length);
			System.arraycopy(configurationProperties.getHeaders(), 0, combinedHeadersToMap,
					BinderHeaders.STANDARD_HEADERS.length, configurationProperties.getHeaders().length);
			headersToMap = combinedHeadersToMap;
		}
		return headersToMap;
	}

	public void setExtendedBindingProperties(RocketMQExtendedBindingProperties extendedBindingProperties) {
		this.extendedBindingProperties = extendedBindingProperties;
	}


	Map<String, TopicInformation> getTopicsInUse() {
		return this.topicsInUse;
	}

	@Override
	public RocketMQConsumerProperties getExtendedConsumerProperties(String channelName) {
		return this.extendedBindingProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public RocketMQProducerProperties getExtendedProducerProperties(String channelName) {
		return this.extendedBindingProperties.getExtendedProducerProperties(channelName);
	}

	@Override
	protected MessageHandler createProducerMessageHandler(final ProducerDestination destination,
			ExtendedProducerProperties<RocketMQProducerProperties> producerProperties, MessageChannel errorChannel)
			throws Exception {

		TopicConfig topicConfig = new TopicConfig(destination.getName());

		RocketMQMessageHandler handler = new RocketMQMessageHandler(this.resourceManager, producerProperties,
				Collections.singletonList(topicConfig));

		return handler;
	}


	@Override
	@SuppressWarnings("unchecked")
	protected MessageProducer createConsumerEndpoint(final ConsumerDestination destination, final String group,
			final ExtendedConsumerProperties<RocketMQConsumerProperties> extendedConsumerProperties) {

		boolean anonymous = !StringUtils.hasText(group);
		Assert.isTrue(!anonymous || !extendedConsumerProperties.getExtension().isEnableDlq(),
				"DLQ support is not available for anonymous subscriptions");
		String consumerGroup = anonymous ? "anonymous-" + UUID.randomUUID().toString() : group;

		return new RocketMQMessageListener(resourceManager, destination.getName(), consumerGroup,
				extendedConsumerProperties);
	}

	@Override
	protected ErrorMessageStrategy getErrorMessageStrategy() {
		return new RawRecordHeaderErrorMessageStrategy();
	}

	@Override
	protected MessageHandler getErrorMessageHandler(final ConsumerDestination destination, final String group,
			final ExtendedConsumerProperties<RocketMQConsumerProperties> extendedConsumerProperties) {
		if (extendedConsumerProperties.getExtension().isEnableDlq()) {

			final DefaultMQProducer producer = new DefaultMQProducer(
					"errorChannelGroup" + UUID.randomUUID().toString());
			producer.setNamesrvAddr(this.configurationProperties.getNameSrvConnectionString());
			return new MessageHandler() {

				@Override
				public void handleMessage(Message<?> message) throws MessagingException {
//					final ConsumerRecord<?, ?> record = message.getHeaders()
//							.get("RAW_DATA", ConsumerRecord.class);
					
					final byte[] payload;
					if (message.getPayload() instanceof Throwable) {
						final Throwable throwable = (Throwable) message.getPayload();
						final String failureMessage = throwable.getMessage();

						try {
							MessageValues messageValues = EmbeddedHeaderUtils
									.extractHeaders(MessageBuilder.withPayload((byte[]) message.getPayload()).build(), false);
							//messageValues.put(X_ORIGINAL_TOPIC, record.topic());
							messageValues.put(X_EXCEPTION_MESSAGE, failureMessage);
							messageValues.put(X_EXCEPTION_STACKTRACE, getStackTraceAsString(throwable));

							final String[] headersToEmbed = new ArrayList<>(messageValues.keySet())
									.toArray(new String[messageValues.keySet().size()]);
							payload = EmbeddedHeaderUtils.embedHeaders(messageValues,
									EmbeddedHeaderUtils.headersToEmbed(headersToEmbed));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					} else {
						payload = message.getPayload() != null ? CommonUtils.toArray(ByteBuffer.wrap((byte[]) message.getPayload()))
								: null;
					}
					String dlqName = StringUtils.hasText(extendedConsumerProperties.getExtension().getDlqName())
							? extendedConsumerProperties.getExtension().getDlqName()
							: "error." + destination.getName() + "." + group;

					org.apache.rocketmq.common.message.Message msg = new org.apache.rocketmq.common.message.Message(
							dlqName, "errorchannelmsg", payload);
					try {
						producer.send(msg, new SendCallback() {
							StringBuilder sb = new StringBuilder().append(" a message with payload='")
									.append(toDisplayString(ObjectUtils.nullSafeToString(payload), 50)).append("'");

							@Override
							public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
								if (RocketMQMessageChannelBinder.this.logger.isDebugEnabled()) {
									RocketMQMessageChannelBinder.this.logger.debug("Sent to DLQ " + sb.toString());
								}
							}

							@Override
							public void onException(Throwable e) {
								RocketMQMessageChannelBinder.this.logger.error("Error sending to DLQ " + sb.toString(), e);
							}

						});
					} catch (MQClientException | RemotingException | InterruptedException e) {
						e.printStackTrace();
					} finally {
						producer.shutdown();
					}
				}
			};
		}
		return null;
	}

	private boolean isAutoCommitOnError(ExtendedConsumerProperties<RocketMQConsumerProperties> properties) {
		return properties.getExtension().getAutoCommitOnError() != null
				? properties.getExtension().getAutoCommitOnError()
				: properties.getExtension().isAutoCommitOffset() && properties.getExtension().isEnableDlq();
	}

//	private TopicPartitionInitialOffset[] getTopicPartitionInitialOffsets(
//			Collection<PartitionInfo> listenedPartitions) {
//		final TopicPartitionInitialOffset[] topicPartitionInitialOffsets = new TopicPartitionInitialOffset[listenedPartitions
//				.size()];
//		int i = 0;
//		for (PartitionInfo partition : listenedPartitions) {
//
//			topicPartitionInitialOffsets[i++] = new TopicPartitionInitialOffset(partition.topic(),
//					partition.partition());
//		}
//		return topicPartitionInitialOffsets;
//	}

	private String toDisplayString(String original, int maxCharacters) {
		if (original.length() <= maxCharacters) {
			return original;
		}
		return original.substring(0, maxCharacters) + "...";
	}

	private String getStackTraceAsString(Throwable cause) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter, true);
		cause.printStackTrace(printWriter);
		return stringWriter.getBuffer().toString();
	}

	public static class TopicInformation {

		private final String consumerGroup;

		//private final Collection<PartitionInfo> partitionInfos;

		public TopicInformation(String consumerGroup) {
			this.consumerGroup = consumerGroup;
		}

		public String getConsumerGroup() {
			return consumerGroup;
		}

		public boolean isConsumerTopic() {
			return consumerGroup != null;
		}


	}

}
