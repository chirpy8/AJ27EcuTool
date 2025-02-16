A tool for reflashing a Jaguar AJ27 ECU.
The tool uses a Java based PC program (executable .jar) combined with an Arduino Uno R3 with Sparkfun Canbus adaptor to interface to the ECU.
See posts [here](https://chirpy8.github.io/)  more details.
Supported processes are:
* Uploading reflash firmware code to CPU1 and CPU2 RAM
* Reflashing CPU1 and CPU2
* Downloading the current firmware from CPU1 and CPU2
* Downloading the RAM contents of CPU1 and CPU2
* Downloading the key persistent data from the ECU (Configuration and VIN)
* Uploading key persistent data to the ECU (for cloning)
