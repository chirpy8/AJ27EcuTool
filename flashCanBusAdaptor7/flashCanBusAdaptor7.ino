

/*
 * Works with AJ27EcuTool to interface between PC and ECU using canbus shield and serial port
 * 
 * Created 10/21/23
 * Modified 1/10/25
 * Author: Chirpy
 * Version 3.3
 * 
 * Receive 10 byte packet from PC and forward as canbus message to ECU
 * Receive canbus messages from ECU and forward as 10 byte packets to PC
 * 
 * packet format
 *  bytes 0 and 1 are SID, 11 bit left justified
 *  bytes 2-9 are data messages, byte 2 is the length of the remainging 7 byte message
 *
 *  canbus receive interrupt is used to trigger receipt of messages from ECU and forwarding 10 byte serial message to PC
 *  serial receive is used to trigger receipt of 10 byte serial message from PC and forwarding to ECU
 *
 *  Serial format if 500k baud, 8 bit data, 1 stop bit, no parity
 *  Canbus format is standard addressing, 500 kbps
 *
 * canbus receive by ECU SID is 0x20 - left justified version is 0x04 0x00
 * canbus transmit from ECU SID is 0x30 - left justified form is 0x06 0x00
 *
 * special SID of 0x10 is used to receive PC ping, and also 1k download messages (service 0x36 messages)
 * special SID of 0x50 is used to confirm full receipt of 1k download back to PC
 */

/*
 * Note Sparkfun Canbus shield uses:
 * D2 for interrupts from MCP2515
 * D9 for data card CS - not used
 * D10 for CAN Chip Select
 * Green LEDs on D7 for LED2 and D8 for LED1
 * Red LED shows power
 * 
 */

//definitions to allow fastest possible SPI transfers 

#define DELAY_CYCLES(n) __builtin_avr_delay_cycles(n)
 
 #include <SPI.h>

volatile byte transmitWritePointer = 0;
byte transmitReadPointer = 0;
byte dataLength;
byte spiRx;
byte canStat;
byte temp;
boolean uploadOngoing = false;
int uploadPacketCount = 0;
int uploadByteCounter = 0;
int downloadByteCounter = 0;
int readOutCounter = 0;
volatile boolean canbusRx = false;
volatile boolean activeDownload = false;
volatile boolean readyToForwardDownload = false;
boolean downloadForwarding = false;
byte blockCounter = 0;
byte forwardedCounter = 0;
byte serialRxPacket[10];
byte pcPingAck[10] = {0x0a, 0x00, 0x02, 0xaa, 0xaa, 0, 0, 0, 0, 0};
byte ecuUploadPacket[10] = {0x04, 0x00, 0x07, 0x36, 0,0,0,0,0,0};
byte ecuUploadNotify1[10] = {0x04, 0x00, 0x02, 0x31, 0xa2,0,0,0,0,0}; //upload complete message 02 31 a2
byte ecuUploadNotify2[10] = {0x04, 0x00, 0x02, 0x31, 0xb2,0,0,0,0,0}; //upload complete message 02 31 b2
byte uploadCompleteAck1[10] = {0x0a, 0x00, 0x03, 0x31, 0xa2, 0, 0, 0, 0, 0};
byte uploadCompleteAck2[10] = {0x0a, 0x00, 0x03, 0x31, 0xb2, 0, 0, 0, 0, 0};
byte uploadAck1[10] = {0x0a, 0x00, 0x03, 0x7f, 0x31, 0xa2, 0, 0, 0, 0};
byte uploadAck2[10] = {0x0a, 0x00, 0x03, 0x7f, 0x31, 0xb2, 0, 0, 0, 0};
byte uploadRequestAck[10] = {0x0a, 0x00, 0x03, 0x7f, 0x34, 0, 0, 0, 0, 0};
byte downloadPacket[10] = {0x06, 0x00, 0x07, 0x36, 0, 0, 0, 0, 0, 0};
byte transmitBuffer[8][10];
byte dataBlock[1024];
byte spiReadPointer = 0;
volatile byte spiWritePointer = 0;
byte spiBuffer[8][10];
byte spiDataPacket[10];
boolean processingUpload = false;
volatile boolean pingSent = false;
byte ecuBuffer[8][10];
byte ecuWritePointer = 0;
byte ecuReadPointer = 0;
byte packetSent = false;
boolean validateReflashCPU1 = false;
boolean validateReflashCPU2 = false;

