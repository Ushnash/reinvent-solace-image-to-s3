/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.solace.sample.imagepersistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SpringJCSMPFactory;

@SpringBootApplication
public class ImagePersistenceApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ImagePersistenceApplication.class);

    @Autowired
    private SpringJCSMPFactory solaceFactory;

    @Autowired
    private ImagePersistenceMessageConsumer msgConsumer;

    @Value("${app.queue}")
    private String imageQueueName;

    @Override
    public void run(String... strings) throws Exception {
	final JCSMPSession session = solaceFactory.createSession();

	final EndpointProperties queueEndpointProps = new EndpointProperties();
	// set queue permissions to "consume" and access-type to "exclusive"
	queueEndpointProps.setPermission(EndpointProperties.PERMISSION_CONSUME);
	queueEndpointProps.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);

	// create the queue object locally
	final Queue queue = JCSMPFactory.onlyInstance().createQueue(imageQueueName);

	// Actually provision it, and do not fail if it already exists
	session.provision(queue, queueEndpointProps, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

	logger.info("Subscribed to queue {}", imageQueueName);

	// Create a Flow be able to bind to and consume messages from the Queue.
	final ConsumerFlowProperties flow_prop = new ConsumerFlowProperties();
	flow_prop.setEndpoint(queue);
	flow_prop.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_AUTO);

	EndpointProperties consumerEndpointProps = new EndpointProperties();
	consumerEndpointProps.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);

	// ImagePersistenceMessageConsumer msgConsumer = new
	// ImagePersistenceMessageConsumer();
	FlowReceiver cons = session.createFlow(msgConsumer, flow_prop, consumerEndpointProps);

	logger.info("Connected. Awaiting message...");
	cons.start();

	// Consumer session is now hooked up and running!
    }

    public static void main(String[] args) {
	SpringApplication.run(ImagePersistenceApplication.class, args);
    }
}
