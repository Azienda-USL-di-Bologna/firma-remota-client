package it.bologna.ausl.firmaremota.client.exceptions;

/**
 *
 * @author guido
 */
public class FirmaRemotaClientException extends Exception {

    public FirmaRemotaClientException(String message) {
        super(message);
    }

    public FirmaRemotaClientException(Throwable cause) {
        super(cause);
    }

    public FirmaRemotaClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
