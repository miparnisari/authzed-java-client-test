package org.example;

import com.authzed.api.materialize.v0.WatchPermissionsRequest;
import com.authzed.api.materialize.v0.WatchPermissionsServiceGrpc;
import com.authzed.api.v1.*;
import com.authzed.grpcutil.BearerToken;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;


public class Main {
    /**
     * Configures SSL to trust all certificates and bypass hostname verification.
     * This allows the application to connect to servers with untrusted or self-signed certificates.
     * WARNING: This approach is insecure for production use.
     */
    private static void configureTrustAllSSL() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Do nothing - trust all clients
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Do nothing - trust all servers
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    public static void main(String[] args) throws Exception {
        configureTrustAllSSL();
        
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget("URL_HERE")
                .useTransportSecurity()
                .build();

        BearerToken bearerToken = new BearerToken("TOKEN_HERE");
        WatchPermissionsServiceGrpc.WatchPermissionsServiceBlockingStub permissionsService = WatchPermissionsServiceGrpc
                .newBlockingStub(channel)
                .withCallCredentials(bearerToken);
        try {
            WatchPermissionsRequest request = WatchPermissionsRequest.newBuilder().build();

            permissionsService.watchPermissions(request).forEachRemaining(response -> {
                System.out.println("Received permission update: " + response);
            });
        } catch (Exception e) {
            System.out.println("Failed to call watch: " + e.getMessage());
        }
    }
}