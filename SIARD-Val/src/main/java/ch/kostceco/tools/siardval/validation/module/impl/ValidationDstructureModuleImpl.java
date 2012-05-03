/*== SIARD-Val ==================================================================================
The SIARD-Val application is used for validate SIARD-Files. 
Copyright (C) 2012 Claire Röthlisberger (KOST-CECO), Martin Kaiser (KOST-CECO), XYZ (xyz)
-----------------------------------------------------------------------------------------------
SIARD-Val is a development of the KOST-CECO. All rights rest with the KOST-CECO. 
This application is free software: you can redistribute it and/or modify it under the 
terms of the GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version. 
This application is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the follow GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program; 
if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
Boston, MA 02110-1301 USA or see <http://www.gnu.org/licenses/>.
==============================================================================================*/

package ch.kostceco.tools.siardval.validation.module.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import ch.kostceco.tools.siardval.exception.module.ValidationDstructureException;
import ch.kostceco.tools.siardval.service.ConfigurationService;
import ch.kostceco.tools.siardval.validation.ValidationModuleImpl;
import ch.kostceco.tools.siardval.validation.module.ValidationDstructureModule;

/**
 * Validierungsschritt D (Struktur-Validierung)
 * Stimmt die Struktur aus metadata.xml mit der Datei-Struktur von content überein? 
 * valid --> schema0/table3 in metadata.xml == schema0/table3/tabe3.xsd und table3.xml in content
 * ==> Bei den Module A, B, C und D wird die Validierung abgebrochen, sollte das Resulat invalid sein!
 * @author Ec Christian Eugster
 */

public class ValidationDstructureModuleImpl extends ValidationModuleImpl implements ValidationDstructureModule {

    public ConfigurationService configurationService;
    

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }


    @Override
    public boolean validate(File siardDatei) throws ValidationDstructureException 
    {
    	boolean valid = true;
        try {
        	/*
        	 * Extract the metadata.xml from the temporare work folder and build a jdom document
        	 */
            String pathToWorkDir = getConfigurationService().getPathToWorkDir();
            File metadataXml = new File(new StringBuilder(pathToWorkDir).append(File.separator).append("header").append(File.separator).append("metadata.xml").toString());
            InputStream fin = new FileInputStream(metadataXml);
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(fin);
            fin.close();

            /*
             * read the document and for each schema and table entry verify existence in temporary extracted structure
             */
        	Namespace ns = Namespace.getNamespace("http://www.bar.admin.ch/xmlns/siard/1.0/metadata.xsd");
        	// select schema elements and loop
        	List<Element> schemas = document.getRootElement().getChild("schemas", ns).getChildren("schema", ns);
        	for (Element schema : schemas)
        	{
        		Element schemaFolder = schema.getChild("folder", ns);
                File schemaPath = new File(new StringBuilder(pathToWorkDir).append(File.separator).append("content").append(File.separator).append(schemaFolder.getText()).toString());
        		if (schemaPath.isDirectory())
        		{
        			List<Element> tables = schema.getChild("tables", ns).getChildren("table", ns);
        			for (Element table : tables)
        			{
        				Element tableFolder = table.getChild("folder", ns);
                		File tablePath = new File(new StringBuilder(schemaPath.getAbsolutePath()).append(File.separator).append(tableFolder.getText()).toString());
                		if (tablePath.isDirectory())
                		{
                			File tableXml = new File(new StringBuilder(tablePath.getAbsolutePath()).append(File.separator).append(tableFolder.getText() +".xml").toString());
                			File tableXsd = new File(new StringBuilder(tablePath.getAbsolutePath()).append(File.separator).append(tableFolder.getText() +".xsd").toString());
                			if (!tableXml.isFile())
                			{
                				valid = false;
                    			getMessageService().logError(
                                        getTextResourceService().getText(MESSAGE_MODULE_D) + 
                                        getTextResourceService().getText(MESSAGE_DASHES) + 
                                        getTextResourceService().getText(MESSAGE_MODULE_D_INVALID_FILE) + tableXml.getName());                
                			}
                			if (!tableXsd.isFile())
                			{
                   				valid = false;
                    			getMessageService().logError(
                                        getTextResourceService().getText(MESSAGE_MODULE_D) + 
                                        getTextResourceService().getText(MESSAGE_DASHES) + 
                                        getTextResourceService().getText(MESSAGE_MODULE_D_INVALID_FILE) + tableXsd.getName());                
                			}
                		}
                		else
                		{
                			valid = false;
                    		getMessageService().logError(
                                    getTextResourceService().getText(MESSAGE_MODULE_D) + 
                                    getTextResourceService().getText(MESSAGE_DASHES) + 
                                    getTextResourceService().getText(MESSAGE_MODULE_D_INVALID_FOLDER) + tablePath.getName());                
                		}
        			}
        		}
        		else
        		{
        			valid = false;
            		getMessageService().logError(
                            getTextResourceService().getText(MESSAGE_MODULE_D) + 
                            getTextResourceService().getText(MESSAGE_DASHES) + 
                            getTextResourceService().getText(MESSAGE_MODULE_D_INVALID_FOLDER) + schemaPath.getName());                
        		}
        		
        	}
        } 
        catch (java.io.IOException ioe) 
        {
        	valid = false;
    		getMessageService().logError(
                    getTextResourceService().getText(MESSAGE_MODULE_D) + 
                    getTextResourceService().getText(MESSAGE_DASHES) + 
                    "IOException " + 
                    ioe.getMessage());                
        } 
        catch (JDOMException e) 
        {
        	valid = false;
           getMessageService().logError(
                    getTextResourceService().getText(MESSAGE_MODULE_D) + 
                    getTextResourceService().getText(MESSAGE_DASHES) + 
                    "JDOMException " + 
                    e.getMessage());                
        } 

        return valid;
    }
 }
