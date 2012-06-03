package ch.kostceco.tools.siardval.validation.module.impl;
 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import ch.enterag.utils.zip.EntryInputStream;
import ch.enterag.utils.zip.FileEntry;
import ch.enterag.utils.zip.Zip64File;
import ch.kostceco.tools.siardval.exception.module.ValidationEcolumnException;
import ch.kostceco.tools.siardval.service.ConfigurationService;
import ch.kostceco.tools.siardval.validation.ValidationModuleImpl;
import ch.kostceco.tools.siardval.validation.bean.SiardTable;
import ch.kostceco.tools.siardval.validation.module.ValidationEcolumnModule;

public class ValidationEcolumnModuleImpl extends ValidationModuleImpl implements ValidationEcolumnModule {
    /*Validation parameters*/
	private boolean verboseMode;
	
	/*Service related properties*/
	private ConfigurationService configurationService;
 
	/*Validation context related properties*/
	private Properties validationProperties;
 
	/*Content of the SIARD package*/
	private HashMap<String, File> siardFiles;
	private File metadataXML;
	private Document metadataXMLDocument;
	private String contentPath; 
	private String headerPath;
	
	/*SIARD XML processing related properties*/
	private List<Element> xmlElements;
	private List<Element> xsdElements;
	
	private List<String> xmlElementsSequence;
	private List<String> xsdElementsSequence;
	
	private List<SiardTable> siardTables;
 
	/*General XML related properties for JDOM Access*/
	private String namespaceURI;
	private String xmlPrefix;
	private String xsdPrefix;
	
	private Namespace xmlNamespace;
	private Namespace xsdNamespace;
  
