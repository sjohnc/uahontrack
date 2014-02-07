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

#include <SPI.h>
#include <Mirf.h>
#include <nRF24L01.h>
#include <MirfHardwareSpiDriver.h>
byte data=0xff;
void setup(){
  Serial.begin(9600);
  Mirf.spi = &MirfHardwareSpi;
  Mirf.init();
  Mirf.setRADDR((byte *)"00001");
  Mirf.payload = sizeof(byte);
  Mirf.config();  
  Serial.println("Listening..."); 
}

void loop(){
  if(!Mirf.isSending() && Mirf.dataReady()){
    Serial.println("Got packet");
  
  Mirf.getData(&data);
  Serial.print("Data: ");
  Serial.println(data);
  data+=1;
  Mirf.setTADDR((byte *)"mastr");
  Mirf.send(&data);
  Serial.println("Sending");
  while(Mirf.isSending())
  {}
  Serial.println("Reply sent.");
  }
}
