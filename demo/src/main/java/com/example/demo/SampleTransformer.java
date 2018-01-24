/*
 * Copyright 2015 the original author or authors.
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

package com.example.demo;

import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;

import com.example.demo.Bar;

/**
 * @author Ilayaperumal Gopinathan
 */
//@EnableBinding(Processor.class)
public class SampleTransformer {

	// Transformer application definition

//	@StreamListener(Processor.INPUT)
//	@SendTo(Processor.OUTPUT)
//	public RemoteApplicationEvent receive(RemoteApplicationEvent barMessage) {
//		System.out.println("******************");
//		System.out.println("At the transformer");
//		System.out.println("Received value " + barMessage.getSource() + " of type " + barMessage.getClass());
//		System.out.println("Transforming the value to " + barMessage.getSource() + " and with the type "
//				+ barMessage.getClass());
//		System.out.println("******************");
//
//		
//		
//		//Message<Bar> msg = MessageBuilder.withPayload(barMessage).build();
//		return barMessage;
//	}

	// @StreamListener(Processor.INPUT)
	// @SendTo(Processor.OUTPUT)
	// public String receive(RemoteApplicationEvent barMessage) {
	// System.out.println("******************");
	// System.out.println("At the transformer");
	// System.out.println("Received value " + barMessage.toString() + " of type " +
	// barMessage.getClass());
	// System.out.println("Transforming the value to " +
	// barMessage.toString().toUpperCase() + " and with the type "
	// + barMessage.getClass());
	// System.out.println("******************");
	// String value = "{\"value\":\"" + barMessage.toString().toUpperCase() + "\"}";
	// System.out.println(value);
	//// return
	// MessageBuilder.withPayload(value).setHeader(MessageHeaders.CONTENT_TYPE,
	// "application/json").build();
	// return value;
	// }

}
