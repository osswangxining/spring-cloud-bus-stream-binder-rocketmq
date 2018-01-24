package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.bus.event.RefreshListener;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.MessageConverter;

@SpringBootApplication
@RefreshScope
@RemoteApplicationEventScan(basePackages= {"com.example.demo", "org.springframework.cloud.bus.event"})
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	//
	// @StreamListener(Processor.INPUT)
	// @SendTo(Processor.OUTPUT)
	// public byte[] handle(byte[] in){
	// return new String(in).toUpperCase().getBytes();
	// }
//	@Bean
//	public MessageConverter customMessageConverter() {
//		return new MyCustomMessageConverter();
//	}
	
	
}
