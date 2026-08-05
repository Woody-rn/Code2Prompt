package ru.npepub.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Manages SSL/TLS certificates. Auto-generates self-signed certificate on first run.
 * Certificate stored at ~/.code2prompt/keystore.jks.
 */

@C2PComponent
class CertificateManager {
    private static final Logger log = LoggerFactory.getLogger(CertificateManager.class);

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));
    private static final Path C2P_DIR = USER_HOME.resolve(".code2prompt");
    private static final Path KEYSTORE_PATH = C2P_DIR.resolve("keystore.jks");
    private static final String KEYSTORE_PASSWORD = "code2prompt2025";

    public Path ensureCertificate() throws Exception {
        Files.createDirectories(C2P_DIR);

        if (!Files.exists(KEYSTORE_PATH)) {
            log.info("🔐 Generating self-signed certificate...");
            generateSelfSignedCertificate();
            log.info("✅ Certificate generated at: {}", KEYSTORE_PATH);
        } else {
            log.info("✅ Certificate found at: {}", KEYSTORE_PATH);
        }

        return KEYSTORE_PATH;
    }

    public SSLContext createSSLContext() throws Exception {
        Path keystorePath = ensureCertificate();
        char[] password = KEYSTORE_PASSWORD.toCharArray();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath.toFile())) {
            keyStore.load(fis, password);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, password);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom()
        );

        return sslContext;
    }

    private void generateSelfSignedCertificate() throws Exception {
        String javaHome = System.getProperty("java.home");
        String keytoolPath = Paths.get(javaHome, "bin", "keytool").toString();

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            keytoolPath += ".exe";
        }

        String[] command = {
                keytoolPath,
                "-genkey",
                "-alias", "code2prompt",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-keystore", KEYSTORE_PATH.toString(),
                "-dname", "CN=Code2Prompt, OU=Dev, O=npepub, L=City, S=State, C=RU",
                "-storepass", KEYSTORE_PASSWORD,
                "-keypass", KEYSTORE_PASSWORD,
                "-validity", "730"
        };

        log.info("Running keytool...");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try {
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                try (InputStream is = process.getInputStream()) {
                    String output = new String(is.readAllBytes());
                    log.error("Keytool failed with exit code {}: {}", exitCode, output);
                    throw new RuntimeException("Failed to generate certificate. Exit code: " + exitCode);
                }
            }

            log.info("✅ Self-signed certificate generated successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Certificate generation interrupted", e);
        }
    }
}