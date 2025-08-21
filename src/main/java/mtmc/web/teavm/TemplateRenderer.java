package mtmc.web.teavm;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import mtmc.emulator.MonTanaMiniComputer;
import mtmc.web.MTMCWebView;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Template renderer to generate full index.html for TeaVM build
 */
public class TemplateRenderer {
    
    public static void main(String[] args) {
        try {
            String outputDir = args.length > 0 ? args[0] : "dist";
            renderTemplates(outputDir);
        } catch (Exception e) {
            System.err.println("Failed to render templates: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void renderTemplates(String outputDir) throws IOException {
        // Create a mock computer with default state for template rendering
        MonTanaMiniComputer mockComputer = new MonTanaMiniComputer();
        MTMCWebView computerView = new MTMCWebView(mockComputer);
        
        // Create Pebble engine (same config as WebServer)
        PebbleEngine engine = new PebbleEngine.Builder().cacheActive(true).build();
        
        // Render the main template with tea environment
        String html = render(engine, "templates/index.html", computerView, "tea");
        
        // Ensure output directory exists
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        // Write the rendered HTML
        Path indexPath = outputPath.resolve("index.html");
        Files.write(indexPath, html.getBytes(StandardCharsets.UTF_8));
        
    }
    
    private static String render(PebbleEngine engine, String templateName, MTMCWebView computerView, String environment) {
        PebbleTemplate template = engine.getTemplate(templateName);
        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, Map.of("computer", computerView, "environment", environment));
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
