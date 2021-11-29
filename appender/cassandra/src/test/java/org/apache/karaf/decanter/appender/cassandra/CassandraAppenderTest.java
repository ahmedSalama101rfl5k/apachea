package org.apache.karaf.decanter.appender.cassandra;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.config.Schema;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.karaf.decanter.api.marshaller.Marshaller;
import org.apache.karaf.decanter.marshaller.json.JsonMarshaller;
import org.hamcrest.core.IsNot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class CassandraAppenderTest {

    private static final String KEYSPACE = "decanter";
    private static final int CASSANDRA_PORT = 9142;
    private static final String CASSANDRA_HOST = "localhost";
    private static final String TABLE_NAME = "decanter";
    private static final String TOPIC = "decanter/collect/jmx";
    private static final long TIMESTAMP = 1454428780634L;

    private static final Logger logger = LoggerFactory.getLogger(CassandraAppenderTest.class);
    private CassandraDaemon cassandraDaemon;
    
    @Before
    public void setUp() throws Exception {

        System.setProperty("cassandra-foreground", "false");
        System.setProperty("cassandra.boot_without_jna", "true");

        cassandraDaemon = new CassandraDaemon(true);
        logger.info("starting cassandra deamon");
        cassandraDaemon.init(null);
        cassandraDaemon.start();
        
        logger.info("cassandra up and runnign");
        
    }

    @After
    public void tearDown() throws Exception {
        Schema.instance.clear();
        logger.info("stopping cassandra");
        cassandraDaemon.stop();
        logger.info("destroying the cassandra deamon");
        cassandraDaemon.destroy();
        logger.info("cassandra is removed");
        cassandraDaemon = null;
    }

    @Test
    public void testHandleEvent() throws Exception {
        Marshaller marshaller = new JsonMarshaller();
        CassandraAppender appender = new CassandraAppender(marshaller, KEYSPACE, TABLE_NAME, CASSANDRA_HOST, CASSANDRA_PORT);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(EventConstants.TIMESTAMP, TIMESTAMP);
        Event event = new Event(TOPIC, properties);
        
        appender.handleEvent(event);
        
        Session session = getSesion();
        
        ResultSet execute = session.execute("SELECT * FROM "+ KEYSPACE+"."+TABLE_NAME+";");
        List<Row> all = execute.all();
        assertThat(all, not(nullValue()));
        
        assertThat(all.get(0).getTimestamp("timeStamp").getTime(), is(TIMESTAMP));
        
        session.close();
    }

    private Session getSesion() {
        Builder clusterBuilder = Cluster.builder().addContactPoint(CASSANDRA_HOST);
        clusterBuilder.withPort(CASSANDRA_PORT);

        Cluster cluster = clusterBuilder.build();
        return cluster.connect();
    }
    
}
