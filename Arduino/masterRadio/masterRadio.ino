/**
 * A Mirf example to test the latency between two Ardunio.
 *
 * Pins:
 * Hardware SPI:
 * MISO -> 12
 * MOSI -> 11
 * SCK -> 13
 *
 * Configurable:
 * CE -> 8
 * CSN -> 7
 *
 */

#include <SPI.h>
#include <Mirf.h>
#include <nRF24L01.h>
#include <MirfHardwareSpiDriver.h>

#define RX_ADDRESS (byte *)"mastr"
#define MAX_TRAINS 10
#define MSG_SIZE 29
#define DEBUG_1
//#define DEBUG_2
//#define DEBUG_3

byte radioDataRx[MSG_SIZE];
byte radioDataTx[MSG_SIZE];
byte address[6]="00000";
byte trainList[MAX_TRAINS];
unsigned long time = millis();
byte numOfTrains=1;
int currentTrainIndex = 0;

union {
  byte asByte[4];
  float asFloat;
} 
AccelX;

union {
  byte asByte[4];
  float asFloat;
} 
AccelY;

union {
  byte asByte[4];
  float asFloat;
} 
AccelZ;

union {
  byte asByte[4];
  float asFloat;
} 
Yaw;

union {
  byte asByte[4];
  float asFloat;
} 
Pitch;

union {
  byte asByte[4];
  float asFloat;
} 
Roll;

union {
  byte asByte[4];
  long asLong;
} 
tieCount;


void setup(){
  Serial.begin(115200);
  Mirf.spi = &MirfHardwareSpi;
  Mirf.init();
  Mirf.setRADDR(RX_ADDRESS);
  Mirf.payload = MSG_SIZE;
  Mirf.config();

  trainList[0]=2;
  nextTrain();
}

void loop(){  
  
  if(Mirf.dataReady())
  {
    Mirf.getData(radioDataRx);
    sendData();
    nextTrain();
  }
  else
  {
    if(( millis() - time) > 100/numOfTrains)
    {
      Mirf.setTADDR(address);
      radioDataTx[0]=1;
      Mirf.send(radioDataTx);
      while(Mirf.isSending())
      {
      }
      time=millis();
    }
  }

  //Serial.write(10);
  //Serial.write(1);


  //Serial.write(data,sizeof(data));
  /*Serial.write(0);
   Serial.write(1);
   Serial.write(2);
   Serial.write(3);*/



} 
void nextTrain()
{
  if(trainList[currentTrainIndex]<10)
  {
    for(int i=0;i<3;i++)
      address[i] = '0';
    address[4] = trainList[currentTrainIndex]+'0';
    address[5] = NULL;
  }
  if(trainList[currentTrainIndex+1]>0)
    currentTrainIndex++;
  else
  {
    numOfTrains = currentTrainIndex+1;
    currentTrainIndex = 0;  
  }
}



void sendData()
{   
#ifdef DEBUG_1
  for(int i=0;i<4;i++)
    AccelX.asByte[i]=radioDataRx[i];
  Serial.print("X: ");
  Serial.println(AccelX.asFloat);

  for(int i=0;i<4;i++)
    AccelY.asByte[i]=radioDataRx[i+4];
  Serial.print("Y: ");
  Serial.println(AccelY.asFloat);

  for(int i=0;i<4;i++)
    AccelZ.asByte[i]=radioDataRx[i+8];
  Serial.print("Z: ");
  Serial.println(AccelZ.asFloat);

  for(int i=0;i<4;i++)
    Roll.asByte[i]=radioDataRx[i+12];
  Serial.print("Roll: ");
  Serial.println(Roll.asFloat);

  for(int i=0;i<4;i++)
    Pitch.asByte[i]=radioDataRx[i+16];
  Serial.print("Pitch: ");
  Serial.println(Pitch.asFloat);

  for(int i=0;i<4;i++)
    Yaw.asByte[i]=radioDataRx[i+20];
  Serial.print("Yaw: ");
  Serial.println(Yaw.asFloat);

  for(int i=0;i<4;i++)
    tieCount.asByte[i]=radioDataRx[i+24];  
  Serial.print("Counts: ");  
  Serial.println(tieCount.asLong);
  Serial.print("BarCode: ");
  Serial.println(radioDataRx[28]);
  Serial.println();  
#endif

#ifdef DEBUG_2
  Serial.write(10);
  Serial.write(trainList[currentTrainIndex]);
  Serial.write(radioDataRx,sizeof(radioDataRx));
#endif

#ifdef DEBUG_3
  for(int i=0;i<4;i++)
    AccelX.asByte[i]=radioDataRx[i];
  Serial.println(AccelX.asFloat);
#endif
}
