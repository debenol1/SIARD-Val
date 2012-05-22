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
import org.jaxen.JaxenException;
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
	
	//Content of the SIARD package
	private HashMap<String, File> siardFiles;
	private HashMap<String, Document> siardDocuments;
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
	
	private void prepareColumnValidation(File siardDatei) 
			throws JDOMException, IOException, JaxenException {
		    	
    	try {
    		
    		//Assigning properties to the validation context
          	this.setValidationProperties(this.initializeProperties());
          	
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
	
	private Properties initializeProperties() throws IOException {
		
		//Initializing the validation context properties
    	String propertiesName = "/validation.properties";
    	InputStream propertiesInputStream = getClass().getResourceAsStream(propertiesName);
    	
    	Properties properties = new Properties();
		properties.load(propertiesInputStream);
		
		return properties;
	}
	
	private void prepareXMLAccess(Properties properties) {
			
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
			throws JDOMException, FileNotFoundException, IOException, JaxenException {
		
		//Initialize the empty HashMap
		HashMap<String, Document> siardDocuments = new HashMap<String, Document>();
		
		//Create JDOMDocument of metadata.XML
		InputStream inputStream = new FileInputStream(metadataXML);
		SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(inputStream);
        
        //Add JDOMDocument of metadata.xml to the resulting HashMap 
        siardDocuments.put(metadataXML.getPath(), document);
        
        //Setting the namespace URI to the validation Context
        Element rootElement = document.getRootElement();
        String namespaceURI = rootElement.getNamespaceURI();
                
        //Assigning the XML namespace to the validation context
        this.setNamespaceURI(namespaceURI);
        
        //Build JDOM Documents for all referenced table.xsd schemata
        String pathToSchemaElements = properties.getProperty("xpath.to.siard.schemas");
        
        JDOMXPath xpathToSchemaElements = new JDOMXPath(pathToSchemaElements);
        
        /*SimpleNamespaceContext simpleNamespaceContext = new SimpleNamespaceContext();
		simpleNamespaceContext.addNamespace(this.getXmlPrefix(), this.getNamespaceURI());
		
		xpathToSchemaElements.setNamespaceContext(simpleNamespaceContext);*/
		
        System.out.println(pathToSchemaElements.toString());
        
        //@SuppressWarnings("unchecked")
		/*List<Element> siardSchemas = ((List<Element>) xpathToSchemaElements.selectNodes(document));
        
        
        
        for (Element e : siardSchemas) {
        	System.out.println(e.getName());
        }*/
        
		return siardDocuments;
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

}
