package iterative.harmony.backend

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class BackendTests {
    // The test below checks that the springboot context loads correctly
    @Test fun contextLoads() {}
}
