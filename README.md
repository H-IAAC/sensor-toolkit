# sensor-toolkit

This is a toolkit to help getting sensor's data from Android devices.

- Start/Stop collecting sensor's data
- Save data into local database
- Allow Android clients development (on same device)
- Allow external clients development (on same network)

Supported sensors:

- Accelerometer
- Ambient Temperature
- Gyroscope
- Light
- Magnetic Field
- Proximity
- Gravity
- GPS


## Project Setup

Clone this project. Build and install the SensorAgent application on Android device. Build and install SensorManager on Android device.
To create your own clients, follow the instructions on the README files from `SensorSDK` and `ClientAPILibrary` folders.


## Application architecture

### Clock synchronization
![image](https://github.com/H-IAAC/sensor-toolkit/assets/117912051/177ed8db-8625-424f-bad1-cf96ae4a443c)

### Sensor Data Collection
![image](https://github.com/H-IAAC/sensor-toolkit/assets/117912051/11c593e3-d722-4e57-9930-c4b631a277f4)
