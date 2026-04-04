package com.pharmarag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.pharmarag.service.ChromaService;
import com.pharmarag.service.OpenFdaIngestionService;

@SpringBootTest
@ActiveProfiles("test")
class PharmaRagApplicationTests {

    // Mock out services that try to connect to external systems on startup
    @MockBean
    ChromaService chromaService;

    @MockBean
    OpenFdaIngestionService openFdaIngestionService;

    @Test
    void contextLoads() {
        // Verifies the Spring context initializes correctly with all beans wired
    }
}
