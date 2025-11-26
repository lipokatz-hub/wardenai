# Building WardenAI

## Prerequisites

- **Java Development Kit (JDK) 21** or higher
- **Apache Maven 3.6+**
- **Git** (for cloning)
- **Internet connection** (for downloading dependencies)

## Quick Build

```bash
# Clone the repository
git clone https://github.com/yourusername/wardenai.git
cd wardenai

# Build with Maven
mvn clean package

# Find the JAR
ls -lh target/WardenAI-1.0.0-SNAPSHOT.jar
```

The compiled JAR will be in `target/WardenAI-1.0.0-SNAPSHOT.jar`.

## Detailed Build Instructions

### Step 1: Install Java 21

**Linux (Ubuntu/Debian)**:
```bash
sudo apt update
sudo apt install openjdk-21-jdk
java -version  # Verify installation
```

**macOS (Homebrew)**:
```bash
brew install openjdk@21
java -version
```

**Windows**:
1. Download from [Adoptium](https://adoptium.net/)
2. Install the MSI package
3. Verify with `java -version` in cmd

### Step 2: Install Maven

**Linux (Ubuntu/Debian)**:
```bash
sudo apt update
sudo apt install maven
mvn -version  # Verify installation
```

**macOS (Homebrew)**:
```bash
brew install maven
mvn -version
```

**Windows**:
1. Download from [maven.apache.org](https://maven.apache.org/download.cgi)
2. Extract to `C:\Program Files\Maven`
3. Add to PATH: `C:\Program Files\Maven\bin`
4. Verify with `mvn -version` in cmd

### Step 3: Clone Repository

```bash
git clone https://github.com/yourusername/wardenai.git
cd wardenai
```

Or download ZIP from GitHub and extract.

### Step 4: Build

**Standard Build**:
```bash
mvn clean package
```

**Skip Tests** (faster):
```bash
mvn clean package -DskipTests
```

**Verbose Build** (for debugging):
```bash
mvn clean package -X
```

### Step 5: Locate JAR

```bash
# The compiled JAR is in target/
ls -lh target/WardenAI-1.0.0-SNAPSHOT.jar

# Copy to your server
cp target/WardenAI-1.0.0-SNAPSHOT.jar /path/to/server/plugins/
```

## Build Output

Successful build output:
```
[INFO] --- maven-jar-plugin:3.3.0:jar (default-jar) @ wardenai ---
[INFO] Building jar: /path/to/wardenai/target/WardenAI-1.0.0-SNAPSHOT.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: XX.XXX s
[INFO] Finished at: 2025-XX-XXT07:XX:XXZ
[INFO] ------------------------------------------------------------------------
```

The JAR should be approximately 50-100 KB (without dependencies, they're provided by Paper).

## Maven Build Phases

Understanding what Maven does:

1. **clean** - Deletes `target/` directory
2. **validate** - Validates project structure
3. **compile** - Compiles Java source files
4. **test** - Runs unit tests (if any)
5. **package** - Creates JAR file
6. **verify** - Runs integration tests (if any)
7. **install** - Installs JAR to local Maven repository
8. **deploy** - Deploys JAR to remote repository

## Dependencies

WardenAI uses the following dependencies (automatically downloaded by Maven):

**Provided Dependencies** (supplied by Paper server):
- `io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT`

**Compile Dependencies** (bundled in JAR):
- `io.github.kornkutan:groq4j:1.0.0` (Groq API client)
- `com.squareup.okhttp3:okhttp:4.12.0` (HTTP client)
- `com.google.code.gson:gson:2.10.1` (JSON processing)

**Repository Sources**:
- Paper API: `https://repo.papermc.io/repository/maven-public/`
- Others: Maven Central

## Troubleshooting

### Maven Not Found

**Error**: `mvn: command not found`

**Fix**:
```bash
# Verify Maven installation
which mvn

# If not installed, install Maven (see Step 2)
```

### Java Version Mismatch

**Error**: `Unsupported class file major version 65`

**Fix**:
```bash
# Check Java version (must be 21+)
java -version

# Set JAVA_HOME if needed
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### Dependency Download Failure

**Error**: `Could not resolve dependencies for project com.wardenai:wardenai`

**Fix**:
1. Check internet connection
2. Try again (sometimes repositories are temporarily down)
3. Clear Maven cache: `rm -rf ~/.m2/repository`
4. Rebuild: `mvn clean package`

### Paper API Not Found

**Error**: `Could not find artifact io.papermc.paper:paper-api`

**Fix**: Verify Paper repository is in `pom.xml`:
```xml
<repositories>
    <repository>
        <id>papermc</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>
```

### Build Fails on Windows

**Error**: File path issues or encoding errors

**Fix**:
```bash
# Use Windows paths
mvn clean package -Dfile.encoding=UTF-8

# Or use Git Bash instead of cmd
```

## IDE Setup

### IntelliJ IDEA

1. **Open Project**: File → Open → Select `wardenai/pom.xml`
2. **Trust Project**: Click "Trust Project"
3. **Maven Import**: Wait for Maven to download dependencies
4. **Build**: Build → Build Project (Ctrl+F9)
5. **Run**: Not applicable (this is a server plugin)

**Run Configuration** (for testing):
1. Add Configuration → JAR Application
2. Path to JAR: `/path/to/paper-1.21.1.jar`
3. VM Options: `-Xms2G -Xmx2G`
4. Working Directory: Your test server folder

### Eclipse

1. **Import**: File → Import → Maven → Existing Maven Projects
2. **Select**: Browse to `wardenai/` folder
3. **Finish**: Eclipse imports and builds automatically
4. **Build**: Project → Clean → Build All

### Visual Studio Code

1. **Open Folder**: File → Open Folder → Select `wardenai/`
2. **Extensions**: Install "Java Extension Pack" and "Maven for Java"
3. **Build**: Terminal → Run Task → `maven: package`

## Custom Build

### Change Version

Edit `pom.xml`:
```xml
<version>1.0.0-SNAPSHOT</version>  <!-- Change to 1.0.1, 2.0.0, etc. -->
```

Rebuild:
```bash
mvn clean package
```

### Shade Dependencies

To bundle dependencies in JAR (not recommended for Paper plugins):

1. Add Maven Shade Plugin to `pom.xml`
2. Configure to shade dependencies
3. Rebuild with `mvn clean package`

**Note**: Paper plugins should use `provided` scope to avoid conflicts.

### Custom Packaging

**JAR with dependencies**:
```bash
mvn clean compile assembly:single
```

**Source JAR**:
```bash
mvn source:jar
```

**Javadoc JAR**:
```bash
mvn javadoc:jar
```

## Continuous Integration

### GitHub Actions

Create `.github/workflows/build.yml`:

```yaml
name: Build WardenAI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2

    - name: Build with Maven
      run: mvn clean package

    - name: Upload artifact
      uses: actions/upload-artifact@v3
      with:
        name: WardenAI
        path: target/WardenAI-*.jar
```

This automatically builds on every push/PR.

## Verification

### Verify JAR Contents

```bash
# List files in JAR
jar tf target/WardenAI-1.0.0-SNAPSHOT.jar

# Should contain:
# - META-INF/MANIFEST.MF
# - plugin.yml
# - config.yml
# - knowledge-base.txt
# - com/wardenai/*.class
```

### Check Plugin Metadata

```bash
# Extract plugin.yml
unzip -p target/WardenAI-1.0.0-SNAPSHOT.jar plugin.yml

# Verify:
# - name: WardenAI
# - main: com.wardenai.WardenAI
# - api-version: 1.21
```

### Test on Server

1. Copy JAR to test server's `plugins/` folder
2. Start server
3. Check console for "WardenAI enabled successfully!"
4. Test command: `/wai hello`

## Release Process

### 1. Update Version

```bash
# Remove -SNAPSHOT suffix
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit
```

### 2. Build Release

```bash
mvn clean package
```

### 3. Create Git Tag

```bash
git add pom.xml
git commit -m "Release version 1.0.0"
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### 4. Create GitHub Release

1. Go to repository → Releases → New Release
2. Tag: `v1.0.0`
3. Title: `WardenAI v1.0.0`
4. Upload: `target/WardenAI-1.0.0.jar`
5. Write changelog
6. Publish release

## Development Build

For development (with debug symbols):

```bash
mvn clean package -Pdevelopment
```

For production (optimized, no debug):

```bash
mvn clean package -Pproduction
```

## Clean Build

To completely clean and rebuild:

```bash
# Clean everything
mvn clean
rm -rf target/

# Clear Maven cache (if needed)
rm -rf ~/.m2/repository/com/wardenai

# Rebuild
mvn package
```

## FAQ

**Q: How long does building take?**
A: First build: 2-5 minutes (downloads dependencies). Subsequent: 10-30 seconds.

**Q: Can I build without internet?**
A: Only if dependencies are already cached in `~/.m2/repository/`.

**Q: Why is the JAR so small?**
A: Dependencies are `provided` by Paper server, so they're not bundled.

**Q: Can I use Gradle instead?**
A: Yes, but you'll need to convert `pom.xml` to `build.gradle`.

**Q: Do I need to rebuild after changing config.yml?**
A: No, config.yml is external. Only rebuild after code changes.

---

**Build Issues?** Open an issue on GitHub with your Maven output.
