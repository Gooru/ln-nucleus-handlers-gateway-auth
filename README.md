Nucleus Auth Handler
================

This is the Auth handler for Project Nucleus. 

This project contains just one main verticle which is responsible for listening for Auth address on message bus. The gateway
 passes on the the session token of every incoming request, which is then validated by Auth handler.


TODO
----
* Provide Verticle to listen to auth request
* Validate the request with Redis
* Provide the response in pre defined format

To understand build related stuff, take a look at **BUILD_README.md**.


