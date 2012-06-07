/*== SIARD-Val ===================================================================================
The SIARD-Val application is used for validate SIARD-Files. 
Copyright (C) 2012 Claire Röthlisberger (KOST-CECO), Martin Kaiser (KOST-CECO), Christian Eugster,
Olivier Debenath
--------------------------------------------------------------------------------------------------
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
==================================================================================================*/

package ch.kostceco.tools.siardval.service.impl;

import java.io.File;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import ch.kostceco.tools.siardval.SIARDVal;
import ch.kostceco.tools.siardval.logging.Logger;
import ch.kostceco.tools.siardval.service.ConfigurationService;
import ch.kostceco.tools.siardval.service.impl.ConfigurationServiceImpl;
import ch.kostceco.tools.siardval.service.TextResourceService;

/**
 * @author Rc Claire Röthlisberger, KOST-CECO
 */

public class ConfigurationServiceImpl implements ConfigurationService
{

	private static final Logger	LOGGER	= new Logger(
												ConfigurationServiceImpl.class );
	XMLConfiguration			config	= null;
	private TextResourceService	textResourceService;

	public TextResourceService getTextResourceService()
	{
		return textResourceService;
	}

	public void setTextResourceService( TextResourceService textResourceService )
	{
		this.textResourceService = textResourceService;
	}

	// Holt sich das Configuration-file.
	private XMLConfiguration getConfig()
	{
		if ( this.config == null ) {
			try {
				String path = "configuration/SIARDVal.conf.xml";
				URL locationOfJar = SIARDVal.class.getProtectionDomain()
						.getCodeSource().getLocation();
				String locationOfJarPath = locationOfJar.getPath();
				if ( locationOfJarPath.endsWith( ".jar" ) ) {
					File file = new File( locationOfJarPath );
					String fileParent = file.getParent();
					path = fileParent + "/" + path;
				}
				config = new XMLConfiguration( path );
			} catch ( ConfigurationException e ) {
				LOGGER.logError( getTextResourceService().getText(
						MESSAGE_CONFIGURATION_ERROR_1 ) );
				// Das Konfigurations-File konnte nicht gelesen werden.
				LOGGER.logError( getTextResourceService().getText(
						MESSAGE_CONFIGURATION_ERROR_2 ) );
				// Im gleichen Verzeichnis wie das ".jar"-File muss sich ein
				// Ordner namens "configuration" befinden.
				LOGGER.logError( getTextResourceService().getText(
						MESSAGE_CONFIGURATION_ERROR_3 ) );
				// Im configuration-Ordner wiederum muss die Konfigurationsdatei
				// "SIARDVal.conf.xml" liegen.
				System.exit( 0 );
			}
		}
		return config;
	}

	@Override
	public String getPathToWorkDir()
	{
		/**
		 * Gibt den Pfad des Arbeitsverzeichnisses zurück. Dieses Verzeichnis
		 * wird zum Entpacken des .zip-Files verwendet.
		 * 
		 * @return Pfad des Arbeitsverzeichnisses
		 */
		Object prop = getConfig().getProperty( "pathtoworkdir" );
		if ( prop instanceof String ) {
			String value = (String) prop;
			return value;
		}
		return null;
	}
}
