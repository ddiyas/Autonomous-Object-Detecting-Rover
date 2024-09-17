#include <AFMotor.h>
#include <NewPing.h>
#include <Servo.h>
#include <SoftwareSerial.h>

#define TRIG_PIN A5
#define ECHO_PIN A0
#define MAX_DISTANCE 300
#define MAX_SPEED 190
#define MAX_SPEED_OFFSET 20
#define RX_PIN 1
#define TX_PIN 0

NewPing sonar(TRIG_PIN, ECHO_PIN, MAX_DISTANCE);

AF_DCMotor motor1(1, MOTOR12_1KHZ);
AF_DCMotor motor2(2, MOTOR12_1KHZ);
AF_DCMotor motor3(3, MOTOR34_1KHZ);
AF_DCMotor motor4(4, MOTOR34_1KHZ);
Servo myservo;

boolean goesForward = false;
int distance = 100;
int speedSet = 0;
int stopSignal;
bool keepLooping = true;
SoftwareSerial bluetooth(TX_PIN, RX_PIN);

void setup() {
  Serial.begin(9600);
  Serial.println("Trying to setup");

  bluetooth.begin(9600);

  myservo.attach(10);
  myservo.write(0);
  delay(2000);
  distance = readPing();
  delay(100);
  distance = readPing();
  delay(100);
  distance = readPing();
  delay(100);
  distance = readPing();
  delay(100);
  Serial.println("Setup complete");
}

void loop() {
  if (keepLooping) {
      stopSignal = bluetooth.read();
      Serial.println("DATA: " + String(stopSignal) + ".");
    
    if (stopSignal == 3) {
      Serial.println("SIGNAL RECEIVED BRO WE GOT ITTTT");
      moveForward();
      delay(300);
      moveBackward();
      delay(300);
      turnRight();
      delay(3000);
      moveForward();
      delay(300);
      moveBackward();
      delay(300);
      keepLooping = false;
      moveStop();
    }
    int distanceR = 0;
    int distanceL = 0;
    delay(40);
    Serial.println("NO obstacle detected - " + String(distance));

    if (distance <= 100) {
      Serial.println("Obstacle detected - " + String(distance));
      moveStop();
      delay(100);
      moveBackward();
      delay(300);
      moveStop();
      delay(200);
      distanceR = lookRight();
      delay(200);
      distanceL = lookLeft();
      delay(200);

      if (distanceR >= distanceL) {
        turnRight();
        moveStop();
      } else {
        turnLeft();
        moveStop();
      }
    } else {
      moveForward();
    }
    distance = readPing();
  }
}

int lookRight() {
  myservo.write(70);
  delay(500);
  int distance = readPing();
  delay(100);
  myservo.write(0);
  return distance;
}

int lookLeft() {
  myservo.write(110);
  delay(500);
  int distance = readPing();
  delay(100);
  myservo.write(0);
  return distance;
  delay(100);
}

int readPing() {
  delay(70);
  int cm = sonar.ping_cm();
  if (cm == 0) {
    cm = 250;
  }
  return cm;
}

void moveStop() {
  motor1.run(RELEASE);
  motor2.run(RELEASE);
  motor3.run(RELEASE);
  motor4.run(RELEASE);
}

void moveForward() {

  if (!goesForward) {
    goesForward = true;
    motor1.run(FORWARD);
    motor2.run(FORWARD);
    motor3.run(FORWARD);
    motor4.run(FORWARD);
    for (speedSet = 0; speedSet < MAX_SPEED; speedSet += 2) {
      motor1.setSpeed(speedSet);
      motor2.setSpeed(speedSet);
      motor3.setSpeed(speedSet);
      motor4.setSpeed(speedSet);
      delay(5);
    }
  }
}

void moveBackward() {
  goesForward = false;
  motor1.run(BACKWARD);
  motor2.run(BACKWARD);
  motor3.run(BACKWARD);
  motor4.run(BACKWARD);
  for (speedSet = 0; speedSet < MAX_SPEED; speedSet += 2) {
    motor1.setSpeed(speedSet);
    motor2.setSpeed(speedSet);
    motor3.setSpeed(speedSet);
    motor4.setSpeed(speedSet);
    delay(5);
  }
}

void turnRight() {
  motor1.run(FORWARD);
  motor2.run(FORWARD);
  motor3.run(BACKWARD);
  motor4.run(BACKWARD);
  delay(500);
  motor1.run(FORWARD);
  motor2.run(FORWARD);
  motor3.run(FORWARD);
  motor4.run(FORWARD);
}

void turnLeft() {
  motor1.run(BACKWARD);
  motor2.run(BACKWARD);
  motor3.run(FORWARD);
  motor4.run(FORWARD);
  delay(500);
  motor1.run(FORWARD);
  motor2.run(FORWARD);
  motor3.run(FORWARD);
  motor4.run(FORWARD);
}
