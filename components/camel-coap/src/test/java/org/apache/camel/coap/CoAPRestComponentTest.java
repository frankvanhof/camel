/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.coap;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.IOConverter;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.junit.Test;

public class CoAPRestComponentTest extends CamelTestSupport {
    int coapport = AvailablePortFinder.getNextAvailable();
    int jettyport = AvailablePortFinder.getNextAvailable();
    
    @Test
    public void testCoAP() throws Exception {
        NetworkConfig.createStandardWithoutFile();
        CoapClient client;
        CoapResponse rsp;
        
        client = new CoapClient("coap://localhost:" + coapport + "/TestResource/Ducky");
        client.setTimeout(1000000);
        rsp = client.get();
        assertEquals("Hello Ducky", rsp.getResponseText());
        rsp = client.post("data", MediaTypeRegistry.TEXT_PLAIN);
        assertEquals("Hello Ducky: data", rsp.getResponseText());
        
        client = new CoapClient("coap://localhost:" + coapport + "/TestParms?id=Ducky");
        client.setTimeout(1000000);
        rsp = client.get();
        assertEquals("Hello Ducky", rsp.getResponseText());
        rsp = client.post("data", MediaTypeRegistry.TEXT_PLAIN);
        assertEquals("Hello Ducky: data", rsp.getResponseText());
        
        
        URL url = new URL("http://localhost:" + jettyport + "/TestResource/Ducky");
        InputStream ins = url.openConnection().getInputStream();
        assertEquals("Hello Ducky", IOConverter.toString(new InputStreamReader(ins)));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration("coap").host("localhost").port(coapport);
                restConfiguration("jetty").host("localhost").port(jettyport);
                rest("/TestParms")
                    .get().to("direct:get1")
                    .post().to("direct:post1");
                rest("/TestResource")
                    .get("/{id}").to("direct:get1")
                    .post("/{id}").to("direct:post1");
                
                from("direct:get1").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        String id = exchange.getIn().getHeader("id", String.class);
                        exchange.getOut().setBody("Hello " + id);
                    }
                });
                from("direct:post1").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        String id = exchange.getIn().getHeader("id", String.class);
                        exchange.getOut().setBody("Hello " + id + ": " + exchange.getIn().getBody(String.class));
                    }
                });
            }
        };
    }
}
