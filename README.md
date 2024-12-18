
# Version info
 - Framework: Spring boot 3.3.5
 - Language: Kotlin 1.9.24
 - DB: postgresql 16
 - JDK: 21
 - Gradle: 8.10.2
 - Apache Ant: 1.10.14

# Development process
 - Pull the most recent version of dev
 - Create (checkout) a named feature branch from dev
   - `git checkout -b desc-of-feature`
 - Make changes
   - Write tests for changes
   - Run all tests
   - Write meaningful commit message
 - Run the linter from the terminal: `./gradlew ktfmtFormat`
   - To just check, run `./gradlew ktfmtCheck`
   - You can also add ktfmt to your IDE as a plugin
 - Commit linted changes
 - Open PR against dev branch
 - Request to have dev merged to main for deployment of new feature
   - PR's merged to main trigger CI/CD

# QOL
- TODO: Figure out how to add hot reload/rebuild

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

### set environment variables
 - open the repository and create a `.env` file containing the following:
```
DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/harmony;
DATABASE_USER=harmony_app
DATABASE_PASSWORD=
```

### run the application
```
// build and run the docker image locally
./gradlew runDockerContainer

// OPTIONAL - use gradle to run the appdirectly if you'd like to run the app w/o docker(DATABASE_URL needs to be localhost)
./gradlew bootRun
```

#### Look for a message like this in the terminal
```
Tomcat started on port 8080 (http) with context path '/'
```

#### Open the app in a web browser and check the health endpoint to verify it's running
 - Go to `http://localhost:8080/health` in a web browser
 - If you see "I'm here!" then the application is running locally.

