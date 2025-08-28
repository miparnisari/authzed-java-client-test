package org.example;

import com.authzed.api.v1.*;
import com.authzed.grpcutil.BearerToken;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
                .build();

        BearerToken bearerToken = new BearerToken("TOKEN_HERE");
        WatchServiceGrpc.WatchServiceBlockingStub watchClient = WatchServiceGrpc
                .newBlockingStub(channel)
                .withCallCredentials(bearerToken);

        ZedToken lastZedToken = ZedToken.newBuilder().setToken("").build();

        while(true) {
            try {
                WatchRequest.Builder builder = WatchRequest.newBuilder()
                        .addOptionalUpdateKinds(com.authzed.api.v1.WatchKind.WATCH_KIND_INCLUDE_CHECKPOINTS);

                if (!lastZedToken.getToken().isEmpty()) {
                    System.out.println("Resuming watch from token: " + lastZedToken.getToken());
                    builder.setOptionalStartCursor(lastZedToken);
                }

                WatchRequest request = builder.build();

                Iterator<WatchResponse> watchStream = watchClient.watch(request);

                while (watchStream.hasNext()) {
                    WatchResponse msg = watchStream.next();

                    if (msg.getUpdatesCount() > 0) {
                        for (var update : msg.getUpdatesList()) {
                            System.out.println("Received update: " + update);
                        }
                    } else {
                        System.out.println("No changes made in SpiceDB");
                    }

                    if (!msg.getChangesThrough().getToken().isEmpty()) {
                        lastZedToken = msg.getChangesThrough();
                    }
                }

            } catch (Exception e) {
                if (e instanceof StatusRuntimeException sre && (sre.getStatus().getCode().equals(Status.UNAVAILABLE.getCode()) ||
                        (sre.getStatus().getCode().equals(Status.INTERNAL.getCode())) && sre.getMessage().contains("stream timeout"))) {
                    // Probably a server restart. Retry.
                } else {
                    System.out.println("Error calling watch: " + e.getMessage());
                    return;
                }
            }
        }
    }
}