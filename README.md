
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

// exit psql
exit
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


# Development
 - Make changes
 - write meaningful commit messages
 - run the linter from the terminal: `./gradlew ktfmtFormat`
   - To just check, run `./gradlew ktfmtCheck`
   - You can also add ktfmt to your IDE as a plugin

# QOL
 - TODO: Figure out how to add hot reload/rebuild
