selectrix4java
===============

####(Java library to communicate with an selectrix bus system.)

###Supported Format: SX1

*Tested with: FCC, Stärz Interface, Stärz Functionmodule, D&H BM 8i and D&H Decoder*

**RXTX binaries of target OS are required(!) for serial access to COM and USB interfaces.**
(http://www.jcontrol.org/download/readme_rxtx_en.html)

Setup project: ```mvn clean verify```

The goal "verify" is required to install the shipped RXTX JAR from the "lib" folder to your local maven repository.

Afterwards you can simply use ```mvn clean validate```.

####Take a look into the wiki for usage instructions.