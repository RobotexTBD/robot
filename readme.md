# TBD

This is the source code repository for the brain of a robot named "TBD", who will compete in Robotex 2016 1vs1 football 
competition.

## Technologies

The business logic of the robot's brain is implemented in Java, image processing is implemented in OpenCL.

## Requirements
 
To build and run the robot brain you need to have the following software installed:

* Java 8 JDK & JRE
* OpenCL drivers
* Maven 3
* A webcam

## Building and running

To build the program, copy ```/src/main/resources/example.properties``` to 
```/src/main/resources/application.properties```, fill in your properties and run ```mvn clean compile install``` 
in the project root directory.

To run the program, run ```mvn exec:java``` in the project root directory.