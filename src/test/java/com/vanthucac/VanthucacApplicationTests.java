package com.vanthucac;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"spring.data.redis.host=localhost",
		"spring.data.redis.port=6379",
		"spring.mail.host=smtp.gmail.com",
		"spring.mail.username=test@test.com",
		"spring.mail.password=test",
		"app.jwt.secret=test-secret-key-for-testing-only-minimum-256-bits-long",
		"app.commission.rate=0.05",
		"app.google-books.api-key=test",
		"app.aws.access-key=test",
		"app.aws.secret-key=test",
		"app.aws.region=ap-southeast-1",
		"app.aws.s3.bucket=test-bucket"
})
class VanthucacApplicationTests {

	@Test
	void contextLoads() {
	}
}