package com.chatroomrealtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching 
public class ChatRoomRealTimeApplication {
	public static void main(String[] args) {
		SpringApplication.run(ChatRoomRealTimeApplication.class, args);
	}

}
