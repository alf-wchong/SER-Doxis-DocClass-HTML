# Doxis Document Class Printer

A Java application that prints document classes and their attributes in a Doxis Organization, generating HTML tables for easy viewing and analysis.

## Features

- Retrieves document classes and attribute definitions via REST API
- Generates individual HTML tables for each document class
- Creates an index page with search functionality
- Shows attribute counts for each document class
- Allows toggling visibility of unknown attributes
- Detailed attribute information including:
  - Attribute Name
  - Short Name
  - Data Type
  - Default Value
  - Length
  - Multivalue Type
  - Fulltext Usage
  - Mandatory status
  - Readonly status

## Prerequisites

- Java 17 or higher
- Eclipse IDE
- Maven (if using dependencies management)
- Access to the REST API endpoints
- Git

## Building in Eclipse
1. Clone the repository:
```bash
git clone https://github.com/alf-wchong/SER-Doxis-DocClass-HTML.git
```
2. Import the project into Eclipse:
- File → Import → Git → Projects from Git (with smart import)
- Select "Existing local repository"
- Browse to the cloned repository location
- Follow the wizard to complete the import
3. Build the project:
- Project → Clean...
- Project → Build Project
4. Create a runnable JAR file:
- Right-click on the project
- Export → Java → Runnable JAR file
- Select the main class as DocumentTypesFetcher
- Choose destination for the JAR file

## Deployment
1. Create the following directory structure on the target host:
```bash
/YourDirectory
├── thisCode.jar
├── config.properties
├── data/
└── logs/
```
2. Copy files:
- Copy the JAR file to `YourDirectory`
- Copy [`config.properties`](config.properties) to `YourDirectory`
3. Configure the application:
- Edit [config.properties](config.properties).
- This project utilizes logging to track HTTP requests. To enable detailed logging of these requests, you need to set `logging.level` to `FINER` in the [config.properties](config.properties) file

## Running the Application
1. Navigate to the application directory `YourDirectory`
2. Run the application
```bash
java -Dconfig.dir=. -jar thisCode.jar
```

## Ouput Locations
- Generated HTML files:
  - generated_tables/index.html - Main index page
  - generated_tables/table_*.html - Individual document class tables
- Data files:
  - data/document_types.json - Raw document types data
  - data/attribute_definitions.json - Raw attribute definitions data
- Log files:
  - logs/documenttypesfetcher_YYYY-MM-DD.log - Main application logs
  - logs/jsontohtmltableconverter_YYYY-MM-DD.log - Table generation logs
  - logs/httprequestlogger_YYYY-MM-DD.log - API call logs (when logging.level=FINER)
 
## Viewing Results
1. Open generated_tables/index.html in a web browser
2. Use the search box to filter document classes
3. Click on any document class to view its detailed attribute table
4. Use the "Show Unknown Attributes" checkbox in each table to toggle visibility of unknown attributes

## Troubleshooting
- Check the log files in the logs directory for errors
- For API call details, set logging.level=FINE in config.properties
- Ensure proper network connectivity to the API endpoints
- Verify credentials in config.properties

## Notes
- The application caches attribute definitions to improve performance
- Unknown attributes are marked in red in the generated tables

## Contributing
1. Fork the repository
2. Create your feature branch (git checkout -b feature/AmazingFeature)
3. Commit your changes (git commit -m 'Add some AmazingFeature')
4. Push to the branch (git push origin feature/AmazingFeature)
5. Open a Pull Request
