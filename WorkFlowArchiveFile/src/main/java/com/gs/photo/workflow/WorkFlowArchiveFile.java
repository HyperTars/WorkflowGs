package com.gs.photo.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@PropertySource("file:${user.home}/config/application.properties")
@EnableAsync
public class WorkFlowArchiveFile {

	public static void main(String[] args) {
		SpringApplication.run(
			WorkFlowArchiveFile.class,
			args);
	}
}
