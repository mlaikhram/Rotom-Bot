import listener.RotomListener;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main{

    public static void main(String[] args) throws LoginException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("bot.properties"));

        JDABuilder builder = new JDABuilder(AccountType.BOT);
        RotomListener rotomListener = new RotomListener(prop.getProperty("bulbapedia"));
        builder.setToken(prop.getProperty("token"));
        builder.addEventListeners(rotomListener);
        JDA jda = builder.build();
        rotomListener.setJDA(jda);
        jda.getPresence().setActivity(Activity.watching("@Rotom help"));
    }
}
