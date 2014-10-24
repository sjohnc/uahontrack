#include <FastSerial.h>
#include <SPI.h>
#include <Mirf.h>
#include <nRF24L01.h>
#include <MirfHardwareSpiDriver.h>
/**
 * An Mirf example which copies back the data it recives.
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



#define COUNTS_PER_CM .36585366    //For Tie counting
#define RX_ADDRESS (byte *)"00002"
#define TX_ADDRESS (byte *)"mastr"
#define MSG_SIZE 29

volatile unsigned long timeSinceLastBar;
volatile  long tieCount = 0;
volatile byte barCodeCount = 0;
volatile  long countAtLastBar = 0;

FastSerialPort0(Serial);		// Instantiate the fast serial driver

long lastTime = 0;

byte radioDataTx[MSG_SIZE];
byte radioDataRx[MSG_SIZE];

union {
  byte asByte[4];
  long asLong;
} 
tieCountUnion;

void setup(){
  attachInterrupt(0, tieISR, CHANGE);          //Set up interrupt for tie detection pin D2
  attachInterrupt(1, barCodeISR, CHANGE);      //Set up interrupt for bar code reading on D3
  Serial.begin(38400, 128, 16);
  Mirf.spi = &MirfHardwareSpi;
  Mirf.init();
  Mirf.setRADDR(RX_ADDRESS);
  Mirf.payload = MSG_SIZE;
  Mirf.config();  
  //Serial.println("Running...");
}

void loop(){    
  
  if(Serial.available()>23)
  {
    for(int i=0;i<24;i++)
      radioDataTx[i] = Serial.read();
      delay(10);
    while(Serial.available())
      Serial.read();        
    //sendRadioData();
  }
  if(Mirf.dataReady())
  {
    Mirf.getData(radioDataRx);
    switch(radioDataRx[0])
    {
      case 1:
        sendRadioData();
      break;
    }
  }
}


void sendRadioData()
{  
  if((timeSinceLastBar + 500 < millis()) && barCodeCount > 0)
  {    
    noInterrupts();       
    tieCount = tieCount - countAtLastBar;    
    interrupts();    
    
    radioDataTx[28] = barCodeCount/2; 
    barCodeCount = 0;
  }
  else
    radioDataTx[28]=0;
  noInterrupts();
  tieCountUnion.asLong = tieCount;
  interrupts();
  
  for(int i=0;i<4;i++)
      radioDataTx[24+i] = tieCountUnion.asByte[i];
  Mirf.setTADDR(TX_ADDRESS);
  Mirf.send(radioDataTx);
  while(Mirf.isSending())
  {}
}


void tieISR()          // Tie counting ISR
{
  tieCount++;  
}

void barCodeISR()      // Bar Code ISR
{
  barCodeCount++; 
  timeSinceLastBar = millis();
  countAtLastBar = tieCount;
}
