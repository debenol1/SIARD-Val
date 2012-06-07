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
package ch.kostceco.tools.siardval.controller;

import java.io.File;

import ch.kostceco.tools.siardval.exception.module.ValidationAzipException;
import ch.kostceco.tools.siardval.exception.module.ValidationBprimaryStructureException;
import ch.kostceco.tools.siardval.exception.module.ValidationCheaderException;
import ch.kostceco.tools.siardval.exception.module.ValidationDstructureException;
import ch.kostceco.tools.siardval.exception.module.ValidationHcontentException;
import ch.kostceco.tools.siardval.exception.module.ValidationIrecognitionException;
import ch.kostceco.tools.siardval.exception.module.ValidationJsurplusFilesException;
import ch.kostceco.tools.siardval.logging.Logger;
import ch.kostceco.tools.siardval.logging.MessageConstants;
import ch.kostceco.tools.siardval.service.TextResourceService;
import ch.kostceco.tools.siardval.validation.module.ValidationAzipModule;
import ch.kostceco.tools.siardval.validation.module.ValidationBprimaryStructureModule;
import ch.kostceco.tools.siardval.validation.module.ValidationCheaderModule;
import ch.kostceco.tools.siardval.validation.module.ValidationDstructureModule;
import ch.kostceco.tools.siardval.validation.module.ValidationHcontentModule;
import ch.kostceco.tools.siardval.validation.module.ValidationIrecognitionModule;
import ch.kostceco.tools.siardval.validation.module.ValidationJsurplusFilesModule;
//import ch.kostceco.tools.siardval.validation.module.ValidationJsurplusFilesModule;
//import ch.kostceco.tools.siardval.validation.module.ValidationKconstraintModule;

/**
 * SIARDVal -->
 * 
 * Der Controller ruft die benötigten Module zur Validierung der SIARD-Datei in
 * der benötigten Reihenfolge auf.
 * 
 * Die Validierungs-Module werden mittels Spring-Dependency-Injection
 * eingebunden.
 */

public class Controller implements MessageConstants {

    private static final Logger LOGGER = new Logger(Controller.class);
    private TextResourceService textResourceService;
    
    private ValidationAzipModule validationAzipModule;
    private ValidationBprimaryStructureModule validationBprimaryStructureModule;
    private ValidationCheaderModule validationCheaderModule;
    private ValidationDstructureModule validationDstructureModule;
//    private ValidationEcolumnModule validationEcolumnModule;
//    private ValidationFrowModule validationFrowModule;
//  private ValidationGchecksumModule validationGchecksumModule;
    private ValidationHcontentModule validationHcontentModule;
    private ValidationIrecognitionModule validationIrecognitionModule;
    private ValidationJsurplusFilesModule validationJsurplusFilesModule;
//    private ValidationKconstraintModule validationKconstraintModule;


    public ValidationAzipModule getValidationAzipModule() {
        return validationAzipModule;
    }
    public void setValidationAzipModule(ValidationAzipModule validationAzipModule) {
        this.validationAzipModule = validationAzipModule;
    }

    public ValidationBprimaryStructureModule getValidationBprimaryStructureModule() {
        return validationBprimaryStructureModule;
    }
    public void setValidationBprimaryStructureModule(ValidationBprimaryStructureModule validationBprimaryStructureModule) {
        this.validationBprimaryStructureModule = validationBprimaryStructureModule;
    }
    
    public ValidationCheaderModule getValidationCheaderModule() {
        return validationCheaderModule;
    }
    public void setValidationCheaderModule(ValidationCheaderModule validationCheaderModule) {
        this.validationCheaderModule = validationCheaderModule;
    }

