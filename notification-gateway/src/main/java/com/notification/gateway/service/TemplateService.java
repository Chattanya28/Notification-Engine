package com.notification.gateway.service;

import com.notification.gateway.model.Template;
import com.notification.gateway.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateEngine templateEngine;

    public TemplateService(
            TemplateRepository templateRepository,
            @org.springframework.beans.factory.annotation.Qualifier("stringTemplateEngine") TemplateEngine templateEngine) {
        this.templateRepository = templateRepository;
        this.templateEngine = templateEngine;
    }

    // Static helper to build and configure a Thymeleaf template engine for raw strings
    public static TemplateEngine createThymeleafTemplateEngine() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        // Use SpringTemplateEngine so that SpEL is used instead of OGNL
        org.thymeleaf.spring6.SpringTemplateEngine engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    /**
     * Replaces variable tokens like {{name}} or {{link}} in the content.
     * We convert double curly braces to standard Thymeleaf expressions (e.g. [(${name})])
     * so that Thymeleaf engine can parse them seamlessly.
     */
    public String render(String templateContent, Map<String, Object> variables) {
        if (templateContent == null || templateContent.isEmpty()) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return templateContent;
        }

        try {
            // Convert {{variable}} to [(${variable})] for Thymeleaf text mode compatibility
            String processedTemplate = templateContent;
            for (String key : variables.keySet()) {
                // Safely replace {{key}} with Thymeleaf standard text expression
                processedTemplate = processedTemplate.replace("{{" + key + "}}", "[(${" + key + "})]");
            }

            Context context = new Context();
            context.setVariables(variables);
            return templateEngine.process(processedTemplate, context);
        } catch (Exception e) {
            log.error("Failed to render template content", e);
            throw new IllegalArgumentException("Template rendering failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Template saveTemplate(Template template) {
        // Ensure name is unique
        templateRepository.findByName(template.getName()).ifPresent(t -> {
            throw new IllegalArgumentException("Template with name '" + template.getName() + "' already exists");
        });
        log.info("Saving new template '{}'", template.getName());
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public Optional<Template> getTemplateByName(String name) {
        return templateRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Template> getAllTemplates() {
        return templateRepository.findAll();
    }

    @Transactional
    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
        log.info("Deleted template with ID {}", id);
    }
}
