package com.example.demo;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.endpoint.BusEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusGuavaAutoConfiguration extends BusAutoConfiguration {
	public BusGuavaAutoConfiguration() {
	}

	@Configuration
	protected static class BusGuavaConfiguration {
		protected BusGuavaConfiguration() {
		}

		@Bean
		@ConditionalOnProperty(value = { "spring.cloud.bus.guava.enabled" }, matchIfMissing = true)
		public GuavaCacheChangeListener guavaCacheChangeListener() {
			return new GuavaCacheChangeListener();
		}

		@Configuration
		@ConditionalOnClass({ Endpoint.class })
		@ConditionalOnProperty(value = { "endpoints.spring.cloud.bus.guava.enabled" }, matchIfMissing = true)
		protected static class GuavaBusEndpointConfiguration {
			protected GuavaBusEndpointConfiguration() {
			}

			@Bean
			public GuavaCacheBusEndpoint environmentBusEndpoint(ApplicationContext context, BusEndpoint busEndpoint) {
				return new GuavaCacheBusEndpoint(context, context.getId(), busEndpoint);
			}
		}
	}

}
