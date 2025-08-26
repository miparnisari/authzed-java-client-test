package org.example;

import com.authzed.api.materialize.v0.WatchPermissionsRequest;
import com.authzed.api.materialize.v0.WatchPermissionsServiceGrpc;
import com.authzed.api.v1.*;
import com.authzed.grpcutil.BearerToken;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import javax.net.ssl.*;
import java.security.KeyStore;


public class Main {
    public static void main(String[] args) throws Exception {
        KeyStore systemKeyStore = KeyStore.getInstance("KeychainStore");
        systemKeyStore.load(null, null);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(systemKeyStore);
        SslContext sslContext = GrpcSslContexts.forClient()
                .trustManager(tmf)
                .build();

        ManagedChannel channel = NettyChannelBuilder
                .forTarget("url-here")
                .sslContext(sslContext)
                .build();

        BearerToken bearerToken = new BearerToken("token-here");
        WatchPermissionsServiceGrpc.WatchPermissionsServiceStub watchClient = WatchPermissionsServiceGrpc
                .newStub(channel)
                .withCallCredentials(bearerToken);

        try {
            WatchPermissionsRequest request = WatchPermissionsRequest.newBuilder().build();

            watchClient.watchPermissions(request, new io.grpc.stub.StreamObserver<>() {
                @Override
                public void onNext(com.authzed.api.materialize.v0.WatchPermissionsResponse response) {
                    System.out.println("Received permission update: " + response);
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Stream error: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("Stream completed");
                }
            });
            
            // Keep the main thread alive to receive streaming responses
            Thread.sleep(30000);
        } catch (Exception e) {
            System.out.println("Failed to call watch: " + e.getMessage());
        }
    }
}