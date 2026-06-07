package com.notification.gateway;

import com.notification.gateway.model.NotificationType;
import com.notification.gateway.model.Template;
import com.notification.gateway.repository.TemplateRepository;
import com.notification.gateway.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.thymeleaf.TemplateEngine;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TemplateServiceTest {

    private TemplateService templateService;

    @Mock
    private TemplateRepository templateRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        TemplateEngine engine = TemplateService.createThymeleafTemplateEngine();
        templateService = new TemplateService(templateRepository, engine);
    }

    @Test
    public void testRenderTemplate_WithVariables() {
        String templateContent = "Hello {{name}}, your link is {{link}}.";
        Map<String, Object> variables = Map.of(
                "name", "Alice",
                "link", "http://verify.com"
        );

        String result = templateService.render(templateContent, variables);

        assertEquals("Hello Alice, your link is http://verify.com.", result);
    }

    @Test
    public void testRenderTemplate_NoVariables() {
        String templateContent = "Hello Guest!";
        String result = templateService.render(templateContent, Map.of());

        assertEquals("Hello Guest!", result);
    }

    @Test
    public void testGetTemplateByName() {
        String name = "welcome-email";
        Template template = Template.builder()
                .id(1L)
                .name(name)
                .content("Hello {{name}}")
                .channel(NotificationType.EMAIL)
                .build();

        when(templateRepository.findByName(name)).thenReturn(Optional.of(template));

        Optional<Template> result = templateService.getTemplateByName(name);

        assertEquals(true, result.isPresent());
        assertEquals("Hello {{name}}", result.get().getContent());
    }
}
