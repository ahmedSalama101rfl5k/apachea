/*
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
package org.apache.karaf.decanter.collector.camel;

import java.util.EventObject;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.management.event.ExchangeCompletedEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeFailureHandledEvent;
import org.apache.camel.management.event.ExchangeRedeliveryEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.camel.management.event.ServiceStartupFailureEvent;
import org.apache.camel.management.event.ServiceStopFailureEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecanterEventNotifier extends EventNotifierSupport {

    private static final Logger LOG = LoggerFactory.getLogger(DecanterEventNotifier.class.getName());

    private EventAdmin eventAdmin;
    private String camelContextMatcher = ".*";
    private String routeMatcher = ".*";
    private DefaultExchangeExtender dextender = new DefaultExchangeExtender();
    private DecanterCamelEventExtender extender;

    public EventAdmin getEventAdmin() {
        return eventAdmin;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void setCamelContextMatcher(String camelContextMatcher) {
        this.camelContextMatcher = camelContextMatcher;
    }

    public void setRouteMatcher(String routeMatcher) {
        this.routeMatcher = routeMatcher;
    }

    public void setExtender(DecanterCamelEventExtender extender) {
        this.extender = extender;
    }
    
    public void setIncludeBody(boolean includeBody) {
        dextender.setIncludeBody(includeBody);
    }

    public void setIncludeHeaders(boolean includeHeaders) {
        dextender.setIncludeHeaders(includeHeaders);
    }

    public void setIncludeProperties(boolean includeProperties) {
        dextender.setIncludeProperties(includeProperties);
    }
    
    private boolean isIgnored(EventObject event) {
        if (isIgnoredBySourceType(event)) {
            return true;
        }
        
        if (event instanceof ExchangeSentEvent && isIgnoreExchangeSentEvents()) {
            return true;
        }
        if (event instanceof ExchangeSendingEvent && isIgnoreExchangeSendingEvents()) {
            return true;
        }
        if (event instanceof ExchangeFailureHandledEvent && isIgnoreExchangeFailedEvents()) {
            return true;
        }
        if (event instanceof ExchangeRedeliveryEvent && isIgnoreExchangeRedeliveryEvents()) {
            return true;
        }
        if (event instanceof ExchangeCompletedEvent && isIgnoreExchangeCompletedEvent()) {
            return true;
        }
        if (event instanceof ExchangeCreatedEvent && isIgnoreExchangeCreatedEvent()) {
            return true;
        }
            
        if (event instanceof ServiceStartupFailureEvent && isIgnoreServiceEvents()) {
            return true;
        }
        if (event instanceof ServiceStopFailureEvent && isIgnoreServiceEvents()) {
            return true;
        }
        
        return false;
    }

    private boolean isIgnoredBySourceType(EventObject event) {
        Object source = event.getSource();
        return (source instanceof Exchange && isIgnoreExchangeEvents()
            || source instanceof Route && isIgnoreRouteEvents()
            || source instanceof CamelContext && isIgnoreCamelContextEvents());
    }

    @Override
    public boolean isEnabled(EventObject eventObject) {
        if (eventObject == null) {
            return false;
        }
        if (isIgnored(eventObject)) {
            return false;
        }
        Object source = eventObject.getSource();
        if (source instanceof Exchange) {
            Exchange exchange = (Exchange)source;
            boolean contextMatches = exchange.getContext().getName().matches(camelContextMatcher);
            if (exchange.getFromRouteId() != null) {
                return exchange.getFromRouteId().matches(routeMatcher) && contextMatches;
            } else {
                return contextMatches;
            }
        } else if (source instanceof CamelContext) {
            CamelContext context = (CamelContext)eventObject.getSource();
            return context.getName().matches(camelContextMatcher);
        } else if (source instanceof Route) {
            Route route = (Route)source;
            boolean contextMatches = route.getRouteContext().getCamelContext().getName().matches(camelContextMatcher);
            return contextMatches && route.getId().matches(routeMatcher);
        } else {
            return false;
        }
    }

    public void notify(EventObject event) throws Exception {
        try {
            Map<String, Object> eventMap = new CamelEventMapper().toMap(event);
            Object source = event.getSource();
            if (source instanceof Exchange) {
                dextender.extend(eventMap, (Exchange)source);
                if (extender != null) {
                    extender.extend(eventMap, (Exchange)source);
                }
            }
            eventAdmin.postEvent(new Event("decanter/collect/camel/event", eventMap));
        } catch (Exception ex) {
            LOG.warn("Failed to handle event", ex);
        }
    }

    protected void doStart() throws Exception {
    }

    protected void doStop() throws Exception {
    }

}
