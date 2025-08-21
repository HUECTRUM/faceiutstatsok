# FaceIT Statistics Discord Bot

**ALWAYS follow these instructions first** and only use search or bash commands if you encounter unexpected information that does not match the documentation here.

FaceIT Statistics Discord Bot is a Spring Boot application written in Groovy that provides Counter-Strike FaceIT statistics through Discord chat commands. The bot responds to commands like `.help`, `.maxlvl <player>`, `.mapstats <room_id>`, and `.findfaceit <steam_id>`. It's deployed as a Docker container on AWS ECS.

## Environment Setup

**Bootstrap the development environment:**
1. Install Java 11: `sudo apt-get update && sudo apt-get install -y openjdk-11-jdk`
2. Set Java 11 as default: `sudo update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java`
3. Export JAVA_HOME: `export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64`
4. Download Gradle 6.9.1: `wget https://services.gradle.org/distributions/gradle-6.9.1-bin.zip && unzip gradle-6.9.1-bin.zip`
5. **CRITICAL**: Use the downloaded Gradle version: `./gradle-6.9.1/bin/gradle` (NOT system gradle)

## Building and Testing

**Build the application:**
- Ensure Java 11: `export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64`
- `./gradle-6.9.1/bin/gradle build` -- takes ~15 seconds. **NEVER CANCEL**. Set timeout to 120+ seconds.
- **First build may take longer** due to dependency downloads. **NEVER CANCEL** initial builds. Set timeout to 600+ seconds.

**Run tests:**
- `./gradle-6.9.1/bin/gradle test` -- takes ~1 second. Set timeout to 60+ seconds.
- Tests are located in `ast/src/test/groovy/` for the AST annotation module

**Clean build (when needed):**
- `./gradle-6.9.1/bin/gradle clean build` -- takes ~20 seconds. **NEVER CANCEL**. Set timeout to 180+ seconds.

## Running the Application

**Prerequisites for running:**
- The application **REQUIRES** a Discord bot token set as environment variable `token`
- Without a valid token, the application will start but fail to connect to Discord

**Run using Gradle (development):**
- `./gradle-6.9.1/bin/gradle bootRun` -- starts Spring Boot application. Set timeout to 60+ seconds.
- Application starts on port 8080 by default

**Run using JAR (production-like):**
- First build: `./gradle-6.9.1/bin/gradle build`
- Run: `java -jar build/libs/faceitstatsok-0.0.1-SNAPSHOT.jar`
- With token: `export token=<discord-token> && java -jar build/libs/faceitstatsok-0.0.1-SNAPSHOT.jar`

**Expected startup behavior:**
- Application will show Spring Boot banner and startup logs
- Without token: fails with "You cannot login without a token!" error
- With invalid token: attempts Discord connection, then fails with authentication error
- With valid token: connects to Discord and becomes ready to respond to commands

## Docker Deployment

**Build Docker image:**
- `docker build -t faceitstatsok --build-arg TOKEN_ARG=<discord-token> .` -- takes ~3 seconds. Set timeout to 300+ seconds.
- **Note**: Docker warns about secrets in build args - this is expected for this application design

**Docker image details:**
- Base image: `bellsoft/liberica-openjdk-alpine:11`
- Exposes port 8080
- JAR location: `/app.jar`
- Environment variable: `token` (set from TOKEN_ARG build argument)

## Project Structure

**Multi-project Gradle setup:**
- Root project: main Discord bot application
- `ast/` subproject: custom Groovy AST transformations and annotations

**Key directories:**
- `src/main/groovy/com/nothing/` -- main application source
  - `AppEntrypoint.groovy` -- Spring Boot main class
  - `listeners/` -- Discord message listeners (commands)
  - `service/` -- business logic and external API integration
  - `configuration/` -- Spring configuration classes
  - `annotations/` -- custom annotations
- `src/main/resources/` -- configuration files
  - `connection.properties` -- FaceIT API configuration
  - `steam.properties` -- Steam API configuration
