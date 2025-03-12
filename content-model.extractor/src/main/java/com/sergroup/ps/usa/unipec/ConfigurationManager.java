package com.sergroup.ps.usa.unipec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigurationManager
{
	private static final Logger LOGGER = Logger.getLogger(ConfigurationManager.class.getName());
	private static Properties properties;
	private static ConfigurationManager instance;

	private ConfigurationManager()
	{
		loadConfiguration();
	}

	public static ConfigurationManager getInstance()
	{
		if (instance == null)
		{
			instance = new ConfigurationManager();
		}
		return instance;
	}

	private void loadConfiguration()
	{
		properties = new Properties();

		// Get config directory from system property
		String configDir = System.getProperty("config.dir", "config");

		// Build configuration file locations array with configDir
		String[] configLocations =
		{ configDir + "/config.properties", // from Java option
				"config/config.properties", // default config directory
				System.getProperty("user.home") + "/app-config/config.properties", // user home
				System.getProperty("user.dir") + "/config.properties", // current directory
				"config.properties" // classpath root
		};

		boolean loaded = false;

		for (String location : configLocations)
		{
			Path configPath = Paths.get(location);
			if (Files.exists(configPath))
			{
				try (InputStream input = Files.newInputStream(configPath))
				{
					properties.load(input);
					LOGGER.info("Loaded configuration from: " + configPath.toAbsolutePath());
					loaded = true;
					break;
				} catch (IOException e)
				{
					LOGGER.warning("Could not load configuration from " + location + ": " + e.getMessage());
				}
			}
		}

		if (!loaded)
		{
			String error = "Could not find config.properties in any of the expected locations. " + "Searched in: " + String.join(", ", configLocations);
			LOGGER.severe(error);
			throw new RuntimeException(error);
		}
	}

	public String getProperty(String key)
	{
		return properties.getProperty(key);
	}

	public String getProperty(String key, String defaultValue)
	{
		return properties.getProperty(key, defaultValue);
	}

	public void validateRequiredProperties(String... requiredProps)
	{
		List<String> missingProps = new ArrayList<>();

		for (String prop : requiredProps)
		{
			if (getProperty(prop) == null)
			{
				missingProps.add(prop);
			}
		}

		if (!missingProps.isEmpty())
		{
			String error = "Missing required properties: " + String.join(", ", missingProps);
			LOGGER.severe(error);
			throw new IllegalStateException(error);
		}
	}
}