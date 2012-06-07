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

package ch.kostceco.tools.siardval.validation.module.impl;

import java.io.File;

import ch.kostceco.tools.siardval.exception.module.ValidationIrecognitionException;
//import ch.kostceco.tools.siardval.util.Util;
import ch.kostceco.tools.siardval.validation.ValidationModuleImpl;
import ch.kostceco.tools.siardval.validation.module.ValidationIrecognitionModule;

/**
 * Validierungsschritt I (SIARD-Erkennung)
 * Wird die SIARD-Datei als SIARD erkannt? 
 * valid --> Extension = .siard
 * @author Rc Claire Röthlisberger, KOST-CECO
 */

public class ValidationIrecognitionModuleImpl extends ValidationModuleImpl implements ValidationIrecognitionModule {

    @Override
    public boolean validate(File siardDatei) throws ValidationIrecognitionException {
    	

    	/**
    	 * Validierung ob die Extension .siard lautet
    	 */
        if (! siardDatei.getAbsolutePath().toLowerCase().endsWith(".siard")){
        	getMessageService().logError(
                    getTextResourceService().getText(MESSAGE_MODULE_I) + 
                    getTextResourceService().getText(MESSAGE_DASHES) + 
                    getTextResourceService().getText(MESSAGE_MODULE_I_NOTALLOWEDEXT));
            // Die SIARD-Datei wurde nicht als solche erkannt, weil sie keine .siard Extension hat.
            return false;
        } 

        /**
         * Validierung ob die PUID richtig erkannt wird (z.B mit DROID)
         * => Auf diese Validierung kann verzichtet werden, da bereits vorgängig geprüft wurde ob es ein unkomprimiertes
         * ZIP mit dem entsprechenden metadata.xml ist. 
         */            

       return true;
    }
}
