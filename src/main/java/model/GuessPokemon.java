package model;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GuessPokemon {

    private MessageReceivedEvent event;
    private String name;
    private ByteArrayOutputStream imageStream;
    private CompletableFuture<Message> namePromise;
    private CompletableFuture<Message> imagePromise;

    public GuessPokemon() {
        this.name = "PENDING";
    }

    public GuessPokemon(MessageReceivedEvent event, String name, ByteArrayOutputStream imageStream, CompletableFuture<Message> namePromise, CompletableFuture<Message> imagePromise) {
        this.event = event;
        this.name = name;
        this.imageStream = imageStream;
        this.namePromise = namePromise;
        this.imagePromise = imagePromise;
    }

    public void fastForward(Map<String, GuessPokemon> activeWTPChannels) {
        if (namePromise != null) {
            namePromise.cancel(true);
        }
        if (imagePromise != null) {
            imagePromise.cancel(true);
        }
        event.getChannel().sendMessage("It's " + name + "!").queue();
        event.getChannel().sendFile(new ByteArrayInputStream(imageStream.toByteArray()), name + ".png").queue((message) -> {
            if (activeWTPChannels.containsKey(event.getChannel().getId())) {
                activeWTPChannels.remove(event.getChannel().getId());
            }
        });
    }
}