	/*Logging information*/
	private StringBuilder validationLog;
	
	
	/* [E] */
	@SuppressWarnings("finally")
	@Override
	public boolean validate(File siardDatei) throws ValidationEcolumnException {
		 //Validation parameters
		 this.setVerboseMode(true);
		 //All over validation flag
		 boolean valid = true;
		 try {
			 //Initialize the validation context
			 if (prepareValidation(siardDatei) == false) {
				 valid = false;
				 getMessageService().logInfo(
	                     getTextResourceService().getText(MESSAGE_MODULE_E) +
	                     getTextResourceService().getText(MESSAGE_DASHES) +
	                     getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_VALIDATION_CONTEXT));
			 } else if (this.isVerboseMode()) {
				 getMessageService().logInfo(this.getValidationLog().toString());
			 }
			 //Get the prepared SIARD tables from the validation context
			 List<SiardTable> siardTables = this.getSiardTables();
			 //Get the Java properties from the validation context
			 Properties properties = this.getValidationProperties();
			 if (properties == null) {
				 valid = false;
				 getMessageService().logInfo(
	                     getTextResourceService().getText(MESSAGE_MODULE_E) +
	                     getTextResourceService().getText(MESSAGE_DASHES) +
	                     getTextResourceService().getText(MESSAGE_MODULE_E_MISSING_PROPERTIES));
			 }
			 if (siardTables == null) {
				 valid = false;
				 getMessageService().logInfo(
	                     getTextResourceService().getText(MESSAGE_MODULE_E) +
	                     getTextResourceService().getText(MESSAGE_DASHES) +
	                     getTextResourceService().getText(MESSAGE_MODULE_E_MISSING_SIARD_TABLES));
			 }
			 //Validates the number of the attributes
			 if (validateAttributeCount(siardTables, properties) == false) {
				 valid = false;
					 getMessageService().logInfo(
	                 getTextResourceService().getText(MESSAGE_MODULE_E) +
	                 getTextResourceService().getText(MESSAGE_DASHES) +
	                 getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_COUNT));
			 } else if (this.isVerboseMode()){
				 getMessageService().logInfo(this.getValidationLog().toString());
			 }
			 //Validates the nullable property in metadata.xml
			 if (validateAttributeOccurrence(siardTables, properties) == false) {
				valid = false;
					 getMessageService().logInfo(
	                 getTextResourceService().getText(MESSAGE_MODULE_E) +
	                 getTextResourceService().getText(MESSAGE_DASHES) +
	                 getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_OCCURRENCE));
			 } else if (this.isVerboseMode()) {
				 getMessageService().logInfo(this.getValidationLog().toString());
			 }
			 
			 //Validates the type of table attributes in metadata.xml
			 if (validateAttributeType(siardTables, properties) == false) {
				valid = false;
					 getMessageService().logInfo(
	                 getTextResourceService().getText(MESSAGE_MODULE_E) +
	                 getTextResourceService().getText(MESSAGE_DASHES) +
	                 getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_TYPE));
			 } else if (this.isVerboseMode()) {
				 	getMessageService().logInfo(this.getValidationLog().toString());
			 }	
			 
	         //Validates the sequence of table attributes in metadata.xml
			 if (validateAttributeSequence(properties) == false) {
				valid = false;
					getMessageService().logInfo(
	                getTextResourceService().getText(MESSAGE_MODULE_E) +
	                getTextResourceService().getText(MESSAGE_DASHES) +
	                getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_SEQUENCE));
			 } else if (this.isVerboseMode()) {
				 getMessageService().logInfo(this.getValidationLog().toString());
			 }			 
		} catch (Exception je) {
			valid = false;
				getMessageService().logError(
				getTextResourceService().getText(MESSAGE_MODULE_E) +
	            getTextResourceService().getText(MESSAGE_DASHES) + je.getMessage());
		} finally {
			//System.out.println(this.getValidationLog().toString());
			getMessageService().logInfo(this.getValidationLog().toString());
			return valid;
		}
	}
	
	/* [E.0] */
	private boolean prepareValidation(File siardFile) throws IOException, JDOMException {
		StringBuilder validationLog = new StringBuilder();
		this.setValidationLog(validationLog);
		//All over preparation flag
		boolean prepared = true;
		//Load the Java properties to the validation context
		boolean propertiesLoaded = initializeProperties();
		//Initialize internal path configuration of the SIARD archive
		boolean pathInitialized = initializePath(this.getValidationProperties());
		//Extract the SIARD archive and distribute the content to the validation context
		boolean siardArchiveExtracted = extractSiardArchive(siardFile, this.getValidationProperties());
		//Pick the metadata.xml and load it to the validation context
		boolean metadataXMLpicked = pickMetadataXML(this.getValidationProperties());
		//Prepare the XML configuration and store it to the validation context
		boolean xmlAccessPrepared = prepareXMLAccess(this.getValidationProperties(), this.getMetadataXML());
		//Prepare the data to be validated such as metadata.xml and the according XML schemas
		boolean validationDataPrepared = prepareValidationData(this.getValidationProperties(), this.getMetadataXML());
		//Verifying whether the preparation steps were successful
		if (propertiesLoaded == true &&
				pathInitialized == true &&
				xmlAccessPrepared == true &&
				siardArchiveExtracted == true &&
				metadataXMLpicked == true && 
				validationDataPrepared == true) {
			prepared = true;
		}
		return prepared;
	}
	
	/* [E.1] */
	private boolean validateAttributeCount(List<SiardTable> siardTables, 
			Properties properties) throws Exception {
		boolean valid = true;
		//Initializing validation Logging
		StringBuilder validationLog = new StringBuilder();
		String methodTitle = properties.
				getProperty("attribute.count.validator");
		String methodDescription = properties.
				getProperty("attribute.count.validator.description");
		validationLog.append(methodTitle);
		validationLog.append(methodDescription);
	    //Iteratic over all SIARD tables to count the table attributes
		//and compare it to the number of registered attributes in the according XML schemas
		for (SiardTable siardTable : siardTables) {
			int metadataXMLColumnsCount = siardTable.getMetadataXMLElements().size();
			int tableXSDColumnsCount = siardTable.getTableXSDElements().size();
			if (metadataXMLColumnsCount == tableXSDColumnsCount) {
				valid = true;
			}
			//Preparing validation log entry
			String validationLogSceleton = properties.
					getProperty("attribute.count.validator.log.entry");
			String validationLogEntry = MessageFormat.
					format(validationLogSceleton, 
						   siardTable.getTableName(), 
						   metadataXMLColumnsCount,
						   tableXSDColumnsCount,
						   valid);
			validationLog.append(validationLogEntry);
			validationLog.append(properties.getProperty("newline"));
		}
		//Write the local validation log to the validation context
		this.setValidationLog(validationLog);
		//Return the current validation state
		return valid;
	}
	
	/* [E.2] */
	private boolean validateAttributeOccurrence(List<SiardTable> siardTables, 
			Properties properties) throws Exception {
		boolean valid = true;
		//Initializing validation Logging
		StringBuilder validationLog = new StringBuilder();
		String methodTitle = properties.
				getProperty("attribute.occurrence.validator");
		String methodDescription = properties.
				getProperty("attribute.occurrence.validator.description");
		validationLog.append(methodTitle);
		validationLog.append(methodDescription);
		//Iterate over the SIARD tables and verify the nullable attribute
		for (SiardTable siardTable : siardTables) {
			//Add table info to the log entry
			String validationLogTableSceleton = properties.
					getProperty("attribute.occurrence.validator.log.table");
			String validationLogTableEntry = MessageFormat.
					format(validationLogTableSceleton, 
						   siardTable.getTableName());
			validationLog.append(validationLogTableEntry);
			//Number of attributes in metadata.xml
			int metadataXMLColumnsCount = siardTable.getMetadataXMLElements().size();
			//Number of attributes in the according XML schemata
			int tableXSDColumnsCount = siardTable.getTableXSDElements().size();
			//Start the validation if the allover number is equal in metadata.xml and XML schemata	 
			if (metadataXMLColumnsCount == tableXSDColumnsCount) {
				//Element/Attributes of the actual SIARD table
				List<Element> xmlElements = siardTable.getMetadataXMLElements();
				//Elements/Attributes of the according XML schema
				List<Element> xsdElements = siardTable.getTableXSDElements();
				Namespace xmlNamespace = this.getXmlNamespace();
				for ( int i = 0; i < metadataXMLColumnsCount; i++) {
					//Actual Element of the metadata.xml									
					Element xmlElement = xmlElements.get(i);
					//Actual Element of the according XML schema
					Element xsdElement = xsdElements.get(i);
					String nullableElementDescription = properties.
							getProperty("siard.metadata.xml.nullable.element.name");
					String minuOccursAttributeDescription = properties.
							getProperty("siard.table.xsd.attribute.minOccurs.name");
					//Value of the nullable Element in metadata.xml
					String nullable = xmlElement.getChild(nullableElementDescription, 
							xmlNamespace).getValue();
					//Value of the minOccurs attribute in the according XML schema
					String minOccurs = xsdElement.
							getAttributeValue(minuOccursAttributeDescription);
					//If the nullable Element is set to true and the minOccurs attribute is null
					if (nullable.equalsIgnoreCase("true") && minOccurs == null) {
						//Validation fails becaus the minOccurs attribute must be set to zero
						valid = false;
					//If the nullable Element is set to true and the minOccurs attribute is set to zero
					} else if (nullable.equalsIgnoreCase("true") && minOccurs.equalsIgnoreCase("0")) {
						//Validation succeded. Statement is leftt empty not to overwrite previous false values
					//If the nullable Element is set to false and the minOccurs attribute is null
					} else if (nullable.equalsIgnoreCase("false") && minOccurs == null) {
						//Validation succeded. Statement is left empty not to overwrite previous false values
					}
					//Add column info to the log entry
					String validationLogColumnSceleton = properties.
							getProperty("attribute.occurrence.validator.log.column");
					String validationLogColumnName = properties.getProperty("attribute.occurrence.column.name");
					String validationLogColumnNullable = properties.getProperty("attribute.occurrence.nullable");
					String validationLogColumnMinOccurs = properties.getProperty("attribute.occurrence.minOccurs");
					Element columnName = xmlElement.getChild(validationLogColumnName, this.getXmlNamespace());
					Element columnNullable = xmlElement.getChild(validationLogColumnNullable, this.getXmlNamespace());
					String columnMinOccurs = xsdElement.getAttributeValue(validationLogColumnMinOccurs);
					String validationLogColumnEntry = MessageFormat.
							format(validationLogColumnSceleton, 
								   columnName.getValue(),
								   columnNullable.getName(),
								   columnNullable.getValue(),
								   validationLogColumnMinOccurs,
								   columnMinOccurs, 
								   valid);
					validationLog.append(validationLogColumnEntry);
					validationLog.append(properties.getProperty("newline"));
				} 
			} else {
				//Validation fails if allover number differs in metadata.xml and XML schemata
				valid = false;
			}
		}
		//Write the local validation log to the validation context
	    this.setValidationLog(validationLog);
	    //Return the current validation state
		return valid;
	}
	/* [E.3] */
	private boolean validateAttributeType(List<SiardTable> siardTables, 
			Properties properties) throws Exception {
		boolean valid = true;
		//Initializing validation Logging
		StringBuilder validationLog = new StringBuilder();
		String methodTitle = properties.
				getProperty("attribute.type.validator");
		String methodDescription = properties.
				getProperty("attribute.type.validator.description");
		validationLog.append(methodTitle);
		validationLog.append(methodDescription);
		//List of all XML column elements to verify the allover sequence
		List<String> xmlElementSequence = new ArrayList<String>();
		//List of all XSD column elements 
		List<String> xsdElementSequence = new ArrayList<String>();
		//Iterate over the SIARD tables and verify the column types
		for (SiardTable siardTable : siardTables) {
			//Elements of the actual SIARD table
			List<Element> xmlElements = siardTable.getMetadataXMLElements();
			//Elements of the according XML schema
			List<Element> xsdElements = siardTable.getTableXSDElements();
			//Update validation log
			String tableNameSceleton = properties.getProperty("attribute.type.validator.log.table");
			String tableName = MessageFormat.
					format(tableNameSceleton, 
							   siardTable.getTableName());
			validationLog.append(tableName);
			//Verify whether the number of column elements in XML and XSD are equal 
			if (xmlElements.size() == xsdElements.size()) {
				for ( int i = 0; i < xmlElements.size(); i++ ) {
					Element xmlElement = xmlElements.get(i);
					Element xsdElement = xsdElements.get(i);
					//Retrieve the Elements name
					String xmlTypeElementName = properties.getProperty("siard.metadata.xml.type.element.name");
					String xsdTypeAttributeName = properties.getProperty("siard.table.xsd.type.attribute.name");
					//Retrieve the original column type from metadata.xml
					String leftSide = xmlElement.getChild(xmlTypeElementName, this.getXmlNamespace()).getValue();
					//Retrieve the original column type from table.xsd
					String rightSide = xsdElement.getAttributeValue(xsdTypeAttributeName);
					String delimiter = properties.getProperty("attribute.sequence.validator.original.type.delimiter");
					//Trim the column types - eliminates the brackets and specific numeric parameters
					String trimmedExpectedType = trimLeftSideType(leftSide, delimiter);
					//Designing expected column type in table.xsd, called "rightSide"
					String expectedType = properties.getProperty(trimmedExpectedType);
					//Convey the column types for the all over sequence test [E.4] 
					xmlElementSequence.add(expectedType);
					xsdElementSequence.add(rightSide);
					//Verify, whether the column type in XML is equal to the one in XSD
					if (expectedType.equalsIgnoreCase(rightSide)) {
					} else {
						valid = false;
					}
					//Add column info to the log entry
					String validationLogTypeSceleton = properties.
							getProperty("attribute.type.validator.log.column");
					String validationLogColumnName = properties.getProperty("attribute.occurrence.column.name");
					Element columnName = xmlElement.getChild(validationLogColumnName, this.getXmlNamespace());
					String validationLogColumnEntry = MessageFormat.
							format(validationLogTypeSceleton, 
								   columnName.getValue(),
								   leftSide,
								   expectedType,
								   rightSide,
								   valid);
					validationLog.append(validationLogColumnEntry);
					validationLog.append(properties.getProperty("newline"));
				}
			} else {
				valid = false;
			}
		}
		//Save the allover column elements for [E.4]
		this.setXmlElementsSequence(xmlElementSequence);
		this.setXsdElementsSequence(xsdElementSequence);
		//Writes back validatable XML elements
		if (this.getXmlElementsSequence() != null && this.getXsdElementsSequence() != null) {
			valid = true;
		}
		//Write the local validation log to the validation context
	    this.setValidationLog(validationLog);
	    //Return the current validation state
		return valid;
	}
	/* [E.4] */
	private boolean validateAttributeSequence(Properties properties) 
			throws Exception {
		boolean valid = true;
		//Initializing validation Logging
		StringBuilder validationLog = new StringBuilder();
		String methodTitle = properties.
			getProperty("attribute.sequence.validator");
		String methodDescription = properties.
			getProperty("attribute.sequence.validator.description");
		validationLog.append(methodTitle);
		validationLog.append(methodDescription);
		//Retrieve the sequence of all column types from metadata.xml
		List<String> xmlTypesSequence = this.getXmlElementsSequence();
		//Retrieve the sequence of all column types from the according table.xsd
		List<String> xsdTypesSequence = this.getXsdElementsSequence();
		int xmlTypesSequenceCount = xmlTypesSequence.size();
		int xsdTypesSequenceCount = xsdTypesSequence.size();
		//Verify whether the number of all column type elements are equal in metadata.xml and according table.xsd
		if (xmlTypesSequenceCount == xsdTypesSequenceCount) {
			for ( int i = 0; i < xmlTypesSequenceCount; i++ ) {
				String xmlType = xmlTypesSequence.get(i);
				String xsdType = xsdTypesSequence.get(i);
				if ( !xmlType.equalsIgnoreCase(xsdType) ) {
					valid = true;
				}
				String validationLogSequenceSceleton = properties.
						getProperty("attribute.sequence.validator.log.column");
				String validationLogSequenceEntry = MessageFormat.
						format(validationLogSequenceSceleton, 
							   xmlType,
							   xsdType,
							   valid);
				validationLog.append(validationLogSequenceEntry);
			}
		} else {
			valid = false;
		}
		//Write the local validation log to the validation context
		this.setValidationLog(validationLog);
		return valid; 
	}
	
	/*Internal helper methods*/	
	/* [E.0.1] */
	private boolean initializeProperties() throws IOException {
		boolean successfullyCommitted = false;
		String me = "[E.0.1] initializeProperties() ";
		//Initializing the validation context properties
		String propertiesName = "/validation.properties";
		InputStream propertiesInputStream = getClass().getResourceAsStream(propertiesName);
		Properties properties = new Properties();
		properties.load(propertiesInputStream);
		this.setValidationProperties(properties);
		if (this.getValidationProperties() != null) {
			successfullyCommitted = true;
			//Set header line to validation log
			String headerLine = properties.getProperty("header.line");
			this.getValidationLog().append(headerLine);
			String message = properties.getProperty("successfully.executed");
			this.getValidationLog().append(me + message);
		}
		return successfullyCommitted;
	}
	/* [E.0.2] */
	private boolean initializePath(Properties properties) {
		boolean successfullyCommitted = false;
		String me = "[E.0.2] initializePath(Properties properties) ";
		StringBuffer headerPath = new StringBuffer();
		StringBuffer contentPath = new StringBuffer();
		String workDir = this.getConfigurationService().getPathToWorkDir();
		headerPath.append(workDir);
		headerPath.append(File.separator);
		headerPath.append(properties.getProperty("header.suffix"));
		contentPath.append(workDir);
		contentPath.append(File.separator);
		contentPath.append(properties.getProperty("content.suffix"));
		this.setHeaderPath(headerPath.toString());
		this.setContentPath(contentPath.toString());
		if (this.getHeaderPath() != null && this.getContentPath() != null) {
			successfullyCommitted = true;
			String message = properties.getProperty("successfully.executed");
			this.getValidationLog().append(me + message);
		}
		return successfullyCommitted;
	} 
	/* [E.0.5] */
	private boolean prepareXMLAccess(Properties properties, File metadataXML) 
			throws JDOMException, IOException {
		boolean successfullyCommitted = false;
		String me = "[E.0.5] prepareXMLAccess(Properties properties, File metadataXML) ";
		StringBuffer headerPath = new StringBuffer();
		StringBuffer contentPath = new StringBuffer();
		InputStream inputStream = new FileInputStream(metadataXML);
  		SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(inputStream);
        //Assigning JDOM Document to the validation context
        this.setMetadataXMLDocument(document);
		String xmlPrefix = properties.getProperty("metadata.xml.prefix");
		String xsdPrefix = properties.getProperty("table.xsd.prefix");
		//Setting the namespaces to access metadata.xml and the different table.xsd
		Element rootElement = document.getRootElement();
		String namespaceURI = rootElement.getNamespaceURI();
		Namespace xmlNamespace = Namespace.getNamespace(xmlPrefix, namespaceURI);
		Namespace xsdNamespace = Namespace.getNamespace(xsdPrefix, namespaceURI);
		//Assigning prefix to the validation context
		this.setXmlPrefix(xmlPrefix);
		this.setXsdPrefix(xsdPrefix);
		//Assigning namespace info to the validation context
		this.setXmlNamespace(xmlNamespace);
		this.setXsdNamespace(xsdNamespace);
		if ( this.getXmlNamespace() != null &&
			 this.getXsdNamespace() != null &&
			 this.getXmlPrefix() != null &&
			 this.getXsdPrefix() != null &&
			 this.getMetadataXMLDocument() != null) {
			String message = properties.getProperty("successfully.executed");
			this.getValidationLog().append(me + message);
			successfullyCommitted = true;
		}
		return successfullyCommitted;
	}    
	/* */
	private String trimLeftSideType(String leftside, String delimiter) {
		int i = leftside.indexOf(delimiter);
		if (i > -1) {
			String trimmedLeftSideType = leftside.substring(0, i);
			return trimmedLeftSideType;
		} else {
			return leftside;
		}
	}
	/* [E.0.3] */
    private boolean extractSiardArchive (File packedSiardArchive, Properties properties)
			   throws FileNotFoundException, IOException {
    	boolean sucessfullyCommitted = false;
    	String me = "[E.0.3] extractSiardArchive (File packedSiardArchive) ";
		StringBuffer headerPath = new StringBuffer();
		StringBuffer contentPath = new StringBuffer();
		String workDir = this.getConfigurationService().getPathToWorkDir();
		Zip64File zipfile = new Zip64File(packedSiardArchive);
		List<FileEntry> fileEntryList = zipfile.getListFileEntries();
		String pathToWorkDir = getConfigurationService().getPathToWorkDir();
		File tmpDir = new File(pathToWorkDir);
		HashMap<String, File> extractedSiardFiles = new HashMap<String, File>();
		for (FileEntry fileEntry : fileEntryList) {
			if (!fileEntry.isDirectory()) {
				byte[] buffer = new byte[8192];
			    EntryInputStream eis = zipfile.openEntryInputStream(fileEntry.getName());
			    File newFile = new File(tmpDir, fileEntry.getName());
			    File parent = newFile.getParentFile();
			    if (!parent.exists()) {
			    	parent.mkdirs();
			    }
			    FileOutputStream fos = new FileOutputStream(newFile);          
			    for (int iRead = eis.read(buffer); iRead >= 0; iRead = eis.read(buffer)){
			    	fos.write(buffer, 0, iRead);
			    }
			    extractedSiardFiles.put(newFile.getPath(), newFile);
			    eis.close();
			    fos.close();                
			}
		}
		this.setSiardFiles(extractedSiardFiles);
		if (this.getSiardFiles() != null) {
			String message = properties.getProperty("successfully.executed");
			this.getValidationLog().append(me + message);
			sucessfullyCommitted = true;
		}
		return sucessfullyCommitted;
	}
    /* [E.0.4] */
	private boolean pickMetadataXML (Properties properties) {
		boolean successfullyCommitted = false;
		String me = "[E.0.4] pickMetadataXML (Properties properties) ";
		HashMap<String, File> siardFiles = this.getSiardFiles();
		String pathToMetadataXML = this.getConfigurationService().getPathToWorkDir();
		pathToMetadataXML = pathToMetadataXML+properties.getProperty("siard.description");
		File metadataXML = siardFiles.get(pathToMetadataXML);
		this.setMetadataXML(metadataXML);
		if (this.getMetadataXML() != null) {
			String message = properties.getProperty("successfully.executed");
			this.getValidationLog().append(me + message);
			successfullyCommitted = true;
		}
		return successfullyCommitted;
	}
	/* [E.0.6] */
	private boolean prepareValidationData (Properties properties, File metadataXML) 
			throws JDOMException, IOException {
		boolean successfullyCommitted = false;
		String me = "[E.0.6] prepareValidationData (Properties properties, File metadataXML) ";
		List<SiardTable> siardTables = new ArrayList<SiardTable>();
		Document document = this.getMetadataXMLDocument();
        Element rootElement = document.getRootElement();
        String workingDirectory = this.getConfigurationService().getPathToWorkDir();     
        String siardSchemasElementsName = properties.getProperty("siard.metadata.xml.schemas.name");
        List<Element> siardSchemasElements = rootElement.getChildren(siardSchemasElementsName, 
        this.getXmlNamespace());
        for (Element siardSchemasElement : siardSchemasElements) {
           	List<Element> siardSchemaElements = siardSchemasElement.getChildren(properties.
        			getProperty("siard.metadata.xml.schema.name"), this.getXmlNamespace());
        	for (Element siardSchemaElement : siardSchemaElements) {
           		String schemaFolderName = siardSchemaElement.getChild(properties.
        				getProperty("siard.metadata.xml.schema.folder.name"), this.getXmlNamespace()).getValue();
        		Element siardTablesElement = siardSchemaElement.getChild(properties.
        				getProperty("siard.metadata.xml.tables.name"), this.getXmlNamespace());
        		List<Element> siardTableElements = siardTablesElement.getChildren(properties.
        				getProperty("siard.metadata.xml.table.name"), this.getXmlNamespace());
        		for (Element siardTableElement : siardTableElements) {
           			Element siardColumnsElement = siardTableElement.getChild(properties.
        					getProperty("siard.metadata.xml.columns.name"), this.getXmlNamespace());
        			List<Element> siardColumnElements = siardColumnsElement.getChildren(properties.
        					getProperty("siard.metadata.xml.column.name"), this.getXmlNamespace());
        			String tableName = siardTableElement.getChild(properties.
        					getProperty("siard.metadata.xml.table.folder.name"), this.getXmlNamespace()).getValue();
           			SiardTable siardTable = new SiardTable();
        			siardTable.setMetadataXMLElements(siardColumnElements);
        			siardTable.setTableName(tableName);
        			String siardTableFolderName = siardTableElement.getChild(properties.
        					getProperty("siard.metadata.xml.table.folder.name"), this.getXmlNamespace()).getValue();
        			StringBuilder pathToTableSchema = new StringBuilder();
        		    pathToTableSchema.append(workingDirectory);
        		    pathToTableSchema.append(File.separator);
        		    pathToTableSchema.append(properties.getProperty("siard.path.to.content"));
        		    pathToTableSchema.append(File.separator);
        		    pathToTableSchema.append(schemaFolderName.replaceAll(" ", ""));
        		    pathToTableSchema.append(File.separator);
        		    pathToTableSchema.append(siardTableFolderName.replaceAll(" ", ""));
        		    pathToTableSchema.append(File.separator);
        		    pathToTableSchema.append(siardTableFolderName.replaceAll(" ", ""));
        		    pathToTableSchema.append(properties.getProperty("siard.table.xsd.file.extension"));
        		    File tableSchema = this.getSiardFiles().get(pathToTableSchema.toString());
           			SAXBuilder builder = new SAXBuilder();
        			Document tableSchemaDocument = builder.build(tableSchema);
           			Element tableSchemaRootElement = tableSchemaDocument.getRootElement();
        			Namespace namespace = tableSchemaRootElement.getNamespace();
           			Element tableSchemaComplexType = tableSchemaRootElement.getChild(properties.
        					getProperty("siard.table.xsd.complexType"), namespace);
        			Element tableSchemaComplexTypeSequence = tableSchemaComplexType.getChild(properties.
        					getProperty("siard.table.xsd.sequence"), namespace);
        			List<Element> tableSchemaComplexTypeElements = tableSchemaComplexTypeSequence.getChildren(properties.
        					getProperty("siard.table.xsd.element"), namespace);
        			siardTable.setTableXSDElements(tableSchemaComplexTypeElements);
        			siardTables.add(siardTable);
           			this.setSiardTables(siardTables);	
        		}		
        	}
        }
        if (this.getSiardTables() != null) {
        	String message = properties.getProperty("successfully.executed");
			this.getValidationLog().append(me + message);
        	successfullyCommitted = true;
        }
		return successfullyCommitted;
	}
	
	//Setter and Getter methods
	/**
	 * @return the configurationService
	 */
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	/**
	 * @param configurationService the configurationService to set
	 */
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	/**
	 * @return the validationProperties
	 */
	public Properties getValidationProperties() {
		return validationProperties;
	}

	/**
	 * @param validationProperties the validationProperties to set
	 */
	public void setValidationProperties(Properties validationProperties) {
		this.validationProperties = validationProperties;
	}

	/**
	 * @return the siardFiles
	 */
	public HashMap<String, File> getSiardFiles() {
		return siardFiles;
	}

	/**
	 * @param siardFiles the siardFiles to set
	 */
	public void setSiardFiles(HashMap<String, File> siardFiles) {
		this.siardFiles = siardFiles;
	}

	/**
	 * @return the metadataXML
	 */
	public File getMetadataXML() {
		return metadataXML;
	}

	/**
	 * @param metadataXML the metadataXML to set
	 */
	public void setMetadataXML(File metadataXML) {
		this.metadataXML = metadataXML;
	}

	/**
	 * @return the contentPath
	 */
	public String getContentPath() {
		return contentPath;
	}

	/**
	 * @param contentPath the contentPath to set
	 */
	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}

	/**
	 * @return the headerPath
	 */
	public String getHeaderPath() {
		return headerPath;
	}

	/**
	 * @param headerPath the headerPath to set
	 */
	public void setHeaderPath(String headerPath) {
		this.headerPath = headerPath;
	}

	/**
	 * @return the xmlElements
	 */
	public List<Element> getXmlElements() {
		return xmlElements;
	}

	/**
	 * @param xmlElements the xmlElements to set
	 */
	public void setXmlElements(List<Element> xmlElements) {
		this.xmlElements = xmlElements;
	}

	/**
	 * @return the xsdElements
	 */
	public List<Element> getXsdElements() {
		return xsdElements;
	}

	/**
	 * @param xsdElements the xsdElements to set
	 */
	public void setXsdElements(List<Element> xsdElements) {
		this.xsdElements = xsdElements;
	}

	/**
	 * @return the xmlElementsSequence
	 */
	public List<String> getXmlElementsSequence() {
		return xmlElementsSequence;
	}

	/**
	 * @param xmlElementsSequence the xmlElementsSequence to set
	 */
	public void setXmlElementsSequence(List<String> xmlElementsSequence) {
		this.xmlElementsSequence = xmlElementsSequence;
	}

	/**
	 * @return the xsdelementsSequence
	 */
	public List<String> getXsdElementsSequence() {
		return xsdElementsSequence;
	}

	/**
	 * @param xsdelementsSequence the xsdelementsSequence to set
	 */
	public void setXsdElementsSequence(List<String> xsdElementsSequence) {
		this.xsdElementsSequence = xsdElementsSequence;
	}

	/**
	 * @return the namespaceURI
	 */
	public String getNamespaceURI() {
		return namespaceURI;
	}

	/**
	 * @param namespaceURI the namespaceURI to set
	 */
	public void setNamespaceURI(String namespaceURI) {
		this.namespaceURI = namespaceURI;
	}

	/**
	 * @return the xmlPrefix
	 */
	public String getXmlPrefix() {
		return xmlPrefix;
	}

	/**
	 * @param xmlPrefix the xmlPrefix to set
	 */
	public void setXmlPrefix(String xmlPrefix) {
		this.xmlPrefix = xmlPrefix;
	}

	/**
	 * @return the xsdPrefix
	 */
	public String getXsdPrefix() {
		return xsdPrefix;
	}

	/**
	 * @param xsdPrefix the xsdPrefix to set
	 */
	public void setXsdPrefix(String xsdPrefix) {
		this.xsdPrefix = xsdPrefix;
	}

	/**
	 * @return the xmlNamespace
	 */
	public Namespace getXmlNamespace() {
		return xmlNamespace;
	}

	/**
	 * @param xmlNamespace the xmlNamespace to set
	 */
	public void setXmlNamespace(Namespace xmlNamespace) {
		this.xmlNamespace = xmlNamespace;
	}

	/**
	 * @return the xsdNamespace
	 */
	public Namespace getXsdNamespace() {
		return xsdNamespace;
	}

	/**
	 * @param xsdNamespace the xsdNamespace to set
	 */
	public void setXsdNamespace(Namespace xsdNamespace) {
		this.xsdNamespace = xsdNamespace;
	}

	/**
	 * @return the siardTables
	 */
	public List<SiardTable> getSiardTables() {
		return siardTables;
	}

	/**
	 * @param siardTables the siardTables to set
	 */
	public void setSiardTables(List<SiardTable> siardTables) {
		this.siardTables = siardTables;
	}

	public Document getMetadataXMLDocument() {
		return metadataXMLDocument;
	}

	public void setMetadataXMLDocument(Document metadataXMLDocument) {
		this.metadataXMLDocument = metadataXMLDocument;
	}

	public StringBuilder getValidationLog() {
		return validationLog;
	}

	public void setValidationLog(StringBuilder validationLog) {
		this.validationLog = validationLog;
	}

	/**
	 * @return the verboseMode
	 */
	public boolean isVerboseMode() {
		return verboseMode;
	}

	/**
	 * @param verboseMode the verboseMode to set
	 */
	public void setVerboseMode(boolean verboseMode) {
		this.verboseMode = verboseMode;
	}
}


