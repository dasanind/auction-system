# auction-system
Distributed Auction System

There are three jar files implementing the publish-subscribe
auctioning systems.  These are (1) Buyer.jar, (2) Broker.jar, and (3)
Seller.jar.  There is also a configuration file config.txt that allows
the user to tweak the different design parameters before executing the
system.

To start a process, use the following commands:

 java -jar Buyer.jar config.txt
 java -jar Broker.jar config.txt
 java -jar Seller.jar config.txt

The number of brokers, buyers, and sellers that can be started is
given by the parameters in config.txt.

The different configuration parameters are the following:

- brokerServerPortNum: 

  We use this number as a base to calculate the different ports for
  the threads in the different processes to listen to.  This number
  need not be changed by the user as long as the ports being opened
  are actually accessible to the user process.  (They seem to be
  accessible on Mac OS X Lion.)  If they are not accessible, then the
  number should be changed to a value such that all the calculated
  ports are accessible.

- brokerServerName:  

  The name of the server in which the process runs.  Default is
  localhost.  (Although the parameter is named "brokerServerName" it
  is not only for the broker process.  Every process, including the
  seller and buyer, uses this parameter to determine the address of
  the machine where it is run.  If the different components are to be
  run on different machines, please use different versions of the
  config.txt file for different components, with different values of
  "brokerServerName".)

- numberOfBrokers:  

  Number of broker processes.  We implicitly assume that this many
  broker processes are always running when an active auction is going
  on.  That is, a broker is never assumed to leave.

- branchingFactor: The maximum number of child brokers connected to
  any broker b.  Note that the connection is implemented
  hierarchically.  Thus, the maximum number of brokers connected to b
  is typically this number plus 1 (unless b is a root, in which case
  it is this number).

- brokerCapacity: Number of clients (seller or buyer) that can be
  connected to a single broker.
