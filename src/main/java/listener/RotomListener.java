package listener;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.MessageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
//        System.out.println("my id: " + myID);
        User author = event.getAuthor();
        MessageChannel sourceChannel = event.getChannel();
        String rawMessage = event.getMessage().getContentRaw();
        String[] messageTokens = rawMessage.split(" ");

        if (event.isFromType(ChannelType.TEXT) && MessageUtils.isUserMention(messageTokens[0])) {
//            System.out.println("message received from " + author + "!");
//            System.out.println(rawMessage);
//            System.out.println("my id is " + myID);

//            System.out.println("id: " + MessageUtils.mentionToUserID(messageTokens[0]));
            if (MessageUtils.mentionToUserID(messageTokens[0]).toString().equals(myID)) {
                System.out.println("in the loop");

                if (messageTokens.length >= 2 && messageTokens[1].equals("guess")) {
                    try {
                        whosThatPokemon(event);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
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
        System.out.println("done");
    }

    public void whosThatPokemon(MessageReceivedEvent event) throws IOException {
        Random rng = new Random();
        Object[] pokemonNames = pokemonLinks.keySet().toArray();
        String pokemon = pokemonNames[rng.nextInt(pokemonNames.length)].toString();
        Document doc = Jsoup.connect(bulbapedia + pokemonLinks.get(pokemon)).get();

        System.out.println(bulbapedia + pokemonLinks.get(pokemon));

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
            event.getChannel().sendFile(shadowImage(new ByteArrayInputStream(baos.toByteArray())), "shadow.png").complete();
            event.getChannel().sendMessage("It's " + pokemon + "!").completeAfter(10, TimeUnit.SECONDS);
            event.getChannel().sendFile(new ByteArrayInputStream(baos.toByteArray()), pokemon + ".png").complete();
        }
    }

    public InputStream shadowImage(InputStream imageStream) throws IOException {
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
