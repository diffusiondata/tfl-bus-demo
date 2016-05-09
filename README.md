# tfl-bus-demo
This is a demo using data powered by TFL (Transport For London) Bus Arrival API. 

More on this is available here - http://content.tfl.gov.uk/tfl-live-bus-river-bus-arrivals-api-documentation-v16.pdf

# Requirements
 - An instance of Diffusion or a Reappt service (version 5.7.0  or greater)


# Usage

Android Studio (2.0)
----------------
The project can be imported into Android Studio by:

 - Navigate to ```File``` -> ```New``` -> ```Import Module```
 - Select the directory of the tfl-bus-demo project
 
 From here you can build/run the demo in an emulator or physical device.
 
Gradle Build
----------------
 - Navigate into the bus demo project directory in your terminal
 - Run the command ```./gradlew assembleDebug```
 - An .apk file should be available in the directory ```./app/build/outputs/apk```
 
