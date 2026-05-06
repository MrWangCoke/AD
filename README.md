# AD Project

Android client and Spring Boot backend for the campus account/binding app.

## Project Structure

```text
AD-project/
  android-app/   Android Jetpack Compose app
  backend/       Spring Boot backend API
```

## Android App

Open `android-app/` in Android Studio.

The debug API base URL is configured in:

```text
android-app/app/build.gradle.kts
```

For emulator access to a backend running on this computer, use:

```kotlin
buildConfigField("String", "AUTH_BASE_URL", "\"http://10.0.2.2:8080/\"")
```

For the deployed backend, use:

```kotlin
buildConfigField("String", "AUTH_BASE_URL", "\"https://ahutdx.online/\"")
```

## Backend

Use JDK 17 for the backend. The Maven project is configured with
`<java.version>17</java.version>`.

Run from `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend uses PostgreSQL. Configure database settings with environment variables:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/campus_network_db"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
```

For local development, you can also copy `backend/.env.local.example` to
`backend/.env.local`, fill in your PostgreSQL password, and start with:

```powershell
.\start-backend.ps1
```

If you start from IntelliJ IDEA directly, add these values in
`Run/Debug Configurations > Environment variables`:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/campus_network_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password
```

Default table for users:

```text
users
```

## GitHub Notes

Do not commit local generated files such as `build/`, `target/`, `.gradle/`, `.idea/`, or `local.properties`.

Do not commit real database passwords or private server credentials.
