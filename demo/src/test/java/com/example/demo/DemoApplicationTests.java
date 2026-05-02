package com.example.demo;

import com.example.demo.service.BanPickRoomTimeoutScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
	}

	@Test
	void schedulingIsDisabledDuringTests() {
		assertThat(environment.getProperty("spring.task.scheduling.enabled", Boolean.class)).isFalse();
		assertThat(applicationContext.getBeanNamesForType(ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
		assertThat(applicationContext.getBeanNamesForType(BanPickRoomTimeoutScheduler.class)).isEmpty();
	}

}
