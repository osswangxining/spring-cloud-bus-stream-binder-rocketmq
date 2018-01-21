/*
 * Copyright 2016-2017 the original author or authors.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.rocketmq.config.RocketMQBinderConfiguration;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQConsumerProperties;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQProducerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { RocketMQBinderAutoConfigurationPropertiesTest.KafkaBinderConfigProperties.class,
		RocketMQBinderConfiguration.class })
@TestPropertySource(locations = "classpath:binder-config-autoconfig.properties")
public class RocketMQBinderAutoConfigurationPropertiesTest {

	@Autowired
	private RocketMQMessageChannelBinder kafkaMessageChannelBinder;

	@Autowired
	private RocketMQBinderHealthIndicator kafkaBinderHealthIndicator;

	@Test
	public void testRocketMQBinderConfigurationWithRocketMQProperties() throws Exception {
		assertNotNull(this.kafkaMessageChannelBinder);
		ExtendedProducerProperties<RocketMQProducerProperties> producerProperties = new ExtendedProducerProperties<>(
				new RocketMQProducerProperties());
		Method getProducerFactoryMethod = RocketMQMessageChannelBinder.class.getDeclaredMethod("getProducerFactory",
				ExtendedProducerProperties.class);
		getProducerFactoryMethod.setAccessible(true);
		DefaultKafkaProducerFactory producerFactory = (DefaultKafkaProducerFactory) getProducerFactoryMethod
				.invoke(this.kafkaMessageChannelBinder, producerProperties);
		Field producerFactoryConfigField = ReflectionUtils.findField(DefaultKafkaProducerFactory.class, "configs",
				Map.class);
		ReflectionUtils.makeAccessible(producerFactoryConfigField);
		Map<String, Object> producerConfigs = (Map<String, Object>) ReflectionUtils.getField(producerFactoryConfigField,
				producerFactory);
		assertTrue(producerConfigs.get("batch.size").equals(10));
		assertTrue(producerConfigs.get("key.serializer").equals(LongSerializer.class));
		assertTrue(producerConfigs.get("key.deserializer") == null);
		assertTrue(producerConfigs.get("value.serializer").equals(LongSerializer.class));
		assertTrue(producerConfigs.get("value.deserializer") == null);
		assertTrue(producerConfigs.get("compression.type").equals("snappy"));
		List<String> bootstrapServers = new ArrayList<>();
		bootstrapServers.add("10.98.09.199:9092");
		bootstrapServers.add("10.98.09.196:9092");
		assertTrue((((List<String>) producerConfigs.get("bootstrap.servers")).containsAll(bootstrapServers)));
		Method createKafkaConsumerFactoryMethod = RocketMQMessageChannelBinder.class.getDeclaredMethod(
				"createKafkaConsumerFactory", boolean.class, String.class, ExtendedConsumerProperties.class);
		createKafkaConsumerFactoryMethod.setAccessible(true);
		ExtendedConsumerProperties<RocketMQConsumerProperties> consumerProperties = new ExtendedConsumerProperties<>(
				new RocketMQConsumerProperties());
		DefaultKafkaConsumerFactory consumerFactory = (DefaultKafkaConsumerFactory) createKafkaConsumerFactoryMethod
				.invoke(this.kafkaMessageChannelBinder, true, "test", consumerProperties);
		Field consumerFactoryConfigField = ReflectionUtils.findField(DefaultKafkaConsumerFactory.class, "configs",
				Map.class);
		ReflectionUtils.makeAccessible(consumerFactoryConfigField);
		Map<String, Object> consumerConfigs = (Map<String, Object>) ReflectionUtils.getField(consumerFactoryConfigField,
				consumerFactory);
		assertTrue(consumerConfigs.get("key.deserializer").equals(LongDeserializer.class));
		assertTrue(consumerConfigs.get("key.serializer") == null);
		assertTrue(consumerConfigs.get("value.deserializer").equals(LongDeserializer.class));
		assertTrue(consumerConfigs.get("value.serialized") == null);
		assertTrue(consumerConfigs.get("group.id").equals("groupIdFromBootConfig"));
		assertTrue(consumerConfigs.get("auto.offset.reset").equals("earliest"));
		assertTrue((((List<String>) consumerConfigs.get("bootstrap.servers")).containsAll(bootstrapServers)));
	}

	@Test
	public void testKafkaHealthIndicatorProperties() {
		assertNotNull(this.kafkaBinderHealthIndicator);
		Field consumerFactoryField = ReflectionUtils.findField(RocketMQBinderHealthIndicator.class, "consumerFactory",
				ConsumerFactory.class);
		ReflectionUtils.makeAccessible(consumerFactoryField);
		DefaultKafkaConsumerFactory consumerFactory = (DefaultKafkaConsumerFactory) ReflectionUtils.getField(
				consumerFactoryField, this.kafkaBinderHealthIndicator);
		Field configField = ReflectionUtils.findField(DefaultKafkaConsumerFactory.class, "configs", Map.class);
		ReflectionUtils.makeAccessible(configField);
		Map<String, Object> configs = (Map<String, Object>) ReflectionUtils.getField(configField, consumerFactory);
		assertTrue(configs.containsKey("bootstrap.servers"));
		List<String> bootstrapServers = new ArrayList<>();
		bootstrapServers.add("10.98.09.199:9092");
		bootstrapServers.add("10.98.09.196:9092");
		assertTrue(((List<String>) configs.get("bootstrap.servers")).containsAll(bootstrapServers));
	}

	public static class KafkaBinderConfigProperties {

		@Bean
		KafkaProperties kafkaProperties() {
			return new KafkaProperties();
		}

	}
}
