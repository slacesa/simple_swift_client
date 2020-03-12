# Simple Swift Client
[![vertx 3.8.5](https://img.shields.io/badge/vert.x-3.8.5-purple.svg)](https://vertx.io)

This **Java** library provides simple tools to authenticate with **Keystone v3** to your cloud storage using **Swift APIs** allowing the following operations:
* Authentication
* List files
* Upload file
* Download file
* Delete file
* Backup folder (Zips folder and uploads the generated password protected zip file)

Feel free to **check the tests** for guidance on how to setup and use the library

This library uses **Vert.X** to simplify handling Asynchronous tasks, **Zip4j** to zip folders and **Joda-Time** to interact with dates. 
The features are optimized to work on the [OVH Storage API](https://docs.ovh.com/gb/en/storage/pca/dev/)

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