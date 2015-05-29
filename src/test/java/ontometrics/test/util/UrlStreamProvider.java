package ontometrics.test.util;

import com.ontometrics.integrations.sources.InputStreamHandler;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * URL resource provider, opens resource using {@link java.net.URL#openStream()}
 *
 * UrlResourceProvider.java
 */
public class UrlStreamProvider implements StreamProvider {

    private UrlStreamProvider() {}

    public static UrlStreamProvider instance (){
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            ;
        }
        return new UrlStreamProvider();
    }



    @Override
    public <RES> RES openResourceStream(URL resourceUrl, InputStreamHandler<RES> inputStreamHandler) throws Exception {
        InputStream is = null;
        try {
            return inputStreamHandler.handleStream(is = resourceUrl.openStream(), HttpStatus.SC_OK);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