void processECUBuffer() //sendPacket is assumed to have 10 elements
{

  //wait if ping just sent until it is acked, or if packet sent and waiting for ack
  if (!pingSent && !packetSent && (ecuReadPointer != ecuWritePointer))
  {
    byte sendPacket[10];

    for (byte i=0;i<10;i++)
    {
      sendPacket[i] = ecuBuffer[ecuReadPointer][i];
    }
    ecuReadPointer = (ecuReadPointer + 1) & 0x7;
    packetSent = true;
    sendToECU(sendPacket);
  }
}

void bufferedSendToECU(byte bufferedPacket[10])
{
  for (byte x=0;x<10;x++)
  {
    ecuBuffer[ecuWritePointer][x] = bufferedPacket[x];
  }
  ecuWritePointer = ((ecuWritePointer + 1) & 0x07);
}


void sendToECU(byte sendPacket[10]) //sendPacket is assumed to have 10 elements
{
    // use TXbuffer 1
    boolean isBusy = true;

    do
    {
      noInterrupts();
      //read status of TXB 1
      PORTB = 0x02; 
      SPDR = 0xa0; //read status
      while ((SPSR & 0x80) == 0) {};
      temp = SPDR;

      SPDR = 0x00; //receive contents
      while ((SPSR & 0x80) == 0) {};
      spiRx = SPDR;
      PORTB = 0x06;

      interrupts();

      // bit 4 is TXREQ1 status
      isBusy = ((spiRx & 0x10) != 0);
    }
    while (isBusy);

    // send data in TX1 buffer

    noInterrupts();

    PORTB = 0x02;
    SPDR = 0x42; // load TX buffer 1 starting a SIDH
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = sendPacket[0]; //load tx buffer with SIDH
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = sendPacket[1]; //load tx buffer with SIDL
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0; //load tx buffer with EID8
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0; //load tx buffer with EID0
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0x08; //load tx buffer with DLC
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    for (byte x = 2;x<10;x++) 
    {       
      SPDR = sendPacket[x]; //load tx buffer with D0 - D7
      while ((SPSR & 0x80) == 0) {};
      temp = SPDR;
    }

    PORTB = 0x06; // No CS
    DELAY_CYCLES(2);

    //initiate message transmission by setting TXB1CTRL.TXREQ
    PORTB = 0x02; // No CS   
    SPDR = 0x82; // set TXB1CTRL.TXREQ
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    PORTB = 0x06; // No CS

    TCNT2 = 0x0; //  reset ping timeout since packet just sent

    interrupts();
  
}



void uploadData(boolean targetIs1) //true for cpu1, false for cpu2
{
  uploadByteCounter = 0;

  for (int x=0;x<170;x++)
  {
    for (int i=4;i<10;i++) 
    {       
      ecuUploadPacket[i] = dataBlock[uploadByteCounter]; //load tx buffer with D2 - D7
      uploadByteCounter++;
    }
    sendToECU(ecuUploadPacket);
  }
        
  //send last 4 bytes

  for (int i=4;i<8;i++) 
  {       
      ecuUploadPacket[i] = dataBlock[uploadByteCounter]; //load tx buffer with D2 - D7
      uploadByteCounter++;
  }
  ecuUploadPacket[8] = 0;
  ecuUploadPacket[9] = 0;

  sendToECU(ecuUploadPacket);

  if (targetIs1)
  {
    sendToECU(ecuUploadNotify1);
  }
  else
  {
    sendToECU(ecuUploadNotify2);
  } 

processingUpload = true;

}

