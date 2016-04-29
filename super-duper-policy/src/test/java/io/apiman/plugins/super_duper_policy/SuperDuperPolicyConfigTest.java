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
package io.apiman.plugins.super_duper_policy;

import io.apiman.gateway.engine.policies.config.MultipleMatchType;
import io.apiman.gateway.engine.policies.config.UnmatchedRequestType;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test
 *
 * @author rubenrm1@gmail.com
 * @author rachel.yordan@redhat.com
 * Test the {@link SuperDuperConfig}.
 *
 */

@SuppressWarnings({ "nls" })

public class SuperDuperPolicyConfigTest {

    @Test
    public void testParseConfiguration() {
        SuperDuperPolicy policy = new SuperDuperPolicy();

        String config = "{}";
        Object parsed = policy.parseConfiguration(config);
        SuperDuperConfig parsedConfig = (SuperDuperConfig) parsed;
        
        Assert.assertNotNull(parsed);
        Assert.assertEquals(SuperDuperConfig.class, parsed.getClass());
       
        Assert.assertNotNull(parsedConfig.getRules());
        Assert.assertTrue(parsedConfig.getRules().isEmpty());
    }
    
    
    @Test
    public void testBooleanFlags() {
        SuperDuperPolicy policy = new SuperDuperPolicy();

        String config = "{}";
        Object parsed = policy.parseConfiguration(config);
        SuperDuperConfig parsedConfig = (SuperDuperConfig) parsed;
        
        config = "{\r\n" +
                "    \"requestUnmatched\" : \"pass\",\r\n" +
                "    \"multiMatch\" : \"any\"\r\n" +
                "}\r\n" +
                "";
        parsed = policy.parseConfiguration(config);
        parsedConfig = (SuperDuperConfig) parsed;
        Assert.assertEquals(Collections.emptyList(), parsedConfig.getRules());
        Assert.assertEquals(MultipleMatchType.any, parsedConfig.getMultiMatch());
        Assert.assertEquals(UnmatchedRequestType.pass, parsedConfig.getRequestUnmatched());
    }

}
