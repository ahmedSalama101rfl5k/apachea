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
package org.apache.karaf.decanter.appender.file;

import org.apache.karaf.decanter.marshaller.csv.CsvMarshaller;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.event.Event;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class TestFileAppender {

    @Test
    public void testAppender() throws Exception {
        FileAppender fileAppender = new FileAppender();
        fileAppender.marshaller = new CsvMarshaller();
        fileAppender.open("target/test-classes/decanter");
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        map.put("c", "d");
        fileAppender.handleEvent(new Event("testTopic", map));
        fileAppender.handleEvent(new Event("testTopic", map));
        fileAppender.handleEvent(new Event("testTopic", map));
        fileAppender.deactivate();

        File file = new File("target/test-classes/decanter");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Assert.assertEquals("a=b,c=d,event.topics=testTopic", line);
            }
        }
    }

}
