package com.sergroup.ps.usa.unipec;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AttributeDefinitionsCache
{
	private static final Logger LOGGER = LoggingManager.getInstance().getLogger(AttributeDefinitionsCache.class);
	private static final Map<String, JsonNode> attributeDefinitions = new HashMap<>();
	private static final ObjectMapper mapper = new ObjectMapper();
	private static String attributeDefinitionsPath;

	public static void initialize(String jwt, String baseUrl) throws IOException, InterruptedException
	{
		LOGGER.info("Initializing attribute definitions cache");

		// Get path from configuration
		attributeDefinitionsPath = ConfigurationManager.getInstance().getProperty("attribute.definitions.path");

		// Create directories if they don't exist
		Files.createDirectories(Paths.get(attributeDefinitionsPath).getParent());

		// Fetch from API and save to file
		fetchAndSaveAttributeDefinitions(jwt, baseUrl);

		// Load from file into memory
		loadAttributeDefinitionsFromFile();
	}

	private static void fetchAndSaveAttributeDefinitions(String jwt, String baseUrl) throws IOException, InterruptedException
	{
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

		String attributesUrl = baseUrl + "attributeDefinitions";
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(attributesUrl)).header("Authorization", "Bearer " + jwt).header("accept", "application/json").GET().build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 200)
		{
			// Save to configured path
			Files.writeString(Paths.get(attributeDefinitionsPath), response.body());
			LOGGER.info("Saved attribute definitions to: " + attributeDefinitionsPath);
		} else
		{
			throw new IOException("Failed to fetch attribute definitions. Status: " + response.statusCode());
		}
	}

	private static void loadAttributeDefinitionsFromFile() throws IOException
	{
		LOGGER.info("Loading attribute definitions from file: " + attributeDefinitionsPath);
		String jsonContent = Files.readString(Paths.get(attributeDefinitionsPath));
		JsonNode attributesArray = mapper.readTree(jsonContent);

		attributeDefinitions.clear();
		for (JsonNode attr : attributesArray)
		{
			attributeDefinitions.put(attr.get("uuid").asText(), attr);
		}
		LOGGER.info("Cached " + attributeDefinitions.size() + " attribute definitions from file");
	}

	public static JsonNode getAttributeDefinition(String uuid)
	{
		return attributeDefinitions.getOrDefault(uuid, null);
	}

	public static String getAttributeName(String uuid)
	{
		JsonNode attr = attributeDefinitions.get(uuid);
		return attr != null ? attr.get("name").asText() : "Unknown (" + uuid + ")";
	}

	// Helper method to check if an attribute exists
	public static boolean hasAttribute(String uuid)
	{
		return attributeDefinitions.containsKey(uuid);
	}

	// Helper method to get total number of cached attributes
	public static int getCacheSize()
	{
		return attributeDefinitions.size();
	}
}