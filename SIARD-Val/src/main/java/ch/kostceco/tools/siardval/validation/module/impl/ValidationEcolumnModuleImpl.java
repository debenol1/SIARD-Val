package ch.kostceco.tools.siardval.validation.module.impl;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.*;
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
	private String contentPath;
	private String headerPath;
	
	/*SIARD XML processing related properties*/
	private List<Element> xmlElements;
	private List<Element> xsdElements;
	
	private List<String> xmlElementsSequence;
	private List<String> xsdelementsSequence;
	
	private List<SiardTable> siardTables;
 
	/*General XML related properties for JDOM Access*/
	private String namespaceURI;
	private String xmlPrefix;
	private String xsdPrefix;
	private Namespace xmlNamespace;
	private Namespace xsdNamespace;
  
	@Override
	public boolean validate(File siardDatei) throws ValidationEcolumnException {
		
		 //All over validation flag
		 boolean valid = true;
		 
		 //Validation Java properties
		 Properties properties = this.getValidationProperties();
		 
		 //Validation data
		 List<SiardTable> siardTables = this.getSiardTables();
	  
		 try {
		  
			 //Initialize the validation context
			 prepareValidation(siardDatei);
	  
			 //Validates the number of the attributes
			 if (validateAttributeCount(siardTables) == false) {
				 valid = false;
				 getMessageService().logInfo(
	                     getTextResourceService().getText(MESSAGE_MODULE_E) +
	                     getTextResourceService().getText(MESSAGE_DASHES) +
	                     getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_COUNT));
			 }
	  
			 //Validates the nullable property in metadata.xml
			 if (validateAttributeOccurrence(siardTables, properties) == false) {
				 valid = false;
				 getMessageService().logInfo(
	                     getTextResourceService().getText(MESSAGE_MODULE_E) +
	                     getTextResourceService().getText(MESSAGE_DASHES) +
	                     getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_OCCURRENCE));
			 }
	  
			 //Validates the sequence of table attributes in metadata.xml
			 if (validateAttributeSequence() == false) {
				 valid = false;
				 getMessageService().logInfo(
	                     getTextResourceService().getText(MESSAGE_MODULE_E) +
	                     getTextResourceService().getText(MESSAGE_DASHES) +
	                     getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_SEQUENCE));
			 }
	  
			 //Validates the type of table attributes in metadata.xml
			 if (validateAttributeType(siardTables, properties) == false) {
				 valid = false;
				 getMessageService().logInfo(
	                     getTextResourceService().getText(MESSAGE_MODULE_E) +
	                     getTextResourceService().getText(MESSAGE_DASHES) +
	                     getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_TYPE));
			 }	
			 
		 } catch (Exception je) {
	   valid = false;
	   getMessageService().logError(
	                    getTextResourceService().getText(MESSAGE_MODULE_E) +
	                    getTextResourceService().getText(MESSAGE_DASHES) +
	                    "JaxenException " +
	                    je.getMessage());
	  }
	  return valid;
	}
	
	private boolean prepareValidation(File siardFile) throws IOException {
		
		boolean prepared = false;
		
		boolean propertiesLoaded = initializeProperties();
		boolean pathInitilized = initializePath(this.getValidationProperties());
		boolean xmlAcccessPrepared = prepareXMLAccess(this.getValidationProperties());
		
		return prepared;
	}
	
	private boolean validateAttributeCount(List<SiardTable> siardTables) {
		
		boolean valid = true;
		 
		for (SiardTable siardTable : siardTables) {
			
			int metadataXMLColumnsCount = siardTable.getMetadataXMLElements().size();
			int tableXSDColumnsCount = siardTable.getTableXSDElements().size();
		 
			if (metadataXMLColumnsCount == tableXSDColumnsCount) {
				valid = true;
			}
		}
		return valid;
	}
	
	private boolean validateAttributeOccurrence(List<SiardTable> siardTables, Properties properties) {
		
		boolean valid = true;
		
		for (SiardTable siardTable : siardTables) {
			
			int metadataXMLColumnsCount = siardTable.getMetadataXMLElements().size();
			int tableXSDColumnsCount = siardTable.getTableXSDElements().size();
		 
			if (metadataXMLColumnsCount == tableXSDColumnsCount) {
			 
				List<Element> xmlElements = siardTable.getMetadataXMLElements();
				List<Element> xsdElements = siardTable.getTableXSDElements();
			 
				Namespace xmlNamespace = this.getXmlNamespace();
				Namespace xsdNamespace = this.getXsdNamespace();
			 
				for ( int i = 0; i < metadataXMLColumnsCount; i++) {
				 
					Element xmlElement = xmlElements.get(i);
					Element xsdElement = xsdElements.get(i);
				 
					String nullableElementDescription = properties.getProperty("metadata.xml.nullable");
					String minuOccursAttributeDescription = properties.getProperty("table.xsd.attribute.minOccurs.name");
				 
					String nullable = xmlElement.getChild(nullableElementDescription, xmlNamespace).getValue();
					String minOccurs = xsdElement.getAttributeValue(minuOccursAttributeDescription, xsdNamespace);
				 
					if (nullable.equalsIgnoreCase("true") && minOccurs == null) {
						valid = false;
					} else if (nullable.equalsIgnoreCase("true") && minOccurs.equalsIgnoreCase("0")) {
						//Keep empty not to overwrite previous false values
					} else if (nullable.equalsIgnoreCase("false") && minOccurs == null) {
					}
					//Keep empty not to overwrite previous false values
				} 
			} else {
				valid = false;
			}
		}
		return valid;
	}
	
	private boolean validateAttributeType(List<SiardTable> siardTables, Properties properties) {
		
		boolean valid = true;
		
		for (SiardTable siardTable : siardTables) {
		
			List<Element> xmlElements = siardTable.getMetadataXMLElements();
			List<Element> xsdElements = siardTable.getTableXSDElements();
		 	
			if (xmlElements.size() == xsdElements.size()) {
		
				for ( int i = 0; i < xmlElements.size(); i++ ) {
						
					Element xmlElement = xmlElements.get(i);
					Element xsdElement = xsdElements.get(i);
				 
					String xmlTypeElementName = properties.getProperty("siard.metadata.xml.type.element.name");
					String xmlNameElementName = properties.getProperty("siard.metadata.xml.name.element.name");
					String xsdTypeAttributeName = properties.getProperty("siard.table.xsd.type.attribute.name");
						
					//Check the nullable Element
					String leftSide = xmlElement.getChild(xmlTypeElementName, this.getXmlNamespace()).getValue();
						
					//Check the minOccurs Attribute
					String rightSide = xsdElement.getAttributeValue(xsdTypeAttributeName);
						
					//?
					//String elementName = xmlElement.getChild("name", this.getXmlNamespace()).getValue();
						
					String delimiter = properties.getProperty("attribute.type.validator.original.type.delimiter");
						
					String trimmedExpectedType = trimLeftSideType(leftSide, delimiter);
					String expectedType = properties.getProperty(trimmedExpectedType);
				
					//All over list to check the sequence of all types
					this.getXmlElementsSequence().add(expectedType);
					this.getXsdelementsSequence().add(rightSide);
				
					if (expectedType.equalsIgnoreCase(rightSide)) {
					} else {
						valid = false;
					}
				}
			} else {
				valid = false;
			}
		}		
		return valid;
	}
	
	private boolean validateAttributeSequence() {
		
		boolean valid = true;
	
		List<String> xmlTypesSequence = this.getXmlElementsSequence();
		List<String> xsdTypesSequence = this.getXsdelementsSequence();
		 
		int xmlTypesSequenceCount = xmlTypesSequence.size();
		int xsdTypesSequenceCount = xsdTypesSequence.size();
		 
		if (xmlTypesSequenceCount == xsdTypesSequenceCount) {
		 
			for ( int i = 0; i < xmlTypesSequenceCount; i++ ) {
			 
				String xmlType = xmlTypesSequence.get(i);
				String xsdType = xsdTypesSequence.get(i);
				 
				if ( !xmlType.equalsIgnoreCase(xsdType) ) {
					valid = false;
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
		 
		 
		 
	private boolean prepareXMLAccess(Properties properties) {
		
		boolean successfullyCommitted = false;
		   
		//Setting the namespaces to access metadata.xml and the different table.xsd
		Namespace xmlNamespace = Namespace.getNamespace(xmlPrefix, namespaceURI);
		Namespace xsdNamespace = Namespace.getNamespace(xsdPrefix, namespaceURI);
		       
		//Assigning namespace info to the validation context
		this.setXmlNamespace(xmlNamespace);
		this.setXsdNamespace(xsdNamespace);
		       
		//Setting the XML prefix to access metadata.xml and the different table.xsd
		//The prefices are stored in validation.properties
		String xmlPrefix = properties.getProperty("metadata.xml.prefix");
		String xsdPrefix = properties.getProperty("table.xsd.prefix");
		       
		//Assigning prefix to the validation context
		this.setXmlPrefix(xmlPrefix);
		this.setXsdPrefix(xsdPrefix);
		
		if ( this.getXmlNamespace() != null &&
			 this.getXsdNamespace() != null &&
			 this.getXmlPrefix() != null &&
			 this.getXsdPrefix() != null ) {
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
	public List<String> getXsdelementsSequence() {
		return xsdelementsSequence;
	}

	/**
	 * @param xsdelementsSequence the xsdelementsSequence to set
	 */
	public void setXsdelementsSequence(List<String> xsdelementsSequence) {
		this.xsdelementsSequence = xsdelementsSequence;
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
 
	
	
 
}