    public ValidationDstructureModule getValidationDstructureModule() {
        return validationDstructureModule;
    }
    public void setValidationDstructureModule(ValidationDstructureModule validationDstructureModule) {
        this.validationDstructureModule = validationDstructureModule;
    }

/*    public ValidationEcolumnModule getValidationEcolumnModule() {
        return validationEcolumnModule;
    }
    public void setValidationEcolumnModule(ValidationEcolumnModule validationEcolumnModule) {
        this.validationEcolumnModule = validationEcolumnModule;
    }*/

/*    public ValidationFrowModule getValidationFrowModule() {
        return validationFrowModule;
    }
    public void setValidationFrowModule(ValidationFrowModule validationFrowModule) {
        this.validationFrowModule = validationFrowModule;
    }*/

    /*    public ValidationGchecksumModule getValidationGchecksumModule() {
    return validationGchecksumModule;
}
public void setValidationGchecksumModule(ValidationGchecksumModule validationGchecksumModule) {
    this.validationGchecksumModule = validationGchecksumModule;
}*/

    public ValidationHcontentModule getValidationHcontentModule() {
        return validationHcontentModule;
    }
    public void setValidationHcontentModule(ValidationHcontentModule validationHcontentModule) {
        this.validationHcontentModule = validationHcontentModule;
    }

    public ValidationIrecognitionModule getValidationIrecognitionModule() {
        return validationIrecognitionModule;
    }
    public void setValidationIrecognitionModule(ValidationIrecognitionModule validationIrecognitionModule) {
        this.validationIrecognitionModule = validationIrecognitionModule;
    }

    public ValidationJsurplusFilesModule getValidationJsurplusFilesModule() {
        return validationJsurplusFilesModule;
    }
    public void setValidationJsurplusFilesModule(ValidationJsurplusFilesModule validationJsurplusFilesModule) {
        this.validationJsurplusFilesModule = validationJsurplusFilesModule;
    }

/*    public ValidationKconstraintModule getValidationKconstraintModule() {
        return validationKconstraintModule;
    }
    public void setValidationKconstraintModule(ValidationKconstraintModule validationKconstraintModule) {
        this.validationKconstraintModule = validationKconstraintModule;
    }*/


    public TextResourceService getTextResourceService() {
        return textResourceService;
    }

