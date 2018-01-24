package com.example.demo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.bus.endpoint.AbstractBusEndpoint;
import org.springframework.cloud.bus.endpoint.BusEndpoint;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public class GuavaCacheBusEndpoint extends AbstractBusEndpoint {

	public GuavaCacheBusEndpoint(ApplicationEventPublisher context, String appId, BusEndpoint busEndpoint) {
		super(context, appId, busEndpoint);
	}

	@RequestMapping(value = { "removecache" }, method = { RequestMethod.GET })
	@ResponseBody
	@ManagedOperation
	public void removecache(
			@RequestParam(value = "destination", required = false) String destination) {
		Map<String, String> params = new HashMap<>();
		params.put("key1", "value1");
		GuavaCacheChangeRemoteApplicationEvent guavaCacheChangeRemoteApplicationEvent = new GuavaCacheChangeRemoteApplicationEvent(this, this.getInstanceId(), destination, params);
		this.publish(guavaCacheChangeRemoteApplicationEvent);
	}

	@RequestMapping(value = "refresh", method = RequestMethod.GET)
	@ResponseBody
	@ManagedOperation
	public void refresh(
			@RequestParam(value = "destination", required = false) String destination) {
		publish(new RefreshRemoteApplicationEvent(this, getInstanceId(), destination));
	}

}
