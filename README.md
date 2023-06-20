# KDoctor
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Homebrew](https://badgen.net/homebrew/v/kdoctor)](https://formulae.brew.sh/formula/kdoctor)

KDoctor is a command-line tool that helps to set up the environment for [Kotlin Multiplatform Mobile](https://kotlinlang.org/lp/mobile/) app development.

![](https://github.com/Kotlin/kdoctor/raw/master/img/screen_1.jpg)

## Overview
KDoctor ensures that all [required components](https://kotlinlang.org/docs/kmm-setup.html) are properly installed and ready for use.
If something is missed or not configured, KDoctor highlights the problem and suggests how to fix the problem. 

KDoctor runs the following diagnostics:
* System - checks an operating system version
* JDK - checks that JDK installation and JAVA_HOME setting
* Android Studio - checks Android Studio installation, Kotlin and Kotlin Multiplatform Mobile plugins 
* Xcode - checks Xcode installation and setup
* Cocoapods - checks ruby environment and cocoapods gem installation

Extra diagnostics:
* Synthetic generated project - downloads and builds project from https://github.com/Kotlin/kdoctor/tree/template
* Local Gradle Project - checks a user's project in the current directory

## Requirements

KDoctor works on MacOS only.

## Installation

### Manual

You can download an archive from GitHub:
1. Go to [Releases](https://github.com/Kotlin/kdoctor/releases) page
2. Download the latest available version there
3. Unpack it to preferred place

### Homebrew

Install via [Homebrew](https://brew.sh/) with the following command:
```bash
brew install kdoctor
```

### Build From Source

To build from source use:
```bash
git clone https://github.com/Kotlin/kdoctor
cd kdoctor
./gradlew assembleReleaseExecutableMacos
```

## Usage

Call KDoctor in the console and wait till it completes the diagnostics 

```
kdoctor
Diagnosing Kotlin Multiplatform Mobile environment...
```

Once KDoctor finishes the diagnostics, it yields a report.  If no problems are found your system is ready for Kotlin Multiplatform Mobile development:

```
Your system is ready for Kotlin Multiplatform Mobile Development!
```

If KDoctor notifies that there are problems, please review the report:

```
KDoctor has diagnosed one or more problems while checking your environment.
Please check the output for problem description and possible solutions.
```

Check the results of each diagnostic. There are 3 possible statuses:
* `[✓]` - Success
* `[✖]` - Failure
* `[!]` - Warning

Pay the most attention to the failed diagnostics (`[✖]`) and look for messages that start with * for problem description and potential solution. 

It is also worth checking diagnostics with warnings (`[!]`) and even successful messages as they may contain useful notes and tips.

Execute KDoctor with a `-h` option to print all available options

```
kdoctor -h
Usage: kdoctor [OPTIONS]

Options:
  --version      Report a version of KDoctor
  -v, --verbose  Report an extended information
  -a, --all      Run extra diagnostics such as a build of a synthetic project
                 and an analysis of a project in the current directory
  -h, --help     Show this message and exit
```

## Sample verbose output

```
kdoctor -v
Environment diagnose:
[✓] Operation System
  ➤ Version OS: macOS 13.1
    CPU: Apple M1 Max

[✓] Java
  ➤ Java (openjdk version "11.0.16" 2022-07-19 LTS)
    Location: /Users/me/.sdkman/candidates/java/current/bin/java
  ➤ JAVA_HOME: /Users/me/.sdkman/candidates/java/current

[✓] Android Studio
  i Multiple Android Studio installations found
  ➤ Android Studio (AI-222.4459.24.2221.9445173)
    Location: /Users/me/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4459.24.2221.9445173/Android Studio Preview.app
    Bundled Java: openjdk 17.0.4.1 2022-08-12
    Kotlin Plugin: 222-1.8.0-release-AS3739.54
    Kotlin Multiplatform Mobile Plugin: 1.0.0-SNAPSHOT
  ➤ Android Studio (AI-221.6008.13.2211.9477386)
    Location: /Users/me/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-2/221.6008.13.2211.9477386/Android Studio.app
    Bundled Java: openjdk 11.0.15 2022-04-19
    Kotlin Plugin: 221-1.7.21-release-for-android-studio-AS5591.52
    Kotlin Multiplatform Mobile Plugin: 0.5.2(221)-3
  i Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.
    Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section

[✓] Xcode
  ➤ Xcode (14.2)
    Location: /Applications/Xcode-14.2.0.app
  i Xcode JAVA_HOME: /Users/me/Library/Java/JavaVirtualMachines/jbr-17.0.5/Contents/Home
    Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths

[✓] Cocoapods
  ➤ ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])
  ➤ ruby gems (3.3.26)
  ➤ cocoapods (1.11.3)

Recommendations:
  ➤ IDE doesn't suggest running all tests in file if it contains more than one class
    More details: https://youtrack.jetbrains.com/issue/KTIJ-22078
Conclusion:
  ✓ Your system is ready for Kotlin Multiplatform Mobile Development!
```

## Issue Tracker
[KDoctor](https://youtrack.jetbrains.com/issues/KT?q=Subsystems:%20KDoctor%20) subsystem in Kotlin issue tracker

## License
[Apache-2.0](https://github.com/Kotlin/kdoctor/blob/master/LICENSE)
