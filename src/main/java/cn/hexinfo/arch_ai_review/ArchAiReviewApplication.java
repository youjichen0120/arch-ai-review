package cn.hexinfo.arch_ai_review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ArchAiReviewApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArchAiReviewApplication.class, args);
	}

}
