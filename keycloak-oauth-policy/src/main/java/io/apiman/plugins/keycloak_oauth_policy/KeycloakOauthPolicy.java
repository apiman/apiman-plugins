/*
 * Copyright 2015 JBoss Inc
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
package io.apiman.plugins.keycloak_oauth_policy;

import io.apiman.common.logging.ApimanLoggerFactory;
import io.apiman.common.logging.IApimanLogger;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.components.ISharedStateComponent;
import io.apiman.gateway.engine.metrics.RequestMetric;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policies.AuthorizationPolicy;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.gateway.engine.policy.PolicyContextKeys;
import io.apiman.plugins.keycloak_oauth_policy.beans.ForwardAuthInfo;
import io.apiman.plugins.keycloak_oauth_policy.beans.KeycloakOauthConfigBean;
import io.apiman.plugins.keycloak_oauth_policy.failures.PolicyFailureFactory;
import io.apiman.plugins.keycloak_oauth_policy.util.Holder;
import org.apache.commons.lang.StringUtils;
import org.keycloak.TokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.rotation.JWKPublicKeyLocator;
import org.keycloak.common.VerificationException;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.util.Collections;

/**
 * A Keycloak OAuth policy.
 *
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class KeycloakOauthPolicy extends AbstractMappedPolicy<KeycloakOauthConfigBean> {

    private static final IApimanLogger LOGGER = ApimanLoggerFactory.getLogger(KeycloakOauthPolicy.class);
    private static final String AUTHORIZATION_KEY = "Authorization"; //$NON-NLS-1$
    private static final String ACCESS_TOKEN_QUERY_KEY = "access_token"; //$NON-NLS-1$
    private static final String BEARER = "Bearer "; //$NON-NLS-1$
    private static final String NEGOTIATE = "Negotiate "; //$NON-NLS-1$
    private final PolicyFailureFactory failureFactory = new PolicyFailureFactory();
    private KeycloakDeployment keycloakDeployment = null;
    private String realmCache = null;

    /**
     * @see io.apiman.gateway.engine.policies.AbstractMappedPolicy#getConfigurationClass()
     */
    @Override
    public Class<KeycloakOauthConfigBean> getConfigurationClass() {
        return KeycloakOauthConfigBean.class;
    }

    /**
     * @see io.apiman.gateway.engine.policies.AbstractMappedPolicy#doApply(io.apiman.gateway.engine.beans.ApiRequest,
     *      io.apiman.gateway.engine.policy.IPolicyContext, java.lang.Object,
     *      io.apiman.gateway.engine.policy.IPolicyChain)
     */
    @Override
    protected void doApply(final ApiRequest request, final IPolicyContext context,
            final KeycloakOauthConfigBean config, final IPolicyChain<ApiRequest> chain) {

        if (keycloakDeployment == null || checkIfRealmChanged(config.getRealm())) {
            configureKeycloakDeployment(config);
        }

        final String rawToken = getRawAuthToken(request);
        final Holder<Boolean> successStatus = new Holder<>(true);

        if (rawToken == null) {
            if (config.getRequireOauth()) {
                doFailure(successStatus, chain, failureFactory.noAuthenticationProvided(context));
            } else {
                chain.doApply(request);
            }
        } else if (doTokenAuth(successStatus, request, context, config, chain, rawToken).getValue()) {
            // Transport security check
            if (config.getRequireTransportSecurity() && !request.isTransportSecure()) {
                // If we've detected a situation where we should blacklist a token
                if (config.getBlacklistUnsafeTokens()) {
                    blacklistToken(context, rawToken, (IAsyncResult<Void> result) -> {
                        if (result.isError()) {
                            LOGGER.error("An error occurred when blacklisting a token",
                                 result.getError());
                        }
                    });
                }

                doFailure(successStatus, chain, failureFactory.noTransportSecurity(context));
                return;
            }

            // If enabled we check against the blacklist
            if (config.getBlacklistUnsafeTokens()) {
                isBlacklistedToken(context, rawToken, (IAsyncResult<Boolean> result) -> {
                    if (result.isError()) {
                        throwError(successStatus, chain, result.getError());
                    } else if (result.getResult()) {
                        doFailure(successStatus, chain, failureFactory.blacklistedToken(context));
                    } else {
                        chain.doApply(request);
                    }
                });
            } else {
                if (successStatus.getValue()) {
                    chain.doApply(request);
                }
            }
        }
    }

    private void doFailure(Holder<Boolean> successStatus, IPolicyChain<?> chain, PolicyFailure failure) {
        chain.doFailure(failure);
        successStatus.setValue(false);
    }

    private void throwError(Holder<Boolean> successStatus, IPolicyChain<?> chain, Throwable error) {
        chain.throwError(error);
        successStatus.setValue(false);
    }

    private Holder<Boolean> doTokenAuth(Holder<Boolean> successStatus, ApiRequest request,
            IPolicyContext context, KeycloakOauthConfigBean config, IPolicyChain<ApiRequest> chain,
            String rawToken) {
        try {

            TokenVerifier<AccessToken> verifier = TokenVerifier.create(rawToken, AccessToken.class)
                    .withChecks(TokenVerifier.IS_ACTIVE);

            AccessToken parsedToken;
            if (config.getRealmCertificate() != null) {
                parsedToken = verifier.publicKey(config.getRealmCertificate().getPublicKey())
                        .verify()
                        .getToken();
            } else {
                String kid = verifier.getHeader().getKeyId();
                parsedToken = verifier.publicKey(keycloakDeployment.getPublicKeyLocator().getPublicKey(kid, keycloakDeployment))
                        .verify()
                        .getToken();
            }

            delegateKerberosTicket(request, config, parsedToken);
            forwardHeaders(request, config, rawToken, parsedToken);
            stripAuthTokens(request, config);
            forwardAuthRoles(context, config, parsedToken);

            RequestMetric metric = context.getAttribute(PolicyContextKeys.REQUEST_METRIC, (RequestMetric) null);
            if (metric != null) {
                metric.setUser(parsedToken.getPreferredUsername());
            }

            return successStatus.setValue(true);
        } catch (VerificationException e) {
            System.out.println(e);
            chain.doFailure(failureFactory.verificationException(context, e));
            return successStatus.setValue(false);
        }
    }

    private void forwardAuthRoles(IPolicyContext context, KeycloakOauthConfigBean config,
            AccessToken parsedToken) {

        if (config.getForwardRoles().getActive()) {
            Access access = null;

            if (config.getForwardRoles().getApplicationName() != null) {
                access = parsedToken.getResourceAccess(config.getForwardRoles().getApplicationName());
            } else {
                access = parsedToken.getRealmAccess();
            }

            if (access == null || access.getRoles() == null) {
                context.setAttribute(AuthorizationPolicy.AUTHENTICATED_USER_ROLES, Collections.<String>emptySet());
            } else {
                context.setAttribute(AuthorizationPolicy.AUTHENTICATED_USER_ROLES, access.getRoles());
            }
        }
    }

    private void delegateKerberosTicket(ApiRequest request, KeycloakOauthConfigBean config,
            AccessToken parsedToken) {
        String serializedGssCredential = (String) parsedToken.getOtherClaims().get(
                KerberosConstants.GSS_DELEGATION_CREDENTIAL);

        if (config.getDelegateKerberosTicket()) {
            request.getHeaders().put(AUTHORIZATION_KEY, NEGOTIATE + serializedGssCredential);
        }
    }

    private String getRawAuthToken(ApiRequest request) {
        String rawToken = StringUtils.strip(request.getHeaders().get(AUTHORIZATION_KEY));

        if (rawToken != null && StringUtils.startsWithIgnoreCase(rawToken, BEARER)) {
            rawToken = StringUtils.removeStartIgnoreCase(rawToken, BEARER);
        } else {
            rawToken = request.getQueryParams().get(ACCESS_TOKEN_QUERY_KEY);
        }

        return rawToken;
    }

    private void stripAuthTokens(ApiRequest request, KeycloakOauthConfigBean config) {
        if (config.getStripTokens()) {
            request.getHeaders().remove(AUTHORIZATION_KEY);
            request.getQueryParams().remove(ACCESS_TOKEN_QUERY_KEY);
        }
    }

    private void forwardHeaders(ApiRequest request, KeycloakOauthConfigBean config, String rawToken,
            AccessToken parsedToken) {
        for (ForwardAuthInfo entry : config.getForwardAuthInfo()) {
            String headerValue = isToken(entry.getField()) ? rawToken :
                ClaimLookup.getClaim(parsedToken, entry.getField());
            // Add the header if we've been able to look it up, else it'll just be empty.
            request.getHeaders().put(entry.getHeader(), headerValue);
        }
    }

    @SuppressWarnings("nls")
    private boolean isToken(String field) {
        return field.toLowerCase().equals("access_token");
    }

    private void isBlacklistedToken(IPolicyContext context, String rawToken,
            final IAsyncResultHandler<Boolean> resultHandler) {
        ISharedStateComponent dataStore = getDataStore(context);
        dataStore.<Boolean> getProperty("apiman-keycloak-blacklist", rawToken, false, //$NON-NLS-1$
                resultHandler);
    }

    private void blacklistToken(IPolicyContext context, String rawToken,
            final IAsyncResultHandler<Void> resultHandler) {
        ISharedStateComponent dataStore = getDataStore(context);
        dataStore.<Boolean> setProperty("apiman-keycloak-blacklist", rawToken, true, //$NON-NLS-1$
                resultHandler);
    }

    private ISharedStateComponent getDataStore(IPolicyContext context) {
        return context.getComponent(ISharedStateComponent.class);
    }

    private void configureKeycloakDeployment(KeycloakOauthConfigBean config) {
        if (config.getRealm() == null) return;

        AdapterConfig adapterConfig = new AdapterConfig();
        String realmUrl = getRealmUrl(config);
        adapterConfig.setRealm(getRealmName(realmUrl));
        adapterConfig.setAuthServerUrl(getAuthServerUrl(realmUrl));
        // We just need a dummy resource because we only need the keycloak deployment for validating tokens
        adapterConfig.setResource("api-gateway-api");

        keycloakDeployment = KeycloakDeploymentBuilder.build(adapterConfig);
        keycloakDeployment.setPublicKeyLocator(new JWKPublicKeyLocator());

        realmCache = realmUrl;
    }

    private String getRealmUrl(KeycloakOauthConfigBean config) {
        String realmUrl = config.getRealm();
        if (realmUrl.endsWith("/")) {
            realmUrl = realmUrl.substring(0, realmUrl.length() - 1);
        }
        return realmUrl;
    }

    private String getAuthServerUrl(String realmUrl) {
        return StringUtils.substringBefore(realmUrl, "/realms/").trim();
    }

    private String getRealmName(String realmUrl) {
        return StringUtils.substringAfter(realmUrl, "/realms/").trim();
    }

    private boolean checkIfRealmChanged(String realm) {
        return !realmCache.equals(realm);
    }
}
