# Rotom-Bot
Discord Bot that allows users to play "Who's that Pokemon?". A random Pokemon image is pulled from Bulbapedia and darkened to allow users to guess before revealing the image and name of the Pokemon.

### How to Install
You will need Gradle to run the Bot. Clone this repository, and run the following Gradle command (I use Intellij to open and run the project):
```
gradle clean jar
```
This will build the .jar file into the `target` folder. Before running the .jar file, you will need to create a file called `bot.properties` and place it in the same directory as the .jar file. `bot.properties` should contain the following lines:
```
token=<BOT_TOKEN>
bulbapedia=https://bulbapedia.bulbagarden.net
```
Replace `<BOT_TOKEN>` with the token obtained from your own Discord Bot (More details on creating a Discord Bot can be found [here](https://discord.com/developers/docs/intro)). Once this is set up, you can run the .jar file using the following command:
```
java -jar rotom-bot-1.0-SNAPSHOT.jar
```
Once the Bot is running, you can invite it to your Discord Server from the [Discord Developer Portal](https://discord.com/developers/applications) and interact with it from your text channels.

### Usage
`@Rotom guess [from gen [start gen] [to [end gen]]] [for [guess time]]`

Start a "Who's that Pokemon?" game session. I will select a Pokemon from [start gen] to [end gen] and reveal the answer in [guess time] seconds. All variables are optional, and will default to all generations for 10 seconds.

`@Rotom end`

End the current "Who's that Pokemon?" game session.

`@Rotom help`

View the list of commands that can be run from this bot.

### Example Usage
![alt text](https://i.ibb.co/THh5Qk8/rotom-sample.png)
