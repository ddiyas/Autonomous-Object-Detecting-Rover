# Autonomous Object-Detecting Rover
### *AKA Vroom Vroom Machine*

## How it works
The user can input a particular object they want the rover search for. The rover then moves around the room with the phone mounted on it in searching for the inputted object using the phone's camera. It avoids obstacles in its way such as walls or furniture. The object detection is done with a Tensorflow Lite object detection model that I coded into the app. Once the object is detected by the app, it sends a BLE signal to the Arduino to signal it to stop moving.

![image](https://github.com/user-attachments/assets/223150c2-6684-4485-b359-82a4cbba672a)

## Components
* Arduino UNO
* Ultrasonic and Bluetooth Low Energy (BLE) sensors
* Android phone (running the app)
* Motor driver + motors + wheels

## Future Improvements
I'd like to experiment with creating a self-mapping or self-navigating rovers in the future. I think it would be really cool if I could use the ultrasonic sensor to create a very rough map of the room based on the distance the rover is from obstacles. I could then perhaps create some kind of ML model to determine where in the map the rover currently is based upon on the distance away it is from obstacles. I could also do something like this with images.
