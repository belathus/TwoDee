package logic;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import sheets.PPManager;

import java.util.concurrent.ExecutionException;

/**
 * This class adds plot points, subtracts plot points, and sets plot points for players. This class will also keep
 * track of doom points
 */
public class PlotPointHandler {

    private PPManager ppManager = new PPManager();
    private UserInfo userInfo = new UserInfo();
    private String[] args;
    private MessageAuthor messageAuthor;
    private DiscordApi api;

    public PlotPointHandler(String args, MessageAuthor author, DiscordApi api) {
        this.args = args.split(" ");
        this.messageAuthor = author;
        this.api = api;
    }

    //2 args : ~p [add|sub|addall|set] number
    //3 args: ~p name [add|sub|addall|set] number
//    @Command(aliases = {"~p", "~plot", "~plotpoints"}, description = "Manages plot points and doom points", usage = "~p <name> <[add|sub|addall|set]> [number]")
    public EmbedBuilder processCommandType() {
        String commandType = "";
        String target = "";
        int amount;
        //Get user's plot points
        if (args.length == 2) {
            String userID = convertPingToID(args[1]);
            return getPlotPoints(userID);
        }
        //No user specified
        else if (args.length == 3) {
            target = messageAuthor.getIdAsString();
            commandType = args[1];
            amount = Integer.parseInt(args[2]);
        }
        //User specified
        else {
            target = convertPingToID(args[1]);
            commandType = args[2];
            amount = Integer.parseInt(args[3]);
        }
        return executeCommand(commandType, target, amount);
    }

    //Execute a command based on the command type. If an invalid command is entered, send an error embed message
    private EmbedBuilder executeCommand(String commandType, String target, int number) {
        switch (commandType) {
            case "add":
                return addPlotPoints(target, number);

            case "sub":
                return addPlotPoints(target, number * -1);

            case "addall":
                return addPlotPointsToAll(number);

            case "set":
                return setPlotPoints(target, number);

            default:
                return new EmbedBuilder()
                        .setAuthor(messageAuthor)
                        .setTitle("Invalid command");
        }
    }

    //Converts a ping into a user ID
    private String convertPingToID(String arg) {
        return arg.replaceAll("[^A-Za-z0-9]", "");
    }

    private EmbedBuilder setPlotPoints(String target, int number) {
        ppManager.setPlotPoints(target, number);
        return getPlotPoints(target);
    }

    private EmbedBuilder addPlotPointsToAll(int number) {
        for (String ID : userInfo.getUsers()) {
            try {
                if (isConnected(ID)){
                    ppManager.setPlotPoints(ID, ppManager.getPlotPoints(ID) + number);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        EmbedBuilder allPlayerEmbed = new EmbedBuilder()
                .setTitle("Everyone's plot points!");
        for (String id : userInfo.getUsers()) {
            try {
                allPlayerEmbed.addField(api.getUserById(id).get().getName(), String.valueOf(ppManager.getPlotPoints(id)), true);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return getPlotPoints(null);
    }

    private boolean isConnected(String ID) throws InterruptedException, ExecutionException {
        return api.getServerVoiceChannelById("468046159781429254").get().isConnected(api.getUserById(ID).get());
    }

    private EmbedBuilder addPlotPoints(String target, int number) {
        ppManager.setPlotPoints(target, ppManager.getPlotPoints(target) + number);
        return getPlotPoints(target);
    }

    private EmbedBuilder getPlotPoints(String target) {
        try {
            return new EmbedBuilder()
                    .setAuthor(api.getUserById(target).get())
                    .setTitle("Plot points")
                    .setDescription(String.valueOf(ppManager.getPlotPoints(target)));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new EmbedBuilder()
                    .setAuthor(messageAuthor)
                    .setTitle("User not found!");
        }
    }

}
