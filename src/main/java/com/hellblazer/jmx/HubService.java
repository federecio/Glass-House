/** 
 * (C) Copyright 2012 Hal Hildebrand, All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.hellblazer.jmx;

import static com.hellblazer.slp.ServiceScope.SERVICE_TYPE;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.hellblazer.gossip.configuration.v1.Helper;
import com.hellblazer.jmx.cascading.CascadingService;
import com.hellblazer.jmx.discovery.Hub;
import com.hellblazer.jmx.rest.JmxHealthCheck;
import com.hellblazer.jmx.rest.MBeanResource;
import com.hellblazer.nexus.GossipScope;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Environment;

/**
 * @author hhildebrand
 * 
 */
public class HubService extends Service<HubConfiguration> {

    public static void main(String[] argv) throws Exception {
        new HubService().run(argv);
    }

    public HubService() {
        super("JMX Command n' Control");
        addJacksonModule(Helper.getModule());
    }

    private Hub hub;

    /* (non-Javadoc)
     * @see com.yammer.dropwizard.AbstractService#initialize(com.yammer.dropwizard.config.Configuration, com.yammer.dropwizard.config.Environment)
     */
    @Override
    protected void initialize(HubConfiguration configuration,
                              Environment environment) throws Exception {
        MBeanServer mbs = MBeanServerFactory.createMBeanServer(configuration.domainName);
        GossipScope scope = new GossipScope(configuration.gossip.construct());
        scope.start();
        CascadingService cascadingService = new CascadingService();
        mbs.registerMBean(cascadingService, new ObjectName(configuration.name));
        hub = new Hub(cascadingService, configuration.sourcePattern,
                      configuration.sourceMap, scope, configuration.targetPath);
        for (String serviceType : configuration.serviceNames) {
            hub.listenFor("(" + SERVICE_TYPE + "=" + serviceType + ")");
        }
        environment.addHealthCheck(new JmxHealthCheck());
        environment.addResource(new MBeanResource(mbs));
    }

}
