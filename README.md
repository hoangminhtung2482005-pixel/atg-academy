# ATG Academy

ATG Academy is a full-stack web platform for Arena of Valor (Lien Quan Mobile) players. It helps players understand the current meta, compare hero strength, publish strategy guides, explore esports rankings, and experiment with draft planning through a Ban/Pick lab.

## Live Demo

Coming soon

## Project Overview

ATG Academy combines structured game data with community-created strategy content. The platform is designed for players who want faster access to practical insights: which heroes are strong, how the meta is shifting, what strategies work by role, and how competitive teams perform over time.

## Why ATG Academy?

Arena of Valor players often rely on scattered information from videos, social posts, personal notes, and tournament results. This makes it difficult to evaluate heroes consistently or prepare for ranked matches and competitive drafts.

ATG Academy solves this by centralizing tier lists, meta analysis, strategy guides, esports rankings, and ban/pick planning in one web application.

## Highlights

- Tier List system with `S`, `A`, `B`, `C`, and `D` rankings
- Meta analysis for current hero trends and role strength
- Guide platform for creating, reading, searching, and filtering strategy content
- Esports ranking system powered by Elo-based scoring
- Ban/Pick lab for draft planning and matchup preparation

## Features

### Tier List

- Official and community tier list views
- Hero rankings from `S` to `D`
- Rating support for community interaction
- Admin controls for curated rankings

### Meta Preview

- Homepage preview of current meta highlights
- Fast access to strong heroes, trends, and strategic content
- Dedicated meta endpoint for maintaining featured analysis

### Strategy Guides

- Create and browse user-generated guides
- Search and filter content by relevant criteria
- Support for hero builds, items, arcana, enchantments, and tactical notes

### Esports Ranking

- Team ranking table with Elo score tracking
- Match result management for ranking updates
- Admin tooling for team and match data

### Ban/Pick Lab

- Draft preparation workspace
- Ban and pick planning for ranked and tournament scenarios
- Designed to support strategic matchup discussion

### Admin Dashboard

- User management
- Content and guide moderation
- Esports team and match management
- Meta and tier list administration

## Tech Stack

### Backend

- Java 17
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Google OAuth / Google JWT validation

### Database

- MySQL
- Hibernate ORM

### Frontend

- HTML
- Tailwind CSS
- Vanilla JavaScript
- Static assets served by Spring Boot

### Deployment

- VPS
- Nginx
- MySQL Server

## Architecture Overview

ATG Academy follows a layered Spring Boot architecture with a static frontend served from the backend application.

```text
Browser
  |
  | HTML / Tailwind CSS / Vanilla JS
  v
Static Frontend
  |
  | REST API
  v
Controller Layer
  |
  v
Service Layer
  |
  v
Repository Layer
  |
  v
MySQL Database
```

### Backend Layers

- **Controller**: Exposes REST endpoints and handles HTTP requests.
- **Service**: Contains business logic such as Elo calculation, admin workflows, and data validation.
- **Repository**: Provides database access through Spring Data JPA.
- **Database**: Stores users, heroes, guides, rankings, matches, and tier list data.

Authentication is handled through Google identity tokens and secured API access. Admin-only features are protected through role-based security rules.

## Installation & Setup

### Prerequisites

- Java 17 or later
- Maven or the included Maven Wrapper
- MySQL 8+
- Git
- Nginx for production deployment

### Clone the Repository

```bash
git clone https://github.com/your-username/atg-academy.git
cd atg-academy/demo
```

### Configure MySQL

Create the application database:

```sql
CREATE DATABASE aov_tactics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update `src/main/resources/application.properties` with your local database and security values:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/aov_tactics?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password

spring.jpa.hibernate.ddl-auto=update

app.security.google-client-id=your_google_client_id
app.security.admin-emails=admin@example.com
app.security.staff-emails=
```

### Run the Application

Using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The application will be available at:

```text
http://localhost:8080
```

### Build for Production

```bash
./mvnw clean package
```

Run the packaged application:

