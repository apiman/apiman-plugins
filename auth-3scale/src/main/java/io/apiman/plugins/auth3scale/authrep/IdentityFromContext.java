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

import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.IdentifierElement;
import io.apiman.gateway.engine.policy.IPolicyContext;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public interface IdentityFromContext {
    default String getIdentityElementFromContext(IPolicyContext context, ApiRequest request, Api api, String canonicalName) {
        String userKey = context.getAttribute("IdentityFromContext::" + canonicalName, null);
        if (userKey == null) {
            // canonicalName -> IdentifierElement{ assignedName, location, canonicalName }
            IdentifierElement element = api.getIdentifiers().get(canonicalName);

            if (element.getLocation() == IdentifierElement.ElementLocationEnum.HEADER) {
                userKey = request.getHeaders().get(element.getAssignedName());
            } else { // else QUERY
                userKey = request.getQueryParams().get(element.getAssignedName());
            }
            context.setAttribute("IdentityFromContext::" + canonicalName, userKey);
        }
        return userKey;
    }
}
