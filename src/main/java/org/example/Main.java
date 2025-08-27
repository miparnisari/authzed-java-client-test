package org.example;

import com.authzed.api.v1.*;
import com.authzed.grpcutil.BearerToken;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


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
                .forTarget("URL_HERE")
                .sslContext(sslContext)
                .disableServiceConfigLookUp()
                .defaultServiceConfig(Map.of(
                        "methodConfig", List.of(
                                Map.of(
                                        "name",  List.of(
                                                Map.of(
                                                        "service", "authzed.api.v1.WatchService",
                                                        "method", "Watch"
                                                )
                                        ),
                                        "retryPolicy", Map.of(
                                                "maxAttempts", "5",
                                                "initialBackoff", "1s",
                                                "backoffMultiplier", "4.0",
                                                "maxBackoff", "30s",
                                                "retryableStatusCodes", List.of("UNAVAILABLE", "INTERNAL")
                                        )
                                )
                        )
                ))
                .build();

        BearerToken bearerToken = new BearerToken("TOKEN_HERE");
        WatchServiceGrpc.WatchServiceBlockingStub watchClient = WatchServiceGrpc
                .newBlockingStub(channel)
                .withCallCredentials(bearerToken);

        ZedToken lastZedToken = ZedToken.newBuilder().setToken("").build();

        while(true) {
            try {
                WatchRequest.Builder builder = WatchRequest.newBuilder();

                if (!lastZedToken.getToken().isEmpty()) {
                    builder.setOptionalStartCursor(lastZedToken);
                }

                WatchRequest request = builder.build();

                Iterator<WatchResponse> watchStream = watchClient.watch(request);

                while (watchStream.hasNext()) {
                    WatchResponse msg = watchStream.next();
                    System.out.println("Received watch response: " + msg);

                    if (!msg.getChangesThrough().getToken().isEmpty()) {
                        lastZedToken = msg.getChangesThrough();
                    }
                }

            } catch (Exception e) {
                System.out.println("Error calling watch: " + e.getMessage());
                return;
            }
        }
    }
}