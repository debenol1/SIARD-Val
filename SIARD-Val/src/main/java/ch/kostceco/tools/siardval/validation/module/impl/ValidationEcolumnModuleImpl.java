package ch.kostceco.tools.siardval.validation.module.impl;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import ch.kostceco.tools.siardval.validation.module.ValidationEcolumnModule;

public class ValidationEcolumnModuleImpl extends ValidationModuleImpl implements ValidationEcolumnModule {
 
 //Service related properties
 private ConfigurationService configurationService;
 
 //Validation context related properties
 private Properties validationProperties;
 
 //Content of the SIARD package
 private HashMap<String, File> siardFiles;
 private HashMap<String, Document> siardDocuments;
 private String contentPath;
 private String headerPath;
 private File metadataXML;
 
 private List<Element> siardSchemas;
 private List<Element> xmlElements;
 private List<Element> xsdElements;
 
 //XML related properties
 private String namespaceURI;
 
 private String xmlPrefix;
 private String xsdPrefix;
 
 private Namespace xmlNamespace;
 private Namespace xsdNamespace;
    
 @Override
 public boolean validate(File siardDatei) throws ValidationEcolumnException {
  
  //Test parameter. True if all validations passed successfully
  boolean valid = true;
  
  try {
   
   //Initialize the validation context
   prepareColumnValidation(siardDatei);
  
   //Validates the number of the attributes
   if (validateAttributeCount() == false) {
    valid = false;
    getMessageService().logInfo(
                     getTextResourceService().getText(MESSAGE_MODULE_E) +
                     getTextResourceService().getText(MESSAGE_DASHES) +
                     getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_COUNT));
   }
  
   //Validates the nullable property in metadata.xml
   if (validateAttributeOccurrence() == false) {
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
   if (validateAttributeType() == false) {
    valid = false;
    getMessageService().logInfo(
                     getTextResourceService().getText(MESSAGE_MODULE_E) +
                     getTextResourceService().getText(MESSAGE_DASHES) +
                     getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_TYPE));
   }
  
  } catch (JDOMException jde) {
   valid = false;
   getMessageService().logError(
                    getTextResourceService().getText(MESSAGE_MODULE_E) +
                    getTextResourceService().getText(MESSAGE_DASHES) +
                    "JDOMException " +
                    jde.getMessage());
   
  } catch (IOException ioe) {
   valid = false;
   getMessageService().logError(
                    getTextResourceService().getText(MESSAGE_MODULE_E) +
                    getTextResourceService().getText(MESSAGE_DASHES) +
                    "IOException " +
                    ioe.getMessage());
   
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
 
 private void prepareColumnValidation(File siardDatei)
   throws JDOMException, IOException {
       
     try {
      
      //Assigning properties to the validation context
           this.setValidationProperties(this.initializeProperties());
           
           //Assigning package internal paths
           this.initializePath(this.getValidationProperties());
           
           //Assigning JDOM documents for all involved XML files to the validation context
           this.prepareXMLAccess(this.getValidationProperties());
           
           //Assigning extracted SIARD content to the validation context
           this.setSiardFiles(this.extractSiardArchive(siardDatei));
                      
           //Assigning metadata.xml to the validation context
            this.setMetadataXML(this.pickMetadataXML(this.getValidationProperties()));
           
            //Assigning JDOM Documents for all XML files in SIARD archive to the validation context
            this.buildDocuments(this.getMetadataXML(), this.getValidationProperties());
           
                      
     } catch (Exception e) {
      System.out.println(e.getMessage());
     }
 }
 
 //Validation methods
 private boolean validateAttributeCount() throws JDOMException, IOException {
  return false;
 }
 
 private boolean validateAttributeOccurrence() throws JDOMException, IOException {
  return false;
 }
 
 private boolean validateAttributeSequence() throws JDOMException, IOException {
  return false;
 }
 
 private boolean validateAttributeType() throws JDOMException, IOException {
  return false;
 }
 
 private Properties initializeProperties() throws IOException {
  
  //Initializing the validation context properties
     String propertiesName = "/validation.properties";
     InputStream propertiesInputStream = getClass().getResourceAsStream(propertiesName);
     
     Properties properties = new Properties();
  properties.load(propertiesInputStream);
  
  return properties;
 }
 
 private void initializePath(Properties properties) {
   
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
  
 }
 
 private void prepareXMLAccess(Properties properties) {
   
  //Setting the namespaces to access metadata.xml and the different table.xsd
        /*Namespace xmlNamespace = Namespace.getNamespace(xmlPrefix, namespaceURI);
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
        this.setXsdPrefix(xsdPrefix);*/
       
 }
 
 //Helper methods
 private HashMap<String, File> extractSiardArchive(File packedSiardArchive)
   throws FileNotFoundException, IOException {
      
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
       
        return extractedSiardFiles;
 }
 
 private File pickMetadataXML (Properties properties) {
  
  HashMap<String, File> siardFiles = this.getSiardFiles();
  String pathToMetadataXML = this.getConfigurationService().getPathToWorkDir();
  
  pathToMetadataXML = pathToMetadataXML+properties.getProperty("siard.description");
       File metadataXML = siardFiles.get(pathToMetadataXML);
       
  return metadataXML;
  
 }
 
 private HashMap<String, Document> buildDocuments(File metadataXML, Properties properties)
   throws JDOMException, FileNotFoundException, IOException,JaxenException {
  
  //Initialize the empty HashMap
  HashMap<String, Document> siardDocuments = new HashMap<String, Document>();
  
  
  
  		//Create JDOMDocument of metadata.XML
  		InputStream inputStream = new FileInputStream(metadataXML);
  		SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(inputStream);
       
        //Add JDOMDocument of metadata.xml to the resulting HashMap
        //siardDocuments.put(metadataXML.getPath(), document);
       
        //Setting the namespace URI to the validation Context
        //Element rootElement = document.getRootElement();
        //String namespaceURI = rootElement.getNamespaceURI();
               
        //Assigning the XML namespace to the validation context
        //this.setNamespaceURI(namespaceURI);
       
        //Build JDOM Documents for all referenced table.xsd schemata
        String pathToSchemaElements = properties.getProperty("xpath.to.siard.schemas");
        
      	JDOMXPath xpath = new JDOMXPath(pathToSchemaElements);
		
        /*PathFactoryctory = XPathFactory.instance();
        XPathExpression xPathExpression =*/ 
      
        //XPathExpression<Element> xpath =
        	    //XPathFactory.instance().compile(pathToSchemaElements, Filters.element());
        	/*List<Element> elements = xpath.evaluate(document);
        	for (Element emt : elements) {
        	    System.out.println("XPath has result: " + emt.getName());
        	}*/
        
        //XPathFactory xPathFactory = XPathFactory.newInstance();
		//XPath xPath = xPathFactory.newXPath();
		//xPath.setNamespaceContext(new SiardNamespaceContext());
		//XPathExpression expression = xPath.compile(pathToSchemaElements);*/
		
		/*InputSource inputSource = new InputSource(new FileInputStream(metadataXML));
		
		NodeList siardSchemaElements = (NodeList) xPath.evaluate(pathToSchemaElements, inputSource, XPathConstants.NODESET);*/
		
        
  
  //@SuppressWarnings("unchecked")
  //List<Element> siardSchemaElements = ((List<Element>) xpathToSchemaElements.selectNodes(document));
        //Path to
  String filePathPrefix = this.getConfigurationService().getPathToWorkDir();
  
  
  //Iterating through the result set to retrieve all table.xsd files
        /*for (Node siardSchemaElement : siardSchemaElements) {
         
         Element siardSchemaFolderElement = siardSchemaElement.
           getChild(properties.getProperty("siard.metadata.xml.schema.folder.name"));
         
         Element siardTablesElement = siardSchemaFolderElement.getChild("siard.metadata.xml.tables.name");
         
         List<Element> siardTableElements = siardTablesElement.getChildren("siard.metadata.xml.table.name");
         
         StringBuilder schemaFolderPath = new StringBuilder();
         schemaFolderPath.append(this.getContentPath());
         schemaFolderPath.append(File.pathSeparator);
         schemaFolderPath.append(siardSchemaFolderElement.getValue());
         
         for (Element siardTableElement : siardTableElements) {
          
          Element siardTableFolderElement = siardTableElement.getChild("siard.metadata.xml.table.folder.name");
          
          StringBuilder tableFolderPath = new StringBuilder();
          tableFolderPath.append(schemaFolderPath);
          tableFolderPath.append(File.pathSeparator);
          tableFolderPath.append(siardTableFolderElement.getValue());
          tableFolderPath.append(File.pathSeparator);
          tableFolderPath.append(properties.getProperty("siard.metadata.xml.table.schema.extension"));
          
          String tableXMLSchemaFilePath = tableFolderPath.toString();
          
          File tableXMLSchemaFile = this.getSiardFiles().get(tableXMLSchemaFilePath);
          InputStream tableXMLSchemaInputStream = new FileInputStream(tableXMLSchemaFile);
          Document tableXMLSchemaDocument = builder.build(tableXMLSchemaInputStream);
          
          siardDocuments.put(tableXMLSchemaFilePath, tableXMLSchemaDocument);
         }
        }*/
  return null;
 }
 
 /*private class SiardNamespaceContext implements NamespaceContext
 {
 	private static final String SIARD_URI = "http://www.bar.admin.ch/xmlns/siard/1.0/metadata.xsd";

 	private static final String DB_URI = "http://db.apache.org/torque/4.0/templates/database";

 	@Override
		public String getNamespaceURI(String prefix) 
		{
			if (prefix == null)
			{
				throw new NullPointerException("Null prefix is not allowed");
			}
			if ("siard".equals(prefix)) {
				return SIARD_URI;
			}
			if ("db".equals(prefix))
			{
				return DB_URI;
			}
			return XMLConstants.XML_NS_URI;
		}

		@Override
		public String getPrefix(String uri)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<String> getPrefixes(String uri) 
		{
			throw new UnsupportedOperationException();
		}
 	
 }*/
 
 //Setter and getter methods
 public Properties getValidationProperties() {
  return validationProperties;
 }
 
 public void setValidationProperties(Properties validationProperties) {
  this.validationProperties = validationProperties;
 }
 
 public ConfigurationService getConfigurationService() {
     return configurationService;
 }
 
 public void setConfigurationService(ConfigurationService configurationService) {
     this.configurationService = configurationService;
 }
 
 public String getNamespaceURI() {
  return namespaceURI;
 }
 
 public void setNamespaceURI(String namespaceURI) {
  this.namespaceURI = namespaceURI;
 }
 
 public String getXmlPrefix() {
  return xmlPrefix;
 }
 
 public void setXmlPrefix(String xmlPrefix) {
  this.xmlPrefix = xmlPrefix;
 }
 
 public String getXsdPrefix() {
  return xsdPrefix;
 }
 
 public void setXsdPrefix(String xsdPrefix) {
  this.xsdPrefix = xsdPrefix;
 }
 
 public Namespace getXmlNamespace() {
  return xmlNamespace;
 }
 
 public void setXmlNamespace(Namespace xmlNamespace) {
  this.xmlNamespace = xmlNamespace;
 }
 
 public Namespace getXsdNamespace() {
  return xsdNamespace;
 }
 
 public void setXsdNamespace(Namespace xsdNamespace) {
  this.xsdNamespace = xsdNamespace;
 }
 
 public List<Element> getXmlElements() {
  return xmlElements;
 }
 
 public void setXmlElements(List<Element> xmlElements) {
  this.xmlElements = xmlElements;
 }
 
 public List<Element> getXsdElements() {
  return xsdElements;
 }
 
 public void setXsdElements(List<Element> xsdElements) {
  this.xsdElements = xsdElements;
 }
 
 public List<Element> getSiardSchemas() {
  return siardSchemas;
 }
 
 public void setSiardSchemas(List<Element> siardSchemas) {
  this.siardSchemas = siardSchemas;
 }
 
 public HashMap<String, File> getSiardFiles() {
  return siardFiles;
 }
 
 public void setSiardFiles(HashMap<String, File> siardFiles) {
  this.siardFiles = siardFiles;
 }
 
 public HashMap<String, Document> getSiardDocuments() {
  return siardDocuments;
 }
 
 public void setSiardDocuments(HashMap<String, Document> siardDocuments) {
  this.siardDocuments = siardDocuments;
 }
 
 public File getMetadataXML() {
  return metadataXML;
 }
 
 public void setMetadataXML(File metadataXML) {
  this.metadataXML = metadataXML;
 }
 
 public String getContentPath() {
  return contentPath;
 }
 
 public void setContentPath(String contentPath) {
  this.contentPath = contentPath;
 }
 
 public String getHeaderPath() {
  return headerPath;
 }
 
 public void setHeaderPath(String headerPath) {
  this.headerPath = headerPath;
 }
 
}

//-----------------

/* package column.validation.impl.bean;

import java.util.List;

import org.jdom.Element;

public class Table {
	
	private String name;
	private List<Element> columns;
	private List<Element> columnsReference;
	
	public List<Element> getColumns() {
		return columns;
	}
	public void setColumns(List<Element> columns) {
		this.columns = columns;
	}
	
	public List<Element> getColumnsReference() {
		return columnsReference;
	}
	public void setColumnsReference(List<Element> columnsReference) {
		this.columnsReference = columnsReference;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}

package column.validation.impl.bean;

public class Column {
	
	private String name;
	private String type;
	private String typeOriginal;
	private String nullable;
	public String getNullable() {
		return nullable;
	}
	public void setNullable(String nullable) {
		this.nullable = nullable;
	}
	public String getTypeOriginal() {
		return typeOriginal;
	}
	public void setTypeOriginal(String typeOriginal) {
		this.typeOriginal = typeOriginal;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	

}

package column.validation.impl.plugin;

import java.util.List;
import java.util.Properties;

import column.validation.api.ValidationPlugin;
import column.validation.impl.bean.Table;
import column.validation.impl.context.ValidationContext;

public class AttributeCountValidator implements ValidationPlugin {
			
	private ValidationContext validationContext;
	private StringBuffer report;
	private Boolean passed;
	
	public AttributeCountValidator() {
		this.setPassed(true);
	}
	
	@Override
	public void execute() {
		
		ValidationContext context = this.getValidationContext();
		List<Table> tables = context.getTables();
		StringBuffer validationReport = new StringBuffer();
		Properties properties = context.getValidationProperties();
		
		validationReport.append(properties.getProperty("attribute.count.validator.title"));
		validationReport.append('\n');
		validationReport.append('\n');
		
		for (Table table : tables) {
			
			int columnCount = table.getColumns().size();
			int columnReferenceCount = table.getColumnsReference().size();
			
			validationReport.append(table.getName() + ": ");
			validationReport.append(columnCount);
			validationReport.append(",");
			validationReport.append(columnReferenceCount);
					
			if (columnCount == columnReferenceCount) {
				validationReport.append(properties.getProperty("attribute.count.validator.passed"));
			} else {
				this.setPassed(false);
				validationReport.append(properties.getProperty("attribute.count.validator.failed"));
			}
			validationReport.append('\n');
		}
		
		if (getPassed() == true) {
			validationReport.append('\n');
			validationReport.append(properties.getProperty("attribute.count.validator.test.passed"));
			validationReport.append('\n');
	    } else {
	    	validationReport.append('\n');
			validationReport.append(properties.getProperty("attribute.count.validator.test.failed"));
			validationReport.append('\n');
		}
		validationReport.append(properties.getProperty("attribute.count.validator.end"));
		validationReport.append('\n');
		this.setReport(validationReport);
	}

	@Override
	public void loadValidationContext(ValidationContext validationContext) {
		setValidationContext(validationContext);	
	}

	private ValidationContext getValidationContext() {
		return validationContext;
	}

	private void setValidationContext(ValidationContext validationContext) {
		this.validationContext = validationContext;
	}

	public StringBuffer getReport() {
		return report;
	}

	public void setReport(StringBuffer report) {
		this.report = report;
	}

	public Boolean getPassed() {
		return passed;
	}

	public void setPassed(Boolean passed) {
		this.passed = passed;
	}

}


package column.validation.impl.plugin;

import java.util.List;
import java.util.Properties;

import org.jdom.Element;

import column.validation.api.ValidationPlugin;
import column.validation.impl.bean.Table;
import column.validation.impl.context.ValidationContext;

public class AttributeOccurenceValidator implements ValidationPlugin {
	
	private ValidationContext validationContext;
	private StringBuffer report;
	private Boolean passed;
	
	public AttributeOccurenceValidator() {
		this.setPassed(true);
	}
	
	@Override
	public void execute() {
		
		ValidationContext context = this.getValidationContext();
		List<Table> tables = context.getTables();
		StringBuffer validationReport = new StringBuffer();
		Properties properties = context.getValidationProperties();
		
		validationReport.append(properties.getProperty("attribute.occurrence.validator.title"));
		validationReport.append('\n');
		validationReport.append('\n');
		
		for (Table table : tables) {
			
			validationReport.append(table.getName());
			validationReport.append('\n');
			
			List<Element> xmlElements = table.getColumns();
			List<Element> xsdElements = table.getColumnsReference();
			
			if (xmlElements.size() == xsdElements.size()) {
			
				int i = 0;
						
				while ( i < xmlElements.size()) {
				
					Element xmlElement = xmlElements.get(i);
					Element xsdElement = xsdElements.get(i);
				
					//Check the nullable Element
					String leftSide = xmlElement.getChild("nullable", context.getXmlNamespace()).getValue();
				
					//Check the minOccurs Attribute
					String rightSide = xsdElement.getAttributeValue("minOccurs");
				
					String elementName = xmlElement.getChild("name", context.getXmlNamespace()).getValue();
				
					validationReport.append(elementName);
					validationReport.append(": ");
					validationReport.append(properties.getProperty("attribute.occurrence.validator.nullable"));
					validationReport.append(leftSide);
					validationReport.append(", ");
					validationReport.append(properties.getProperty("attribute.occurrence.validator.min.occurs"));
					validationReport.append(rightSide);
				
					try {
						if (leftSide.equalsIgnoreCase("true") && rightSide == null) {
							this.setPassed(false);
							validationReport.append(properties.getProperty("attribute.occurrence.validator.failed"));
						} else if (leftSide.equalsIgnoreCase("true") && rightSide.equalsIgnoreCase("0")) {
							validationReport.append(properties.getProperty("attribute.occurrence.validator.passed"));
						} else if (leftSide.equalsIgnoreCase("false") && rightSide == null) {
							validationReport.append(properties.getProperty("attribute.occurrence.validator.passed"));
						} else {
							this.setPassed(false);
							validationReport.append(properties.getProperty("attribute.occurrence.validator.failed"));
						}
						validationReport.append('\n');
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						i = i + 1;
					}
				}
			} else {
				this.setPassed(false);
			}
		}
		
		if (getPassed() == true) {
			validationReport.append('\n');
			validationReport.append(properties.getProperty("attribute.occurrence.validator.test.passed"));
			validationReport.append('\n');
	    } else {
	    	validationReport.append('\n');
			validationReport.append(properties.getProperty("attribute.occurrence.validator.test.failed"));
			validationReport.append('\n');
		}
		
		validationReport.append(properties.getProperty("attribute.occurrence.validator.end"));
		validationReport.append('\n');
		
		this.setReport(validationReport);
	}

	@Override
	public void loadValidationContext(ValidationContext validationContext) {
		setValidationContext(validationContext);
	}

	public ValidationContext getValidationContext() {
		return validationContext;
	}

	public void setValidationContext(ValidationContext validationContext) {
		this.validationContext = validationContext;
	}

	@Override
	public StringBuffer getReport() {
		return this.report;
	}

	public void setReport(StringBuffer report) {
		this.report = report;
	}

	public Boolean getPassed() {
		return passed;
	}

	public void setPassed(Boolean passed) {
		this.passed = passed;
	}

}

package column.validation.impl.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jdom.Element;

import column.validation.api.ValidationPlugin;
import column.validation.impl.bean.Table;
import column.validation.impl.context.ValidationContext;

public class AttributeSequenceValidator implements ValidationPlugin {
	
	private ValidationContext validationContext;
	private StringBuffer report;
	private Boolean passed;
	
	public AttributeSequenceValidator() {
		this.setPassed(true);
	}
	
	@Override
	public void execute() {
		
		ValidationContext context = this.getValidationContext();
		List<Table> tables = context.getTables();
		StringBuffer validationReport = new StringBuffer();
		Properties properties = context.getValidationProperties();
		
		validationReport.append(properties.getProperty("attribute.sequence.validator.title"));
		validationReport.append('\n');
		validationReport.append('\n');
		
		for (Table table : tables) {
			
			validationReport.append(table.getName());
			validationReport.append('\n');
			
			List<Element> xmlElements = table.getColumns();
			List<Element> xsdElements = table.getColumnsReference();
			
			List<String> xmlTypeSequence = new ArrayList<String>();
			List<String> xsdTypeSequence = new ArrayList<String>();
			
			if (xmlElements.size() == xsdElements.size()) {
			
				int i = 0;
			
				while ( i < xmlElements.size()) {
				
					Element xmlElement = xmlElements.get(i);
					Element xsdElement = xsdElements.get(i);
				
					String leftSide = xmlElement.getChild("type", context.getXmlNamespace()).getValue();
					String rightSide = xsdElement.getAttributeValue("type");
				
					//String elementName = xmlElement.getChild("name", context.getXmlNamespace()).getValue();
					String delimiter = properties.getProperty("attribute.sequence.validator.original.type.delimiter");
					String trimmedExpectedType = trimLeftSideType(leftSide, delimiter);
				
					//String expectedType = properties.getProperty(trimmedExpectedType);
				
					xmlTypeSequence.add(properties.getProperty(trimmedExpectedType));
					xsdTypeSequence.add(rightSide);
				
					validationReport.append(properties.getProperty(trimmedExpectedType));
					validationReport.append(": ");
					validationReport.append(rightSide);
				
					i = i + 1;
				
					if (properties.getProperty(trimmedExpectedType).equalsIgnoreCase(rightSide)) {
						validationReport.append(properties.getProperty("attribute.sequence.validator.passed"));
					} else {
						this.setPassed(false);
						validationReport.append(properties.getProperty("attribute.sequence.validator.failed"));
					}
					validationReport.append('\n');
				}
				validationReport.append('\n');
			} else {
				this.setPassed(false);
			}
		}
		if (this.getPassed() == true) {
			validationReport.append(properties.getProperty("attribute.sequence.validator.test.passed"));
		} else {
			validationReport.append(properties.getProperty("attribute.sequence.validator.test.failed"));
		}
		validationReport.append('\n');
		validationReport.append('\n');
		this.setReport(validationReport);
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

	@Override
	public void loadValidationContext(ValidationContext validationContext) {
		setValidationContext(validationContext);	
	}

	public ValidationContext getValidationContext() {
		return validationContext;
	}

	public void setValidationContext(ValidationContext validationContext) {
		this.validationContext = validationContext;
	}

	@Override
	public StringBuffer getReport() {
		return this.report;
	}

	public Boolean getPassed() {
		return passed;
	}

	public void setPassed(Boolean passed) {
		this.passed = passed;
	}

	public void setReport(StringBuffer report) {
		this.report = report;
	}

}

package column.validation.impl.plugin;

import java.util.List;
import java.util.Properties;

import org.jdom.Element;

import column.validation.api.ValidationPlugin;
import column.validation.impl.bean.Table;
import column.validation.impl.context.ValidationContext;

public class AttributeTypeValidator implements ValidationPlugin {
	
	private ValidationContext validationContext;
	private StringBuffer report;
	private Boolean passed;
	
	public AttributeTypeValidator() {
		this.setPassed(true);
	}
	
	@Override
	public void execute() {
		
		ValidationContext context = this.getValidationContext();
		List<Table> tables = context.getTables();
		StringBuffer validationReport = new StringBuffer();
		Properties properties = context.getValidationProperties();
		
		validationReport.append(properties.getProperty("attribute.type.validator.title"));
		validationReport.append('\n');
		validationReport.append('\n');
		
		for (Table table : tables) {
			
			validationReport.append(table.getName());
			validationReport.append('\n');
			
			List<Element> xmlElements = table.getColumns();
			List<Element> xsdElements = table.getColumnsReference();
			
			if (xmlElements.size() == xsdElements.size()) {
			
				int i = 0;
			
				while ( i < xmlElements.size()) {
				
					Element xmlElement = xmlElements.get(i);
					Element xsdElement = xsdElements.get(i);
				
					//Check the nullable Element
					String leftSide = xmlElement.getChild("type", context.getXmlNamespace()).getValue();
				
					//Check the minOccurs Attribute
					String rightSide = xsdElement.getAttributeValue("type");
				
					String elementName = xmlElement.getChild("name", context.getXmlNamespace()).getValue();
				
					String delimiter = properties.getProperty("attribute.type.validator.original.type.delimiter");
				
					String trimmedExpectedType = trimLeftSideType(leftSide, delimiter);
					String expectedType = properties.getProperty(trimmedExpectedType);
				
					validationReport.append(elementName);
					validationReport.append(": ");
					validationReport.append(" ");
					validationReport.append(leftSide);
					validationReport.append(", ");
					validationReport.append(properties.getProperty("attribute.type.validator.expected.type"));
					validationReport.append(": ");
					validationReport.append(expectedType);
					validationReport.append(", ");
					validationReport.append(properties.getProperty("attribute.type.validator.defined.type"));
					validationReport.append(": ");
					validationReport.append(rightSide);
					validationReport.append(". ");
				
					if (expectedType.equalsIgnoreCase(rightSide)) {
						validationReport.append(properties.getProperty("attribute.type.validator.passed"));
					} else {
						this.setPassed(false);
						validationReport.append(properties.getProperty("attribute.type.validator.failed"));
					}
					i = i + 1;
					validationReport.append('\n');
				}
			} else {
				this.setPassed(false);
			}
		}
		
		if (getPassed() == true) {
			validationReport.append('\n');
			validationReport.append(properties.getProperty("attribute.type.validator.test.passed"));
			validationReport.append('\n');
	    } else {
	    	validationReport.append('\n');
			validationReport.append(properties.getProperty("attribute.type.validator.test.failed"));
			validationReport.append('\n');
		}
		
		validationReport.append(properties.getProperty("attribute.type.validator.end"));
		validationReport.append('\n');
		
		this.setReport(validationReport);
		
		
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
	
	
	@Override
	public void loadValidationContext(ValidationContext validationContext) {
		setValidationContext(validationContext);
	}

	public ValidationContext getValidationContext() {
		return validationContext;
	}

	public void setValidationContext(ValidationContext validationContext) {
		this.validationContext = validationContext;
	}

	@Override
	public StringBuffer getReport() {
		return this.report;
	}

	public void setReport(StringBuffer report) {
		this.report = report;
	}

	public Boolean getPassed() {
		return passed;
	}

	public void setPassed(Boolean passed) {
		this.passed = passed;
	}

}

package column.validation.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import column.validation.api.PreValidator;
import column.validation.api.ValidationPlugin;
import column.validation.impl.bean.Table;
import column.validation.impl.context.ValidationContext;

public class PreColumnValidator implements PreValidator {
	
	private ValidationContext validationContext;

	@Override
	public void loadValidationContext(String entryPath) {
		
		ValidationContext context = new ValidationContext(entryPath);
		Properties properties = context.getValidationProperties();
		
		Document columns = loadXMLRessource(context.getMetadataXML());
		
		Element columnsRootElement = columns.getRootElement();
		
		String namespaceURI = columnsRootElement.getNamespaceURI();
				
		String xmlPrefix = properties.getProperty("metadata.xml.prefix");
		String xsdPrefix = properties.getProperty("table.xsd.prefix");
		
		Namespace xmlNamespace = createNamespace(xmlPrefix, namespaceURI);
		Namespace xsdNamespace = createNamespace(xsdPrefix, namespaceURI);
		
		context.setNamespaceURI(namespaceURI);
		
		context.setXmlPrefix(xmlPrefix);
		context.setXsdPrefix(xsdPrefix);
		
		context.setXmlNamespace(xmlNamespace);
		context.setXsdNamespace(xsdNamespace);
		
		context.setColumns(columns);
		
		this.setValidationContext(context);
	}

	@Override
	//Extracts the tables an the according XSD schema an stores them to the Validation Context
	public void loadValidationSource() {
		
		ValidationContext context = getValidationContext();
		Namespace xmlNamespace = context.getXmlNamespace();
		Properties properties = context.getValidationProperties();
		
		extractDatabaseSchemata();
		
		List<Element> schemaElements = context.getDatabaseSchemata();
		List<Table> tables = new ArrayList<Table>();
		
		//Load <schema> Elements
		for (Element schemaElement : schemaElements) {
			
			StringBuffer schemaFolder = new StringBuffer();
			
			schemaFolder.append(context.getPathToContent());
			schemaFolder.append("/");
			schemaFolder.append(schemaElement.getChild("folder", xmlNamespace).getValue());
						
			//Load <tables> Element
			Element tablesElement = schemaElement.getChild("tables", xmlNamespace);
			List<Element> tableElements = tablesElement.getChildren("table", xmlNamespace);
				
			//Load <table> Elements
			for (Element tableElement : tableElements) {
				
				
				
				//Load XML Elements
				StringBuffer tableFolder = new StringBuffer();
					
				tableFolder.append(schemaFolder.toString());
				tableFolder.append("/");
				tableFolder.append(tableElement.getChild("folder", xmlNamespace).getValue());
				tableFolder.append("/");
				tableFolder.append(tableElement.getChild("folder", xmlNamespace).getValue());
				tableFolder.append(".xsd");
					
				Document columnSchema = loadXMLRessource(new File(tableFolder.toString()));
					
				//Load XSD Elements
				String xpath = properties.getProperty("xpath.to.schema");
				String xsdPrefix = context.getXsdPrefix();
				
				//NamespaceURI of each XSD Schema. Therefore it is not loaded from the Validation
				//Context but extracted from the XSD schemata
				String namespaceURI = columnSchema.getRootElement().getNamespaceURI();
			    
				Element columnsElement = tableElement.getChild("columns", xmlNamespace);
				List<Element> columns = columnsElement.getChildren("column", xmlNamespace);
						
				List<Element> xsdElements = loadXMLElements(xpath, xsdPrefix, namespaceURI, columnSchema);
					
				Table table = new Table();
				
				table.setName(tableElement.getChild("folder", xmlNamespace).getValue());
				table.setColumns(columns);
				table.setColumnsReference(xsdElements);
				
				tables.add(table);							
			}
		}
		context.setTables(tables);
		this.setValidationContext(context);
	}

	@Override
	public void loadValidationPlugins(List<ValidationPlugin> plugins) {
		
		ValidationContext context = getValidationContext();
		context.setValidationPlugins(plugins);
		
	}
	
	//Private Methods
	//Generates JDOM Document from File
	private Document loadXMLRessource(File file) {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(file);
			return document;
		} catch (JDOMException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}		
	}
	
	private Namespace createNamespace(String prefix, String namespaceURI) {
		Namespace namespace = Namespace.getNamespace(prefix, namespaceURI);
		return namespace;
	}
	
	private List<Element> loadXMLElements(String xpathString, 
			String prefix, String namespaceURI, Document document) {
		
		try {
			
			JDOMXPath xpath = new JDOMXPath(xpathString);
			
			SimpleNamespaceContext simpleNamespaceContext = new SimpleNamespaceContext();
			simpleNamespaceContext.addNamespace(prefix, namespaceURI);
			
			xpath.setNamespaceContext(simpleNamespaceContext);
			List<Element> elements = ((List<Element>) xpath.selectNodes(document));
			
			return elements;
			
		} catch (JaxenException e) {
			e.printStackTrace();
			return null;
		}
	}
	 
	//Provides Section of Metadata.xml which is to be validated and stores it to the Validation Context
	private void extractDatabaseSchemata() {
		
		ValidationContext context = getValidationContext();
		Properties properties = context.getValidationProperties();
		
		String prefix = context.getXmlPrefix();
		String xpath = properties.getProperty("xpath.to.data");
		
		String namespaceURI = context.getNamespaceURI();
		
		Document document = context.getColumns();
		
		context.setDatabaseSchemata(loadXMLElements(xpath, prefix, namespaceURI, document));
	}
	
	//Getter and Setter
	public ValidationContext getValidationContext() {
		return validationContext;
	}

	public void setValidationContext(ValidationContext validationContext) {
		this.validationContext = validationContext;
	}

}
*/