ISR(INT0_vect)
{
  //receive new canbus packet, check headers, filter, and forward to PC
  //check both RX buffers for a message
  byte txPacket[10];
  boolean receivedData = false;
  boolean pingResponse = false;
  byte canbusRxBuffer = 0;

  //read canstat  register which receive buffer triggered the interrupt
  PORTB = 0x02; //CS
  SPDR = 0x03; // read
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x0e; //CANSTAT register
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x00; //read contents
  while ((SPSR & 0x80) == 0) {};
  canStat = SPDR;

  PORTB = 0x06;

  if ((canStat & 0x0e) == 0x0c)  //0x0c is RXB0 interrupt
  {
    canbusRxBuffer = 1;
    // indicate to read RX buffer 0
  }
  else if ((canStat & 0x0e) == 0x0e)  //0x0e is RXB1 interrupt
  {
    canbusRxBuffer = 2;
    // indicate to read RX buffer 1
  }

  if (canbusRxBuffer == 1)
  {
    //read rx buffer 0 - message ID and length
    PORTB = 0x02; //CS
    SPDR = 0x90; // send read RX buffer 0 instruction
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;
  
    SPDR = 0x00; //receive RXB0SIDH
    while ((SPSR & 0x80) == 0) {};  
    spiRx = SPDR; //store value of sidh in spiRX

    SPDR = 0x00; // start next SPI transfer - receive RXB0SIDL

    //if message from ECU in boot mode, spiRX will equal 0x06 0x00 (id=0x030)
    //if ECU is in normal mode, heartbeat will have spiRX = 0xfa 0x00 (id = 0x7d0)
    //if ECU is sending a diagnostic response, spiRX == 0xfd 0x80 (id 0x7ec)
    // if not any of these then abort rest of data read
    if ((spiRx == 0x06) || (spiRx == 0xfa) || (spiRx == 0xfd))
    {
      receivedData = true;
      txPacket[0] = spiRx;

      while ((SPSR & 0x80) == 0) {};  
      spiRx = SPDR; //store value of sidl in spiRX

      //if message from ECU, spiRX will equal 0x00, if not then abort rest of data read
      if (((spiRx == 0x00) && (txPacket[0] != 0xfd)) || ((spiRx == 0x80) && (txPacket[0] == 0xfd)))
      {
        txPacket[1] = spiRx;
        SPDR = 0x00; //start next SPI transfer - receive RXB0EID8
        while ((SPSR & 0x80) == 0) {};
        temp = SPDR;
 
        SPDR = 0x00; //receive RXB0EID0
        while ((SPSR & 0x80) == 0) {};
        temp = SPDR;

        SPDR = 0x00; //receive RXB0DLC
        while ((SPSR & 0x80) == 0) {};
        spiRx = SPDR; //store value
        dataLength = (spiRx & 0x0f) + 2; //store value - add 2 for loop below

        for (int x=2; x<10;x++) //executes to receive 8 data bytes, DLC field length is ignored
        {
          SPDR = 0x00; //receive RXB0Dn
          while ((SPSR & 0x80) == 0) {};
          txPacket[x] = SPDR;
        }
      }
    }
    
    PORTB = 0x06; // No CS
    DELAY_CYCLES(2);
  
    //clear the interrupt flag CANINTF.RX0IF
    PORTB = 0x02; //CS
    SPDR = 0x05; // send bit modify instruction
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;
 
    SPDR = 0x2c; // CANINTF register
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0x01; // mask for RX0IF
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0x00; // reset RX0IF
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    PORTB = 0x06; // No CS

  }
  else if (canbusRxBuffer == 2)
  {

    //read rx buffer 1 - message ID and length
    PORTB = 0x02; //CS
    SPDR = 0x94; // send read RX buffer 1 instruction
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;
  
    SPDR = 0x00; //receive RXB1SIDH
    while ((SPSR & 0x80) == 0) {};
    spiRx = SPDR; //store value

    SPDR = 0x00; // start next SPI transfer - receive RXB1SIDL

    //if message from ECU in boot mode, spiRX will equal 0x06 0x00 (id=0x030)
    //if ECU is in normal mode, heartbeat will have spiRX = 0xfa 0x00 (id = 0x7d0)
    //if ECU is sending a diagnostic response, spiRX == 0xfd 0x80 (id 0x7ec)
    // if not any of these then abort rest of data read
    if ((spiRx == 0x06) || (spiRx == 0xfa) || (spiRx == 0xfd))
    {
      receivedData = true;
      txPacket[0] = spiRx;

      while ((SPSR & 0x80) == 0) {};
      spiRx = SPDR; //store value of sidl in spiRX

      //if message from ECU, spiRX will equal 0x00, if not then abort rest of data read
      if (((spiRx == 0x00) && (txPacket[0] != 0xfd)) || ((spiRx == 0x80) && (txPacket[0] == 0xfd)))
      {
        txPacket[1] = spiRx;
        SPDR = 0x00; //start next SPI transfer - receive RXB0EID8
        while ((SPSR & 0x80) == 0) {};
        temp = SPDR;
 
        SPDR = 0x00; //receive RXB0EID0
        while ((SPSR & 0x80) == 0) {};
        temp = SPDR;

        SPDR = 0x00; //receive RXB0DLC
        while ((SPSR & 0x80) == 0) {};
        spiRx = SPDR; //store value
        dataLength = (spiRx & 0x0f) + 2; ; //store value - add 2 for loop below

        for (int x=2; x<10;x++) //executes to receive 8 data bytes, DLC field length is ignored
        {
          SPDR = 0x00; //receive RXB0Dn
          while ((SPSR & 0x80) == 0) {};
          txPacket[x] = SPDR;
        }
      }
    }

    PORTB = 0x06; // No CS
    DELAY_CYCLES(2);
  
    //clear the interrupt flag CANINTF.RX1IF
    PORTB = 0x02; //CS
    SPDR = 0x05; // send bit modify instruction
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;
  
    SPDR = 0x2c; // CANINTF register
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0x02; // mask for RX1IF
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0x00; // reset RX0IF
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    PORTB = 0x06; // No CS
  }

  if (receivedData)
  {
    for (int x=0;x<10;x++)
    {
      spiBuffer[spiWritePointer][x] = txPacket[x];
    }
    spiWritePointer = ((spiWritePointer + 1) & 0x07);

    //reset ping sent flag if ping response
    if ((txPacket[3] == 0x7f) && (txPacket[4] == 0x00) && (txPacket[2] == 0x02) && (txPacket[0] == 0x06) && (txPacket[1] == 0x00))
    {
      pingSent = false; //used to avoid sending packet to ecu between ping sent and ping ack
    }
  }
}


