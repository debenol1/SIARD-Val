package ch.kostceco.tools.siardval.validation.module.impl;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
	
	@SuppressWarnings("finally")
	@Override
	public boolean validate(File siardDatei) throws ValidationEcolumnException {
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
			 }
			 //System.out.println(this.getValidationLog().toString());
			 //Validates the nullable property in metadata.xml
			 if (validateAttributeOccurrence(siardTables, properties) == false) {
				valid = false;
					 getMessageService().logInfo(
	                 getTextResourceService().getText(MESSAGE_MODULE_E) +
	                 getTextResourceService().getText(MESSAGE_DASHES) +
	                 getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_OCCURRENCE));
			 }
			 //System.out.println(this.getValidationLog().toString());
			 //Validates the type of table attributes in metadata.xml
			 if (validateAttributeType(siardTables, properties) == false) {
				valid = false;
					 getMessageService().logInfo(
	                 getTextResourceService().getText(MESSAGE_MODULE_E) +
	                 getTextResourceService().getText(MESSAGE_DASHES) +
	                 getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_TYPE));
			 }	
	         //Validates the sequence of table attributes in metadata.xml
			 if (validateAttributeSequence() == false) {
				valid = false;
					getMessageService().logInfo(
	                getTextResourceService().getText(MESSAGE_MODULE_E) +
	                getTextResourceService().getText(MESSAGE_DASHES) +
	                getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_SEQUENCE));
			 }
			 //System.out.println(this.getValidationLog().toString());			 
		} catch (Exception je) {
			valid = false;
			getMessageService().logError(
				getTextResourceService().getText(MESSAGE_MODULE_E) +
	            getTextResourceService().getText(MESSAGE_DASHES) + je.getMessage());
		} finally {
		 return valid;
		}
	}
	
	private boolean prepareValidation(File siardFile) throws IOException, JDOMException {
		//All over preparation flag
		boolean prepared = true;
		//Load the Java properties to the validation context
		boolean propertiesLoaded = initializeProperties();
		//Initialize internal path configuration of the SIARD archive
		boolean pathInitialized = initializePath(this.getValidationProperties());
		//Extract the SIARD archive and distribute the content to the validation context
		boolean siardArchiveExtracted = extractSiardArchive(siardFile);
		//Pick the metadata.xml and load it to the validation context
		boolean metadataXMLpicked = pickMetadataXML(this.getValidationProperties());
		//Prepare the XML configuration and store it to the validation context
		boolean xmlAcccessPrepared = prepareXMLAccess(this.getValidationProperties(), this.getMetadataXML());
		//Prepare the data to be validated such as metadata.xml and the according XML schemas
		boolean validationDataPrepared = prepareValidationData(this.getValidationProperties(), this.getMetadataXML());
		//Verifying whether the preparation steps were successful
		if (propertiesLoaded == true &&
				pathInitialized == true &&
				xmlAcccessPrepared == true &&
				siardArchiveExtracted == true &&
				metadataXMLpicked == true && 
				validationDataPrepared == true) {
			prepared = true;
		}
		return prepared;
	}
	
	//[E.1]
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
						   tableXSDColumnsCount);
			validationLog.append(validationLogEntry);
			validationLog.append(properties.getProperty("newline"));
		}
		//Write the local validation log to the validation context
		this.setValidationLog(validationLog);
		//Return the current validation state
		return valid;
	}
	
	//[E.2]
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
			System.out.println(validationLogTableEntry);
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
					//Add column info to the log entry
					String validationLogColumnSceleton = properties.
							getProperty("attribute.occurrence.validator.log.column");
					String validationLogColumnName = properties.getProperty("attribute.occurrence.column.name");
					Element columnName = xmlElement.getChild(validationLogColumnName, this.getXmlNamespace());
					String validationLogColumnEntry = MessageFormat.
							format(validationLogColumnSceleton, 
								   columnName.getValue(),
								   xsdElement.getAttribute(properties.
											getProperty("attribute.occurrence.minOccurs")),
								   xsdElement.getAttributeValue(properties.
											getProperty("attribute.occurrence.minOccurs")));
					validationLog.append(validationLogColumnEntry);
					validationLog.append(properties.getProperty("newline"));
					System.out.println(validationLogColumnEntry);
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
					//Keep empty not to overwrite previous false values
				} 
			} else {
				//Validation fails if allover number differs in metadata.xml and XML schemata
				valid = false;
				
			}
		}
		
		return valid;
	}
	
	private boolean validateAttributeType(List<SiardTable> siardTables, Properties properties) {
		
		boolean valid = true;
		
		List<String> xmlElementSequence = new ArrayList<String>();
		List<String> xsdElementSequence = new ArrayList<String>();
		
		for (SiardTable siardTable : siardTables) {
			
			List<Element> xmlElements = siardTable.getMetadataXMLElements();
			List<Element> xsdElements = siardTable.getTableXSDElements();
		 	
			if (xmlElements.size() == xsdElements.size()) {
				
				
				
				for ( int i = 0; i < xmlElements.size(); i++ ) {
					
					Element xmlElement = xmlElements.get(i);
					Element xsdElement = xsdElements.get(i);
					
					
					
					String xmlTypeElementName = properties.getProperty("siard.metadata.xml.type.element.name");
					//String xmlNameElementName = properties.getProperty("siard.metadata.xml.name.element.name");
					String xsdTypeAttributeName = properties.getProperty("siard.table.xsd.type.attribute.name");
					
					//Check the nullable Element
					String leftSide = xmlElement.getChild(xmlTypeElementName, this.getXmlNamespace()).getValue();
						
					//Check the minOccurs Attribute
					String rightSide = xsdElement.getAttributeValue(xsdTypeAttributeName);
					
					//?
					//String elementName = xmlElement.getChild("name", this.getXmlNamespace()).getValue();
					
					String delimiter = properties.getProperty("attribute.sequence.validator.original.type.delimiter");
					
					String trimmedExpectedType = trimLeftSideType(leftSide, delimiter);
					
					String expectedType = properties.getProperty(trimmedExpectedType);
					
					
					//All over list to check the sequence of all types
					
					xmlElementSequence.add(expectedType);
					xsdElementSequence.add(rightSide);
					
					
					if (expectedType.equalsIgnoreCase(rightSide)) {
					} else {
						valid = false;
					}
				}
			} else {
				valid = false;
			}
		}
		this.setXmlElementsSequence(xmlElementSequence);
		this.setXsdElementsSequence(xsdElementSequence);
		
		if (this.getXmlElementsSequence() != null && this.getXsdElementsSequence() != null) {
			valid = true;
		}
		
		
		return valid;
	}
	
	private boolean validateAttributeSequence() {
		
		boolean valid = true;
		
	
		List<String> xmlTypesSequence = this.getXmlElementsSequence();
		List<String> xsdTypesSequence = this.getXsdElementsSequence();
		int xmlTypesSequenceCount = xmlTypesSequence.size();
		int xsdTypesSequenceCount = xsdTypesSequence.size();
		if (xmlTypesSequenceCount == xsdTypesSequenceCount) {
			for ( int i = 0; i < xmlTypesSequenceCount; i++ ) {
				String xmlType = xmlTypesSequence.get(i);
				String xsdType = xsdTypesSequence.get(i);
				 
				if ( !xmlType.equalsIgnoreCase(xsdType) ) {
					valid = true;
				}
			}
		} else {
			valid = false;
		}
		
		return valid; 
	}
	
	//Internal helper methods
	
	
	private boolean initializeProperties() throws IOException {
		
		boolean successfullyCommitted = false;
		
		//Initializing the validation context properties
		String propertiesName = "/validation.properties";
		InputStream propertiesInputStream = getClass().getResourceAsStream(propertiesName);
		     
		Properties properties = new Properties();
		properties.load(propertiesInputStream);
		
		this.setValidationProperties(properties);
		
		if (this.getValidationProperties() != null) {
			successfullyCommitted = true;
		}
		
		return successfullyCommitted;
	}
		 
	private boolean initializePath(Properties properties) {
		
		boolean successfullyCommitted = false;
		   
		StringBuffer headerPath = new StringBuffer();
		StringBuffer contentPath = new StringBuffer();
		  
		String workDir = this.getConfigurationService().getPathToWorkDir();
		  
		headerPath.append(workDir);
		headerPath.append(File.pathSeparator);
		headerPath.append(properties.getProperty("header.suffix"));
		  
		contentPath.append(workDir);
		contentPath.append(File.pathSeparator);
		contentPath.append(properties.getProperty("content.suffix"));
		  
		this.setHeaderPath(headerPath.toString());
		this.setContentPath(contentPath.toString());
		
		if (this.getHeaderPath() != null && this.getContentPath() != null) {
			successfullyCommitted = true;
		}
		
		return successfullyCommitted;
	}
		 
		 
		 
	private boolean prepareXMLAccess(Properties properties, File metadataXML) throws JDOMException, IOException {
		
		boolean successfullyCommitted = false;
		
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
			successfullyCommitted = true;
		}
		return successfullyCommitted;
	}
		 
	private String trimLeftSideType(String leftside, String delimiter) {
			 
		int i = leftside.indexOf(delimiter);
				
		if (i > -1) {
			String trimmedLeftSideType = leftside.substring(0, i);
			return trimmedLeftSideType;
		} else {
			return leftside;
		}
	}
	
    private boolean extractSiardArchive(File packedSiardArchive)
			   throws FileNotFoundException, IOException {
    	
    	boolean sucessfullyCommitted = false;
			      
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
			sucessfullyCommitted = true;
		}
		
		return sucessfullyCommitted;
	}
			 
	private boolean pickMetadataXML (Properties properties) {
		
		boolean successfullyCommitted = false;
		
		HashMap<String, File> siardFiles = this.getSiardFiles();
		String pathToMetadataXML = this.getConfigurationService().getPathToWorkDir();
			  
		pathToMetadataXML = pathToMetadataXML+properties.getProperty("siard.description");
		File metadataXML = siardFiles.get(pathToMetadataXML);
		
		this.setMetadataXML(metadataXML);
		
		if (this.getMetadataXML() != null) {
			successfullyCommitted = true;
		}
		return successfullyCommitted;
			  
	}
	
	private boolean prepareValidationData(Properties properties, File metadataXML) throws JDOMException, IOException {
		
		
		
		boolean successfullyCommitted = false;
		
		List<SiardTable> siardTables = new ArrayList<SiardTable>();
		
		
		
		Document document = this.getMetadataXMLDocument();
        
        Element rootElement = document.getRootElement();
        String workingDirectory = this.getConfigurationService().getPathToWorkDir();        
        String siardSchemasElementsName = properties.getProperty("siard.metadata.xml.schemas.name");
        List<Element> siardSchemasElements = rootElement.getChildren(siardSchemasElementsName, this.getXmlNamespace());
        
        for (Element siardSchemasElement : siardSchemasElements) {
        	
        	List<Element> siardSchemaElements = siardSchemasElement.getChildren(properties.getProperty("siard.metadata.xml.schema.name"), this.getXmlNamespace());
        	
        	for (Element siardSchemaElement : siardSchemaElements) {
        		
        		String schemaFolderName = siardSchemaElement.getChild(properties.getProperty("siard.metadata.xml.schema.folder.name"), this.getXmlNamespace()).getValue();
        		
        		Element siardTablesElement = siardSchemaElement.getChild(properties.getProperty("siard.metadata.xml.tables.name"), this.getXmlNamespace());
        		
        		List<Element> siardTableElements = siardTablesElement.getChildren(properties.getProperty("siard.metadata.xml.table.name"), this.getXmlNamespace());
        		
        		for (Element siardTableElement : siardTableElements) {
        			
        			
        			Element siardColumnsElement = siardTableElement.getChild(properties.getProperty("siard.metadata.xml.columns.name"), this.getXmlNamespace());
        			
        			List<Element> siardColumnElements = siardColumnsElement.getChildren(properties.getProperty("siard.metadata.xml.column.name"), this.getXmlNamespace());
        			
        			String tableName = siardTableElement.getChild(properties.getProperty("siard.metadata.xml.table.folder.name"), this.getXmlNamespace()).getValue();
        			
        			
        			SiardTable siardTable = new SiardTable();
        			siardTable.setMetadataXMLElements(siardColumnElements);
        			siardTable.setTableName(tableName);
        			
        			String siardTableFolderName = siardTableElement.getChild(properties.getProperty("siard.metadata.xml.table.folder.name"), this.getXmlNamespace()).getValue();
        			
        		    StringBuilder pathToTableSchema = new StringBuilder();
        		    
        		    pathToTableSchema.append(workingDirectory);
        		    pathToTableSchema.append("/");
        		    pathToTableSchema.append(properties.getProperty("siard.path.to.content"));
        		    pathToTableSchema.append("/");
        		    pathToTableSchema.append(schemaFolderName.replaceAll(" ", ""));
        		    pathToTableSchema.append("/");
        		    pathToTableSchema.append(siardTableFolderName.replaceAll(" ", ""));
        		    pathToTableSchema.append("/");
        		    pathToTableSchema.append(siardTableFolderName.replaceAll(" ", ""));
        		    pathToTableSchema.append(properties.getProperty("siard.table.xsd.file.extension"));
        		           			
        			File tableSchema = this.getSiardFiles().get(pathToTableSchema.toString());
        			
        	  		SAXBuilder builder = new SAXBuilder();
        			Document tableSchemaDocument = builder.build(tableSchema);
        			
        			
        			Element tableSchemaRootElement = tableSchemaDocument.getRootElement();
        			Namespace namespace = tableSchemaRootElement.getNamespace();
        		
        			Element tableSchemaComplexType = tableSchemaRootElement.getChild(properties.getProperty("siard.table.xsd.complexType"), namespace);
        			Element tableSchemaComplexTypeSequence = tableSchemaComplexType.getChild(properties.getProperty("siard.table.xsd.sequence"), namespace);
        			
        			List<Element> tableSchemaComplexTypeElements = tableSchemaComplexTypeSequence.getChildren(properties.getProperty("siard.table.xsd.element"), namespace);
        			
        			siardTable.setTableXSDElements(tableSchemaComplexTypeElements);
        			siardTables.add(siardTable);
        			this.setSiardTables(siardTables);
        				
        		}		
        	}
        }
        if (this.getSiardTables() != null) {
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
}


