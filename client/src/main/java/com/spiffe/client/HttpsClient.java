package com.spiffe.client;

import io.spiffe.exception.SocketEndpointAddressException;
import io.spiffe.exception.X509SourceException;
import io.spiffe.provider.SpiffeKeyManager;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.provider.SpiffeSslContextFactory.SslContextOptions;
import io.spiffe.provider.SpiffeTrustManager;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.spiffeid.SpiffeIdUtils;
import io.spiffe.workloadapi.DefaultX509Source;
import lombok.val;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Example of a simple HTTPS Client backed by the Workload API to get the X.509 Certificates
 * and trusted cert bundles.
 * <p>
 * The purpose of this class is to show the use of the {@link SpiffeSslContextFactory} to create
 * a {@link SSLContext} that uses X.509-SVID provided by a Workload API. The SSLContext uses the
 * {@link SpiffeKeyManager} and {@link SpiffeTrustManager} for
 * providing certificates and doing chain and SPIFFE ID validation.
 */
public class HttpsClient {

    String spiffeSocket;
    Supplier<Set<SpiffeId>> acceptedSpiffeIdsSetSupplier;
    String serverHost;
    int serverPort;

    public static void main(String[] args) {
        String spiffeSocket = "unix:/tmp/spire-agent/public/api.sock";
        HttpsClient httpsClient = new HttpsClient(4000, args[0], spiffeSocket, () -> Collections.singleton(SpiffeId.parse(args[1])));
        try {
            httpsClient.run();
        } catch (KeyManagementException | NoSuchAlgorithmException | IOException | SocketEndpointAddressException |
                 X509SourceException e) {
            throw new RuntimeException("Error starting Https Client", e);
        }
    }

    HttpsClient(int serverPort, String serverHost, String spiffeSocket, Supplier<Set<SpiffeId>> acceptedSpiffeIdsSetSupplier) {
        this.serverPort = serverPort;
        this.serverHost = serverHost;
        this.spiffeSocket = spiffeSocket;
        this.acceptedSpiffeIdsSetSupplier = acceptedSpiffeIdsSetSupplier;
    }

    void run() throws IOException, SocketEndpointAddressException, KeyManagementException, NoSuchAlgorithmException, X509SourceException {

        val sourceOptions = DefaultX509Source.X509SourceOptions
                .builder()
                .spiffeSocketPath(spiffeSocket)
                .build();
        val x509Source = DefaultX509Source.newSource(sourceOptions);

        SslContextOptions sslContextOptions = SslContextOptions
                .builder()
                .acceptedSpiffeIdsSupplier(acceptedSpiffeIdsSetSupplier)
                .x509Source(x509Source)
                .build();
        SSLContext sslContext = SpiffeSslContextFactory.getSslContext(sslContextOptions);

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(serverHost, serverPort);

        new WorkloadThread(sslSocket, x509Source).start();
    }
}

