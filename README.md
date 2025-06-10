# An Order-Book and Matching Engine Microservices System with a Next.js Front-end

## Introduction
This is a microservices based project that collects orders into an orderbook and then performs order matching.

There is  a Next.js frontend that allows the end-user to send orders to the system. 
The system performs order collection and matching and then informs the user of order completion. 

## Tech Stack at a glance
The frontend is a Next.js 15 app. 
The Microservices are Java Spring Boot apps. 
There are two Microservices:
1. Order-Book : _collect orders from the frontend and pass to the rest of the system_
2. Matching-Engine : _performs the matched between the bid and ask orders and updates transactions into order-book mongodb and trade-book mongodb instances._

Kafka is used for async messaging within the Microservices system. Protobuf messages are used for messaging with Kafka.

## Architectural Diagram
System Diagram:
![architectural-diagram](./images/diagram-export-10-06-2025-14_56_36.png "Architectural Diagram")