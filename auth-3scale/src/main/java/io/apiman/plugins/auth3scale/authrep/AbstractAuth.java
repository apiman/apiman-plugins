/*
 * Copyright 2016 JBoss Inc
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
package io.apiman.plugins.auth3scale.authrep;

import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.PolicyFailure;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public abstract class AbstractAuth
    implements AbstractAuthRepBase {

    public abstract AbstractAuth policyFailureHandler(IAsyncHandler<PolicyFailure> policyFailureHandler);

    public abstract AbstractAuth auth(IAsyncResultHandler<Void> resultHandler);
}
