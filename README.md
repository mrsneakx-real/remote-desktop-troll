# Remote Desktop Troll

> A Java application for **confusing and annoying your friends at work or elsewhere** — built with **Java and JavaFX** and designed for **people with a lot of friends**.

---

## Table of Contents
- [Overview](#overview)
  - [Features](#features)
  - [Roadmap](#roadmap)
- [Software Information](#software-information)
  - [Requirements](#requirements)
- [Getting Started](#getting-started)
  - [Configuration Reference](#configuration-reference)
  - [Contributing](#contributing)

---

# Overview
With this app you can troll your friends with a remote admin dashboard.<br>
You can activate different buttons and make text inputs to affect the "victims" computer.
You can find a feature list below.

## Features
- Admin Dashboard
- Simple button input
- Text input
- ...
- [Exe](#getting-started)

## Roadmap
- [ ] Add encryption
- [ ] Add more documentation
- [ ] Sending and receiving
- [ ] Improve GUI

---

# Software Information
- **Language:** [Java 25](https://www.oracle.com/java/technologies/downloads/#java25)
- **Build tool:** [Gradle](https://gradle.org/)
- **Framework:** Java & [JavaFX](https://www.oracle.com/javase/javafx/)
- **IDE:** [IntelliJ](https://www.jetbrains.com/idea/download/?section=windows) by [JetBrains](https://www.jetbrains.com/)
- **Testing:** Java & User tests

## Requirements

### If running in an IDE *(experimental way, no guarantee)*
- **Java:** 25 
- **Gradle:** 9.2.0
- **Administrator privileges** (optional but highly recommended)
### If running as exe *(recommended way)*
- Windows 11 OS

Verify for running in an IDE:
```bash
//Check Java version

java -version

[Java(TM) SE Runtime Environment (build 25.0.1)]
```
```bash
//Check Gradle version

./gradlew --version

[Gradle 9.2.0]
```


---

# Getting Started

### Test Gradle
```bash
./gradlew test
```

### Start the app (server)
```bash
Start RDT-Server.exe as an Administartor 

or

open PowerShell and run: Start-Process -FilePath "C:\path\to\RDT-Server.exe" -Verb RunAs

//If the app is not able to run as an administrator, some functions will not be available.
```

### Start the app (client)
```bash
Start RDT-Client.exe
 
or

open PowerShell and run: Start-Process -FilePath "C:\path\to\RDT-Server.exe" (run as administrator is optional but recommended)

//To ensure full functionality, we strongly recommend that you also run the client as an administrator.
```

---

## Configuration Reference
List configuration keys and what they do.

| Key          | Required | Default   | Description                      |
|--------------|----------|-----------|----------------------------------|
| IP           | yes      | 127.0.0.1 | Describes the server Ip-adress   |
| Port         | yes      | 9000      | What port the server uses        |
| Verify Token | no       | empty     | Required to use admin privileges |

---

## Contributing
You are permitted to clone this repository on condition that you cite this repository as the source at the top of the readme file.<br>
Failure to comply with this condition will result in us taking further action.

If you would like to actively contribute to the development, you can open an issue with the title: Application for assistance.

---