ISR(TIMER2_COMPA_vect)//interrupt service routine for Timer2
{
  //send a hard coded 01 3f to ECU using SID 0x20 (which is 0x04 0x00 when left justifed)
  // byte ecuPing[] = {0x04, 0x00, 0x01, 0x3f};
  //this is used as a keep-alive to stop timeout when ECU commands have been unlocked
  //especially when downloading 1k data blocks to the Arduino before uploading to the ECU

  byte count=0;

  //wait until TX buffer 0 not busy
  do
  {
    //read status of TXB 0
    PORTB = 0x02; 
    SPDR = 0x03; //read
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0x30; //TXB0CTRL
    while ((SPSR & 0x80) == 0) {};
    temp = SPDR;

    SPDR = 0x00; //receive contents
    while ((SPSR & 0x80) == 0) {};
    spiRx = SPDR;

    PORTB = 0x06;
    count = count + 1;
  }
  while (((spiRx & 0x08) != 0) && (count < 250));

  //send data in TXB 0
  PORTB = 0x02;
  SPDR = 0x40; // load TX buffer 0 starting a SIDH
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x04; //load tx buffer with SIDH
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x00; //load tx buffer with SIDL
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0; //load tx buffer with EID8
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0; //load tx buffer with EID0
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x02; //load tx buffer with DLC
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;
     
  SPDR = 0x01; //load tx buffer with D0
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x3f; //load tx buffer with D0
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  PORTB = 0x06; // No CS
  DELAY_CYCLES(2);

  //initiate message transmission by setting TXB0CTRL.TXREQ
  PORTB = 0x02; // No CS   
  SPDR = 0x81; // set TXB0CTRL.TXREQ
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;
  PORTB = 0x06; // No CS
  DELAY_CYCLES(2);

  pingSent = true; //set flag indicating ping just sent
}


// if serial data received
void checkSerial()
{
  int receivedBytes = Serial.available();

  if (receivedBytes >= 10)
  {
    Serial.readBytes(serialRxPacket, 10);

    if ((serialRxPacket[0] == 0x02) && (serialRxPacket[1] == 0x00))  //SID 0x10
    {
      if ((serialRxPacket[2] == 0x07) && (serialRxPacket[3] == 0x36))
      {
        //receive upload data
        if (uploadOngoing)
        {
          if (uploadPacketCount < 170)
          {
            //receive 6 bytes and add to data block
            for (int x=4;x<10;x++)
            {
              dataBlock[uploadByteCounter] = serialRxPacket[x];
              uploadByteCounter++;
            }
            uploadPacketCount++;
          }
          else if (uploadPacketCount == 170)
          {
            //receive final 4 bytes and add to data block
            for (int x=4;x<8;x++)
            {
              dataBlock[uploadByteCounter] = serialRxPacket[x];
              uploadByteCounter++;
            }
          }
        }  
      }
      else if ((serialRxPacket[2] == 0x02) && (serialRxPacket[3] == 0x55) && (serialRxPacket[4] == 0x55))
      {
        //send acknowledge: pcPingAck
        sendTransmitPacket(pcPingAck);
      }
      else if ((serialRxPacket[2] == 0x02) && (serialRxPacket[3] == 0x34) && (serialRxPacket[4] == 0x00))
      {
        //prepare to receive 1k block upload
        uploadOngoing = true;
        uploadPacketCount = 0;
        uploadByteCounter = 0;
        //send acknowledge
        sendTransmitPacket(uploadRequestAck);
      }
      else if ((serialRxPacket[2] == 0x02) && (serialRxPacket[3] == 0x31) && (serialRxPacket[4] == 0xa2))
      {
        uploadCompleteAck1[5] = (byte) uploadPacketCount; //0x0a, 0x00, 0x03, 0x31, 0xa2, packetcount
        sendTransmitPacket(uploadCompleteAck1);
        //PC indicating upload is complete and requests acknowledge
        if ((uploadOngoing) && (uploadPacketCount == 170))
        {
          uploadOngoing = false;
          uploadData(true);
          sendTransmitPacket(uploadAck1);
         }
        else
        {
          uploadOngoing = false;
        }
      }
      else if ((serialRxPacket[2] == 0x02) && (serialRxPacket[3] == 0x31) && (serialRxPacket[4] == 0xb2))
      {
        uploadCompleteAck2[5] = (byte) uploadPacketCount; //0x0a, 0x00, 0x03, 0x31, 0xb2, packetcount
        sendTransmitPacket(uploadCompleteAck2);
        //PC indicating upload is complete and requests acknowledge
        if ((uploadOngoing) && (uploadPacketCount == 170))
        {
          uploadOngoing = false;
          uploadData(false);
          sendTransmitPacket(uploadAck2);
        }
        else
        {
          uploadOngoing = false;
        }
      }
    }
    else if ((serialRxPacket[0] == 0x04) && (serialRxPacket[1] == 0x00) && (serialRxPacket[2] == 0x07) && (serialRxPacket[3] == 0x35))  //SID 0x20, message 35
    {
      activeDownload = true;
      blockCounter = 0;
      downloadByteCounter = 0;
      bufferedSendToECU(serialRxPacket);
    }
    else
    {
      //forward packet to ECU, for SID 0x20 or SID 0x7e8
      bufferedSendToECU(serialRxPacket);
    }
  }
}

