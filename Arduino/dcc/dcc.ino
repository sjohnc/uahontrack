/*
 Title:  DCC Controller
 Author: Scott Schiavone
 Data:   10/25/2013
 
 Software Description:  
 
 This code takes three bytes of data (address, data, error) and sends them out
 one bit at a time as a DCC signal. The packet format should conform to NMRA 
 standard S 9.2 July 2004.
 
 Hardware Description:
 
 Atmega328P running at 16MHz
 
 The DCC signal is sent to model rail road train track rails through four mosfets
 configured in an H-Bridge pattern. The output pins to the H-Bridge from the Atmega328P 
 are PD5 and PD6.  
 */

#include "TimerOne.h"
#include <Wire.h>

#define ONE_BIT 56                              // 56 µs
#define ZERO_BIT 112                            // 112 µs
#define MAX_TRAINS 10
#define I2C_ADDRESS 2

byte address = 0x00;
byte data = 0x00;
byte error = 0x00;

byte currentTrain = 0;                          // current train being written to
byte bitCount = 0;                              // used for keeping track of what stage in packet program is
byte numberOfTrains = 0;                        // number of trains on track
byte dataBack[32];
unsigned int averageCurrent = 0;
unsigned int currentReadings[100];
byte currentIndex = 0;
int counter = 0;
int total=0;

int tempCount=0;
int fiveSecCounter = 0;
boolean even = false;
byte locoBuff[32];

struct trainStruct{
  byte address;
  byte data;
  byte error;
};

trainStruct train[MAX_TRAINS];


void requestEvent()
{
  Wire.write(dataBack,32);
  for(int i=0;i<32;i++)
    dataBack[i]=0;
}

void receiveEvent(int howMany)
{  
  trainStruct tempStruct;
  byte i;
  boolean alreadyInStruct = false;
  delay(10);
  digitalWrite(1,HIGH);
  switch(Wire.read())
  {
  case 0:              //DCC Command

    if(Wire.available()>=3)
    {
      tempStruct.address = Wire.read();
      tempStruct.data = Wire.read();
      tempStruct.error = Wire.read();       

      if(errorCheck(tempStruct.address,tempStruct.data,tempStruct.error))
      {
        if(tempStruct.address == 0x00)
        {       
          
          for(int i=0;i<MAX_TRAINS;i++)
            {
              train[i].address = 0x00;
              train[i].data = 0x00;
              train[i].error = 0x00;
            }
          /*if(currentTrain!=0)
          {
            train[0].address = 0x00;
            train[0].data = 0x00;
            train[0].error = 0x00;
            while(currentTrain != 0);
            while(currentTrain == 0);
            numberOfTrains=0;
            
          }*/
          //byte temp = currentTrain;
          //for(int i = 0;i<numberOfTrains;i++)
          //{
            //train[i].data = 0x00;
            //train[i].error = (train[i].address ^ 0x00);
          //}
          //while(currentTrain == temp){}
          //while(currentTrain != temp){}
          //while(currentTrain == temp){}
          //numberOfTrains = 0;                   
        }
        else
        {
          i=0;
          while(i < numberOfTrains)
          {
            if(train[i].address == tempStruct.address)
            {
              alreadyInStruct = true;
              break;
            }
            else
              i++;
          }
          if(alreadyInStruct == true)
          {
            train[i].data = tempStruct.data;
            train[i].error = tempStruct.error;
          }
          else
          {
            if(numberOfTrains < MAX_TRAINS)
            {
              train[numberOfTrains].address = tempStruct.address;
              train[numberOfTrains].data = tempStruct.data;
              train[numberOfTrains].error = tempStruct.error;
              numberOfTrains++;
            }             
            //ERROR Too many Trains
          }
        }
      }   
    } 
    break;

  case 1:              // Loconet Command
    tempCount = 0;
    while(Wire.available())
      locoBuff[tempCount++]=Wire.read();

    Serial.write(locoBuff,tempCount-1);
    break;

  case 2:              // IMU Request
    break;

  case 3:              // Current Request   
    dataBack[0] = (averageCurrent>>8) & 0xff;
    dataBack[1] = averageCurrent & 0xff;
    break;

  default:

    break;
  }
  while(Wire.available())
    Wire.read();
}

