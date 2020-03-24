package listener;

import model.GuessPokemon;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.MessageUtils;
import util.NumberUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RotomListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RotomListener.class);
    
    private static final String NATIONAL_DEX_ENDPOINT = "/wiki/List_of_Pokemon_by_National_Pokedex_number";
    private static final int DEFAULT_TIMER_SECONDS = 10;

    private final String bulbapedia;

    // Used to keep track of which channels have an active "who's that pokemon?" challenge
    private final Map<String, GuessPokemon> activeWTPChannels;

    private JDA jda;

    // <Pokemon name, Bulbapedia link> used to pick a random pokemon
    private Map<String, String> pokemonLinks;


    public RotomListener(String bulbapedia) throws IOException {
        this.bulbapedia = bulbapedia;
        this.activeWTPChannels = new HashMap<>();
        initializePokemonLinks();
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String myID = jda.getSelfUser().getId();
        User author = event.getAuthor();
        MessageChannel sourceChannel = event.getChannel();
        String rawMessage = event.getMessage().getContentRaw();
        String[] messageTokens = rawMessage.split(" ");

        if (event.isFromType(ChannelType.TEXT)) {
            if (MessageUtils.isUserMention(messageTokens[0]) && MessageUtils.mentionToUserID(messageTokens[0]).toString().equals(myID)) {
                logger.info("message received from " + author + ": " + rawMessage);

                if (messageTokens.length <= 1 || (messageTokens.length >= 2 && messageTokens[1].equals("help"))) {
                    event.getChannel().sendMessage(MessageUtils.HELP_TEXT).queue();
                }
                else if (messageTokens.length >= 2 && messageTokens[1].equals("guess")) {
                    if (!activeWTPChannels.containsKey(event.getChannel().getId())) {
                        logger.info("who's that pokemon?");
                        try {
                            int timer = DEFAULT_TIMER_SECONDS;
                            if (messageTokens.length >= 3) {
                                timer = NumberUtils.parseInt(messageTokens[2], DEFAULT_TIMER_SECONDS);
                            }
                            whosThatPokemon(event, timer);
                        } catch (IOException e) {
                            logger.error(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else {
                        logger.error("channel: [" + event.getGuild().getName() + "][" + event.getChannel().getName() + "] already has an active \"Who's that Pokemon?\" session");
                        event.getChannel().sendMessage("Please wait for me to respond to the last request!").queue();
                    }
                }
                else if (messageTokens.length >= 2 && messageTokens[1].equals("end")) {
                    if (activeWTPChannels.containsKey(event.getChannel().getId())) {
                        logger.info("cancelling \"Who's that Pokemon?\" session in channel: [" + event.getGuild().getName() + "][" + event.getChannel().getName() + "]");
                        activeWTPChannels.get(event.getChannel().getId()).fastForward(activeWTPChannels);
                    }
                    else {
                        logger.error("channel: [" + event.getGuild().getName() + "][" + event.getChannel().getName() + "] does not have an active \"Who's that Pokemon?\" session");
                        event.getChannel().sendMessage("There is no active guessing game in this channel!").queue();
                    }
                }
                else {
                    logger.error("unknown command");
                    event.getChannel().sendMessage("I didn't quite get that").queue();
                }
            }
        }
        else if (event.isFromType(ChannelType.PRIVATE)) {
            logger.info("private message received from " + author);
            author.openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Please message me from within a public channel!").queue();
            });
        }
    }

    public void initializePokemonLinks() throws IOException {
        pokemonLinks = new HashMap<>();
        logger.info("getting pokemon names and image links from " + bulbapedia + NATIONAL_DEX_ENDPOINT);
        Document doc = Jsoup.connect(bulbapedia + NATIONAL_DEX_ENDPOINT).get();
        Elements tableElements = getToBulbapediaContent(doc).getElementsByTag("table");

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
                pokemonLinks.put(name, endpoint);
                }
            }
        }
        logger.info("done");
    }

    public void whosThatPokemon(MessageReceivedEvent event, int timer) throws IOException {
        activeWTPChannels.put(event.getChannel().getId(), null);
        Random rng = new Random();
        Object[] pokemonNames = pokemonLinks.keySet().toArray();
        String pokemon = pokemonNames[rng.nextInt(pokemonNames.length)].toString();
        Document doc = Jsoup.connect(bulbapedia + pokemonLinks.get(pokemon)).get();

        logger.info("getting pokemon image from " + bulbapedia + pokemonLinks.get(pokemon));

        String photoUrl = "https:" +  getToBulbapediaContent(doc)
                .getElementsByClass("roundy").first()
                .getElementsByTag("tbody").first()
                .getElementsByTag("tr").first()
                .getElementsByTag("td").first()
                .getElementsByClass("roundy").first()
                .getElementsByTag("tbody").first()
                .child(1)
                .getElementsByTag("td").first()
                .getElementsByClass("roundy").first()
                .getElementsByTag("tbody").first()
                .getElementsByTag("tr").first()
                .getElementsByTag("td").first()
                .getElementsByTag("a").first()
                .getElementsByTag("img").first()
                .attr("src");

        try (InputStream in = new URL(photoUrl).openStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            in.transferTo(baos);

            event.getChannel().sendMessage("Who's that pokemon?").complete();
            event.getChannel().sendFile(shadowImage(new ByteArrayInputStream(baos.toByteArray())), "shadow.png").queue();
            logger.info("shadow image sent");

            CompletableFuture<Message> namePromise = event.getChannel().sendMessage("It's " + pokemon + "!").submitAfter(timer, TimeUnit.SECONDS);
            CompletableFuture<Message> imagePromise = event.getChannel().sendFile(new ByteArrayInputStream(baos.toByteArray()), pokemon + ".png").submitAfter(timer, TimeUnit.SECONDS);
            imagePromise.thenAccept((message) -> {
                activeWTPChannels.remove(event.getChannel().getId());
            });

            GuessPokemon guessPokemon = new GuessPokemon(event, pokemon, baos, namePromise, imagePromise);
            activeWTPChannels.put(event.getChannel().getId(), guessPokemon);

            logger.info("colored image sent");
        }
    }

    public InputStream shadowImage(InputStream imageStream) throws IOException {
        logger.info("shadowing image");
        BufferedImage image = ImageIO.read(imageStream);
        int width = image.getWidth();
        int height = image.getHeight();
        WritableRaster raster = image.getRaster();

        for (int xx = 0; xx < width; xx++) {
            for (int yy = 0; yy < height; yy++) {
                int[] pixels = raster.getPixel(xx, yy, (int[]) null);
                if (pixels[3] >= 10) {
                    pixels[0] = 0;
                    pixels[1] = 0;
                    pixels[2] = 0;
                    pixels[3] = 255;
                }
                else {
                    pixels[0] = 0;
                    pixels[1] = 0;
                    pixels[2] = 0;
                    pixels[3] = 0;
                }
                raster.setPixel(xx, yy, pixels);
            }
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image,"png", os);

        logger.info("done");
        return new ByteArrayInputStream(os.toByteArray());
    }

    public Element getToBulbapediaContent(Document doc) {
        return doc
                .getElementById("globalWrapper")
                .getElementById("column-content")
                .getElementById("content")
                .getElementById("outercontentbox")
                .getElementById("contentbox")
                .getElementById("bodyContent")
                .getElementById("mw-content-text");
    }
}
