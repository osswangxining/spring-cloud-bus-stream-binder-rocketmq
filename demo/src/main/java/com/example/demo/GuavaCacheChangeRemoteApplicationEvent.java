package com.example.demo;

import java.util.Map;

import org.springframework.cloud.bus.event.RemoteApplicationEvent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonIgnoreProperties({"source"})
public class GuavaCacheChangeRemoteApplicationEvent extends RemoteApplicationEvent {
	private final Map<String, String> values;

    private GuavaCacheChangeRemoteApplicationEvent() {
        this.values = null;
    }

    public GuavaCacheChangeRemoteApplicationEvent(Object source, String originService, String destinationService, Map<String, String> values) {
        super(source, originService, destinationService);
        this.values = values;
    }

    public Map<String, String> getValues(){
        return values;
    }

}