    public void setTextResourceService(TextResourceService textResourceService) {
        this.textResourceService = textResourceService;
    }

    
    public boolean executeMandatory(File siardDatei) {
        boolean valid = true;
        
        // Validation Step A (Lesbarkeit)
        try {
            if (this.getValidationAzipModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_A)));
                this.getValidationAzipModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_A))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_A));
                // Ein negatives Validierungsresultat in diesem Schritt führt
                // zum Abbruch der weiteren Verarbeitung
                this.getValidationAzipModule().getMessageService().print();
                return false;
            }
        } catch (ValidationAzipException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_A), e.getMessage()));
            this.getValidationAzipModule().getMessageService().print();
            return false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }

        // Validation Step B (primäre Verzeichnisstruktur)
        try {
            if (this.getValidationBprimaryStructureModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_B)));
                this.getValidationBprimaryStructureModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_B))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_B));
                // Ein negatives Validierungsresultat in diesem Schritt führt
                // zum Abbruch der weiteren Verarbeitung
                this.getValidationBprimaryStructureModule().getMessageService().print();
                return false;
            }
        } catch (ValidationBprimaryStructureException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_B), e.getMessage()));
            this.getValidationBprimaryStructureModule().getMessageService().print();
            return false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }

        // Validation Step C (Header-Validierung)
        try {
            if (this.getValidationCheaderModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_C)));
                this.getValidationCheaderModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_C))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_C));
                this.getValidationCheaderModule().getMessageService().print();
                // Ein negatives Validierungsresultat in diesem Schritt führt
                // zum Abbruch der weiteren Verarbeitung
                return false;
            }
        } catch (ValidationCheaderException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_C), e.getMessage()));
            this.getValidationCheaderModule().getMessageService().print();
            return false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }

        // Validation Step D (Struktur-Validierung)
        try {
            if (this.getValidationDstructureModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_D)));
                this.getValidationDstructureModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_D))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_D));
                this.getValidationDstructureModule().getMessageService().print();
                // Ein negatives Validierungsresultat in diesem Schritt führt
                // zum Abbruch der weiteren Verarbeitung
                return false;
            }
        } catch (ValidationDstructureException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_D), e.getMessage()));
            this.getValidationDstructureModule().getMessageService().print();
            return false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }

        return valid;
    }
    
    public boolean executeOptional(File siardDatei) {
        boolean valid = true;

/*        // Validation Step E (Spalten-Validierung)
        try {
            if (this.getValidationEcolumnModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_E)));
                this.getValidationEcolumnModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_E))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_E));
                this.getValidationEcolumnModule().getMessageService().print();
                valid = false;
            }
        } catch (ValidationEcolumnException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_E), e.getMessage()));
            this.getValidationEcolumnModule().getMessageService().print();
            valid = false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }*/


/*        // Validation Step F (Zeilen-Validierung)
        try {
            if (this.getValidationFrowModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_F)));
                this.getValidationFrowModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_F))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_F));
                this.getValidationGrowModule().getMessageService().print();
                valid = false;
            }
        } catch (ValidationFrowException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_F), e.getMessage()));
            this.getValidationFrowModule().getMessageService().print();
            valid = false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }*/

/*        // Validation Step G (Prüfsummen-Validierung)
        try {
            if (this.getValidationGchecksumModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_G)));
                this.getValidationGchecksumModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_G))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_G));
                this.getValidationGchecksumModule().getMessageService().print();
                valid = false;
            }
        } catch (ValidationGchecksumException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_G), e.getMessage()));
            this.getValidationGchecksumModule().getMessageService().print();
            valid = false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }*/


        // Validation Step H (Content-Validierung)
        try {
            if (this.getValidationHcontentModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_H)));
                this.getValidationHcontentModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_H))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_H));
                this.getValidationHcontentModule().getMessageService().print();
                valid = false;
            }
        } catch (ValidationHcontentException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_H), e.getMessage()));
            this.getValidationHcontentModule().getMessageService().print();
            valid = false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }


        // Validation Step I (SIARD-Erkennung)
        try {
            if (this.getValidationIrecognitionModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_I)));
                this.getValidationIrecognitionModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_I))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_I));
                this.getValidationIrecognitionModule().getMessageService().print();
                valid = false;
            }
        } catch (ValidationIrecognitionException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_I), e.getMessage()));
            this.getValidationIrecognitionModule().getMessageService().print();
            valid = false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }


        // Validation Step J (Zusätzliche Primärdateien)
        try {
            if (this.getValidationJsurplusFilesModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_J)));
                this.getValidationJsurplusFilesModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_J))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_J));
                this.getValidationJsurplusFilesModule().getMessageService().print();
                valid = false;
            }
        } catch (ValidationJsurplusFilesException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_J), e.getMessage()));
            this.getValidationJsurplusFilesModule().getMessageService().print();
            valid = false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }


/*        // Validation Step K (Constraint-Validierung)
        try {
            if (this.getValidationKconstraintModule().validate(siardDatei)) {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_VALID,
                        getTextResourceService().getText(MESSAGE_MODULE_K)));
                this.getValidationKconstraintModule().getMessageService().print();
            } else {
                LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID,
                        getTextResourceService().getText(MESSAGE_MODULE_K))
                        + getTextResourceService().getText(MESSAGE_STEPERGEBNIS_K));
                this.getValidationKconstraintModule().getMessageService().print();
                valid = false;
            }
        } catch (ValidationKconstraintException e) {
            LOGGER.logInfo(getTextResourceService().getText(MESSAGE_MODULE_INVALID_2ARGS,
                    getTextResourceService().getText(MESSAGE_MODULE_K), e.getMessage()));
            this.getValidationKconstraintModule().getMessageService().print();
            valid = false;
        } catch (Exception e) {          
            LOGGER.logInfo(getTextResourceService().getText(ERROR_UNKNOWN));
            LOGGER.logError(e.getMessage());
            return false;
        }*/

        return valid;
    }
}
