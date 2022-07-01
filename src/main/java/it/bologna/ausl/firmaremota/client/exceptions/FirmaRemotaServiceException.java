package it.bologna.ausl.firmaremota.client.exceptions;

import org.json.JSONObject;

/**
 *
 * @author bake
 */
public class FirmaRemotaServiceException extends Exception {

    public static enum ErrorTypes {
        DOWNLOAD_TIMEOUT, WRONG_OTP, INVALID_CREDENTIAL, REMOTE_SERVER_ERROR, UNKNOWN;
    };

    private ErrorTypes errorType;
    private int httpStatus;

    public FirmaRemotaServiceException(ErrorTypes errorType, int httpStatus, String message) {
        super(message);
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }

    public FirmaRemotaServiceException(ErrorTypes errorType, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }

    public FirmaRemotaServiceException(ErrorTypes errorType, int httpStatus, Throwable cause) {
        super(cause);
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }

    public FirmaRemotaServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirmaRemotaServiceException(String message) {
        super(message);
    }

    public FirmaRemotaServiceException(Throwable cause) {
        super(cause);
    }

    public ErrorTypes getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorTypes errorType) {
        this.errorType = errorType;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public static FirmaRemotaServiceException createFromJson(String json) throws FirmaRemotaClientException {
        FirmaRemotaServiceException firmaRemotaServiceException;
        try {
            JSONObject errorJson = new JSONObject(json);
            String exceptionClass = errorJson.getString("exception");
            ErrorTypes errorType;
            String errorMessage = null;
            
            if (exceptionClass.endsWith("RemoteFileNotFoundException")) {
                errorType = ErrorTypes.DOWNLOAD_TIMEOUT;
                errorMessage = "la sessione di firma è scaduta, si prega di chiudere la finestra e riprovare";
            }
            else if (exceptionClass.endsWith("WrongTokenException")) {
                errorType = ErrorTypes.WRONG_OTP;
                errorMessage = "il codice OTP è errato o è stato bloccato";
            }
            else if (exceptionClass.endsWith("InvalidCredentialException")) {
                errorType = ErrorTypes.INVALID_CREDENTIAL;
                errorMessage = "username o password errati";
            }
            else if (exceptionClass.endsWith("RemoteServiceException")) {
                errorType = ErrorTypes.REMOTE_SERVER_ERROR;
                errorMessage = errorJson.getString("message");
            } // TODO: aggiungere gli eventuali altri errorType
            else {
                errorType = ErrorTypes.UNKNOWN;
                errorMessage = errorJson.getString("message");
            }

            int httpStatus = errorJson.getInt("status");

            firmaRemotaServiceException = new FirmaRemotaServiceException(errorType, httpStatus, errorMessage);
        }
        catch (Exception ex) {
            throw new FirmaRemotaClientException(json);
        }
        return firmaRemotaServiceException;
    }
}
