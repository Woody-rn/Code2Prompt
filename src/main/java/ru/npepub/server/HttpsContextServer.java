package ru.npepub.server;

import com.sun.net.httpserver.HttpsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.config.PromptConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.model.ProjectInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * HTTPS implementation of ContextServer with self-signed certificate.
 */
@C2PComponent
class HttpsContextServer implements ContextServer {

    private static final Logger log = LoggerFactory.getLogger(HttpsContextServer.class);

    @C2PInject private CertificateManager certificateManager;
    @C2PInject private HttpsServerFactory serverFactory;
    @C2PInject private CorsHandler corsHandler;
    @C2PInject private JsonResponseHelper jsonHelper;
    @C2PInject private ContextRequestHandler requestHandler;
    @C2PInject private ConfigPort configPort;

    private HttpsServer server;

    @Override
    public void start(int port, List<Path> files, ProjectInfo projectInfo) throws Exception {
        PromptConfig promptConfig = configPort.load().prompt();
        requestHandler.updateContext(files, projectInfo, promptConfig);

        var sslContext = certificateManager.createSSLContext();
        server = serverFactory.createServer(port, sslContext);

        server.createContext("/", exchange -> {
            try {
                corsHandler.addCorsHeaders(exchange);
                if (corsHandler.handlePreflight(exchange)) return;
                requestHandler.handleRequest(exchange);
            } catch (Exception e) {
                log.error("Error handling request: {}", exchange.getRequestURI(), e);
                jsonHelper.sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        });

        server.setExecutor(null);
        server.start();

        log.info("🔒 HTTPS Server started on https://localhost:{}", port);
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            log.info("HTTPS Server stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return server != null;
    }


    @Override
    public void updatePrompt(PromptConfig promptConfig) {
        requestHandler.updatePrompt(promptConfig);
    }
}