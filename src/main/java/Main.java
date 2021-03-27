import listener.RotomListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main{

    public static void main(String[] args) throws LoginException, IOException, InterruptedException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("bot.properties"));
        RotomListener rotomListener = new RotomListener(prop.getProperty("bulbapedia"));
        JDA jda = JDABuilder.createDefault(prop.getProperty("token"))
                .addEventListeners(rotomListener)
                .setActivity(Activity.watching("@Rotom help"))
                .build();
        rotomListener.setJDA(jda);
        jda.awaitReady();
    }
}
