# Simple Swift Client
[![vertx 4.3.4](https://img.shields.io/badge/vert.x-4.3.4-purple.svg)](https://vertx.io)

This **Java** library provides simple tools to authenticate with **Keystone v3** to your cloud storage using **Swift APIs** allowing the following operations:
* Authentication
* List files
* Upload file
* Download file
* Delete file
* Backup folder (Zips folder and uploads the generated password protected zip file)

Feel free to **check the tests** for guidance on how to setup and use the library

This library uses **Vert.X** to simplify handling Asynchronous tasks, **Zip4j** to zip folders and **Joda-Time** to interact with dates.
>**Note:** The features are optimized to work on the [OVH Storage API](https://docs.ovh.com/gb/en/storage/pca/dev/)
# Maven
Include the following dependency in your **pom.xml** file:
```
<dependency>
  <groupId>io.github.slacesa.simpleSwiftClient</groupId>
  <artifactId>simple_swift_client</artifactId>
  <version>1.0.9</version>
</dependency>
```
Please check the latest version number on [Simple Swift Client Maven repository](https://mvnrepository.com/artifact/io.github.slacesa.simpleSwiftClient/simple_swift_client)
# Building
To launch your tests:
```
mvn clean test
```
To package library:
```
mvn clean package
```
# Help
* [Vert.x Documentation](https://vertx.io/docs/)
* [OpenStack Swift Documentation](https://docs.openstack.org/swift/latest/)
* [Zip4j Code and Documentation](https://github.com/srikanth-lingala/zip4j)
* [Joda Time Documentation](https://www.joda.org/joda-time/)
