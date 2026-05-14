# SAFBuilder — Modernized Version (v2.0.0)

A tool that turns content files and a metadata spreadsheet into a **Simple Archive Format (SAF)** package for batch import to DSpace.

This version is a modernization of the original [DSpace-Labs/SAFBuilder](https://github.com/DSpace-Labs/SAFBuilder), updated to **Java 17** with a clean architecture and a modern **Desktop UI**.

## 🚀 Key Modern Features
*   **Decoupled Directories:** No longer restricted to the CSV folder. You can independently select the **Source Directory** (where your PDFs are) and the **Output Directory** (where the SAF package will be generated).
*   **Modern UI:** Dark mode interface powered by FlatLaf for a professional look and feel.
*   **Native Packaging:** Generate professional installers (.dmg for Mac, .msi for Windows) or portable app folders that include their own Java runtime.

## Requirements
* **Java 17** or higher
* **Maven 3.8+**
* **WiX Toolset v3.11** (Only required for creating Windows `.msi` installers)

## Quick Start (GUI)
The easiest way to use SAFBuilder is via its modern interface:
```bash
# Run the local script to launch the UI
./safbuilder.sh
```

## Quick Start (CLI)
To process a CSV and generate a SAF package from the command line:
```bash
./safbuilder.sh -c path/to/metadata.csv -d path/to/bitstreams -D path/to/output -z
```

## Usage Options
```bash
usage: safbuilder -c <metadata.csv> [options]
 -c,--csv <arg>           Path to the metadata CSV file.
 -d,--directory <arg>     (Optional) Source directory where content files (PDFs) are located.
 -D,--destination <arg>   (Optional) Destination directory for the generated SAF package.
 -z,--zip                 (Optional) ZIP the resulting SAF directory (saved in Destination).
 -o,--output-name <arg>   (Optional) Custom name for the output folder (default: SimpleArchiveFormat).
 -m,--manifest            (Optional) Generate a skeleton CSV manifest from files in a folder.
 -h,--help                Show this help.
```

## 📦 How to Package & Distribute

### 1. Native Installer (.dmg / .msi)
Creates a formal installer for your operating system.
*   **Mac:** `mvn clean package -Pnative-installer -DskipTests` (Generates `.dmg`)
*   **Windows:** `mvn clean package -Pnative-installer -DskipTests` (Generates `.msi`, requires WiX 3.11)

### 2. Portable Version (App Image)
Creates a self-contained folder that can be shared and run without installation.
```bash
mvn clean package -Pnative-app-image -DskipTests
```
The result will be in `target/installer/SAFBuilder`.

### 3. Executable JAR
The classic single-file distribution:
```bash
mvn clean package -DskipTests
```

---
*Maintained by: Hernán Lagos*
