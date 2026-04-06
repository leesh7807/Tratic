package app.leesh.tratic.docs;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import app.leesh.tratic.analyze.controller.AnalyzeController;
import app.leesh.tratic.analyze.service.AnalyzeService;

@Tag("openapi")
@SpringBootTest(
        classes = OpenApiSpecGeneratorTest.TestApplication.class,
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=false"
        })
@AutoConfigureMockMvc
public class OpenApiSpecGeneratorTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void writes_openapi_json_to_configured_output_path() throws Exception {
        String outputPath = System.getProperty("openapi.output-file");
        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalStateException("openapi.output-file system property must be set");
        }

        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        writeOutput(Path.of(outputPath), content);
    }

    private void writeOutput(Path outputFile, String content) throws IOException {
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, content);
    }

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import(AnalyzeController.class)
    static class TestApplication {
        @Bean
        AnalyzeService analyzeService() {
            return mock(AnalyzeService.class);
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .oauth2Login(oauth -> oauth.disable())
                    .logout(Customizer.withDefaults());
            return http.build();
        }
    }
}
