package com.example.demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Test1 {

	public static void main(String[] args) {
		try {
			test2();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void test2() throws IOException {
		Kryo kryo = new Kryo();
//		kryo.register(Bar.class);
//		kryo.addDefaultSerializer(Bar.class, FieldSerializer.class);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Output output = new Output(baos);
		Bar someObject = new Bar();
		someObject.setValue("hi dasfafaf");
		someObject.setValue2("value2222222");
		kryo.writeObject(output, someObject);

		
		output.flush();
		output.close();
		byte[] byteArray = baos.toByteArray();
		System.out.println("["+new String(byteArray)+"]");
		baos.flush();
		
	
		baos.close();
		
		Input input = new Input(new ByteArrayInputStream(byteArray));
		Bar readObject = kryo.readObject(input, Bar.class);
		System.out.println(readObject.getValue() + "," + readObject.getValue2());
		
        ObjectMapper objectMapper = new ObjectMapper();
        String writeValueAsString = objectMapper.writeValueAsString(readObject);
        System.out.println(writeValueAsString);
	}

	public static void test1(String[] args) {
		ObjectMapper mapper = new ObjectMapper();
		StringWriter sw = new StringWriter();
		Bar bar = new Bar();
		try {
			mapper.writeValue(sw, bar);
			System.out.println(sw.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			Bar result = mapper.readValue(sw.toString(), Bar.class);
			System.out.println(result.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
