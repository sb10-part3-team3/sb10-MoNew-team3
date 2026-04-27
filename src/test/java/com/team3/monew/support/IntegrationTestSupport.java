package com.team3.monew.support;

import com.team3.monew.config.TestcontainersConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfig.class)
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
public abstract class IntegrationTestSupport {

}