```bash
java -jar target/*.jar
```

## API Overview

Base path:

```text
http://localhost:8080/api
```

### Tier Lists

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/tier-lists/official` | Get official tier list data |
| `GET` | `/api/tier-lists/community` | Get community tier list data |
| `GET` | `/api/tier-lists/{id}/ratings` | Get ratings for a tier list entry |
| `POST` | `/api/tier-lists` | Create tier list entry |
| `PUT` | `/api/tier-lists/{id}` | Update tier list entry |
| `POST` | `/api/tier-lists/{id}/rate` | Submit user rating |
| `PUT` | `/api/tier-lists/{id}/admin-rate` | Update admin rating |

### Meta

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/meta` | Get homepage meta preview data |
| `PUT` | `/api/meta` | Update meta preview data |

### Guides

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/guides` | List, search, and filter guides |
| `GET` | `/api/guides/{id}` | Get guide details |
| `POST` | `/api/guides` | Create a new guide |

### Esports

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/esports/teams` | Get public esports team rankings |

### Admin: Users

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/admin/users` | List users |
| `GET` | `/api/admin/users/{id}` | Get user details |
| `PUT` | `/api/admin/users/{id}` | Update user account data |

### Admin: Esports

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/admin/esports/teams` | List teams |
| `GET` | `/api/admin/esports/teams/{id}` | Get team details |
| `POST` | `/api/admin/esports/teams` | Create team |
| `PUT` | `/api/admin/esports/teams/{id}` | Update team |
| `DELETE` | `/api/admin/esports/teams/{id}` | Delete team |
| `GET` | `/api/admin/esports/matches` | List matches |
| `GET` | `/api/admin/esports/matches/{id}` | Get match details |
| `POST` | `/api/admin/esports/matches` | Create match and update rankings |
| `PUT` | `/api/admin/esports/matches/{id}` | Update match |
| `DELETE` | `/api/admin/esports/matches/{id}` | Delete match |
| `DELETE` | `/api/admin/esports/matches` | Delete matches in bulk |
| `POST` | `/api/admin/esports/teams/matches/bulk-import` | Import match data in bulk |

## Folder Structure

```text
atg-academy/
|-- README.md
|-- .github/
`-- demo/
    |-- .mvn/
    |-- src/
    |   |-- main/
    |   |   |-- java/com/example/demo/
    |   |   |   |-- component/
    |   |   |   |-- controller/
    |   |   |   |-- dto/
    |   |   |   |-- entity/
    |   |   |   |-- repository/
    |   |   |   |-- security/
    |   |   |   `-- service/
    |   |   `-- resources/
    |   |       |-- db/
    |   |       |-- static/
    |   |       |   |-- css/
    |   |       |   |-- html/
    |   |       |   |-- images/
    |   |       |   `-- js/
    |   |       `-- application.properties
    |   `-- test/
    |-- mvnw
    |-- mvnw.cmd
    `-- pom.xml
```

## Screenshots

> Screenshots are placeholders and should be replaced with actual product images.

### Home

![Home](./screenshots/home.png)

### Tier List

![Tier List](./screenshots/tier-list.png)

### Guides

![Guides](./screenshots/guides.png)

### Esports

![Esports](./screenshots/esports.png)

## Contributing

Contributions are welcome. To contribute:

1. Fork the repository.
2. Create a feature branch.
3. Make a focused change with clear commits.
4. Run the application locally and verify the affected flow.
5. Open a pull request with a concise description of the change.

Please keep changes consistent with the existing Spring Boot structure and frontend style.

## Future Improvements

- Add automated backend tests for core services and API endpoints
- Add validation and richer error responses for guide creation
- Improve search and filtering for guides and tier lists
- Add comments, ratings, and moderation workflows for guides
- Expand hero analytics with matchup and role-based statistics
- Add real-time draft simulation to the Ban/Pick lab
- Add CI/CD for automated build and deployment
- Externalize production secrets through environment variables
- Add deployment documentation for VPS and Nginx

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
