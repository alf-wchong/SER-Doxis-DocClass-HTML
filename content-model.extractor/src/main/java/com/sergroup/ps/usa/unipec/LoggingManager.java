package com.sergroup.ps.usa.unipec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggingManager
{
	private static final String LOG_DIRECTORY = "logs";
	private static LoggingManager instance;
	private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();

	private LoggingManager()
	{
		createLogDirectory();
	}

	public static LoggingManager getInstance()
	{
		if (instance == null)
		{
			synchronized (LoggingManager.class)
			{
				if (instance == null)
				{
					instance = new LoggingManager();
				}
			}
		}
		return instance;
	}

	private void createLogDirectory()
	{
		try
		{
			Files.createDirectories(Paths.get(LOG_DIRECTORY));
		} catch (IOException e)
		{
			System.err.println("Failed to create log directory: " + e.getMessage());
		}
	}

	public Logger getLogger(Class<?> clazz)
	{
		return loggers.computeIfAbsent(clazz.getName(), className -> {
			Logger logger = Logger.getLogger(className);
			setupLogger(logger, className);
			return logger;
		});
	}

	private void setupLogger(Logger logger, String className)
	{
		try
		{
			// Get log level from configuration
			String configuredLevel = ConfigurationManager.getInstance().getProperty("logging.level", "INFO");
			Level logLevel = Level.parse(configuredLevel.toUpperCase());

			// Create a file handler with daily rolling pattern
			String logFile = String.format("%s/%s_%s.log", LOG_DIRECTORY, className.substring(className.lastIndexOf('.') + 1).toLowerCase(),
					new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

			FileHandler fileHandler = new FileHandler(logFile, true);
			fileHandler.setFormatter(new SimpleFormatter()
			{
				private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

				@Override
				public String format(LogRecord record)
				{
					return String.format(format, new Date(record.getMillis()), record.getLevel().getLocalizedName(), record.getMessage());
				}
			});

			// Remove existing handlers to avoid duplicates
			for (Handler handler : logger.getHandlers())
			{
				logger.removeHandler(handler);
			}

			logger.addHandler(fileHandler);
			logger.setLevel(logLevel);

		} catch (IOException e)
		{
			System.err.println("Failed to setup logger for " + className + ": " + e.getMessage());
		}
	}
}