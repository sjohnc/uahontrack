#include <Wire.h>


#define SLAVE_ADDRESS 2
#define MAX_TRAINS 10

byte msgType;
int bytesRequested = 0;
boolean readRequest = false;
byte buffer[32];
byte trainList[MAX_TRAINS];

unsigned long lastCount = 0;

void setup()
{
  Wire.begin(1);        // join i2c bus (address optional for master)
  Serial.begin(115200);  // start serial for output
}

void loop()
{
  if(Serial.available())
  {
    delay(10);

    int i = 0;
    while(Serial.available())
      buffer[i++]=Serial.read();  
        
    switch(buffer[0])
    {
    case 0: // DCC Command
      readRequest = false;
      break;

    case 1: // Loconet Command
      readRequest = false;
      break;

    case 2: // IMU request
      readRequest = true;
      break;

    case 3: // current request
      bytesRequested = 2;
      readRequest = true;
      break;
    }
    
    Wire.beginTransmission(SLAVE_ADDRESS);
    Wire.write(buffer,32);
    Wire.endTransmission();
  }
  delay(10);
  if(readRequest)
  {
    Wire.requestFrom(SLAVE_ADDRESS, bytesRequested);    // request 6 bytes from slave device #2
    readRequest = false;
  }
  delay(10);
  if(Wire.available() >= bytesRequested)
  {
    while(Wire.available())
      Serial.write(Wire.read());
    bytesRequested = 0;
  }
  
/*  if(lastCount + 1000 < millis())
  Wire.beginTransmission(SLAVE_ADDRESS);
  Wire.write(0x03);
  Wire.endTransmission();
  delay(10);
  Wire.requestFrom(2, 2);    // request 6 bytes from slave device #2
  delay(10);
  while(Wire.available())    // slave may send less than requested
  { 
   byte c = Wire.read(); // receive a byte as character
   Serial.print(c,HEX);         // print the character
  }*/
  /*  Spam Current  
   Serial.println("Begin Transmition");
   Wire.beginTransmission(SLAVE_ADDRESS);
   
   Wire.write(0x03);
   
   Wire.endTransmission();  
   Serial.println("End Transmition");
   
   delay(10);
   Wire.requestFrom(2, 2);    // request 6 bytes from slave device #2
   delay(10);
   Serial.print("Bytes Available: ");
   Serial.println(Wire.available());
   while(Wire.available())    // slave may send less than requested
   { 
   byte c = Wire.read(); // receive a byte as character
   Serial.print(c,HEX);         // print the character
   }
   Serial.println();
   delay(1000);  */
   
}

