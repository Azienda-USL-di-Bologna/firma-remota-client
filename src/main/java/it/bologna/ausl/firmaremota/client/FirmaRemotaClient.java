package it.bologna.ausl.firmaremota.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.bologna.ausl.firmaremota.client.exceptions.FirmaRemotaClientException;
import it.bologna.ausl.firmaremota.client.exceptions.FirmaRemotaServiceException;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *
 * @author Fayssel
 */
public class FirmaRemotaClient {

    private final String url;
    private final String codiceAzienda;
    private final OkHttpClient client;
    private final ObjectMapper objMapper;
    private static final String FIRMA_PATH = "firma";
    private static final String TELEFONA_PATH = "telefona";
    private static final String PREAUTHENTICATION_PATH = "preAutentication";
    private static final String FIRMAREMOTA_PATH = "firmaRemota";
    private static final String TEST_PATH = "/test/{nome}/{cognome}";

    public static class FirmaRemotaClienttHostnameVerifier implements HostnameVerifier {

        private static final String[] VALID_HOSTS = {"localhost", "127.0.0.1"};
        private static final String[] VALID_DOMAINS = {".internal.ausl.bologna.it", ".avec.emr.it"};
        //private static final Logger log = LoggerFactory.getLogger(BabelT2Client.T2ClientHostnameVerifier.class);

        @Override
        public boolean verify(String hostname, SSLSession ssls) {
//            log.debug(String.format("CHECK %s against %s", hostname, ssls.getPeerHost()));
            for (String name : VALID_HOSTS) {
                if (name.equalsIgnoreCase(hostname)) {
                    return true;
                }
            }
            for (String name : VALID_DOMAINS) {
                if (hostname.toLowerCase().endsWith(name.toLowerCase())) {
                    return true;
                }
            }
            //se non sono speciali provo comunque a mathcare host con il peer della sessione TLS
            if (hostname.equalsIgnoreCase(ssls.getPeerHost())) {
                return true;
            }
//            log.error(String.format("Hostname verifier %s %s didn't match", hostname, session.getPeerHost()));
            return false;
        }
    }

    public FirmaRemotaClient(String url, String user, String password) {
        HttpUrl serviceUrl = HttpUrl.parse(url);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if ("https".equalsIgnoreCase(serviceUrl.scheme())) {
            ConnectionSpec connectionSpec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

            builder.connectionSpecs(Collections.singletonList(connectionSpec))
                    .hostnameVerifier(new FirmaRemotaClienttHostnameVerifier());
        }

        client = builder.connectTimeout(2, TimeUnit.MINUTES).readTimeout(2, TimeUnit.MINUTES).writeTimeout(2, TimeUnit.MINUTES).build();
//        client = builder.build();
        objMapper = new ObjectMapper();
        objMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.url = url;
        this.codiceAzienda = null;
    }

    /**
     *
     * @param uri
     *
     * Uri format: http://user:password@host:port/
     *
     */
    public FirmaRemotaClient(String uri, String codiceAzienda) {

        HttpUrl serviceUrl = HttpUrl.parse(uri);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if ("https".equalsIgnoreCase(serviceUrl.scheme())) {
            ConnectionSpec connectionSpec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

            builder.connectionSpecs(Collections.singletonList(connectionSpec))
                    .hostnameVerifier(new FirmaRemotaClienttHostnameVerifier());
        }

        client = builder.connectTimeout(2, TimeUnit.MINUTES).readTimeout(2, TimeUnit.MINUTES).writeTimeout(2, TimeUnit.MINUTES).build();

        objMapper = new ObjectMapper();
        objMapper.enable(SerializationFeature.INDENT_OUTPUT);

        
        this.url = serviceUrl.newBuilder().build().toString();
        this.codiceAzienda = codiceAzienda;
    }
    