- `build/libs/` -- compiled JAR files
- `.github/workflows/aws.yml` -- AWS ECS deployment pipeline

**Important commands the bot responds to:**
- `.help` -- shows available commands
- `.maxlvl <player_name>` -- max level the player has reached
- `.mapstats <room_id>` -- team stats for a matchmaking lobby
- `.findfaceit <steam_link_id>` -- find FaceIT account by Steam ID

## Validation

**Always run these validation steps after making changes:**
1. **Build validation**: `./gradle-6.9.1/bin/gradle build` -- must complete successfully
2. **Test validation**: `./gradle-6.9.1/bin/gradle test` -- all tests must pass
3. **Application startup**: Test that the JAR runs without compilation errors
4. **Docker build**: Verify Docker image builds successfully

**Manual functional testing:**
- **Cannot fully test** without a valid Discord bot token
- **Cannot test Discord integration** in typical development environments
- **Limited validation**: Verify application starts and attempts Discord connection

## CI/CD Pipeline

**GitHub Actions workflow (`.github/workflows/aws.yml`):**
- Triggers on every push
- Uses Java 11.0.3 and Gradle 6.9.1
- Build command: `gradle build -PTOKEN=${{ secrets.TOKEN }}`
- Deploys to AWS ECS after successful build and Docker image creation
- **Critical**: The CI expects specific Gradle and Java versions

## Common Tasks

**Available Gradle tasks:**
```
./gradle-6.9.1/bin/gradle tasks
```

**Key tasks:**
- `build` -- compile, test, and package
- `bootRun` -- run the Spring Boot application
- `test` -- run unit tests
- `clean` -- delete build artifacts
- `bootJar` -- create executable JAR
- `check` -- run all verification tasks

**Dependency management:**
- Spring Boot 2.5.15 (stable version)
- Groovy 3.0.9
- Javacord 3.3.2 (Discord API)
- Spring WebFlux for HTTP clients

## Troubleshooting

**Build fails with dependency issues:**
- Ensure using Gradle 6.9.1: `./gradle-6.9.1/bin/gradle --version`
- Clean and rebuild: `./gradle-6.9.1/bin/gradle clean build`

**Application fails to start:**
- Check Java version: `java -version` (must be 11.x)
- Verify JAR was built: `ls -la build/libs/`
- Check for missing environment variables

**Docker build fails:**
- Ensure application was built first: `./gradle-6.9.1/bin/gradle build`
- Verify JAR exists: `ls -la build/libs/faceitstatsok-0.0.1-SNAPSHOT.jar`

**Connection issues:**
- Discord connection requires valid bot token and internet access
- FaceIT API requires internet access to `open.faceit.com`
- Steam API requires access to `api.steampowered.com`

## Performance Notes

**Expected timing:**
- Clean build: ~20 seconds
- Incremental build: ~15 seconds
- Test suite: ~1 second
- Docker build: ~3 seconds
- Application startup: ~5 seconds

**Resource requirements:**
- Memory: ~500MB minimum for application
- Disk: ~100MB for dependencies and build artifacts
- Network: requires outbound HTTPS for Discord and FaceIT APIs

## Repository Information

**Quick overview with commands:**
```bash
# Repository root
ls -la
# Key files: build.gradle, settings.gradle, Dockerfile, task-definition.json

# Source structure
find src -name "*.groovy" | wc -l  # 35 Groovy files
find src -type d                   # directory structure

# Build artifacts
ls -la build/libs/                 # JAR files after build
```

**Git and deployment:**
- Main application deployment via AWS ECS
- Container registry: ECR (faceitapihelper repository)
- Task definition: `task-definition.json`
- Container port mapping: 9090 -> 80

**Environment variables:**
- `token` (required) -- Discord bot token
- No other environment configuration required for basic operation

---

**Remember**: This application **requires external network access** to Discord, FaceIT, and Steam APIs to function properly. Most development validation focuses on build success and startup behavior rather than full functional testing.