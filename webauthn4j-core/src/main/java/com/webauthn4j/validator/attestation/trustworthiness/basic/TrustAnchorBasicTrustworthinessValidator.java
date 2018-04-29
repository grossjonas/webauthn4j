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

package com.webauthn4j.validator.attestation.trustworthiness.basic;

import com.webauthn4j.anchor.WebAuthnTrustAnchorService;
import com.webauthn4j.attestation.statement.FIDOU2FAttestationStatement;
import com.webauthn4j.attestation.statement.AttestationStatement;
import com.webauthn4j.validator.exception.CertificateException;
import com.webauthn4j.util.CertificateUtil;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.*;
import java.util.EnumSet;
import java.util.Set;

/**
 * TrustAnchorBasicTrustworthinessValidator
 */
public class TrustAnchorBasicTrustworthinessValidator implements BasicTrustworthinessValidator {

    private WebAuthnTrustAnchorService webAuthnTrustAnchorService;

    private boolean isRevocationCheckEnabled = false;

    public TrustAnchorBasicTrustworthinessValidator(WebAuthnTrustAnchorService webAuthnTrustAnchorService) {
        this.webAuthnTrustAnchorService = webAuthnTrustAnchorService;
    }

    public void validate(AttestationStatement attestationStatement) {

        FIDOU2FAttestationStatement fidoU2FAttestationStatement = (FIDOU2FAttestationStatement) attestationStatement;
        CertPath certPath = fidoU2FAttestationStatement.getX5c();
        Set<TrustAnchor> trustAnchors = webAuthnTrustAnchorService.getTrustAnchors();

        CertPathValidator certPathValidator = CertificateUtil.createCertPathValidator();
        PKIXParameters certPathParameters = CertificateUtil.createPKIXParameters(trustAnchors);

        if (isRevocationCheckEnabled()) {
            //Set PKIXRevocationChecker to enable CRL based revocation check, which is disabled by default.
            //Ref. http://docs.oracle.com/javase/7/docs/technotes/guides/security/certpath/CertPathProgGuide.html#AppB
            PKIXRevocationChecker pkixRevocationChecker = (PKIXRevocationChecker) certPathValidator.getRevocationChecker();
            pkixRevocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.PREFER_CRLS));
            certPathParameters.addCertPathChecker(pkixRevocationChecker);
        } else {
            certPathParameters.setRevocationEnabled(false);
        }

        try {
            certPathValidator.validate(certPath, certPathParameters);
        } catch (InvalidAlgorithmParameterException e) {
            throw new com.webauthn4j.validator.exception.CertificateException("invalid algorithm parameter", e);
        } catch (CertPathValidatorException e) {
            throw new CertificateException("invalid cert path", e);
        }
    }

    public boolean isRevocationCheckEnabled() {
        return isRevocationCheckEnabled;
    }

    public void setRevocationCheckEnabled(boolean revocationCheckEnabled) {
        isRevocationCheckEnabled = revocationCheckEnabled;
    }
}
