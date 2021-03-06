package com.ontometrics.util;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author Lumir Mrkva <lumir.mrkva@topmonks.com>
 */
public class NaiveClientBuilder {

    private static SSLContext sslContext = createSSLContext();

    private static CloseableHttpClient httpClient;

    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClients.custom().setSslcontext(sslContext).build();
        }
        return httpClient;
    }

    public static Client newClient() {
        return newBuilder().build();
    }

    public static ClientBuilder newBuilder() {
        return ClientBuilder.newBuilder().sslContext(sslContext);
    }

    private static SSLContext createSSLContext() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }}, null);
        } catch (Exception ignored) {

        }
        return sslContext;
    }

}
