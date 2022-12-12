# KDoctor
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Homebrew](https://badgen.net/homebrew/v/kdoctor)](https://formulae.brew.sh/formula/kdoctor)

KDoctor is a command-line tool that helps to set up the environment for [Kotlin Multiplatform Mobile](https://kotlinlang.org/lp/mobile/) app development.

![](https://github.com/Kotlin/kdoctor/raw/master/img/screen_1.jpg)

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
./gradlew assembleReleaseExecutableMacOs
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
Usage: kdoctor options_list
Options: 
    --version -> print KDoctor version 
    --verbose, -v -> print extended information 
    --debug -> debug mode 
    --help, -h -> Usage info 
```

## Sample verbose output

```
kdoctor -v
[✓] Operation System
  ➤ Version OS: macOS 12.3
    CPU: Apple M1 Max

[✓] Java
  ➤ Java (openjdk version "11.0.16" 2022-07-19 LTS)
    Location: /Users/my/.sdkman/candidates/java/current/bin/java
  ➤ JAVA_HOME: /Users/my/.sdkman/candidates/java/current

[✓] Android Studio
  i Multiple Android Studio installations found
  ➤ Android Studio (AI-222.4345.14.2221.9252092)
    Location: /Users/my/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4345.14.2221.9252092/Android Studio Preview.app
    Bundled Java: openjdk 17.0.4.1 2022-08-12
    Kotlin Plugin: 222-1.7.20-release-AS3739.54
    Kotlin Multiplatform Mobile Plugin: 1.0.0-SNAPSHOT
  ! Android Studio (AI-213.7172.25.2113.9123335)
    Location: /Users/my/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-1/213.7172.25.2113.9123335/Android Studio.app
    Bundled Java: openjdk 11.0.13 2021-10-19
    Kotlin Plugin: 213-1.7.20-release-for-android-studio-AS6777.52
    Kotlin Multiplatform Mobile Plugin: not installed
    Install Kotlin Multiplatform Mobile plugin - https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile
  i Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.
    Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section

[✓] Xcode
  ➤ Xcode (13.4.1)
    Location: /Applications/Xcode.app
  i Xcode JAVA_HOME: /Users/my/Library/Java/JavaVirtualMachines/jbr-17.0.5/Contents/Home
    Xcode JAVA_HOME can be configured in Xcode -> Preferences -> Locations -> Custom Paths

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

## License
[Apache-2.0](https://github.com/Kotlin/kdoctor/blob/master/LICENSE)
