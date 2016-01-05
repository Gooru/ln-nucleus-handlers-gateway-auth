Nucleus Auth Handler
================

This is the Auth handler for Project Nucleus. 

This project contains just one main verticle which is responsible for listening for Auth address on message bus. The gateway
 passes on the the session token of every incoming request, which is then validated by Auth handler.


TODO
----
* Include a way to enable testing where program can supply success response to avoid setting up Redis, if needed
* If need arises, then provide a response transformer
* If performance is getting impacted, move the expiry update to a worker verticle

To understand build related stuff, take a look at **BUILD_README.md**.


