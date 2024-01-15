package com.example.UploadFilePractice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootApplication
@Slf4j
public class UploadFilePracticeApplication implements CommandLineRunner {

	@Autowired
	private StringRedisTemplate template;

	public static void main(String[] args) {
		SpringApplication.run(UploadFilePracticeApplication.class, args);
	}

	@Override
	public void run(String... args) {
		ValueOperations<String, String> ops = this.template.opsForValue();
		String key = "testkey";
		if(!this.template.hasKey(key)){
			ops.set(key, "Hello World");
			log.info("Add a key is done");
		}
		log.info("Return the value from the cache: {}", ops.get(key));
	}

}
