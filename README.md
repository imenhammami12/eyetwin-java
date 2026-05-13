# Eye2Win – Gaming Community Platform

## Overview
This project was developed as part of the PIDEV – 3rd Year Engineering Program at **Esprit School of Engineering** (Academic Year 2025–2026).

Eye2Win is a full-stack web platform designed for gaming enthusiasts, featuring tournaments, live streams, Valorant tracking, team management, and more.

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
- Twig (Symfony templating)
- Webpack Encore
- Node.js & npm

### Backend
- Symfony (PHP 8.1+)
- Doctrine ORM
- MySQL
- Composer

## Architecture
MVC architecture built with Symfony. The application is structured around controllers, Doctrine entities, service classes, and Twig templates.

```
src/
├── Controller/          # HTTP controllers
├── Entity/              # Doctrine entities
├── Repository/          # Doctrine repositories
├── Service/             # Domain services
├── Form/                # Symfony form types
├── Command/             # CLI commands
├── EventListener/       # Symfony event listeners
├── EventSubscriber/     # Event subscribers
└── Twig/                # Twig extensions and templates helpers

templates/              # Twig templates
public/                 # Public assets (JS/CSS) & entry point
config/                 # Symfony configuration files
tests/                  # PHPUnit tests
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
- PHP 8.1 or higher
- Composer
- Node.js & npm (for frontend assets)
- MySQL (or another supported SQL database)
- Docker & Docker Compose (optional, for development)

### 🚀 Installation

1. Clone the repository
```bash
git clone https://github.com/your-org/eyetwin-metamind.git
cd eyetwin-metamind
```

2. Install PHP dependencies
```bash
composer install
```

3. Install JavaScript dependencies & build assets
```bash
npm install
npm run dev
```

4. Configure environment variables
```bash
cp .env .env.local
```
Edit `.env.local` with your database credentials, mailer settings, and other secrets.

5. Setup the database
```bash
php bin/console doctrine:database:create
php bin/console doctrine:migrations:migrate
```

6. Run tests
```bash
php bin/phpunit
```

7. Start the development server
```bash
symfony server:start
```

Or using Docker:
```bash
docker-compose up -d --build
```

### 🛠 Development Tips
- Use `php bin/console doctrine:migrations:diff` to generate migration files after updating entities.
- Assets are managed with Webpack Encore; run `npm run watch` for automatic rebuilding.
- Custom CLI tools are available under `src/Command` (e.g. `DebugVideoUploadCommand`, `ListUsersCommand`).

## Acknowledgments
- [Symfony](https://symfony.com/doc/current/index.html)
- [Doctrine ORM](https://www.doctrine-project.org/projects/orm.html)
- [Webpack Encore](https://symfony.com/doc/current/frontend.html)
- Esprit School of Engineering – Tunisia for the academic framework and support.


