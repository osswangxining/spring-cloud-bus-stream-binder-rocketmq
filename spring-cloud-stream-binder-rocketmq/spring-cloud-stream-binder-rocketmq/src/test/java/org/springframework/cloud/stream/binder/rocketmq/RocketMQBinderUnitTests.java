/*
 * Copyright 2017 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.Test;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.rocketmq.properties.RocketMQConsumerProperties;
import org.springframework.cloud.stream.binder.rocketmq.provisioning.RocketMQTopicProvisioner;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Xi Ning Wang
 *
 */
public class RocketMQBinderUnitTests {

	@Test
	public void testPropertyOverrides() throws Exception {
		RocketMQBinderConfigurationProperties binderConfigurationProperties = new RocketMQBinderConfigurationProperties();
		// AdminUtilsOperation adminUtilsOperation = mock(AdminUtilsOperation.class);
		RocketMQTopicProvisioner provisioningProvider = new RocketMQTopicProvisioner(binderConfigurationProperties);
		RocketMQMessageChannelBinder binder = new RocketMQMessageChannelBinder(binderConfigurationProperties,
				provisioningProvider);
		RocketMQConsumerProperties consumerProps = new RocketMQConsumerProperties();
		ExtendedConsumerProperties<RocketMQConsumerProperties> ecp = new ExtendedConsumerProperties<RocketMQConsumerProperties>(
				consumerProps);
		Method method = RocketMQMessageChannelBinder.class.getDeclaredMethod("createKafkaConsumerFactory",
				boolean.class, String.class, ExtendedConsumerProperties.class);
		method.setAccessible(true);

		// test default for anon
		Object factory = method.invoke(binder, true, "foo", ecp);
		Map<?, ?> configs = TestUtils.getPropertyValue(factory, "configs", Map.class);
		assertThat(configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)).isEqualTo("latest");

		// test default for named
		factory = method.invoke(binder, false, "foo", ecp);
		configs = TestUtils.getPropertyValue(factory, "configs", Map.class);
		assertThat(configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)).isEqualTo("earliest");

		// binder level setting
		binderConfigurationProperties
				.setConfiguration(Collections.singletonMap(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"));
		factory = method.invoke(binder, false, "foo", ecp);
		configs = TestUtils.getPropertyValue(factory, "configs", Map.class);
		assertThat(configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)).isEqualTo("latest");

		// consumer level setting
		consumerProps.setConfiguration(Collections.singletonMap(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"));
		factory = method.invoke(binder, false, "foo", ecp);
		configs = TestUtils.getPropertyValue(factory, "configs", Map.class);
		assertThat(configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)).isEqualTo("earliest");
	}

}
