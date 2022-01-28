# KDoctor

KDoctor is a command-line tool that helps to set up the environment for [Kotlin Multiplatform Mobile](https://kotlinlang.org/lp/mobile/) app development.

## Overview
KDoctor ensures that all [required components](https://kotlinlang.org/docs/kmm-setup.html) are properly installed and ready for use.
If something is missed or not configured Kdoctor highlights the problem and suggests how to fix the problem. 

KDoctor runs the following diagnostics:
* System - checks operating system version
* JDK - checks that JDK installation and JAVA_HOME setting
* Android Studio - checks Android Studio installation, Kotlin and Kotlin Multiplatform Mobile plugins 
* Xcode - checks Xcode installation and setup
* Cocoapods - checks ruby environment, cocoapods and cocoapods-generate gems installation

## Requirements

KDoctor works on macOS only.

## Installation

### Manual

You can download an archive from GitHub:
1. Go to [Releases](https://github.com/Kotlin/kdoctor/releases) page
2. Download the latest available version there
3. Unpack it to preferred place
<!--- uncomment once available on Homebrew
### Homebrew

Install via [Homebrew](https://brew.sh/) with the following command:
```bash
brew install kdoctor
```
-->
### Build From Source

To build from source use:
```bash
git clone https://github.com/Kotlin/kotlin-interactive-shell
cd kdoctor
./gradlew assembleReleaseExecutableMacOs
```

## Usage

Call KDoctor in the console and wait till it completes the diagnostics 

```bash
kdoctor
Diagnosing Kotlin Multiplatform Mobile environment...
```

Once KDoctor finishes the diagnostics, it yields a report.  If no problems are found your system is ready for Kotlin Multiplatform Mobile development:

`Your system is ready for Kotlin Multiplatform Mobile Development!`

If KDoctor notifies that there are problems, please review the report:

```
KDoctor has diagnosed one or more problems while checking your environment.
Please check the output for problem description and possible solutions.
```

Check the results of each diagnostic. There are 3 possible statuses:
* \[v] - Success
* \[x] - Failure
* \[!] - Warning

Pay the most attention to the failed diagnostics ([x]) and look for messages that start with * for problem description and potential solution. 

It is also worth checking diagnostics with warnings ([!]) and even successful messages as they may contain useful notes and tips.

Execute KDoctor with a `-v` option to print the tool's version
```
kdoctor -v
0.0.1
```
##Sample output

```
[v] System                                           
    macOS (11.6.2)
    CPU:  Dual-Core Intel Core i5
    
[v] Java
    Java (java 17.0.1 2021-10-19 LTS)
    Location: /Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home/bin/java
    
    JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home
    
    * Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.
          Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section
    
[v] Android Studio
    Android Studio (2020.3)
    Location: /Applications/Android Studio.app
    Bundled Java: openjdk 11.0.10 2021-01-19
    Kotlin Plugin: 203-1.6.10-release-923-AS7717.8
    Kotlin Multiplatform Mobile Plugin: 0.3.0(203-1.6.0-release-795-IJ)-54
    
[v] Xcode
    Xcode (13.2.1)
    Location: /Applications/Xcode.app
    
[v] Cocoapods
    ruby (ruby 2.6.3p62 (2019-04-16 revision 67580) [x86_64-darwin20])
    
    ruby gems (3.0.9)
    
    cocoapods (1.11.2)
    
    cocoapods-generate (2.2.2)
```

## License
[Apache-2.0](https://github.com/Kotlin/kdoctor/blob/master/LICENSE)