package io.apiman.plugins.auth3scale.util.report;

import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.components.http.IHttpClientResponse;
import io.apiman.gateway.engine.policies.PolicyFailureCodes;

public class AuthResponseHandler implements IAsyncResultHandler<IHttpClientResponse> {
    
    private static final AsyncResultImpl<Void> OK_RESPONSE = AsyncResultImpl.create((Void) null);
    
    private final IAsyncResultHandler<Void> resultHandler;
    private final IAsyncHandler<PolicyFailure> policyFailureHandler;
    private final IPolicyFailureFactoryComponent failureFactory;
    
    public AuthResponseHandler(IAsyncResultHandler<Void> resultHandler, 
            IAsyncHandler<PolicyFailure> policyFailureHandler,
            IPolicyFailureFactoryComponent failureFactory) {
        this.resultHandler = resultHandler;
        this.policyFailureHandler = policyFailureHandler;
        this.failureFactory = failureFactory;
    }

    @Override
    public void handle(IAsyncResult<IHttpClientResponse> result) {
        if (result.isSuccess()) {
            System.err.println("Successfully connected to backend");
            
            IHttpClientResponse response = result.getResult();
            PolicyFailure policyFailure = null;

            switch (response.getResponseCode()) {
                case 200:
                case 202:
                    System.out.println("3scale backend was happy");
                    System.out.println(response.getBody());
                    resultHandler.handle(OK_RESPONSE);
                    break;
                case 403:
                    // May be able to treat all error cases without distinction by using parsed response, maybe?
                    policyFailure = failureFactory.createFailure(PolicyFailureType.Authentication, 
                            PolicyFailureCodes.BASIC_AUTH_FAILED, 
                            response.getResponseMessage());
                    break;
                case 409:  // Possibly over limit
                    policyFailure = failureFactory.createFailure(PolicyFailureType.Other, 
                            PolicyFailureCodes.RATE_LIMIT_EXCEEDED, 
                            response.getResponseMessage());
                    break;
                default:
                    RuntimeException re = new RuntimeException("Unexpected or undocumented response code " + response.getResponseCode());
                    resultHandler.handle(AsyncResultImpl.create(re)); // TODO catchall. policy failure or exception?
                    break;
            }
            
            if (policyFailure != null)
                policyFailureHandler.handle(policyFailure);
            
            response.close();
        } else {
            System.err.println("HTTP request failed ...");
            result.getError().printStackTrace(); // TODO, there's actually no point in returning this to the user, is there.
            resultHandler.handle(AsyncResultImpl.create(result.getError()));
        }
    }

}
