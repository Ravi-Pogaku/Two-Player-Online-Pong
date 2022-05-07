Fun project idea for getting more familiar with networking. Currently client desync is the main issue, I will revisit this project in the future to try making the server also simulate the gamestate and make corrections to clients or something like that.

Steps for server hosting:
Edit PongServer line 32 with desired port.
Edit Pong.java line 50 with desired ip and server port.
Port-forward the chosen port.

Steps for playing game:
Have other player edit Pong.java line 50 with server host's public ipv4.
Run PongServer.java.
Once server is running, players run Pong.java to play
against each other.
