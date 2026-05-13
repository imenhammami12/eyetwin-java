# Eye2Win – Gaming Community Platform

## Overview

This project was developed as part of the PIDEV – 3rd Year Engineering Program at **Esprit School of Engineering** (Academic Year 2025–2026).

Eye2Win is a full-stack desktop application designed for gaming enthusiasts, featuring tournaments, live streams, Valorant tracking, team management, and more.

## Features

- User registration, login, and two-factor authentication
- Profile management and avatar uploads
- Video upload and administration tools
- Valorant statistics and match tracking
- Live streaming support and access control
- Team and tournament management
- Complaint system with categories, priorities and statuses
- Chat and messaging infrastructure
- Admin dashboard and command line utilities

## Tech Stack

### Frontend
- JavaFX (UI framework)
- FXML (declarative UI layouts)
- CSS (JavaFX styling)
- Scene Builder (optional visual layout editor)

### Backend
- Java 17+
- JDBC / Hibernate ORM
- MySQL
- Maven

## Architecture

MVC architecture built with JavaFX. The application is structured around controllers, model classes, service layers, and FXML views.

```
src/
└── main/
    ├── java/
    │   └── com/eye2win/
    │       ├── controller/       # JavaFX FXML controllers
    │       ├── model/            # Entity / data model classes
    │       ├── service/          # Business logic / domain services
    │       ├── repository/       # Data access layer (JDBC/Hibernate)
    │       ├── util/             # Utility classes and helpers
    │       └── Main.java         # Application entry point
    └── resources/
        ├── fxml/                 # FXML layout files
        ├── css/                  # JavaFX stylesheets
        └── images/               # Application assets
```

## Contributors

- [@imenhammami12](https://github.com/imenhammami12)
- [@ayaben03](https://github.com/ayaben03)
- [@chaimaamri](https://github.com/chaimaamri)
- [@islemijko](https://github.com/islemijko)
- [@trikijoe](https://github.com/trikijoe)

## Academic Context

Developed at **Esprit School of Engineering – Tunisia**  
PIDEV – 3A36 | Academic Year 2025–2026

## Getting Started

### 📦 Prerequisites

Before you begin, ensure you have the following installed:

- Java Development Kit (JDK) 17 or higher
- Maven 3.8+
- JavaFX SDK 17+ (if not bundled via Maven)
- MySQL (or another supported SQL database)
- Scene Builder (optional, for editing FXML layouts)

### 🚀 Installation

1. Clone the repository

```bash
git clone https://github.com/your-org/eye2win-javafx.git
cd eye2win-javafx
```

2. Install dependencies

```bash
mvn clean install
```

3. Configure the database connection

Edit `src/main/resources/config.properties` (or your equivalent config file) with your database credentials:

```properties
db.url=jdbc:mysql://localhost:3306/eye2win
db.username=your_username
db.password=your_password
```

4. Set up the database

Run the provided SQL script to create and populate the schema:

```bash
mysql -u your_username -p eye2win < database/schema.sql
```

Or, if using Hibernate with `hbm2ddl.auto`, set it to `update` or `create` in your persistence configuration.

5. Run the application

```bash
mvn javafx:run
```

Or run the `Main.java` class directly from your IDE (IntelliJ IDEA / Eclipse).

### 🛠 Development Tips

- Use **Scene Builder** to visually edit `.fxml` files located in `src/main/resources/fxml/`.
- JavaFX CSS files are in `src/main/resources/css/` — apply them via `scene.getStylesheets().add(...)`.
- Database access is centralized in the `repository/` layer; update connection settings in `config.properties`.
- Run unit tests with:

```bash
mvn test
```

## Acknowledgments

- [JavaFX](https://openjfx.io/) – Open-source UI framework for Java
- [Hibernate ORM](https://hibernate.org/orm/) – Object-relational mapping for Java
- [Maven](https://maven.apache.org/) – Build and dependency management
- [Scene Builder](https://gluonhq.com/products/scene-builder/) – Visual FXML layout editor
- Esprit School of Engineering – Tunisia for the academic framework and support.
