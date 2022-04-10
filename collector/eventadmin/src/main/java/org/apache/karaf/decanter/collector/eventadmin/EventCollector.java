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
package org.apache.karaf.decanter.collector.eventadmin;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import javax.security.auth.Subject;
import java.net.InetAddress;
import java.security.Principal;
import java.util.*;

@Component(
        name = "org.apache.karaf.decanter.collector.eventadmin",
        immediate = true
)
public class EventCollector implements EventHandler {

    private EventAdmin eventAdmin;

    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "eventadmin");
        String karafName = System.getProperty("karaf.name");
        if (karafName != null) {
            data.put("karafName", karafName);
        }
        try {
            data.put("hostAddress", InetAddress.getLocalHost().getHostAddress());
            data.put("hostName", InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            // nothing to do
        }
        for (String property : event.getPropertyNames()) {
            if (property.equals("type")) {
                if (event.getProperty(property) != null) {
                    data.put("eventType", event.getProperty(property).toString());
                } else {
                    data.put("eventType", "eventadmin");
                }
            } else if (property.equalsIgnoreCase("subject")) {
                if (event.getProperty(property) != null && (event.getProperty(property) instanceof Subject)) {
                    data.put(property, convertSubject((Subject) event.getProperty(property)));
                }
            } else {
                data.put(property, event.getProperty(property));
            }
        }
        Event bridge = new Event("decanter/collect/eventadmin/" + topic, data);
        eventAdmin.sendEvent(bridge);
    }

    public Map<String, String> convertSubject(Subject subject) {
        Map<String, String> map = new HashMap<String, String>();
        Set<Principal> principals = subject.getPrincipals();
        for (Principal principal : principals) {
            if (map.get(principal.getClass().getSimpleName()) != null) {
                map.put(principal.getClass().getSimpleName(), map.get(principal.getClass().getSimpleName()) + "," + principal.getName());
            } else {
                map.put(principal.getClass().getSimpleName(), principal.getName());
            }
        }
        return map;
    }

    @Reference
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

}