void forwardDownloadData()
{
  byte bufferSize = getTransmitBufferSize();

  if (forwardedCounter < 170)
  {
    if (bufferSize > 3)
    {
      for (int x=4;x<10;x++)
      {
        downloadPacket[x] = dataBlock[readOutCounter];
        readOutCounter++;
      }
      sendTransmitPacket(downloadPacket);
      forwardedCounter++;
    }
  }
  else if (forwardedCounter == 170)
  {
    //only 4 bytes in last block
    if (bufferSize > 3)
    {
      for (int x=4;x<8;x++)
      {
        downloadPacket[x] = dataBlock[readOutCounter];
        readOutCounter++;
      }
      downloadPacket[8] =0;
      downloadPacket[9] =0;

      sendTransmitPacket(downloadPacket);
      downloadForwarding = false;
    }
  }
}

void sendTransmitPacket(byte packet[10])
{
  for (int x=0;x<10;x++)
  {
    transmitBuffer[transmitWritePointer][x] = packet[x];
  }
  transmitWritePointer = ((transmitWritePointer + 1) & 0x07);
}

byte getTransmitBufferSize()
{
  byte size = ((7 + transmitReadPointer - transmitWritePointer) & 0x07);
  return size;
}

//if Serial transmit has space, and there is data to send, then send it
//pause sending if activeDownload is set
void processTransmitBuffer()
{
  if ((Serial.availableForWrite() >= 10) && !activeDownload)
  {
    if (transmitWritePointer != transmitReadPointer)
    {
      Serial.write(transmitBuffer[transmitReadPointer],10);
      transmitReadPointer = ((transmitReadPointer + 1) & 0x07);
    }
  }
}

void getSPIPacket()
{
  noInterrupts();
  for (int x=0;x<10;x++)
  {
    spiDataPacket[x] = spiBuffer[spiReadPointer][x];
  }
  interrupts();
  spiReadPointer = ((spiReadPointer + 1) & 0x07);
}

boolean spiAvailable()
{
  if (spiReadPointer != spiWritePointer)
  {
    return true;
  }
  else
  {
    return false;
  }
}