// runs only once at startup
void setup()
{
  analogReference(INTERNAL);
  pinMode(A0,INPUT);
  pinMode(A1,INPUT);
  pinMode(0,INPUT);
  pinMode(1,OUTPUT);
  pinMode(2,OUTPUT);
  pinMode(5,OUTPUT);
  pinMode(6,OUTPUT);
  pinMode(7,OUTPUT);
  pinMode(8,OUTPUT);
  pinMode(9,OUTPUT);
  pinMode(10,OUTPUT); 

  digitalWrite(1,LOW); 
  digitalWrite(5,HIGH); 
  digitalWrite(9,HIGH);
  digitalWrite(10, HIGH);
  Serial.begin(16660);
  Timer1.initialize(ONE_BIT);                   // initialize timer1, and set to 56 µs 
  Timer1.attachInterrupt(callback);             // attaches callback()
  Wire.begin(I2C_ADDRESS);                // join i2c bus with address #4
  Wire.onReceive(receiveEvent); // register event
  Wire.onRequest(requestEvent); // register event
}

// callback for timer ISR. Gets called every 56-112µs
void callback()                                 
{
  PORTD ^= 0x60;                                // toggle DCC polarity      

  if(0x20 == (PORTD & 0x20))                    // if ready for new bit
  {
    if(numberOfTrains>0)
    {
      if(bitCount < 14)
        Timer1.initialize(ONE_BIT);               // send preamble

      else if(bitCount == 14)
        Timer1.initialize(ZERO_BIT);              // send Packet Start Bit
      else if(bitCount > 14 && bitCount < 23)
        setBit(address,(22-bitCount));            // send Address Byte

      else if(bitCount == 23)
        Timer1.initialize(ZERO_BIT);              // send Data Start Bit
      else if(bitCount > 23 && bitCount < 32)
        setBit(data,(31-bitCount));               // send Data Byte

      else if(bitCount == 32)
        Timer1.initialize(ZERO_BIT);              // send Error Start Bit
      else if(bitCount > 32 && bitCount < 41)
        setBit(error,(40-bitCount));              // send Error Byte

      else if(bitCount == 41)
          Timer1.initialize(ONE_BIT);               // send Packet End Bit

      if(bitCount > 41)
      {
        if(address == 0x00)
        {
          numberOfTrains=0;
          currentTrain = 0;
        }
        if(currentTrain < numberOfTrains-1)
          currentTrain++;
        else
          currentTrain = 0;  

        address = train[currentTrain].address;
        data = train[currentTrain].data;
        error = train[currentTrain].error;   
        bitCount = 0; 
      }
      else if(errorCheck(address,data,error))
        bitCount++;
      else
        bitCount = 0;   
    }
    else
      Timer1.initialize(ONE_BIT);            
  }
}

// main loop
void loop()
{  
  if(counter+10 < millis())
  {
    // subtract the last reading:
    total= total - currentReadings[currentIndex];
    // read from the sensor:  
    currentReadings[currentIndex] = analogRead(A0);
    // add the reading to the total:
    total= total + currentReadings[currentIndex];
    // advance to the next position in the array:  
    currentIndex = currentIndex + 1;

    // if we're at the end of the array...
    if (currentIndex >= 10)
      // ...wrap around to the beginning:
      currentIndex = 0;

    // calculate the average:
    averageCurrent = total / 10;     
  }  
}

//**********************************
//***** Begin helper functions *****
//**********************************

// sends out zero, or one on DCC depending on input values
void setBit(byte dataByte, byte index)            
{
  if(0 == (bit(index) & dataByte))
    Timer1.initialize(ZERO_BIT);
  else
    Timer1.initialize(ONE_BIT);
}

// checks to make sure the error byte is correct
boolean errorCheck(byte address, byte data, byte error)
{
  if(error == (address ^ data))
    return true;
  else
    return false;
}




