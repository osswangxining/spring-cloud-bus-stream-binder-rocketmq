/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.stream.binder.rocketmq.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.rocketmq.RocketMQBinderHealthIndicator;
import org.springframework.cloud.stream.binder.rocketmq.RocketMQBinderJaasInitializerListener;
import org.springframework.cloud.stream.binder.rocketmq.RocketMQBinderMetrics;
import org.springframework.cloud.stream.binder.rocketmq.RocketMQMessageChannelBinder;
import org.springframework.cloud.stream.binder.rocketmq.properties.JaasLoginModuleConfiguration;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQExtendedBindingProperties;
import org.springframework.cloud.stream.binder.rocketmq.provisioning.RocketMQTopicProvisioner;
import org.springframework.cloud.stream.binder.rocketmq.support.RocketMQResourceManager;
import org.springframework.cloud.stream.config.codec.kryo.KryoCodecAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.codec.Codec;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.util.ObjectUtils;

/**
 * @author Xi Ning Wang
 */
@Configuration
@ConditionalOnMissingBean(Binder.class)
@Import({ KryoCodecAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
@EnableConfigurationProperties({ RocketMQBinderConfigurationProperties.class, RocketMQExtendedBindingProperties.class })
public class RocketMQBinderConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(RocketMQResourceManager.class);

	@Autowired
	private Codec codec;

	@Autowired
	private RocketMQBinderConfigurationProperties configurationProperties;

	@Autowired
	private RocketMQExtendedBindingProperties rocketmqExtendedBindingProperties;

	// @Autowired
	// private ProducerListener producerListener;

	@Autowired
	private ApplicationContext context;

	@Bean
	RocketMQTopicProvisioner provisioningProvider() {
		return new RocketMQTopicProvisioner(this.configurationProperties);
	}

	@Bean
	RocketMQMessageChannelBinder rocketmqMessageChannelBinder() {
		RocketMQMessageChannelBinder messageChannelBinder = new RocketMQMessageChannelBinder(
				this.configurationProperties, provisioningProvider());
		messageChannelBinder.setCodec(this.codec);
		// messageChannelBinder.setProducerListener(producerListener);
		messageChannelBinder.setExtendedBindingProperties(this.rocketmqExtendedBindingProperties);
		return messageChannelBinder;
	}


	@Bean
	RocketMQBinderHealthIndicator healthIndicator(RocketMQMessageChannelBinder kafkaMessageChannelBinder) {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
		if (!ObjectUtils.isEmpty(configurationProperties.getConsumerConfiguration())) {
			props.putAll(configurationProperties.getConsumerConfiguration());
		}
		if (!props.containsKey(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
			props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
					this.configurationProperties.getRocketMQConnectionString());
		}
		ConsumerFactory<?, ?> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
		RocketMQBinderHealthIndicator indicator = new RocketMQBinderHealthIndicator(kafkaMessageChannelBinder,
				consumerFactory);
		indicator.setTimeout(this.configurationProperties.getHealthTimeout());
		return indicator;
	}

	@Bean
	public PublicMetrics kafkaBinderMetrics(RocketMQMessageChannelBinder kafkaMessageChannelBinder) {
		return new RocketMQBinderMetrics(kafkaMessageChannelBinder, configurationProperties);
	}

	@Bean
	public ApplicationListener<?> jaasInitializer() throws IOException {
		return new RocketMQBinderJaasInitializerListener();
	}

	public static class JaasConfigurationProperties {

		private JaasLoginModuleConfiguration rocketmq;

	}
}