void processSPI()
{
  boolean pingResponse = false;

  if (spiAvailable())
  {
    getSPIPacket();

  //check receive packet for special actions, then forward over serial to PC
    if ((spiDataPacket[0] == 0x06) && (spiDataPacket[1] == 0x00)) //if packet is from 0x30 SID
    {
      packetSent = false; //reset packetSent flag since this will be a response from previous send

      //if message is 07 36 and activeDownload flag is set, then buffer packets into dataBlock
      //update blockCounter after each block is uploaded
      //reset activeDownload flag after block 170 - which is only 4 bytes, since all 1024 bytes have been received
      //note response message is 06 7f 35 will get delayed while this data is buffered

      if (activeDownload && (spiDataPacket[2] == 0x07) && (spiDataPacket[3] == 0x36))
      {
        if (blockCounter < 170)
        {
          dataBlock[downloadByteCounter] = spiDataPacket[4];
          downloadByteCounter++;
          dataBlock[downloadByteCounter] = spiDataPacket[5];
          downloadByteCounter++;
          dataBlock[downloadByteCounter] = spiDataPacket[6];
          downloadByteCounter++;
          dataBlock[downloadByteCounter] = spiDataPacket[7];
          downloadByteCounter++;
          dataBlock[downloadByteCounter] = spiDataPacket[8];
          downloadByteCounter++;
          dataBlock[downloadByteCounter] = spiDataPacket[9];
          downloadByteCounter++;
          blockCounter++;
        }
        else if (blockCounter == 170)
        {
          for (int x=4;x<8;x++)
          {
          dataBlock[downloadByteCounter] = spiDataPacket[4];
          downloadByteCounter++;
          dataBlock[downloadByteCounter] = spiDataPacket[5];
          downloadByteCounter++;
          dataBlock[downloadByteCounter] = spiDataPacket[6];
          downloadByteCounter++;
          dataBlock[downloadByteCounter] = spiDataPacket[7];
          downloadByteCounter++;
          }
          readyToForwardDownload = true;
          blockCounter = 0;
          downloadByteCounter = 0;
        }
      }
      else if ((spiDataPacket[2] == 0x2) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x0))
      {
        pingResponse = true; //used to suppress sending to PC over serial
      }
      //if message is 04 67 01 xx yy then this is a request for extended commands unlock
      else if ((spiDataPacket[2] == 0x04) && (spiDataPacket[3] == 0x67) && (spiDataPacket[4] == 0x01))
      {
        //retrieve seed values
        unsigned int seedByte0 = spiDataPacket[5] & 0xff;
        unsigned int seedByte1 = spiDataPacket[6] & 0xff;

        //compute unlock codes
        unsigned int temp1 = ((seedByte0) << 8) + (seedByte1);
        unsigned long temp2 = (temp1 * temp1) + 0x5151;
        byte response3 = (byte) (temp2 & 0xff);
        unsigned long temp3 = (temp2 & 0xff00) >> 8;
        byte response4 = (byte) (temp3 & 0xff);

        //send unlock codes
        byte unlock[10] = {0x04, 0x0, 0x04, 0x27, 0x02, response3, response4, 0, 0, 0};
        sendToECU(unlock);
      }
      //if message is 03 67 02 34 then extended commands are unlocked, and need to start sending keep-alive ping
      else if ((spiDataPacket[2] == 0x03) && (spiDataPacket[3] == 0x67) && (spiDataPacket[4] == 0x02) && (spiDataPacket[5] == 0x34))
      {
        // enable 0x3f keep alive ping to ecu
        TCCR2A = 0x02;
        TCCR2B = 0x07;
        OCR2A = 0x7d;  // 9 ms with 1024 prescalar - 0x8c (64 us per tick) (8ms 0x7d) (7 ms 0x6d) (6ms 0x5e) (4ms 0x3e)
        TCNT2 = 0x0;
        TIMSK2 = 0x02;
      }
      else if ((spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x35))
      {
        //forward resonse to 0x35 download message, even during active download
        sendTransmitPacket(spiDataPacket);
      }
      else if (processingUpload)
      {
        if ((spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x31) && (spiDataPacket[5] == 0xa2))
        {
          //if processing upload, and recieve 31 a2 ack, then send msg 37
          byte msg37[10] = {0x04, 0x0, 0x02, 0x37, 0x80, 0, 0, 0, 0, 0};
          sendToECU(msg37);
        }
        else if ((spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x31) && (spiDataPacket[5] == 0xb2))
        {
          //if processing upload, and recieve 31 a2 ack, then send msg 37
          byte msg37[10] = {0x04, 0x0, 0x02, 0x37, 0x00, 0, 0, 0, 0, 0};
          sendToECU(msg37);
        }
        else if ((spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x37) && (spiDataPacket[5] == 0x80))
        {
          //if processing upload, and recieve 37 80 ack, then send msg 32 a2
          byte msg32_a2[10] = {0x04, 0x0, 0x02, 0x32, 0xa2, 0, 0, 0, 0, 0};
          sendToECU(msg32_a2);
        }
        else if ((spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x37) && (spiDataPacket[5] == 0x00))
        {
          //if processing upload, and recieve 37 80 ack, then send msg 32 a2
          byte msg32_b2[10] = {0x04, 0x0, 0x02, 0x32, 0xb2, 0, 0, 0, 0, 0};
          sendToECU(msg32_b2);
        }
        else if ((spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x32) && (spiDataPacket[5] == 0xa2)
        && (spiDataPacket[6] == 0x00) && (spiDataPacket[7] == 0x00) && (spiDataPacket[8] == 0x21))
        {
          //if processing upload, and receive 32 a2 ack indicating still processing, then re-send msg 32 a2
          byte msg32_a2[10] = {0x04, 0x0, 0x02, 0x32, 0xa2, 0, 0, 0, 0, 0};
          sendToECU(msg32_a2);
        }
        else if ((spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x32) && (spiDataPacket[5] == 0xb2)
        && (spiDataPacket[6] == 0x00) && (spiDataPacket[7] == 0x00) && (spiDataPacket[8] == 0x21))
        {
          //if processing upload, and receive 32 a2 ack indicating still processing, then re-send msg 32 a2
          byte msg32_b2[10] = {0x04, 0x0, 0x02, 0x32, 0xb2, 0, 0, 0, 0, 0};
          sendToECU(msg32_b2);
        }
        else if ((spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x32) && ((spiDataPacket[5] == 0xa2) || (spiDataPacket[5] == 0xb2))
        && (spiDataPacket[6] == 0x00) && (spiDataPacket[7] == 0x00) && (spiDataPacket[8] == 0x00))
        {
          //if processing upload, and receive 32 a2 ack indicating still success, then clear uploadProcessing flag
          processingUpload = false;
        }
      }
      else if ((spiDataPacket[5] == 0xa3) && (spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x31))
      {
        // request to validate reflash checksum on cpu1
        validateReflashCPU1 = true;
        byte msg32_a3[10] = {0x04, 0x0, 0x02, 0x32, 0xa3, 0, 0, 0, 0, 0};
        sendToECU(msg32_a3);
      }
      else if ((spiDataPacket[5] == 0xb3) && (spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x31))
      {
        // request to validate reflash checksum on cpu1
        validateReflashCPU2 = true;
        byte msg32_b3[10] = {0x04, 0x0, 0x02, 0x32, 0xb3, 0, 0, 0, 0, 0};
        sendToECU(msg32_b3);
      }
      else if (validateReflashCPU1 && (spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x32) && ((spiDataPacket[5] == 0xa3))
        && (spiDataPacket[6] == 0x00) && (spiDataPacket[7] == 0x00) && (spiDataPacket[8] == 0x21))
      {
        byte msg32_a3[10] = {0x04, 0x0, 0x02, 0x32, 0xa3, 0, 0, 0, 0, 0};
        sendToECU(msg32_a3);
      }
      else if (validateReflashCPU2 && (spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x32) && ((spiDataPacket[5] == 0xb3))
        && (spiDataPacket[6] == 0x00) && (spiDataPacket[7] == 0x00) && (spiDataPacket[8] == 0x21))
      {
        byte msg32_b3[10] = {0x04, 0x0, 0x02, 0x32, 0xb3, 0, 0, 0, 0, 0};
        sendToECU(msg32_b3);
      }
      else if (validateReflashCPU1 && (spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x32) && ((spiDataPacket[5] == 0xa3))
        && (spiDataPacket[6] == 0x00) && (spiDataPacket[7] == 0x00) && (spiDataPacket[8] == 0x00))
      {
        validateReflashCPU1 = false;
      }
      else if (validateReflashCPU2 && (spiDataPacket[2] == 0x06) && (spiDataPacket[3] == 0x7f) && (spiDataPacket[4] == 0x32) && ((spiDataPacket[5] == 0xb3))
        && (spiDataPacket[6] == 0x00) && (spiDataPacket[7] == 0x00) && (spiDataPacket[8] == 0x00))
      {
        validateReflashCPU2 = false;
      }
    }

    //forward packet to PC
    if(!pingResponse && !activeDownload)
    {
      sendTransmitPacket(spiDataPacket);
      //filter out 02 7f 00 responses to ecu pings
    }

    //reset active download flag here to ensure last packet is buffered not forwarded directly
    if (readyToForwardDownload && activeDownload)
    {
        activeDownload = false;
    }

    if ((spiDataPacket[0] == 0xfd) && (spiDataPacket[1] == 0x80)) //if packet is from 0x7ec SID
    {
      packetSent = false;

      // check for 0x7ec unlock commands message response
      if ((spiDataPacket[2] == 0x04) && (spiDataPacket[3] == 0x67) && (spiDataPacket[4] == 0x01))
      {
        //retrieve seed values
        unsigned int seedByte0 = spiDataPacket[5] & 0xff;
        unsigned int seedByte1 = spiDataPacket[6] & 0xff;

        //compute unlock codes
        unsigned int temp1 = ((seedByte0) << 8) + (seedByte1);
        unsigned long temp2 = (temp1 * temp1) + 0x5151;
        byte response3 = (byte) (temp2 & 0xff);
        unsigned long temp3 = (temp2 & 0xff00) >> 8;
        byte response4 = (byte) (temp3 & 0xff);

        //send unlock codes
        byte unlock[10] = {0xfd, 0x0, 0x04, 0x27, 0x02, response3, response4, 0, 0, 0};
        sendToECU(unlock);
      }


    }

  }
}

