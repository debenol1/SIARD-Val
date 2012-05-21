package ch.kostceco.tools.siardval.validation.module.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipFile;


import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.jdom.JDOMXPath;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;



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
	
	private List<Element> siardSchemas;
	private List<Element> xmlElements;
	private List<Element> xsdElements;
	
	//XML processing related properties
	private String pathToMetadataXML;
	
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
			
		} catch (JaxenException je) {
			valid = false;
			getMessageService().logError(
                    getTextResourceService().getText(MESSAGE_MODULE_E) + 
                    getTextResourceService().getText(MESSAGE_DASHES) + 
                    "JaxenException " + 
                    je.getMessage());
		}
		
		return valid;
	}
	
	private void prepareColumnValidation(File siardDatei) throws JDOMException, IOException, JaxenException {
		    	
    	try {
    		
    		//Initializing the validation context properties
        	String propertiesName = "/validation.properties";
        	InputStream propertiesInputStream = getClass().getResourceAsStream(propertiesName);
        	Properties properties = new Properties();
    		properties.load(propertiesInputStream);
    		
    		//Assigning properties to the validation context
          	this.setValidationProperties(properties);
    				
          	Zip64File zipfile = new Zip64File(siardDatei);
            List<FileEntry> fileEntryList = zipfile.getListFileEntries();
            
            String pathToWorkDir = getConfigurationService().getPathToWorkDir();
            File tmpDir = new File(pathToWorkDir);
            
            for (FileEntry fileEntry : fileEntryList) {
                
            	if (!fileEntry.isDirectory()) {
                    
                	byte[] buffer = new byte[8192];
                    
                	// Scheibe die Datei an den richtigen Ort respektive in den richtigen Ordner der ggf angelegt werden muss.
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
                    eis.close();
                    fos.close();
                    // Festhalten von metadata.xml und metadata.xsd
             
                }
            }
            
            
                      	                      
          	
          	
          	
          	//Building the path to metadata.xml in the SIARD directory
            /*String pathToWorkingDirectory = getConfigurationService().getPathToWorkDir();
    		String relativePathToMetadataXML = properties.getProperty("path.to.metadata.xml");
    		    		   		
    		StringBuilder stringBuilder = new StringBuilder();
    		stringBuilder.append(pathToWorkingDirectory);
    		stringBuilder.append("/");
    		stringBuilder.append(relativePathToMetadataXML);
    		
    		String absolutePathToMetadataXML = stringBuilder.toString();
    		
    		System.out.println(siardDatei.getAbsolutePath());
    		
    		
    		//Assigning the path to metadata.xml to the validation context
    		this.setPathToMetadataXML(absolutePathToMetadataXML);
    		
    		//Creating the JDOM2 Document from the metadata.xml
            File metadataXml = new File(absolutePathToMetadataXML);
            InputStream metadataXMLInputStream = new FileInputStream(metadataXml);
            
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(metadataXMLInputStream);
            
            //Setting the namespace URI
            Element rootElement = document.getRootElement();
            String namespaceURI = rootElement.getNamespaceURI();
            
            this.setNamespaceURI(namespaceURI);
            
            //Setting the XML prefix to access metadata.xml and the different table.xsd
            //The prefices are stored in validation.properties
            String xmlPrefix = properties.getProperty("metadata.xml.prefix");
            String xsdPrefix = properties.getProperty("table.xsd.prefix");
            
            this.setXmlPrefix(xmlPrefix);
            this.setXsdPrefix(xsdPrefix);
            
            //Setting the namespaces to access metadata.xml and the different table.xsd
            Namespace xmlNamespace = Namespace.getNamespace(xmlPrefix, namespaceURI);
            Namespace xsdNamespace = Namespace.getNamespace(xsdPrefix, namespaceURI);
            
            this.setXmlNamespace(xmlNamespace);
            this.setXsdNamespace(xsdNamespace);
            
            //Configuring xpath to get the <schema> elements of metadata.xml to the validation context
            String pathToSchemaElements = properties.getProperty("xpath.to.siard.schemas");
            JDOMXPath xpathToSchemaElements = new JDOMXPath(pathToSchemaElements);
            
            SimpleNamespaceContext simpleNamespaceContext = new SimpleNamespaceContext();
            simpleNamespaceContext.addNamespace(xmlPrefix, namespaceURI);
            
            xpathToSchemaElements.setNamespaceContext(simpleNamespaceContext);
            
            List<Element> siardSchemas = ((List<Element>) xpathToSchemaElements.selectNodes(document));
            
            for (Element e : siardSchemas) {
            	System.out.println(e.getName());
            }
            
            this.setSiardSchemas(siardSchemas);*/
                   	
    	} catch (Exception e) {
    		System.out.println(e.getMessage());
    	} finally {
    		//Cleaning up open streams
            //propertiesInputStream.close();
            //metadataXMLInputStream.close();
    	}
    	
	}
	
	
	private boolean validateAttributeCount() throws JDOMException, IOException, JaxenException {
		return false;
	}
	
	private boolean validateAttributeOccurrence() throws JDOMException, IOException, JaxenException {
		return false;
	}
	
	private boolean validateAttributeSequence() throws JDOMException, IOException, JaxenException {
		return false;
	}
	
	private boolean validateAttributeType() throws JDOMException, IOException, JaxenException {
		return false;
	}
	
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

	public String getPathToMetadataXML() {
		return pathToMetadataXML;
	}

	public void setPathToMetadataXML(String pathToMetadataXML) {
		this.pathToMetadataXML = pathToMetadataXML;
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

}
