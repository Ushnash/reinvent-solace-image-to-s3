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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageListener;

import twitter4j.MediaEntity;
import twitter4j.Status;

@Component
public class ImagePersistenceMessageConsumer implements XMLMessageListener {

    private CountDownLatch latch = new CountDownLatch(1);
    private static final Logger logger = LoggerFactory.getLogger(ImagePersistenceMessageConsumer.class);

    String clientRegion;
    String bucketName;
    String objectKeyPrefix;

    String source;

    // default constructor
    @Autowired
    public ImagePersistenceMessageConsumer(@Value("${aws.clientregion}") String clientRegion,
	    @Value("${aws.s3.bucket}") String bucketName, @Value("${aws.s3.objectkeyprefix}") String objectKeyPrefix,
	    @Value("${app.image.source.default}") String source) {

	this.clientRegion = clientRegion;
	this.bucketName = bucketName;
	this.objectKeyPrefix = objectKeyPrefix;
	this.source = source;

	logger.debug("============= AWS Client Region = {}", clientRegion);
	logger.debug("============= AWS S3 Bucket = {}", bucketName);
	logger.debug("============= AWS Object Key Prefix = {}", objectKeyPrefix);
	logger.debug("============= AWS Default Image Source = {}", source);
    }

    @Override
    public void onReceive(BytesXMLMessage msg) {

	String objKeyName = UUID.randomUUID().toString().replace("-", "") + ".jpg";

	String filePath = null;
	byte[] imageData = null;
	boolean isJpeg = true;

	if (msg instanceof TextMessage) {
	    logger.info("============= TextMessage received: {} ", ((TextMessage) msg).getText());
	} else {

	    logger.info("============= Message received.");

	    ByteBuffer binaryAttachment = msg.getAttachmentByteBuffer();

	    logger.info("============= Checking if this message is a tweet");

	    // See if the message is a tweet
	    try {
		byte[] tweetBytes = binaryAttachment.array();

		Status tweet = (Status) convertFromBytes(tweetBytes);

		source = tweet.getUser().getScreenName();
		logger.info("Tweet received from {}", source);

		filePath = getFilePath(objKeyName, source);

		MediaEntity[] medias = tweet.getMediaEntities(); // get the media entities from the status

		for (MediaEntity m : medias) {

		    try {
			URL url = new URL(m.getMediaURL());

			InputStream in = new BufferedInputStream(url.openStream());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int n = 0;
			while (-1 != (n = in.read(buf))) {
			    out.write(buf, 0, n);
			}
			out.close();
			in.close();

			imageData = out.toByteArray();

			if (!"jpg".equals(getExtension(m.getType()))) {
			    isJpeg = false;
			}

		    } catch (Exception ex) {
			ex.printStackTrace();
		    }

		    // only allowing one image per tweet
		    break;
		}
	    } catch (Exception ex) {

		logger.info("============= Not a tweet. Treating the message as a JPG image.");

		// Treat binaryAttachment as an image and not a tweet
		imageData = new byte[binaryAttachment.remaining()];
		binaryAttachment.get(imageData);

		filePath = getFilePath(objKeyName, source);

	    }

	    try {

		if (isJpeg) {

		    // Build an S3 Client to persist the image.
		    // Passing null to the Credential manager uses the
		    // DefaultAWSCredentialsProviderChain
		    AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion).build();
		    // AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient().build();

		    // Upload a file as a new object with ContentType and title specified.
		    InputStream fileInputStream = new ByteArrayInputStream(imageData);
		    ObjectMetadata metadata = new ObjectMetadata();
		    metadata.setContentType("image/jpeg");
		    metadata.addUserMetadata("x-amz-meta-title", objKeyName);
		    metadata.setContentLength(imageData.length);

		    PutObjectRequest request = new PutObjectRequest(bucketName, objectKeyPrefix + filePath,
			    fileInputStream, metadata);
		    s3Client.putObject(request);

		    logger.info("============= Image persisted to file: {} ", filePath);
		}
	    } catch (AmazonServiceException e) {
		// The call was transmitted successfully, but Amazon S3 couldn't process
		// it, so it returned an error response.
		e.printStackTrace();
	    } catch (SdkClientException e) {
		// Amazon S3 couldn't be contacted for a response, or the client
		// couldn't parse the response from Amazon S3.
		e.printStackTrace();
	    }

	}

	latch.countDown(); // unblock main thread
    }

    private String getFilePath(String objKeyName, String source) {
	String filePath;// Set filePath
	filePath = source + "-" + objKeyName;
	return filePath;
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

    private Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
	try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
	    return in.readObject();
	}
    }

    private String getExtension(String type) {
	if (type.equals("photo")) {
	    return "jpg";
	} else if (type.equals("video")) {
	    return "mp4";
	} else if (type.equals("animated_gif")) {
	    return "gif";
	} else {
	    return "err";
	}
    }

    public CountDownLatch getLatch() {
	return latch;
    }

}
