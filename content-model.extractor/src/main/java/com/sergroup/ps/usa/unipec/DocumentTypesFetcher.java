package com.sergroup.ps.usa.unipec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

public class DocumentTypesFetcher
{
	private static final Logger LOGGER = LoggingManager.getInstance().getLogger(DocumentTypesFetcher.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private static String baseUrl;
	private static String customerName;
	private static String userName;
	private static String password;
	private static String outputJsonPath;
	private static HttpClient httpClient;

	static
	{
		initializeConfiguration();
		httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	}

	private static void initializeConfiguration() 
	{
	    ConfigurationManager config = ConfigurationManager.getInstance();
	    
	    // Validate required properties for DocumentTypesFetcher
	    config.validateRequiredProperties("api.baseUrl","api.customerName","api.userName","api.password","input.json.path");

	    // Load configuration values
	    baseUrl = config.getProperty("api.baseUrl");
	    customerName = config.getProperty("api.customerName");
	    userName = config.getProperty("api.userName");
	    password = config.getProperty("api.password");
	    outputJsonPath = config.getProperty("input.json.path");

	    // Log configuration (mask password)
	    LOGGER.info("Configuration loaded:");
	    LOGGER.info("Base URL: " + baseUrl);
	    LOGGER.info("Customer Name: " + customerName);
	    LOGGER.info("User Name: " + userName);
	    LOGGER.info("Output JSON Path: " + outputJsonPath);
	}

	public static void main(String[] args)
	{
		try
		{
			String jwt = login();
			
			AttributeDefinitionsCache.initialize(jwt, baseUrl); // Initialize attribute definitions cache before processing documents
			fetchAndSaveDocumentTypes(jwt); // Pass the JWT token fetchAndSaveDocumentTypes
			JsonToHtmlTableConverter.process(); 

		} catch (Exception e)
		{
			LOGGER.severe("Error in main process: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static String login() throws IOException, InterruptedException 
	{
	    LOGGER.info("Attempting to login...");
	    
	    String loginUrl = baseUrl + "login";
	    String loginJson = mapper.writeValueAsString(Map.of("customerName", customerName,"userName", userName,"password", password));

	    LOGGER.info("Login URL: " + loginUrl);
	    LOGGER.fine("Login request body: " + loginJson);

	    HttpRequest loginRequest = HttpRequest.newBuilder()
	            .uri(URI.create(loginUrl))
	            .header("Content-Type", "application/json")
	            .POST(HttpRequest.BodyPublishers.ofString(loginJson))
	            .build();

	    HttpRequestLogger.logRequest(loginRequest, loginJson);

	    HttpResponse<String> response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

	    LOGGER.info("Response status code: " + response.statusCode());
	    LOGGER.finer("Response headers: " + response.headers().map());
	    LOGGER.finer("Response body: " + response.body());

	    if (response.statusCode() != 200) 
	    {
	        String errorMsg = "Login failed with status code: " + response.statusCode() + 
	                         ", Response body: " + response.body();
	        LOGGER.severe(errorMsg);
	        throw new IOException(errorMsg);
	    }

	    try 
	    {
	        // The response is the JWT token string directly, wrapped in quotes
	        String jwt = mapper.readValue(response.body(), String.class);
	        LOGGER.info("Login successful, JWT token received");
	        return jwt;

	    } catch (Exception e) {
	        LOGGER.severe("Error parsing login response: " + e.getMessage());
	        throw new IOException("Failed to parse login response", e);
	    }
	}

	private static void fetchAndSaveDocumentTypes(String jwt) throws IOException, InterruptedException
	{
		LOGGER.info("Fetching document types...");

		String documentTypesUrl = baseUrl + "documentTypes";
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(documentTypesUrl)).header("Authorization", "Bearer " + jwt).header("Accept", "application/json").GET()
				.build();

		// Log the curl equivalent
		HttpRequestLogger.logRequest(request, null);

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200)
		{
			String errorMsg = "Failed to fetch document types. Status code: " + response.statusCode();
			LOGGER.severe(errorMsg);
			throw new IOException(errorMsg);
		}

		// Pretty print the JSON before saving
		JsonNode documentTypes = mapper.readTree(response.body());
		String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(documentTypes);

		// Create directories if they don't exist
		Files.createDirectories(Paths.get(outputJsonPath).getParent());

		// Save the JSON file
		Files.writeString(Paths.get(outputJsonPath), prettyJson);
		LOGGER.info("Successfully saved document types to " + outputJsonPath);
	}
}