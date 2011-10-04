This project is currently low on my priorities list and likely won't see a lot of attention in the near future. 
It works with socket.io 0.6, but since socket.io 0.7 was essentially a rewrite, it will require a fair amount of work
to continue moving it forward.

#Socket.io-Netty

This is an implementation of the socket.io server built on top of Netty. Currently, it only
supports 3 of the Socket.io protocols: websocket, flashsocket, and xhr-polling.

For an example of production use, check out http://www.typewire.io

#Roadmap

I'm currently waiting for the socket.io 0.7 release to come out before making any big changes.
As the 0.7 release is looking like a big rewrite, I will take that opportunity to add in the other
supported transports as well. 
