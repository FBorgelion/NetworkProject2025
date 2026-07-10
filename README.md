# 🌐 NetworkProject2025







## 📌 Project overview

NetworkProject2025 is a client-server chat application based on a custom TCP-like protocol implemented over UDP.

The purpose of this project is to understand how reliable communication can be built on top of an unreliable transport protocol. Instead of using TCP directly, the application relies on UDP and adds custom logic to reproduce some TCP-inspired behaviors.

This project focuses on low-level network communication, protocol design, packet handling and client-server message exchange.

## 🎯 Project objective

UDP is fast and lightweight, but it does not guarantee reliability, ordering or connection management by default.

The objective of this project is to implement a simplified protocol inspired by TCP, while still using UDP as the underlying transport layer.

The project demonstrates how a custom protocol can be designed to manage communication between a client and a server.

## ✨ Main features
+ Client-server chat communication
+ UDP-based message exchange
+ Custom TCP-like protocol
+ Packet creation and processing
+ Client/server socket communication
+ Basic protocol management
+ Structured communication flow between the client and the server

##🧠 Technical concepts

This project helped me work on several important networking concepts:

+ Difference between TCP and UDP
+ Socket programming
+ Client/server communication model
+ Packet-based communication
+ Custom protocol design
+ Message formatting and parsing
+ Reliability mechanisms over UDP
+ Network debugging and testing

## 🏗️ Architecture

The project is based on a classic client-server model.

Client  <-------- UDP packets -------->  Server

The client sends messages to the server using UDP.
The server receives, interprets and processes the messages according to the custom protocol rules.

The protocol layer is responsible for defining how messages are structured, sent, received and interpreted.

## 🔁 TCP-like behavior over UDP

Since UDP does not provide the same guarantees as TCP, the project introduces a simplified reliability layer.

Depending on the implementation, this type of protocol can include mechanisms such as:

+ packet identifiers
+ acknowledgements
+ retransmission logic
+ message ordering
+ connection-like communication flow
+ custom headers
+ packet validation

The goal is not to fully recreate TCP, but to better understand the principles behind reliable transport protocols.

## 🛠️ Technologies and concepts used
+ UDP
+ Socket programming
+ Client-server architecture
+ Custom network protocol
+ Packet handling
+ Network communication
+ Chat application logic
