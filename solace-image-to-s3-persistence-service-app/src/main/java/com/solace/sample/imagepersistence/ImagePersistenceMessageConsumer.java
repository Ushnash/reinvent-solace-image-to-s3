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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.solace.services.core.model.SolaceServiceCredentials;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class ImagePersistenceMessageConsumer implements XMLMessageListener {

    private CountDownLatch latch = new CountDownLatch(1);
    private static final Logger logger = LoggerFactory.getLogger(ImagePersistenceMessageConsumer.class);
    private JCSMPSession session = null;

    @Autowired private SpringJCSMPFactory solaceFactory;

    // Other beans that can be used together to generate a customized SpringJCSMPFactory
    @Autowired private SpringJCSMPFactoryCloudFactory springJCSMPFactoryCloudFactory;
    @Autowired private SolaceServiceCredentials solaceServiceCredentials;
    @Autowired private JCSMPProperties jcsmpProperties;

    public ImagePersistenceMessageConsumer(JCSMPSession session) {
        this.session = session;
    }

    @Override
    public void onReceive(BytesXMLMessage msg) {

        FileOutputStream fos = null;

        String clientRegion = Utils.getEnvironmentValue("AWS_REGION", "us-east-2");
        String bucketName = Utils.getEnvironmentValue("REINVENT_BUCKET", "jdiamond-reinvent");
        String objKeyName = UUID.randomUUID() + ".jpg";

        if (msg instanceof TextMessage) {
            logger.info("============= TextMessage received: " + ((TextMessage) msg).getText());
        } else {

            logger.info("============= Message received.");

            String strFilePath =  UUID.randomUUID() + ".jpg";

            // Get the binary attachment into a ByteBuffer and then a byte array
            ByteBuffer binaryAttachment = msg.getAttachmentByteBuffer();

            try {
                fos = new FileOutputStream(strFilePath);

                writeBuffer(binaryAttachment, fos);
                fos.flush();
                fos.close();

                logger.info("============= Face persisted to file : " + strFilePath);

                AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();
                AWSCredentials credentials = provider.getCredentials();

                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(clientRegion)
                        .withCredentials(provider)
                        .build();


                // Upload a file as a new object with ContentType and title specified.
                PutObjectRequest request = new PutObjectRequest(bucketName, "faces/" + objKeyName, new File(strFilePath));
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("image/jpeg");
                metadata.addUserMetadata("x-amz-meta-title", objKeyName);
                request.setMetadata(metadata);
                s3Client.putObject(request);
            }
            catch(AmazonServiceException e) {
                // The call was transmitted successfully, but Amazon S3 couldn't process
                // it, so it returned an error response.
                e.printStackTrace();
            }
            catch(SdkClientException e) {
                // Amazon S3 couldn't be contacted for a response, or the client
                // couldn't parse the response from Amazon S3.
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
             }

        }

        latch.countDown(); // unblock main thread
    }

    @Override
    public void onException(JCSMPException e) {
        logger.info("Consumer received exception:", e);
        latch.countDown(); // unblock main thread
    }

    public void writeBuffer(ByteBuffer buffer, OutputStream stream) throws IOException {
        WritableByteChannel channel = Channels.newChannel(stream);

        channel.write(buffer);
    }

    public CountDownLatch getLatch() {
        return latch;
    }



}