void setup() {

  //setup pins and modes

  pinMode(7, OUTPUT);  //LED2
  digitalWrite(7, LOW);
  
  pinMode(8, OUTPUT);  //LED1
  digitalWrite(8, LOW);

  pinMode(9, OUTPUT);  //chip select for SD card
  digitalWrite(9, HIGH);

  pinMode(10, OUTPUT);  //chip select for SPI canBus module
  digitalWrite(10, HIGH);

  pinMode(11, OUTPUT);  //MOSI for SPI
  pinMode(13, OUTPUT); //SCK for SPI

  pinMode(4, OUTPUT); //used for debugging
  digitalWrite(4, HIGH);

  //configure arduino SPI interface
  SPCR = 0x50; //SPI no interrupt, mode 0, MSB first, 8 MHz clock
  SPSR = 0x01; //SPI interface 8 MHz clock setting SPI2X bit 0
  // SPDR is the SPI data register for byte input/output
  //SPSR bit 7 is the SPI flag which is set for completion of data read/write (collision flag is bit 6)

  delay(1);

   //reset MCP2515 via SPI
  PORTB = 0x02; //write D10 low to chip select MCP2515
  SPDR = 0xc0; //reset MCP2515
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;
  PORTB = 0x06; //raise chip select after SPI transfers
  delay(1);
  
  //configure MCP2515
  
  //configure can bit rate to 500 kbps, and receive interrupts on buffer 0
  PORTB = 0x02; //CS
  SPDR = 0x02; // send write register instruction
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x28;// start at CNF3 register
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x02; //CNF3 PHSEG2 = 2, which is 3 Time Quanta (TQ). Note SyncSeg defaults to 1 TQ
   //note usually use 0x02, but can use 0x82 to enable SOF signal output for debugging
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x90; //CNF2 PHSEG1 = 2, which is 3 Time Quanta (TQ), and PSEG = 0 which is 1 TQ.
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x01; //CNF1 Baud Rate Prescaler = 1, which is 4 * oscillator period, so can speed is 16 MHz / (4 * (3+1+3+1)) = 500 kHz
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  PORTB = 0x06; // No CS
  DELAY_CYCLES(2);

  //turn off buffer 0 receive filters and masks, configure buffer rollover
  PORTB = 0x02; //CS
  SPDR = 0x02; // write
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x60;// RXB0CTRL register
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x64; // turn masks and filters off, with BUKT (rollover)
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  PORTB = 0x06; // No CS
  DELAY_CYCLES(2); //send

  //turn off buffer 1 receive filters and masks
  PORTB = 0x02; //CS
  SPDR = 0x02; // write
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x70;// RXB0CTRL register
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x60; // turn masks and filters off
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  PORTB = 0x06; // No CS
  DELAY_CYCLES(2); //send

  //clear error flags and interrupt flags
  
  PORTB = 0x02; //CS
  SPDR = 0x02; // write
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x2c;// CANINTF register
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x00; // 0x00 to CANINTF
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x00; // 0x00 to EFLG
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  PORTB = 0x06; // No CS
  DELAY_CYCLES(2); //send

 //set normal  mode  CANCTRL = 00000000 = 0x00
  PORTB = 0x02; //CS
  SPDR = 0x02; // write spi
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x0f; // CANCTRL register
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x00; // write 0x00 to set normal mode
   //note usually use 0x00, but can use 0x04 to enable SOF signal output for debugging
   //note one shot mode is enabled by setting bit 3, i.e. 0x08
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  PORTB = 0x06; // No CS
  DELAY_CYCLES(2); //
  
  //initialise serial interface to PC
  Serial.begin(250000);

  //configure Arduino for D2 interrupts
  EICRA = 0x00; //set INT0 to trigger on low level
  //necessary rather than falling edge because interrupts on RX0 and RX1 can both occur before either is cleared 
  EIMSK = 0x01; //enable interrupts on D2 (= INT0)

  //disable timer0 interrupts as not using millis or micros
  TIMSK0 = 0;

  //enable receive interrupts on MCP2515
  PORTB = 0x02; //CS
  SPDR = 0x02; // write spi
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x2b; // write CANINTE
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  SPDR = 0x03; //CANINTE enable receive interrupts on receive buffers 0 and 1
  while ((SPSR & 0x80) == 0) {};
  temp = SPDR;

  PORTB = 0x06; // No CS
  DELAY_CYCLES(2); //
}

void loop() {

  checkSerial();

  if (readyToForwardDownload)
  {
    readyToForwardDownload = false;
    downloadForwarding = true;
    readOutCounter = 0;
    forwardedCounter = 0;
  }

  if (downloadForwarding)
  {
    forwardDownloadData();
  }

  processTransmitBuffer();

  processSPI();

  processECUBuffer();

}
