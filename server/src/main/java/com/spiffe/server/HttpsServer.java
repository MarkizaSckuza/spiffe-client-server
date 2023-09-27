package com.spiffe.server;

import io.spiffe.exception.SocketEndpointAddressException;
import io.spiffe.exception.X509SourceException;
import io.spiffe.provider.SpiffeKeyManager;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.provider.SpiffeSslContextFactory.SslContextOptions;
import io.spiffe.provider.SpiffeTrustManager;
import io.spiffe.provider.exception.SpiffeProviderException;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.workloadapi.DefaultX509Source;
import io.spiffe.workloadapi.X509Source;
import lombok.val;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Example of a simple HTTPS Server backed by the Workload API to get the X.509 certificates
 * and trusted bundles.
 * <p>
 * The purpose of this class is to show the use of the {@link SpiffeSslContextFactory} to create
 * a {@link SSLContext} that uses X.509-SVID provided by a Workload API. The SSLContext uses the
 * {@link SpiffeKeyManager} and {@link SpiffeTrustManager} for
 * providing certificates and doing chain and SPIFFE ID validation.
 * To run this example, Spire should be running, SPIFFE_ENDPOINT_SOCKET env variable should be
 * defined, and a property ssl.spiffe.accept should be defined in the java.security having a
 * spiffe id from a client workload.
 */
//public class HttpsServerRunner implements CommandLineRunner {
public class HttpsServer {
    int port;
    String acceptedSpiffeID;


    String spiffeSocket;

    public static void main(String[] args) {
//        HttpsServer httpsServer = new HttpsServer(4000, args[0]);

        String spiffeSocket = "unix:/run/spire/sockets/agent.sock";
        HttpsServer httpsServer = new HttpsServer(4000, args[0], spiffeSocket);
        try {
            httpsServer.run();
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error starting HttpsServer", e);
        }
    }

//    HttpsServer(int port, String acceptedSpiffeID) {
//        this.port = port;
//        this.acceptedSpiffeID = acceptedSpiffeID;
//    }

    HttpsServer(int port, String acceptedSpiffeID, String spiffeSocket) {
        this.port = port;
        this.acceptedSpiffeID = acceptedSpiffeID;
        this.spiffeSocket = spiffeSocket;
    }



    void run() throws IOException, KeyManagementException, NoSuchAlgorithmException {
//        X509Source x509Source;
//        try {
//            x509Source = X509SourceManager.getX509Source();
//        } catch (SocketEndpointAddressException | X509SourceException e) {
//            throw new SpiffeProviderException("Error at getting the X509Source instance", e);
//        }


        val sourceOptions = DefaultX509Source.X509SourceOptions
                .builder()
                .spiffeSocketPath(spiffeSocket)
                .build();
        X509Source x509Source;
        try {
            x509Source = DefaultX509Source.newSource(sourceOptions);
        } catch (SocketEndpointAddressException | X509SourceException e) {
            throw new SpiffeProviderException("Error at getting the X509Source instance", e);
        }

        Supplier<Set<SpiffeId>> acceptedSpiffeIds = () -> Collections.singleton(SpiffeId.parse(acceptedSpiffeID));

        val sslContextOptions = SslContextOptions
                .builder()
                .x509Source(x509Source)
                .acceptedSpiffeIdsSupplier(acceptedSpiffeIds)
                .build();
        SSLContext sslContext = SpiffeSslContextFactory.getSslContext(sslContextOptions);

        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

        try (SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port)) {
            // Server will validate Client chain and SPIFFE ID
            sslServerSocket.setNeedClientAuth(true);

            SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
            new WorkloadThread(sslSocket, x509Source).start();
        }
    }
}



