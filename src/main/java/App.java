import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class App {
    final static Map<Server, Map<String, Member>> serverMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        String token = "REDACTED";

        Map<String, Member> memberMap = new ConcurrentHashMap<>();
        DiscordApi bot = new DiscordApiBuilder()
                .setToken(token)
                .setAllIntents()
                .login()
                .join();
        User self = bot.getYourself();

        self.getMutualServers()
                .forEach(server -> {
                    serverMap.put(server, new ConcurrentHashMap<>());
                    iterateAndSaveMembers(server);
                    System.out.println("Bot is online as " + self.getDiscriminatedName() + " at " + server.getName());
                });

        bot.addUserChangeStatusListener(event -> {
            User user = event.getUser().get();
            if (event.getNewStatus().equals(UserStatus.ONLINE)) {
                user.getMutualServers()
                        .forEach(server -> {
                            saveOrLoadMember(server, user);
                            Member member = serverMap.get(server).get(user.getDiscriminatedName());
                            member.setOnline();
                            startTimer(server, member);
                        });
            } else if (event.getOldStatus().equals(UserStatus.ONLINE)) {
                user.getMutualServers()
                        .forEach(server -> {
                            Member member = serverMap.get(server).get(user.getDiscriminatedName());
                            member.setOffline();
                            saveMember(server, member);
                        });

            }
        });


        bot.addMessageCreateListener(event -> {
            User author = event.getMessageAuthor().asUser().get();
            String content = event.getMessageContent().toLowerCase();
            if (!author.isYourself()) {
                switch (content) {
                    case "!bonus":
                        Member member = serverMap.get(event.getServer().get()).get(author.getDiscriminatedName());
                        event.getChannel().sendMessage(author.getNicknameMentionTag() + " has " +
                                member.getPoints() + " bonus points!");
                        break;
                    case "!help":
                        // print help statement;
                        break;
                    case "!info":
                        // print info about user;
                        break;
                }
            }
        });
    }

    public static void iterateAndSaveMembers(Server server) {
        server.getMembers().stream()
                .filter(user -> !user.isBot())
                .forEach(user -> {
                    saveOrLoadMember(server, user);
                    if (user.getStatus().equals(UserStatus.ONLINE)) {
                        Member member = serverMap.get(server).get(user.getDiscriminatedName());
                        member.setOnline();
                        startTimer(server, member);
                    }
                });
    }

    public static void saveOrLoadMember(Server server, User user) {
        File newMember = new File(System.getProperty("user.dir") + "\\" + user.getDiscriminatedName() + ".bin");
        String userName = user.getDiscriminatedName();
        if (!newMember.exists()) {
            serverMap.get(server).put(userName, new Member(userName, user.getId()));
            saveMember(server, serverMap.get(server).get(userName));
        } else {
            try {
                FileInputStream input = new FileInputStream(newMember);
                ObjectInputStream reader = new ObjectInputStream(input);
                serverMap.get(server).put(userName, (Member) reader.readObject());
                reader.close();
                input.close();
                System.out.println(userName + " loaded from file");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveMember(Server server, Member member) {
        File newMember = new File(System.getProperty("user.dir") + "\\" + member.getUserName() + ".bin");
        try {
            FileOutputStream output = new FileOutputStream(newMember);
            ObjectOutputStream writer = new ObjectOutputStream(output);
            writer.writeObject(member);
            writer.flush();
            output.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startTimer(Server server, Member member) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if(!member.isOnline())
                executor.shutdown();
            member.addPoints(1);
            saveMember(server, member);
        }, 0, 1, TimeUnit.MINUTES);
    }

}
