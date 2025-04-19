# Mesh Talk SDK 🚀

A powerful, flexible, and easy-to-integrate instant messaging SDK for Java applications.

## 🌟 Overview

Mesh Talk SDK is a comprehensive solution for adding real-time messaging capabilities to your applications. Built with a modular architecture and designed for high performance, it provides everything you need to implement private chats, group messaging, and online status tracking with minimal effort.

## ✨ Features

- **Private Messaging**: Send messages between individual users
- **Group Messaging**: Broadcast messages to multiple users in a group
- **Online Status Tracking**: Check if users are online and which devices they're using
- **Multi-Terminal Support**: Users can connect from multiple devices simultaneously
- **Spring Boot Integration**: Seamless integration with Spring Boot applications
- **Flexible Message Types**: Support for generic message content types
- **Asynchronous Callbacks**: Get notified about message delivery status

## 🏗️ Architecture

Mesh Talk SDK follows a clean, modular architecture based on Domain-Driven Design principles:

```
mesh-talk-sdk/
├── mesh-talk-sdk-application     # Application services and message consumers
├── mesh-talk-sdk-core            # Core components and Spring Boot integration
├── mesh-talk-sdk-domain          # Domain models, interfaces, and annotations
├── mesh-talk-sdk-infrastructure  # Infrastructure services like message multicasting
└── mesh-talk-sdk-interfaces      # External interfaces like message senders
```

### Key Components

- **IMClient**: The main entry point for applications to interact with the messaging system
- **IMSender**: Handles the actual message sending and routing logic
- **MessageListener**: Processes message delivery results
- **MessageListenerMulticaster**: Distributes message results to appropriate listeners

## 🚀 Getting Started

### Prerequisites

- Java 8 or higher
- Spring Boot 2.x
- Maven

### Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.lb</groupId>
    <artifactId>mesh-talk-sdk-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

1. **Send a private message**:

```java
@Autowired
private IMClient imClient;

public void sendMessage() {
    IMPrivateMessage<String> message = new IMPrivateMessage<>();
    message.setSenderId(1001L);
    message.setReceiverId(1002L);
    message.setContent("Hello, how are you?");
    
    imClient.sendPrivateMessage(message);
}
```

2. **Send a group message**:

```java
@Autowired
private IMClient imClient;

public void sendGroupMessage() {
    IMGroupMessage<String> message = new IMGroupMessage<>();
    message.setSenderId(1001L);
    message.setGroupId(2001L);
    message.setContent("Hello everyone!");
    
    imClient.sendGroupMessage(message);
}
```

3. **Check if a user is online**:

```java
@Autowired
private IMClient imClient;

public boolean isUserAvailable(Long userId) {
    return imClient.isOnline(userId);
}
```

4. **Create a message listener**:

```java
@IMListener(listenerType = IMListenerType.PRIVATE_MESSAGE)
public class MyPrivateMessageListener implements MessageListener<String> {
    
    @Override
    public void doProcess(IMSendResult<String> result) {
        // Handle the message result
        System.out.println("Message delivered: " + result.isSuccess());
        System.out.println("Content: " + result.getMessage().getContent());
    }
}
```

## 🧩 Module Details

### mesh-talk-sdk-application

Contains application services and message consumers that process the results of message sending operations.

### mesh-talk-sdk-core

The core module that provides the main client interface and Spring Boot integration. It includes:
- Default implementation of the IMClient interface
- Auto-configuration for Spring Boot
- Redis configuration for message distribution

### mesh-talk-sdk-domain

Contains domain models, interfaces, and annotations that define the core concepts of the messaging system.

### mesh-talk-sdk-infrastructure

Provides infrastructure services like message multicasting that distribute message results to registered listeners.

### mesh-talk-sdk-interfaces

Contains external interfaces like IMSender that handle the actual message sending and routing logic.

## 🔧 Advanced Configuration

### Custom Message Types

The SDK supports generic message content types. You can define your own message types:

```java
public class CustomMessageContent {
    private String text;
    private List<String> attachments;
    // getters and setters
}

IMPrivateMessage<CustomMessageContent> message = new IMPrivateMessage<>();
message.setContent(new CustomMessageContent());
imClient.sendPrivateMessage(message);
```

### Redis Configuration

The SDK uses Redis for message distribution. You can customize the Redis configuration:

```properties
# application.properties
spring.redis.host=localhost
spring.redis.port=6379
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

⭐ Star us on GitHub if you find this project useful!