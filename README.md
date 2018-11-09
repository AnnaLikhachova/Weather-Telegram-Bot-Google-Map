# Weather-Telegram-Bot-Google-Map
Telegram Bot showing weather using Google Map API, OpenWeatherMap API.
JTelegramBot is a Java library with a Telegram Bot API. Weather data provided by OpenWeatherMap. 
Reverse geolocation is provided by Google Map API.

Dependencies 
•	Java 8 
•	jsoup 
•	guava 
•	json 
•	httpcore-nio
• com.mashape.unirest 
•	log4j
•	slf4j-api

How to use Telegram Bot? 
The name of the bot @BotAssistantwbot. 
You can find it in Telegram. 
First you should write command /start and follow instructions.

How Telegram Bot works? 
Telegram apps will have interface shortcuts for these commands. 
/start - begins interaction with the user, e.g., by sending a greeting message. 
This command can also be used to pass additional parameters to the bot. 
/help - returns a help message. It can be a short text about what your bot can do and a list of commands. 
/settings - (if applicable) returns the bot's settings for this user and suggests commands to edit these settings. 
For more see https://core.telegram.org/bots/api

Implementation 
1	Firstly talk to BotFather and follow a few simple steps. Once you've created a bot and received your authorization token, 
head down to the Bot API manual to see what you can teach your bot to do. Add token to the project.
2	Start writing Java code by using long polling mechanism with the help of getUpdates method. 
Extend from TelegramLongPollingBot to implement polling. 
3 Register at https://openweathermap.org/api and get your weatherApiKey. Add it to the project.
4 Register at https://developers.google.com/maps/documentation/geocoding/get-api-key and get your googleApiKey. 
Add it to the project.
5	Finally install the project with Maven.
6 Run as Java app.

Exceptions Handling Most of the methods in this library throws 3 types of exceptions: 
•	JSONException: if a JSON exception occurs. 
• TelegramApiException: if a Telegram exception occurs. 
•	IOException: if an I/O exception occurs. 


Copyright and Licensing Information
This project is licensed.
