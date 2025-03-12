package com.sergroup.ps.usa.unipec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

public class JsonToHtmlTableConverter
{
	private static final Logger LOGGER = LoggingManager.getInstance().getLogger(JsonToHtmlTableConverter.class);
	private static String baseUrl;
	private static String outputDirectory;
	private static final Map<String, String> attributeCache = new ConcurrentHashMap<>();
	private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
	private static final ObjectMapper mapper = new ObjectMapper();
	private static String jsonInputPath;
	private static String jwt;

	static
	{
		initializeConfiguration();
	}

	public static void process()
	{
	    try
	    {
	        LOGGER.info("Starting HTML table conversion process");
	        String jsonInput = readJsonInput();
	        LOGGER.info("JSON input file read successfully");

	        List<TableContent> tables = processJson(jsonInput);  // Changed from List<String>
	        writeOutputFiles(tables);

	        LOGGER.info("Process completed successfully");
	    } catch (Exception e)
	    {
	        LOGGER.severe("Error processing JSON: " + e);
	        e.printStackTrace();
	    }
	}

	private static void initializeConfiguration()
	{
		ConfigurationManager config = ConfigurationManager.getInstance();

		// Validate required properties for JsonToHtmlTableConverter
		config.validateRequiredProperties("api.baseUrl", "input.json.path", "output.directory");

		// Load configuration values
		baseUrl = config.getProperty("api.baseUrl");
		outputDirectory = config.getProperty("output.directory", "output");
		jsonInputPath = config.getProperty("input.json.path");

		// Create output directory if it doesn't exist
		try
		{
			Files.createDirectories(Paths.get(outputDirectory));
		} catch (IOException e)
		{
			LOGGER.severe("Error creating output directory: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private static String readJsonInput() throws IOException
	{
		try
		{
			String content = Files.readString(Paths.get(jsonInputPath));
			LOGGER.fine("Read " + content.length() + " characters from " + jsonInputPath);
			return content;
		} catch (IOException e)
		{
			LOGGER.severe("Error reading JSON file from " + jsonInputPath + ": " + e.getMessage());
			throw e;
		}
	}

	private static List<TableContent> processJson(String jsonInput) throws IOException 
	{
	    LOGGER.info("Starting to process JSON input");
	    
	    JsonNode rootNode = mapper.readTree(jsonInput);
	    List<TableContent> tables = new ArrayList<>();
	    AtomicInteger tableCounter = new AtomicInteger(0);

	    for (JsonNode objectNode : rootNode) 
	    {
	        try 
	        {
	            String documentName = objectNode.get("name").asText();
	            String tableHtml = generateTable(objectNode, tableCounter.incrementAndGet());
	            tables.add(new TableContent(documentName, tableHtml));
	        } catch (Exception e) 
	        {
	            LOGGER.severe("Error generating table: " + e.getMessage());
	        }
	    }

	    LOGGER.info("Generated " + tables.size() + " tables");
	    return tables;
	}

	private static String generateTable(JsonNode objectNode, int tableNumber) throws IOException 
	{
	    LOGGER.fine("Generating table " + tableNumber);
	    
	    String name = objectNode.get("name").asText();
	    LOGGER.fine("Processing table for: " + name);

	    JsonNode attributeDefinitions = objectNode.get("allowedAttributeDefinitions");
	    if (attributeDefinitions == null || !attributeDefinitions.isArray()) {
	        LOGGER.severe("Invalid or missing allowedAttributeDefinitions for " + name);
	        throw new IllegalArgumentException("Invalid allowedAttributeDefinitions");
	    }

	    StringBuilder htmlTable = new StringBuilder();
	    htmlTable.append("<!DOCTYPE html>\n<html>\n<head>\n")
	            .append("<style>\n")
	            .append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n")
	            .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
	            .append("th { background-color: #f2f2f2; position: sticky; top: 0; }\n")
	            .append("tr:nth-child(even) { background-color: #f9f9f9; }\n")
	            .append("tr:hover { background-color: #f5f5f5; }\n")
	            .append(".null-value { color: #999; font-style: italic; }\n")
	            .append(".error-value { color: #ff4444; font-style: italic; }\n")
	            .append(".unknown-row { display: table-row; }\n")
	            .append(".controls { margin-bottom: 20px; }\n")
	            .append(".controls label { display: inline-flex; align-items: center; }\n")
	            .append(".controls input[type='checkbox'] { margin-right: 8px; }\n")
	            .append("</style>\n")
	            .append("<script>\n")
	            .append("function toggleUnknownAttributes() {\n")
	            .append("    const show = document.getElementById('showUnknown').checked;\n")
	            .append("    const rows = document.getElementsByClassName('unknown-row');\n")
	            .append("    for (let row of rows) {\n")
	            .append("        row.style.display = show ? 'table-row' : 'none';\n")
	            .append("    }\n")
	            .append("}\n")
	            .append("</script>\n")
	            .append("</head>\n<body>\n")
	            .append("<h2>").append(name).append("</h2>\n")
	            .append("<div class='controls'>\n")
	            .append("<label><input type='checkbox' id='showUnknown' checked onclick='toggleUnknownAttributes()'> ")
	            .append("Show Unknown Attributes</label>\n")
	            .append("</div>\n")
	            .append("<table>\n")
	            .append("<tr>")
	            .append("<th>Attribute Name</th>")
	            .append("<th>Short Name</th>")
	            .append("<th>Data Type</th>")
	            .append("<th>Default Value</th>")
	            .append("<th>Length</th>")
	            .append("<th>Multivalue Type</th>")
	            .append("<th>Fulltext Usage</th>")
	            .append("<th>Mandatory</th>")
	            .append("<th>Readonly</th>")
	            .append("</tr>\n");

	    for (JsonNode attrDef : attributeDefinitions) {
	        String uuid = attrDef.get("attributeDefinitionUUID").asText();
	        boolean mandatory = attrDef.get("mandatory").asBoolean();
	        boolean readonly = attrDef.get("readonly").asBoolean();
	        
	        // Get the full attribute definition from cache
	        JsonNode fullAttrDef = AttributeDefinitionsCache.getAttributeDefinition(uuid);
	        
	        if (fullAttrDef == null) {
	            LOGGER.warning("Attribute definition not found for UUID: " + uuid);
	            htmlTable.append("<tr class='unknown-row'>")
	                    .append("<td><span class='error-value'>Unknown Attribute (").append(uuid).append(")</span></td>")
	                    .append("<td><span class='error-value'>N/A</span></td>")
	                    .append("<td><span class='error-value'>N/A</span></td>")
	                    .append("<td><span class='error-value'>N/A</span></td>")
	                    .append("<td><span class='error-value'>N/A</span></td>")
	                    .append("<td><span class='error-value'>N/A</span></td>")
	                    .append("<td><span class='error-value'>N/A</span></td>")
	                    .append("<td>").append(mandatory).append("</td>")
	                    .append("<td>").append(readonly).append("</td>")
	                    .append("</tr>\n");
	            continue;
	        }

	        htmlTable.append("<tr>")
	                .append("<td>").append(fullAttrDef.get("name").asText()).append("</td>")
	                .append("<td>").append(formatValue(fullAttrDef.get("shortName"))).append("</td>")
	                .append("<td>").append(formatValue(fullAttrDef.get("attributeDataType"))).append("</td>")
	                .append("<td>").append(formatValue(fullAttrDef.get("defaultValue"))).append("</td>")
	                .append("<td>").append(fullAttrDef.get("length").asInt()).append("</td>")
	                .append("<td>").append(formatValue(fullAttrDef.get("multivalueType"))).append("</td>")
	                .append("<td>").append(formatValue(fullAttrDef.get("fulltextUsage"))).append("</td>")
	                .append("<td>").append(mandatory).append("</td>")
	                .append("<td>").append(readonly).append("</td>")
	                .append("</tr>\n");
	    }

	    htmlTable.append("</table>\n</body>\n</html>");
	    LOGGER.info("Completed generating table " + tableNumber + " for: " + name);
	    return htmlTable.toString();
	}
	
	private static String formatValue(JsonNode node) 
	{
	    if (node == null || node.isNull()) {
	        return "<span class='null-value'>null</span>";
	    }
	    return node.asText();
	}

	private static void writeOutputFiles(List<TableContent> tables) 
	{
	    try 
	    {
	        StringBuilder index = new StringBuilder();
	        index.append("<!DOCTYPE html>\n<html>\n<head>\n")
	             .append("<title>Document Classes</title>\n")
	             .append("<style>\n")
	             .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n")
	             .append(".container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n")
	             .append("h1 { color: #333; margin-bottom: 20px; }\n")
	             .append("#searchBox { width: 100%; padding: 12px; margin-bottom: 20px; border: 1px solid #ddd; border-radius: 4px; font-size: 16px; box-sizing: border-box; }\n")
	             .append("ul { list-style-type: none; padding: 0; }\n")
	             .append("li { margin: 8px 0; padding: 8px; border-radius: 4px; transition: background-color 0.2s; }\n")
	             .append("li:hover { background-color: #f0f0f0; }\n")
	             .append("a { text-decoration: none; color: #0066cc; display: inline-block; }\n")
	             .append("a:hover { color: #004499; }\n")
	             .append(".attribute-count { color: #666; margin-left: 10px; font-size: 0.9em; }\n")
	             .append(".no-attributes { color: #ff4444; }\n")
	             .append(".no-results { display: none; color: #666; font-style: italic; padding: 10px; }\n")
	             .append("</style>\n")
	             .append("<script>\n")
	             .append("function searchDocuments() {\n")
	             .append("    const input = document.getElementById('searchBox').value.toLowerCase();\n")
	             .append("    const items = document.getElementsByTagName('li');\n")
	             .append("    const noResults = document.getElementById('noResults');\n")
	             .append("    let hasResults = false;\n")
	             .append("    for (let item of items) {\n")
	             .append("        const text = item.textContent.toLowerCase();\n")
	             .append("        if (text.includes(input)) {\n")
	             .append("            item.style.display = '';\n")
	             .append("            hasResults = true;\n")
	             .append("        } else {\n")
	             .append("            item.style.display = 'none';\n")
	             .append("        }\n")
	             .append("    }\n")
	             .append("    noResults.style.display = hasResults ? 'none' : 'block';\n")
	             .append("}\n")
	             .append("</script>\n")
	             .append("</head>\n<body>\n")
	             .append("<div class='container'>\n")
	             .append("<h1>Document Classes</h1>\n")
	             .append("<input type='text' id='searchBox' placeholder='Search document classes...' onkeyup='searchDocuments()'>\n")
	             .append("<ul>\n");

	        for (int i = 0; i < tables.size(); i++) {
	            TableContent table = tables.get(i);
	            String fileName = "table_" + (i + 1) + ".html";
	            int attributeCount = countAttributes(table.getHtmlContent());

	            // Add link to index with document name and attribute count
	            index.append("<li>")
	                 .append(String.format("<a href='%s'>%s</a>", fileName, table.getDocumentName()));
	            
	            if (attributeCount == 0) {
	                index.append("<span class='attribute-count no-attributes'>(no attributes)</span>");
	            } else {
	                index.append(String.format("<span class='attribute-count'>(%d attribute%s)</span>", 
	                    attributeCount, attributeCount == 1 ? "" : "s"));
	            }
	            
	            index.append("</li>\n");

	            // Write individual table file
	            Path filePath = Paths.get(outputDirectory, fileName);
	            Files.writeString(filePath, table.getHtmlContent());
	        }

	        index.append("</ul>\n")
	             .append("<div id='noResults' class='no-results'>No matching document classes found</div>\n")
	             .append("</div>\n")
	             .append("</body>\n</html>");

	        Files.writeString(Paths.get(outputDirectory, "index.html"), index.toString());

	        LOGGER.info("Successfully generated all files in " + outputDirectory);

	    } catch (Exception e) 
	    {
	        LOGGER.severe("Error writing output files: " + e.getMessage());
	    }
	}

	// Helper method to count attributes in a table
	private static int countAttributes(String htmlContent) 
	{
	    // Count the number of <tr> tags minus 1 (for header row)
	    int rowCount = htmlContent.split("<tr>").length - 2; // -2 for header row and split artifact
	    return Math.max(0, rowCount); // Ensure we don't return negative numbers
	}

	

}