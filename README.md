
# Spring Image Persistence Service

This project is based on the Solace Spring Boot starter app at https://github.com/SolaceProducts/solace-java-spring-boot 
it has been modified to receive a JPG image as binary attachment and save the image to S3 storage.


## Building the Project Yourself 

This project depends on maven for building. To build the jar locally, check out the project and build from source by doing the following:

    git clone https://github.com/jondiamond/solace-java-emotion-analysis-spring-boot.git
    cd solace-java-emotion-analysis-spring-boot.
    mvn package

This will build the auto-configuration jar and associated sample. 

Note: As currently setup, the build requires Java 1.8. If you want to use another older version of Java adjust the build accordingly.

## Running the Sample 

The simplest way to run the sample is from the project root folder using maven. For example:

	mvn spring-boot:run

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information about Spring Boot Auto-Configuration and Starters try these resources:

- [Spring Docs - Spring Boot Auto-Configuration](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/htmlsingle/#using-boot-auto-configuration)
- [Spring Docs - Developing Auto-Configuration](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/htmlsingle/#boot-features-developing-auto-configuration)
- [GitHub Tutorial - Master Spring Boot Auto-Configuration](https://github.com/snicoll-demos/spring-boot-master-auto-configuration)

For more information about Solace technology in general please visit these resources:

- The Solace Developer Portal website at: http://dev.solace.com
- Understanding [Solace technology.](http://dev.solace.com/tech/)
- Ask the [Solace community](http://dev.solace.com/community/).
