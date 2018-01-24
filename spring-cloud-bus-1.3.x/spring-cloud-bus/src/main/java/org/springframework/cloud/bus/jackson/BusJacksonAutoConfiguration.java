/*
 * Copyright 2013-2017 the original author or authors.
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
 *
 */

package org.springframework.cloud.bus.jackson;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.cloud.bus.endpoint.RefreshBusEndpoint;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.bus.event.UnknownRemoteApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Donovan Muller
 * @author Stefan Pfeiffer
 */
@Configuration
@ConditionalOnBusEnabled
@ConditionalOnClass({ RefreshBusEndpoint.class, ObjectMapper.class })
@AutoConfigureAfter(BusAutoConfiguration.class)
public class BusJacksonAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "busJsonConverter")
	public BusJacksonMessageConverter busJsonConverter() {
		return new BusJacksonMessageConverter();
	}

}

class BusJacksonMessageConverter extends AbstractMessageConverter
		implements InitializingBean {

	private static final String DEFAULT_PACKAGE = ClassUtils
			.getPackageName(RemoteApplicationEvent.class);

	private final ObjectMapper mapper = new ObjectMapper();

	private String[] packagesToScan = new String[] { DEFAULT_PACKAGE };

	public void setPackagesToScan(String[] packagesToScan) {
		List<String> packages = new ArrayList<>(Arrays.asList(packagesToScan));
		if (!packages.contains(DEFAULT_PACKAGE)) {
			packages.add(DEFAULT_PACKAGE);
		}
		this.packagesToScan = packages.toArray(new String[0]);
	}

	public BusJacksonMessageConverter() {
		super(MimeTypeUtils.APPLICATION_JSON);
		mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	private Class<?>[] findSubTypes() {
		List<Class<?>> types = new ArrayList<>();
		if (this.packagesToScan != null) {
			for (String pkg : this.packagesToScan) {
				ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
						false);
				provider.addIncludeFilter(
						new AssignableTypeFilter(RemoteApplicationEvent.class));

				Set<BeanDefinition> components = provider.findCandidateComponents(pkg);
				for (BeanDefinition component : components) {
					try {
						types.add(Class.forName(component.getBeanClassName()));
					}
					catch (ClassNotFoundException e) {
						throw new IllegalStateException(
								"Failed to scan classpath for remote event classes", e);
					}
				}
			}
		}
		return types.toArray(new Class<?>[0]);
	}

	@Override
	protected boolean supports(Class<?> aClass) {
		// This converter applies only to RemoteApplicationEvent and subclasses
		return RemoteApplicationEvent.class.isAssignableFrom(aClass);
	}

	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass,
			Object conversionHint) {
		Object result = null;
		try {
			Object payload = message.getPayload();

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
					return new UnknownRemoteApplicationEvent(new Object(), e.getTypeId(), ((String) payload).getBytes());
				}
			}
		}
		catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			return null;
		}
		return result;
	}
	@Override
	public Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		try {
			Class<?> view = getSerializationView(conversionHint);
//			if (byte[].class == getSerializedPayloadClass()) {
//				ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
//				JsonEncoding encoding = getJsonEncoding(getMimeType(headers));
//				JsonGenerator generator = mapper.getFactory().createGenerator(out, encoding);
//				if (view != null) {
//					mapper.writerWithView(view).writeValue(generator, payload);
//				}
//				else {
//					mapper.writeValue(generator, payload);
//				}
//				payload = out.toByteArray();
//			}
//			else {
				Writer writer = new StringWriter();
				if (view != null) {
					mapper.writerWithView(view).writeValue(writer, payload);
				}
				else {
					mapper.writeValue(writer, payload);
				}
				payload = writer.toString();
//			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
		return payload;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		this.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		this.mapper.registerModule(new SubtypeModule(findSubTypes()));
	}
	
	protected Class<?> getSerializationView(Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter param = (MethodParameter) conversionHint;
			JsonView annotation = (param.getParameterIndex() >= 0 ?
					param.getParameterAnnotation(JsonView.class) : param.getMethodAnnotation(JsonView.class));
			if (annotation != null) {
				return extractViewClass(annotation, conversionHint);
			}
		}
		else if (conversionHint instanceof JsonView) {
			return extractViewClass((JsonView) conversionHint, conversionHint);
		}
		else if (conversionHint instanceof Class) {
			return (Class<?>) conversionHint;
		}

		// No JSON view specified...
		return null;
	}
	
	private Class<?> extractViewClass(JsonView annotation, Object conversionHint) {
		Class<?>[] classes = annotation.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for handler methods with exactly 1 class argument: " + conversionHint);
		}
		return classes[0];
	}
	
	/**
	 * Determine the JSON encoding to use for the given content type.
	 * @param contentType the MIME type from the MessageHeaders, if any
	 * @return the JSON encoding to use (never {@code null})
	 */
	protected JsonEncoding getJsonEncoding(MimeType contentType) {
		if ((contentType != null) && (contentType.getCharset() != null)) {
			Charset charset = contentType.getCharset();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}
}
