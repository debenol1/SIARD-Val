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

package ch.kostceco.tools.siardval.validation.module;

import java.io.File;

import ch.kostceco.tools.siardval.exception.module.ValidationFrowException;
import ch.kostceco.tools.siardval.validation.ValidationModule;

/**
 * Validierungsschritt F (Zeilen-Validierung)
 * Wurden die Angaben aus metadata.xml korrekt in die tableZ.xsd-Dateien übertragen?
 * valid --> gleiche Zeilenzahl	(rows in metadata.xml = max = minOccurs in tableZ.xsd
 * Ansonsten: Enthält tableZ.xml die gleiche Anzahl Zeilen wie in metadata.xml definiert?
 * valid --> gleiche Zeilenzahl	(rows in metadata.xml = Anzahl row in tableZ.xml
 * @author Do Olivier Debenath
 */

public interface ValidationFrowModule extends ValidationModule {

    
    public boolean validate(File siardDatei) throws ValidationFrowException;
    
}
