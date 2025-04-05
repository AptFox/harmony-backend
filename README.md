
### FAQ
- What is gradle?
    - A tool that lets you determine how your app is built (manage dependencies, build the app, etc...)
- What is groovy?
    - The language that gradle uses.
- What is flyaway
    - A tool for DB migrations
- What is lombok
    - A library of annotations to reduce boilerplate code (like getters and setters)
        - annotations - code shortcuts that facilitate framework "magic"
- Linter?
    - ktfmt - https://github.com/cortinico/ktfmt-gradle/tree/main

# Development process
 - Pull the most recent version of dev
 - Create (checkout) a named feature branch from dev
   - `git checkout -b desc-of-feature`
 - Make changes
   - Write tests for changes
     - Most tests should be written for the service layer. 
     - Push as much logic as possible into the service layer when adding features.
   - Run all tests
     - `./gradlew test` or `./gradlew clean test --continue -Dspring.profiles.active=test`
   - Write meaningful commit messages
 - Run the linter from the terminal: `./gradlew ktfmtFormat`
   - To just check, run `./gradlew ktfmtCheck`
   - You can also add ktfmt to your IDE as a plugin
   - Note: You can setup a pre-commit hook for this (Check the QOL section)
 - Commit linted changes
 - Open PR against dev branch
 - Request to have dev merged to main for deployment of new feature
   - PR's merged to `main` trigger CI/CD (deployment to heroku)

# Database changes
 - If you're creating/updating a table, make sure to add a flyway migration script
   - These can be found in `src/main/resources/db/migration`
   - These are raw SQL that should mirror changes in `src/main/kotlin/iterative/harmony/backend/model`
   - To create a new migration, run the following in the terminal:
     - `./gradlew newMigration -Pdesc="TABLE_NAME__description_of_the_migration_here"`
     - Add your SQL to the migration file
     - Run the migration locally: `./gradlew flywayMigrate`
     - Verify that the intended behavior is present in the application
 - Migrations will run automatically during deployment and local docker build
   - If you're not using docker and would like to run migrations locally, run `./gradlew flywayMigrate`

# QOL
- Hot reload:
  - For local dev, SpringDevTools will automatically reload the application ~5 seconds after saving changes to a file.
  - This makes local dev much faster as you don't need to restart the application to see changes.
  - The application must be running debug mode for this to work.
- Linting
  - You can setup a pre-commit hook that runs ktfmt on each commit via gradle by creating a `.git/hooks/pre-commit` file with the following contents:
     ```bash
     #! /bin/sh
     ./gradlew ktfmtFormat
     git add .
     ```
    Then run `chmod +x .git/hooks/pre-commit`
    - You can skip this hook during a commit by adding "--no-verify" to it
  - You can also skip linting during local docker build like this:
     ```bash
       ./gradlew clean build -x ktfmtCheckMain
     ```

# MacOS install instructions:

## Prerequisites
- Homebrew
- Docker

## Install process
### Install jdk:
```
brew install openjdk@21
```

### link jdk
 ```
 sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
 ```

### set jdk on path
```
nvim ~/.zshrc

// Add the following to the bottom of the file
export JAVA_HOME=$(/usr/libexec/java_home)

// exit vim... if you can.

// reload your bash profile
source ~/.zshrc
```

### install gradle
```
brew install gradle
```

### install postgres locally
```
brew install postgres@16

// Add postgres to your path
echo 'export PATH="$PATH:/opt/homebrew/opt/postgresql@16/bin"' >> ~/.zshrc

// reload your bash profile
source ~/.zshrc

// create default user in postgres
createuser -s postgres

// start postgres via homebrew
brew services start postgresql@16

// stop postgres (when you're done)
brew services stop postgresql@16

// login to postgres
psql -U postgres

// create postgres user for harmony (no password set because this user is for local dev only)
postgres=# CREATE USER harmony_app;

// create db for harmony
postgres=# CREATE DATABASE harmony OWNER harmony_app;

// exit psql
exit
```

### generate a JWT secret key
```
openssl rand -base64 32
```

### set environment variables
 - open the repository and create a `.env` file containing the following:
```
FRONT_END_BASE_URL=http://localhost:3000
JWT_SECRET=[THE_JWT_SECRET_KEY_YOU_GENERATED]
JDBC_DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/harmony
#JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/harmony
JDBC_DATABASE_USERNAME=harmony_app
JDBC_DATABASE_PASSWORD=
```

### run the application
```
// build and run the docker image locally
./gradlew runDockerContainer

// OPTIONAL - use gradle to run the app directly if you'd like to run the app w/o docker
//       (JDBC_DATABASE_URL needs to be localhost)
./gradlew bootRun
```

#### Look for a message like this in the terminal
```
Started BackendApplicationKt in 1.931 seconds
```

#### Open the app in a web browser and check the health endpoint to verify it's running
 - Go to `http://localhost:8080/` in a web browser
 - If you see "Hello World" then the application is running locally.

# Troubleshooting
- "Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured."
    - Your database is not configured.
        - Make sure to add the following dependency in the `build.gradle` file
            - `implementation 'org.postgresql:postgresql'`
        - Make sure teh following are set in `application.properties` file
            - `spring.datasource.username=`
            - `spring.datasource.password=`
- psql login issues?
    - a bunch of solutions: https://stackoverflow.com/questions/15301826/psql-fatal-role-postgres-does-not-exist
- I see a login page
    - This means spring security was enabled.
    - Comment out the `spring-boot-starter-security` in `build.gradle`
    - rebuild the application `./gradlew clean build`
    - run the application again `./gradlew bootRun`
- "Error response from daemon: Conflict. The container name "/harmony-backend" is already in use by container"
    - You're trying to rebuild your container and one with the same name already exists
    - Stop & remove the old container before building a fresh one
        - ```
        docker stop harmony-backend
        docker rm harmony-backend
      ```
- "Task :buildDockerImage FAILED"
    - If you see an error like the following:
      ```
          > Task :buildDockerImage FAILED
  
          FAILURE: Build failed with an exception.
  
          * What went wrong:
          Execution failed for task ':buildDockerImage'.
          > A problem occurred starting process 'command 'docker''
      ```
    - The Docker daemon is misbehaving, you may need to restart your machine
    - You can try building the image manually
        - `docker build -t harmony-backend:latest .`
    - You can also try running `./gradlew bootRun` to run the application on localhost
        - You'll need to update your JDBC_DATABASE_URL from `host.docker.internal` to `localhost` in `.env`
- Dependencies not resolving in IntelliJ?
    - Open the `build.gradle` file in IntelliJ
    - Click on the little elephant refresh button in the top right of the window.
    - You can also relaunch your IDE
- If you see a flyway error like the following when building:
  - error:
      ```
         * What went wrong:
         Execution failed for task ':flywayMigrate'.
         > Error occurred while executing flywayMigrate
         Validate failed: Migrations have failed validation
         Detected applied migration not resolved locally: 20250305120418.
         If you removed this migration intentionally, run repair to mark the migration as deleted.
      ```
  - This might have happened because a migration was deleted after it was run. To fix this, run the following command in the terminal:
    - `./gradlew flywayRepair`
    - This will mark the migration as deleted and allow the application to build.

# Version info
- Framework: Spring boot 3.3.5
- Language: Kotlin 1.9.24
- DB: postgresql 16
- JDK: 21
- Gradle: 8.10.2
- Apache Ant: 1.10.14