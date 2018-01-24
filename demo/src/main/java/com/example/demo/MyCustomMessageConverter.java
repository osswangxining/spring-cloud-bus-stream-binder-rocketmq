package com.example.demo;

import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.bus.event.UnknownRemoteApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.util.MimeType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class MyCustomMessageConverter extends AbstractMessageConverter {
	private final ObjectMapper mapper = new ObjectMapper();
	
	public MyCustomMessageConverter() {
		super(new MimeType("application", "json"));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		System.out.println("MyCustomMessageConverter:" + clazz);
		return (Bar.class == clazz);
	}

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		Object result = null;
		try {
			Object payload = message.getPayload();
			System.out.println(
					"MyCustomMessageConverter:" + (payload instanceof Bar) + "," + (payload instanceof byte[]));

			if (payload instanceof byte[]) {
				try {
					result = this.mapper.readValue((byte[]) payload, targetClass);
				} catch (InvalidTypeIdException e) {
					return new UnknownRemoteApplicationEvent(new Object(), e.getTypeId(), (byte[]) payload);
				}
			} else if (payload instanceof String) {
				try {
					result = this.mapper.readValue((String) payload, targetClass);
				} catch (InvalidTypeIdException e) {
					return new UnknownRemoteApplicationEvent(new Object(), e.getTypeId(),
							((String) payload).getBytes());
				}
			}
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			return null;
		}
		return result;
	}

}