    public Response preAuthentication(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaClientException, FirmaRemotaServiceException{
        Request request;

        HttpUrl.Builder callUrlBuilder = HttpUrl.parse(url).newBuilder()
                .addPathSegment(PREAUTHENTICATION_PATH)
                .addQueryParameter("codiceAzienda", codiceAzienda)
                .addQueryParameter("provider", firmaRemotaInformation.getProvider().toString());
        HttpUrl callUrl = callUrlBuilder.build();
        try {
            RequestBody requestBody = RequestBody.
                    create(MediaType.parse("application/json"),
                            objMapper.writeValueAsString(firmaRemotaInformation.getUserInformation()));

            request = new Request.Builder()
                    .url(callUrl)
                    .post(requestBody)
                    .build();
        }
        catch (Exception ex) {
            throw new FirmaRemotaClientException("error building request", ex);
        }
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try {
                    String errorBody = response.body().string();
                    if (errorBody != null && !errorBody.isEmpty())
                        throw FirmaRemotaServiceException.createFromJson(errorBody);
                    else
                        throw new FirmaRemotaClientException(response.toString());
                }
                catch (FirmaRemotaServiceException | FirmaRemotaClientException ex) {
                    throw ex;
                }
                catch (Exception ex) {
                    throw new FirmaRemotaClientException(ex);
                }
            }
            else
                return response;
        } catch (IOException ex) {
            throw new FirmaRemotaClientException(ex);
        }
    }
    

    public void telefona(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaClientException, FirmaRemotaServiceException {

        Response response = preAuthentication(firmaRemotaInformation);
        if (!response.isSuccessful()) {
            try {
                String errorBody = response.body().string();
                if (errorBody != null && !errorBody.isEmpty())
                    throw FirmaRemotaServiceException.createFromJson(errorBody);
                else
                    throw new FirmaRemotaClientException(response.toString());
            }
            catch (FirmaRemotaServiceException | FirmaRemotaClientException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new FirmaRemotaClientException(ex);
            }
        }
        
    }
    
    public FirmaRemotaInformation firmaRemota(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaClientException, FirmaRemotaServiceException{
        Request request;

        HttpUrl.Builder callUrlBuilder = HttpUrl.parse(url).newBuilder()
                .addPathSegment(FIRMAREMOTA_PATH)
                .addQueryParameter("codiceAzienda", codiceAzienda)
                .addQueryParameter("provider", firmaRemotaInformation.getProvider().toString());
        HttpUrl callUrl = callUrlBuilder.build();
        try {

            RequestBody requestBody = RequestBody.
                    create(MediaType.parse("application/json"),
                            objMapper.writeValueAsString(firmaRemotaInformation));

            request = new Request.Builder()
                    .url(callUrl)
                    .post(requestBody)
                    .build();
        } catch (Exception ex) {
            throw new FirmaRemotaClientException(ex);
        }
        System.out.println(client.connectTimeoutMillis());
        System.out.println(client.readTimeoutMillis());
        System.out.println(client.writeTimeoutMillis());
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try {
                    String errorBody = response.body().string();
                    if (errorBody != null && !errorBody.isEmpty())
                        throw FirmaRemotaServiceException.createFromJson(errorBody);
                    else
                        throw new FirmaRemotaClientException(response.toString());
                }
                catch (FirmaRemotaServiceException | FirmaRemotaClientException ex) {
                    throw ex;
                }
                catch (Exception ex) {
                    throw new FirmaRemotaClientException(ex);
                }
            }

            FirmaRemotaInformation res;
            try {
                res = objMapper.readValue(response.body().charStream(), FirmaRemotaInformation.class);
            } catch (IOException ex) {
                throw new FirmaRemotaClientException(ex);
            }
            return res;
        }
        catch (IOException ex) {
            throw new FirmaRemotaClientException("error building request", ex);
        }
    
    }
    
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaClientException, FirmaRemotaServiceException {
        FirmaRemotaInformation firmaRemota;
        try{
            firmaRemota = firmaRemota(firmaRemotaInformation);
        }
        catch (Exception ex) {
            throw ex;
        }
        return firmaRemota;
    }

}
