/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webauthn4j.async.anchor;

import com.webauthn4j.anchor.KeyStoreException;
import com.webauthn4j.async.util.internal.FileAsyncUtil;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.util.AssertUtil;
import com.webauthn4j.util.CertificateUtil;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Load {@link TrustAnchor}s from KeyStore. Loaded trust anchors are cached.
 */
public class KeyStoreTrustAnchorAsyncRepository implements TrustAnchorAsyncRepository {

    // ~ Instance fields
    // ================================================================================================

    private final CompletableFuture<Set<TrustAnchor>> trustAnchors;

    // ~ Constructors
    // ========================================================================================================
    public KeyStoreTrustAnchorAsyncRepository(KeyStore keyStore) {
        AssertUtil.notNull(keyStore, "keyStore must not be null");
        this.trustAnchors = CompletableFuture.completedFuture(loadTrustAnchors(keyStore));
    }

    // ~ Static Methods
    // ========================================================================================================
    public static CompletionStage<KeyStoreTrustAnchorAsyncRepository> createFromKeyStoreFilePath(Path keyStore, String password){
        return loadKeyStore(keyStore, password).thenApply(KeyStoreTrustAnchorAsyncRepository::new);
    }

    // ~ Methods
    // ========================================================================================================

    @Override
    public CompletionStage<Set<TrustAnchor>> find(AAGUID aaguid) {
        return trustAnchors;
    }

    @Override
    public CompletionStage<Set<TrustAnchor>> find(byte[] attestationCertificateKeyIdentifier) {
        return trustAnchors;
    }

    private static @NotNull Set<TrustAnchor> loadTrustAnchors(KeyStore keyStore) {
        try {
            List<String> aliases = Collections.list(keyStore.aliases());
            Set<TrustAnchor> trustAnchors = new HashSet<>();
            for (String alias : aliases) {
                X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
                trustAnchors.add(new TrustAnchor(certificate, null));
            }
            return trustAnchors;
        } catch (java.security.KeyStoreException e) {
            throw new KeyStoreException("Failed to load TrustAnchor from keystore", e);
        }
    }

    private static @NotNull CompletionStage<KeyStore> loadKeyStore(Path keyStore, String password){
        AssertUtil.notNull(keyStore, "keyStore must not be null");
        AssertUtil.notNull(password, "password must not be null");

        return FileAsyncUtil.load(keyStore).thenCompose( bytes ->{
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                KeyStore keyStoreObject = CertificateUtil.createKeyStore();
                keyStoreObject.load(inputStream, password.toCharArray());
                return CompletableFuture.completedFuture(keyStoreObject);
            } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
                throw new KeyStoreException("Failed to load TrustAnchor from keystore", e);
            }
        });
    }

}
