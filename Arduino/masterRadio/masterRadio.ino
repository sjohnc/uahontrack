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
 * Note: To see best case latency comment out all Serial.println
 * statements not displaying the result and load 
 * 'ping_server_interupt' on the server.
 */

#include <SPI.h>
#include <Mirf.h>
#include <nRF24L01.h>
#include <MirfHardwareSpiDriver.h>
byte data=0x00;
byte address[6] = "00001";

void setup(){
  Serial.begin(9600);
  Mirf.spi = &MirfHardwareSpi;
  Mirf.init();
  Mirf.setRADDR((byte *)"mastr");
  Mirf.payload = sizeof(byte);
  Mirf.config();
  
  Serial.println("Beginning ... "); 
}

void loop(){
  unsigned long time = millis();
  
  
  
  Mirf.setTADDR(address);
  Serial.println("Sending");
  Mirf.send(&data);
  while(Mirf.isSending()){
  }
  Serial.println("Finished sending");
  delay(10);
  while(!Mirf.dataReady()){
    if ( ( millis() - time ) > 1000 ) {
      Serial.println("Timeout on response from server!");
      return;
    }
  }
  
  Mirf.getData(&data);
  Serial.print("data: ");
  Serial.println(data);
  
  delay(1000);
} 
  
  
  
