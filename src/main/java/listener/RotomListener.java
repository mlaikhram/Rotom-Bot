package listener;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.MessageUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class RotomListener extends ListenerAdapter {

    private static final String NATIONAL_DEX_ENDPOINT = "/wiki/List_of_Pokemon_by_National_Pokedex_number";
//    private static final String NATIONAL_DEX_ENDPOINT = "/wiki/List_of_Pokémon_by_National_Pokédex_number";

    private JDA jda;

    private final String bulbapedia;

    // <Pokemon name, Bulbapedia link> used to pick a random pokemon
    private Map<String, String> pokemonLinks;

    public RotomListener(String bulbapedia) throws IOException {
        this.bulbapedia = bulbapedia;
        initializePokemonLinks();
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        System.out.println(event.getMessage().getContentRaw());
        if (event.getAuthor().isBot()) {
            return;
        }

        String myID = jda.getSelfUser().getId();
        System.out.println("my id: " + myID);
        User author = event.getAuthor();
        MessageChannel sourceChannel = event.getChannel();
        String rawMessage = event.getMessage().getContentRaw();
        String[] messageTokens = rawMessage.split(" ");

        if (event.isFromType(ChannelType.TEXT) && MessageUtils.isUserMention(messageTokens[0])) {
//            System.out.println("message received from " + author + "!");
//            System.out.println(rawMessage);
//            System.out.println("my id is " + myID);

            System.out.println("id: " + MessageUtils.mentionToUserID(messageTokens[0]));
            if (MessageUtils.mentionToUserID(messageTokens[0]).toString().equals(myID)) {
//            if ("651588878968422411".equals("651588878968422411")) {
                System.out.println("in the loop");

                if (messageTokens.length >= 2 && messageTokens[1].equals("guess")) {
                    whosThatPokemon(event);
                }

                else if (messageTokens.length >= 4 && messageTokens[1].equals("play") && messageTokens[2].equals("with")) {
//                    initializeGame(event);
                    List<User> invitedUsers = new ArrayList<>();
                    for (int i = 3; i < messageTokens.length; ++i) {
                        try {
                            System.out.println(messageTokens[i]);
                            if (MessageUtils.isUserMention(messageTokens[i])) {
                                User target = sourceChannel.getJDA().getUserById(MessageUtils.mentionToUserID(messageTokens[i]));
                                if (target.isBot()) {
                                    throw new Exception(target.getName() + " is a bot! Bots do not know how to play Werewolf");
                                }
                                else if (target.getId().equals(author.getId())) {
                                    throw new Exception("You are moderating this game! You cannot participate as well");
                                }
                                // DMListeners.put(target.getId(), author.getId());
                                invitedUsers.add(target);
                            }
                            else {
                                throw new Exception(messageTokens[i] + " is not a valid user");
                            }
                        }
                        catch (Exception e) {
                            event.getChannel().sendMessage(e.getMessage()).queue();
                        }
                    }
                    // TODO: remove check and add it to session loop
//                    if (invitedUsers.size() < WerewolfSession.MIN_PLAYER_COUNT) {
//                        event.getChannel().sendMessage("You need at least " + WerewolfSession.MIN_PLAYER_COUNT + " valid players to play Werewolf").queue();
//
//                        for (User user : invitedUsers) {
//                            user.openPrivateChannel().queue((channel) -> {
//                                channel.sendMessage(author.getName() + " did not send enough invites. This game has been cancelled");
//                            });
//                            DMListeners.remove(user.getId());
//                        }
//                        sessions.remove(author.getId());
//                    }

//                    else { // TODO: check for players/moderator already in a session or moderating a session
//                        WerewolfSession session = new WerewolfSession(sourceChannel, author, invitedUsers);
//                        sessions.put(author.getId(), session);
//                        Collection<String> userIDs = session.promptPlayers(); // TODO: if response list size == 0, end session
//                        for (String userID : userIDs) {
//                            DMListeners.put(userID, session.getModerator().getId());
//                        }
//                        DMListeners.put(author.getId(), author.getId());
//                        try {
//                            session.openInvites();
//                        }
//                        catch (Exception e) {
//                            session.getChannel().sendMessage("Something went wrong with the session. Closing session").queue();
//                            for (String id : session.getRoles().keySet()) {
//                                DMListeners.remove(id);
//                            }
//                            DMListeners.remove(session.getModerator().getId());
//                            sessions.remove(session.getModerator().getId());
//                        }
//                    }
                }
                else {
                    event.getChannel().sendMessage("I didn't quite get that").queue();
                }
            }
        }
        else if (event.isFromType(ChannelType.PRIVATE)) {
            author.openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Please message me from within a public channel!").queue();
            });
        }
    }

    public void initializePokemonLinks() throws IOException {
        pokemonLinks = new HashMap<>();
        System.out.println("getting tables");
        Document doc = Jsoup.connect(bulbapedia + NATIONAL_DEX_ENDPOINT).get();
//        Document doc = Jsoup.parse(new URL(bulbapedia + NATIONAL_DEX_ENDPOINT).openStream(), "UTF-8", bulbapedia + NATIONAL_DEX_ENDPOINT);
//        System.out.println(doc.toString());
//        System.out.println(doc.getAllElements());
        Elements tableElements = doc
                .getElementById("globalWrapper")
                .getElementById("column-content")
                .getElementById("content")
                .getElementById("outercontentbox")
                .getElementById("contentbox")
                .getElementById("bodyContent")
                .getElementById("mw-content-text")
                .getElementsByTag("table");

        System.out.println("tables");
        for (int i = 0; i < tableElements.size(); ++i) {
            Element table = tableElements.get(i);
            if (table.attr("align").equals("center")) {
                Elements rows = table
                        .getElementsByTag("tbody").first()
                        .getElementsByTag("tr");

                for (int j = 1; j < rows.size(); ++j) {
                    Element pokemonCell = rows.get(j)
                            .getElementsByTag("th").first()
                            .getElementsByTag("a").first();
                String name = pokemonCell.attr("title");
                String endpoint = pokemonCell.attr("href");
                System.out.println(name + " : " + endpoint);
                pokemonLinks.put(name, endpoint);
                }
            }
        }
    }

    public void whosThatPokemon(MessageReceivedEvent event) {
        Random rng = new Random();
        Object[] pokemonNames = pokemonLinks.keySet().toArray();
        String pokemon = pokemonNames[rng.nextInt(pokemonNames.length)].toString();

        event.getChannel().sendMessage(pokemon + ": " + bulbapedia + pokemonLinks.get(pokemon)).queue();
    }
}
