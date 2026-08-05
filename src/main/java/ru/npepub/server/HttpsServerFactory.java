package ru.npepub.server;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import ru.npepub.di.api.C2PComponent;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;

/**
 * Factory for creating HttpsServer instances with SSL configuration.
 */

@C2PComponent
class HttpsServerFactory {

    /**
     * Creates and configures an HttpsServer with the given SSL context.
     *
     * @param port        port to bind to
     * @param sslContext  SSL context for HTTPS
     * @return configured HttpsServer instance
     * @throws Exception if server creation fails
     */

    public HttpsServer createServer(int port, SSLContext sslContext) throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 0);

        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                SSLContext ctx = getSSLContext();
                SSLEngine engine = ctx.createSSLEngine();
                params.setNeedClientAuth(false);
                params.setCipherSuites(engine.getEnabledCipherSuites());
                params.setProtocols(engine.getEnabledProtocols());
                params.setSSLParameters(ctx.getDefaultSSLParameters());
            }
        });

        return server;
    }
}