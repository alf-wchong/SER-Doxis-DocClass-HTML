package com.sergroup.ps.usa.unipec;

import java.net.http.HttpRequest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpRequestLogger
{
	private static final Logger LOGGER = LoggingManager.getInstance().getLogger(HttpRequestLogger.class);

	public static void logRequest(HttpRequest request, String body)
	{
		// Only build the curl command if DEBUG level is enabled
		if (LOGGER.isLoggable(Level.FINER))
		{
			StringBuilder curlCommand = new StringBuilder("curl -X '");
			curlCommand.append(request.method()).append("' ");
			curlCommand.append("'").append(request.uri().toString()).append("' ");

			// Add headers
			request.headers().map().forEach((header, values) -> {
				values.forEach(value -> curlCommand.append("-H '").append(header).append(": ").append(value).append("' "));
			});

			// Add body if present
			if (body != null && !body.isEmpty())
			{
				curlCommand.append("-d '").append(body).append("' ");
			}

			LOGGER.finer("Equivalent cURL command: " + curlCommand.toString().trim());
		}
	}
}