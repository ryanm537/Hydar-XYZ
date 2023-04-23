<h1>What <i>is</i> Hydar?</h1>
Hydar is a web application which primarily offers chat services, which are centered around boards. Users can create <b>boards</b>, 
		which can be set to either public or private. Public boards can be joined by anyone who is given the board's 
		unique ID, whereas private boards (the default) are invite-only. As of now, there are certain 
		boards that will be displayed for every user. These boards are <b>Everything Else</b>, <b>Skyblock</b>, and <b>SAS4</b>.
		As one can guess, these are primarily focused on individual online games, but users can create boards for whatever topics they'd like!
		There are also direct-message channels, which are similar to boards in all ways except that they are private and between two people.
		All you need to send someone a direct-message is their unique user ID.
		<br><br>
		Every board also has a <b>voice channel</b>. This is a chat room in which users can communicate via their microphone. Users can 
		also <b>share their screen</b> while in the voice channel, or can choose to view other users' screens (assuming they are sharing theirs).
		<br><br>
		Within boards, there are certain bot commands that can be used. These commands are focused on the games currently supported by this
		web app. There is also a chat bot that can commune with users in the official public boards, and can be invited to private boards or directly
		meessaged. For more information on bots and other commands that can be used, simply type <b>/help</b> in a board.
		<br><br>
		And finally, this web app is completely <b>anonymous</b>! That means you don't need an account to access the site or use any of it's features.
		<br><br>
<h1>Documentation</h1>
<img src='https://user-images.githubusercontent.com/77253453/233814930-bd1c03e1-a87d-422b-8869-ff4109941307.png' />

Hydar consists of many independent components, as shown in <b>figure 1</b>. The first layer is a custom implementation of an HTTP file server, including support for various HTTP extensions such as caching, partial downloads, compression, and a custom implementation of <b>HTTP/2.0</b>, supporting <b>server-sided events</b>. It also fully supports encrypted sessions using the builtin Java implementation of TLSv1.3. Under default configurations, files will be reloaded periodically through a low overhead watch service, and static files under 1MB will be cached in memory.
<br><br>
The next layer is the <b>custom JSP compiler</b> and Java EE implementation for servlet code, built upon the Hydar HTTP server, allowing for dynamic webapps which are generally compatible with other servlet containers. The servlet code may be compiled completely in memory and no reflection is needed for responding to the requests after compilation. This additionally allows for <b>authenticated WebSocket sessions</b> that can dynamically dispatch servlet requests, and support for sessions in environments without cookies. For a full list of the implemented Java EE APIs and how to set up your own HydarEE app, see the javadoc(soon)
<br><br>
Next is the <b>servlet logic</b> of the webapp, compiled on the HydarEE layer, which provides the functionality for accounts, boards, messages, and authenticated WebRTC signaling for voice and video calls. It also provides easily extensible APIs for bots on the server, which can use server-sided Python and C in a configurable task queue. Some commands are also able to embed interactive JavaScript content, such as /bloons.
<br><br>
<img src='https://user-images.githubusercontent.com/77253453/233814979-20ec0893-7163-4e32-98a6-e0bbb3609f0a.png'/>

Finally, the <b>frontend code</b> contains the client aspects of these, ensuring seamless real-time text, voice, and video with good handling for poor connections. <b>Figure 2</b> shows a typical Hydar session, featuring a voice call and minimized screen-share.
<br><br>
In addition to the above main components, Hydar includes additional, optional modules to improve user experience. These include a pure Java implementation of the <b>STUN/TURN</b> protocol for relaying calls, as well as advanced, custom <b>JDBC connection and statement pooling</b>, which ensures prepared statements are closed properly and connections are reused, and is loaded through JNDI similarly to in other servlet containers. Finally, Hydar can optionally <b>rate limit</b> expensive servlet code through the HydarLimiter API.
<br><br>
