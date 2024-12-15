
# Version info
Framework: Spring boot 3.3.5
Language: Kotlin 1.9.24
DB: postgresql 16
JDK: 21
Gradle: 8.10.2
Apache Ant: 1.10.14

# MacOS install instructions:

## Prerequisites
- Homebrew
- Docker

## Install process
install jdk:
```
brew install openjdk@21
```

link jdk
 ```
 sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
 ```
 
set jdk on path
```
nvim ~/.zshrc

// Add the following to the bottom of the file
export JAVA_HOME=$(/usr/libexec/java_home)

// exit vim... if you can.

// reload your bash profile
source ~/.zshrc
```

install gradle
```
brew install gradle
```

install postgres ( I don't think you have to do this cause Docker will create one for you)
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
```

run the application
```
./gradlew bootRun
```

Look for a message like this in the terminal
```
Tomcat started on port 8080 (http) with context path '/'
```

Open the app in a web browser and check the health endpoint to verify it's running
 - Go to `http://localhost:8080/health` in a web browser
 - If you see "I'm here!" then the application is running locally. 

# FAQ
- What is gradle?
  - A tool that lets you determine how your app is built (manage dependencies, build the app, etc...)
- What is groovy?
  - The language that gradle uses.
- What is flyaway
  - A tool for DB migrations
- What is lombok
  - A library of annotations to reduce boiler plate code (like getters and setters)

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
