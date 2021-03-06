/*
 * (C) Copyright KOST-CECO - All Rights Reserved
 *
 *	 SIARD-Val is a development of the KOST-CECO. All rights rest with the KOST-CECO. 
 * This application is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or (at your option) any later version. 
 * This application is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the follow GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; 
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
 *   Boston, MA 02110-1301 USA or see <http://www.gnu.org/licenses/>.
 *
 */

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

/**
 * The module <code>ValidationEcolumnModule</code> validates the columns 
 * specified in the file <code>metadata.xml</code> against the according
 * XML schema files.
 *
 * <p>
 * The column validation process consists of four steps. a) The validation
 * of the attribute count, b) the validation of the attribute's occurrence
 * - which conforms to the nullable attribute of the table definition - 
 * c) the validation of the attribute's type and d) the validation of the
 * attribute's sequence. The table columns are only valid if - and only if -
 * the four validation steps are completed successfully.
 * 
 * The table and their columns are described in the file <code>metadata.xml</code>
 * The element <code> &lt;table&gt</code> and its children are decisive for the
 * table description:
 *  <blockquote><pre>
 * <code>&lt;table&gt</code>
 * 	<code>&lt;name&gtgv_schaden&lt;/name&gt</code><br>
 * 		<code>&lt;folder&gtgv_schaden&lt;/folder&gt</code><br>
 * 		<code>&lt;description&gtgv_schaden&lt;/description&gt</code><br>
 * 		<code>&lt;columns&gt</code><br>
 * 			<code>&lt;column&gt</code><br>
 * 				<code>&lt;name&gtid&lt;/name&gt</code><br>
 * 				<code>&lt;type&gtid&lt;/type&gt</code><br>
 * 				<code>&lt;typeOriginal&gtid&lt;/typeOriginal&gt</code><br>
 * 				<code>&lt;nullable&gt</code><br>
 * 				<code>&lt;description&gt</code><br>
 * 			<code>&lt;/column&gt</code><br>
 * 		<code>&lt;/columns&gt</code><br>
 * <code>&lt;/table&gt</code><br>
 * </pre></blockquote
 *         
 *             
 *
 * @see          java.util.Locale
 * @see          Format
 * @see          NumberFormat
 * @see          DecimalFormat
 * @see          DecimalFormatSymbols
 * @see          ChoiceFormat
 * @see          DateFormat
 * @see          SimpleDateFormat
 *
 * @author       Olivier Debenath
 */

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
	
	/**
    * Entry point of the column validation. The <code>validate</code> 
    * method first initializes the validation context. Therefore the
    * method <code>prepareValidation</code> is executed prior to the
    * actual validation. The column validation itself is divided into
    * the validation of the attribute count, the validation of the
    * attribute occurrence, the validation of the attribute types and
     * the validation of the attribute's sequence
     * <a href="#patterns">class description</a>.
     *
     * @param SIARD archive containing the tables whose columns are to be
     * validated
     * @exception ValidationEcolumnException if the representation of the 
     * columns is invalid
     */
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
			 //Validates the nullable property in metadata.xml
			 if (validateAttributeOccurrence(siardTables, properties) == false) {
				valid = false;
					 getMessageService().logInfo(
	                 getTextResourceService().getText(MESSAGE_MODULE_E) +
	                 getTextResourceService().getText(MESSAGE_DASHES) +
	                 getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_OCCURRENCE));
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
			 /*
	         //Validates the sequence of table attributes in metadata.xml
			 if (validateAttributeSequence(properties) == false) {
				valid = false;
					getMessageService().logInfo(
	                getTextResourceService().getText(MESSAGE_MODULE_E) +
	                getTextResourceService().getText(MESSAGE_DASHES) +
	                getTextResourceService().getText(MESSAGE_MODULE_E_INVALID_ATTRIBUTE_SEQUENCE));
			 } else if (this.isVerboseMode()) {
				 getMessageService().logInfo(this.getValidationLog().toString());
			 }*/
			 
			 
		} catch (Exception je) {
			valid = false;
				getMessageService().logError(
				getTextResourceService().getText(MESSAGE_MODULE_E) +
	            getTextResourceService().getText(MESSAGE_DASHES) + je.getMessage());
		} finally {
			//System.out.println(this.getValidationLog().toString());
			//If the verbose mode flag is set, the validationLog is beeing flushed
			if (this.isVerboseMode()) {
				 getMessageService().logInfo(this.getValidationLog().toString());
			 }
			return valid;
		}
	}
	
	/* [E.0] */
	private boolean prepareValidation(File siardFile) throws IOException, JDOMException {
		StringBuilder validationLog = new StringBuilder();
		validationLog.append('\n');
		validationLog.append("============================");
		validationLog.append('\n');
		validationLog.append("SIARD VAL Module E Trace Log");
		validationLog.append('\n');
		validationLog.append("============================");
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
		} else {
			//Hard coded validationLog messages in case the properties file could not be accessed
			validationLog.append("The validation E could not be prepared");
			validationLog.append('\n');
			validationLog.append("propertiesLoaded: " + propertiesLoaded);
			validationLog.append('\n');
			validationLog.append("pathInitialized: " + pathInitialized);
			validationLog.append('\n');
			validationLog.append("xmlAccessPrepared: " + xmlAccessPrepared);
			validationLog.append('\n');
			validationLog.append("siardArchiveExtracted: " + siardArchiveExtracted);
			validationLog.append('\n');
			validationLog.append("metadataXMLpicked: " + metadataXMLpicked);
			validationLog.append('\n');
			validationLog.append("validationDataPrepared: " + validationDataPrepared);
			validationLog.append('\n');
		}
		return prepared;
	}
	
	/* [E.1] */
	private boolean validateAttributeCount(List<SiardTable> siardTables, 
			Properties properties) throws Exception {
		boolean validColumn = false;
		boolean validTable = true;
		final String ME = "[E.1] validateAttributeCount(List<SiardTable> siardTables, " +
				"Properties properties) ";
		String message = new String();
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
			//Checks whether the columns count is correct
			if (metadataXMLColumnsCount == tableXSDColumnsCount) {
				validColumn = true;
			} else {
				validColumn = false;
				validTable = false;
			}
			//Preparing validation log entry
			String validationLogSceleton = properties.
					getProperty("attribute.count.validator.log.entry");
			String validationLogEntry = MessageFormat.
					format(validationLogSceleton, 
						   siardTable.getTableName(), 
						   metadataXMLColumnsCount,
						   tableXSDColumnsCount,
						   siardTable.getTableName(),
						   validColumn);
			validationLog.append(validationLogEntry);
			validationLog.append(properties.getProperty("newline"));
		}
		if (validTable) {
			//Updating validation log
			message = properties.getProperty("successfully.executed");
		} else {
			message = properties.getProperty("failed");
		}
		String newLine = properties.getProperty("newline");
		validationLog.append(newLine);
		validationLog.append(ME + message);
		//Write the local validation log to the validation context
		//this.setValidationLog(validationLog);
		this.getValidationLog().append(validationLog);
		//Return the current validation state
		return validTable;
	}
	
	/* [E.2] */
	/**
     * Resolves instances being deserialized to the predefined constants.
     *
     * @throws InvalidObjectException if the constant could not be
     *         resolved.
     * @return resolved MessageFormat.Field constant
     */
	private boolean validateAttributeOccurrence(List<SiardTable> siardTables, 
			Properties properties) throws Exception {
		boolean validOccurrence = false;
		boolean valid = true;
		final String ME = "[E.2] validateAttributeOccurrence(List<SiardTable> siardTables, " +
				"Properties properties) ";
		//Initializing validation Logging
		StringBuilder validationLog = new StringBuilder();
		String message = new String();
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
						validOccurrence = false;
						valid = false;
					//If the nullable Element is set to true and the minOccurs attribute is set to zero
					} else if (nullable.equalsIgnoreCase("true") && minOccurs.equalsIgnoreCase("0")) {
						validOccurrence = true;
					//If the nullable Element is set to false and the minOccurs attribute is null
					} else if (nullable.equalsIgnoreCase("false") && minOccurs == null) {
						validOccurrence = true;
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
								   siardTable.getTableName(),
								   validOccurrence);
					validationLog.append(validationLogColumnEntry);
					validationLog.append(properties.getProperty("newline"));
				} 
			} else {
				valid = false;
				String errorMessage = properties.getProperty("attribute.occurrence.error.wrong.count");
				String validationStatus = properties.getProperty("attribute.occurrence.error.invalid");
				//Validation fails if allover number differs in metadata.xml and XML schemata
				validationLog.append(errorMessage);
				validationLog.append(validationStatus);
				validationLog.append(valid);
				validationLog.append('\n');
			}
		}
		if (valid) {
			//Updating vlidation log
			message = properties.getProperty("successfully.executed");
		} else {
			message = properties.getProperty("failed");
		}
		String newLine = properties.getProperty("newline");
		validationLog.append(newLine);
		validationLog.append(ME + message);
	    //Write the local validation log to the validation context
	    this.getValidationLog().append(validationLog);
	    //Return the current validation state
		return validOccurrence;
	}
	/* [E.3] */
	private boolean validateAttributeType(List<SiardTable> siardTables, 
			Properties properties) throws Exception {
		boolean validType = false;
		boolean valid = true;
		final String ME = "[E.3] validateAttributeType(List<SiardTable> siardTables, " +
				"Properties properties) ";
		//Initializing validation Logging
		StringBuilder validationLog = new StringBuilder();
		String message = new String();
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
					//In case the expected type does not exist
					if (expectedType == null) {
						expectedType = properties.getProperty("atribute.type.validator.unknown.type");
						validType = false;
						valid = false;
					}
					//Convey the column types for the all over sequence test [E.4] 
					xmlElementSequence.add(expectedType);
					xsdElementSequence.add(rightSide);
					//Verify, whether the column type in XML is equal to the one in XSD
					if (expectedType.equalsIgnoreCase(rightSide)) {
						validType = true;
					} else {
						validType = false;
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
								   siardTable.getTableName(),
								   expectedType,
								   siardTable.getTableName(),
								   rightSide,
								   validType);
					validationLog.append(validationLogColumnEntry);
					validationLog.append(properties.getProperty("newline"));
				}
			//If the numbers of columns differs between metadata.xml and according XML schema
			} else {
				valid = false;
				String errorMessage = properties.getProperty("attribute.type.error.wrong.count");
				String validationStatus = properties.getProperty("attribute.type.error.invalid");
				//Validation fails if allover number differs in metadata.xml and XML schemata
				validationLog.append(errorMessage);
				validationLog.append(validationStatus);
				validationLog.append(valid);
				validationLog.append('\n');
			}
		}
		//Save the allover column elements for [E.4]
		this.setXmlElementsSequence(xmlElementSequence);
		this.setXsdElementsSequence(xsdElementSequence);
		//Writes back validatable XML elements
		if (this.getXmlElementsSequence() != null && 
			this.getXsdElementsSequence() != null && 
			properties != null &&
			valid) {
			//Updating validation log
			message = properties.getProperty("successfully.executed");
			validType = true;
		} else {
			message = properties.getProperty("failed");
			validType = false;
		}
		String newLine = properties.getProperty("newline");
		validationLog.append(newLine);
		validationLog.append(ME + message);
		//Write the local validation log to the validation context
	    this.getValidationLog().append(validationLog);
	    //Return the current validation state
		return validType;
	}
	/* [E.4] */
	private boolean validateAttributeSequence(Properties properties) 
			throws Exception {
		boolean valid = true;
		final String ME = "[E.4] validateAttributeSequence(Properties properties)";
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
			//Iterating over the total count of columns
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
		if (valid = true) {
			//Updating validation log
			String message = properties.getProperty("successfully.executed");
			String newLine = properties.getProperty("newline");
			validationLog.append(newLine);
			validationLog.append(ME + message);
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
		//Initializing validation Logging
		StringBuilder validationLog = new StringBuilder();
		//Get the properties file
		InputStream propertiesInputStream = getClass().getResourceAsStream(propertiesName);
		Properties properties = new Properties();
		properties.load(propertiesInputStream);
		this.setValidationProperties(properties);
		//Log messages are created inside the if clause to catch missing properties errors
		if (this.getValidationProperties() != null) {
			successfullyCommitted = true;
			//Set header line to validation log
			String headerLine = properties.getProperty("newline");
			validationLog.append(headerLine);
			String message = properties.getProperty("successfully.executed");
			validationLog.append(me + message);
		} else {
			//Missing properties file => hard coded messages
			String headerLine = "\n";
			validationLog.append(headerLine);
			String message = "has failed";
			validationLog.append(me + message);
		}
		this.getValidationLog().append(validationLog);
		return successfullyCommitted;
	}
	/* [E.0.2] */
	private boolean initializePath(Properties properties) {
		boolean successfullyCommitted = false;
		String me = "[E.0.2] initializePath(Properties properties) ";
		StringBuilder headerPath = new StringBuilder();
		StringBuilder contentPath = new StringBuilder();
		//Initializing validation Logging
	    StringBuilder validationLog = new StringBuilder();
		String workDir = this.getConfigurationService().getPathToWorkDir();
		//Preparing the internal SIARD directory structure
		headerPath.append(workDir);
		headerPath.append(File.separator);
		headerPath.append(properties.getProperty("header.suffix"));
		contentPath.append(workDir);
		contentPath.append(File.separator);
		contentPath.append(properties.getProperty("content.suffix"));
		//Writing back the directory structure to the validation context
		this.setHeaderPath(headerPath.toString());
		this.setContentPath(contentPath.toString());
		if (this.getHeaderPath() != null && 
			this.getContentPath() != null &&
			this.getValidationProperties() != null) {
			//Updating the validation log
			String message = properties.getProperty("successfully.executed");
			validationLog.append(me + message);
			successfullyCommitted = true;
		} else {
			String message = "has failed";
			validationLog.append(me + message);
		}
		this.getValidationLog().append(validationLog);
		return successfullyCommitted;
	} 
	/* [E.0.5] */
	private boolean prepareXMLAccess(Properties properties, File metadataXML) 
			throws JDOMException, IOException {
		boolean successfullyCommitted = false;
		String me = "[E.0.5] prepareXMLAccess(Properties properties, File metadataXML) ";
		//Initializing validation Logging
	    StringBuilder validationLog = new StringBuilder();
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
			 this.getMetadataXMLDocument() != null && 
			 this.getValidationProperties() != null) {
			 //Updating the validation log
			 String message = properties.getProperty("successfully.executed");
			 validationLog.append(me + message);
			 successfullyCommitted = true;
		} else {
			String message = "has failed";
			validationLog.append(me + message);
		}
		this.getValidationLog().append(validationLog);
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
    	//Initializing validation Logging
	    StringBuilder validationLog = new StringBuilder();
		//Initializing the access to the SIARD archive
    	Zip64File zipfile = new Zip64File(packedSiardArchive);
		List<FileEntry> fileEntryList = zipfile.getListFileEntries();
		String pathToWorkDir = getConfigurationService().getPathToWorkDir();
		File tmpDir = new File(pathToWorkDir);
		//Initializing the resulting Hashmap containing all files, indexed by its absolute path
		HashMap<String, File> extractedSiardFiles = new HashMap<String, File>();
		//Iterating over the whole SIARD archive
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
		//Checks whether the siard extraction succeeded or not
		if (this.getSiardFiles() != null) {
			//Upodating the validation log
			String message = properties.getProperty("successfully.executed");
			validationLog.append(me + message);
			sucessfullyCommitted = true;
		} else {
			String message = "has failed";
			validationLog.append(me + message);
		}
		this.getValidationLog().append(validationLog);
		return sucessfullyCommitted;
	}
    /* [E.0.4] */
	private boolean pickMetadataXML (Properties properties) {
		boolean successfullyCommitted = false;
		String me = "[E.0.4] pickMetadataXML (Properties properties) ";
		//Initializing validation Logging
	    StringBuilder validationLog = new StringBuilder();
		HashMap<String, File> siardFiles = this.getSiardFiles();
		String pathToMetadataXML = this.getConfigurationService().getPathToWorkDir();
		pathToMetadataXML = pathToMetadataXML+properties.getProperty("siard.description");
		File metadataXML = siardFiles.get(pathToMetadataXML);
		//Retreave the metadata.xml from the SIARD archive and writes it back to the validation context
		this.setMetadataXML(metadataXML);
		//Checks whether the metadata.xml could be picked up
		if (this.getMetadataXML() != null &&
			properties != null) {
			//Updating the validation log
			String message = properties.getProperty("successfully.executed");
			validationLog.append(me + message);
			successfullyCommitted = true;
		} else {
			String message = "has failed";
			validationLog.append(me + message);
		}
		this.getValidationLog().append(validationLog);
		return successfullyCommitted;
	}
	/* [E.0.6] */
	private boolean prepareValidationData (Properties properties, File metadataXML) 
			throws JDOMException, IOException {
		boolean successfullyCommitted = false;
		String me = "[E.0.6] prepareValidationData (Properties properties, File metadataXML) ";
		//Initializing validation Logging
	    StringBuilder validationLog = new StringBuilder();
		//Gets the tables to be validated
		List<SiardTable> siardTables = new ArrayList<SiardTable>();
		Document document = this.getMetadataXMLDocument();
        Element rootElement = document.getRootElement();
        String workingDirectory = this.getConfigurationService().getPathToWorkDir();     
        String siardSchemasElementsName = properties.getProperty("siard.metadata.xml.schemas.name");
        //Gets the list of <schemas> elements from metadata.xml
        List<Element> siardSchemasElements = rootElement.getChildren(siardSchemasElementsName, 
        		this.getXmlNamespace());
        for (Element siardSchemasElement : siardSchemasElements) {
           	//Gets the list of <schema> elements from metadata.xml
        	List<Element> siardSchemaElements = siardSchemasElement.getChildren(properties.
        			getProperty("siard.metadata.xml.schema.name"), this.getXmlNamespace());
        	//Iterating over all <schema> elements
        	for (Element siardSchemaElement : siardSchemaElements) {
           		String schemaFolderName = siardSchemaElement.getChild(properties.
        				getProperty("siard.metadata.xml.schema.folder.name"), this.getXmlNamespace()).getValue();
        		Element siardTablesElement = siardSchemaElement.getChild(properties.
        				getProperty("siard.metadata.xml.tables.name"), this.getXmlNamespace());
        		List<Element> siardTableElements = siardTablesElement.getChildren(properties.
        				getProperty("siard.metadata.xml.table.name"), this.getXmlNamespace());
        		//Iterating over all containing table elements
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
        		    //Preparing access to the according XML schema file
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
        		    //Retrieve the according XML schema
        		    File tableSchema = this.getSiardFiles().get(pathToTableSchema.toString());
           			SAXBuilder builder = new SAXBuilder();
           			Document tableSchemaDocument = builder.build(tableSchema);
           			Element tableSchemaRootElement = tableSchemaDocument.getRootElement();
        			Namespace namespace = tableSchemaRootElement.getNamespace();
        			//Getting the tags from XML schema to be validated
           			Element tableSchemaComplexType = tableSchemaRootElement.getChild(properties.
        					getProperty("siard.table.xsd.complexType"), namespace);
        			Element tableSchemaComplexTypeSequence = tableSchemaComplexType.getChild(properties.
        					getProperty("siard.table.xsd.sequence"), namespace);
        			List<Element> tableSchemaComplexTypeElements = tableSchemaComplexTypeSequence.getChildren(properties.
        					getProperty("siard.table.xsd.element"), namespace);
        			siardTable.setTableXSDElements(tableSchemaComplexTypeElements);
        			siardTables.add(siardTable);
                    //Writing back the List off all SIARD tables to the validation context
           			this.setSiardTables(siardTables);
        		}		
        	}
        }
        if (this.getSiardTables() != null &&
        	properties != null &&
        	metadataXML != null) {
        	//Updating the validation log
        	String message = properties.getProperty("successfully.executed");
        	String newline = properties.getProperty("newline");
			validationLog.append(me + message);
			validationLog.append(newline);
        	successfullyCommitted = true;
        } else {
        	String message = "has failed";
			validationLog.append(me + message);
			validationLog.append('\n');
        }
        this.getValidationLog().append(validationLog);
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
	/**
	 * @param siardTables the siardTables to set
	 */
	public Document getMetadataXMLDocument() {
		return metadataXMLDocument;
	}
	/**
	 * @param siardTables the siardTables to set
	 */
	public void setMetadataXMLDocument(Document metadataXMLDocument) {
		this.metadataXMLDocument = metadataXMLDocument;
	}
	/**
	 * @param siardTables the siardTables to set
	 */
	public StringBuilder getValidationLog() {
		return validationLog;
	}
	/**
	 * @param siardTables the siardTables to set
	 */
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


