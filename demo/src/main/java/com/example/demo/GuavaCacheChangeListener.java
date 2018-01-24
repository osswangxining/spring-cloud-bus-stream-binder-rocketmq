package com.example.demo;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.context.ApplicationListener;

public class GuavaCacheChangeListener implements ApplicationListener<GuavaCacheChangeRemoteApplicationEvent> {

	@Override
	public void onApplicationEvent(GuavaCacheChangeRemoteApplicationEvent event) {
		Map<String, String> values = event.getValues();
		Set<Entry<String, String>> entrySet = values.entrySet();
		for (Iterator<Entry<String, String>> iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<String, String> entry =iterator.next();
			System.out.println("===========================================================================");
			System.out.println("=========================" + entry.getKey() + ":" + entry.getValue() + "=========================");
			System.out.println("===========================================================================");
		}
		
	}

}
