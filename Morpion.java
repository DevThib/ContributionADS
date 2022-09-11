package fr.devthib.tests.comands;

import fr.devthib.tests.Command;
import fr.devthib.tests.CommandExecutor;
import fr.devthib.tests.Main;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.*;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.interaction.SelectMenuChooseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Morpion implements CommandExecutor {

    User user1;
    User user2;

    HashMap<Integer,User> cases = new HashMap<>();

    User turn;

    @Override
    public void run(MessageCreateEvent event, Command command, String[] args) {

        if (event.getMessage().getMentionedUsers().size() != 0 && !event.getMessage().getMentionedUsers().get(0).getMentionTag().equals(event.getMessageAuthor().asUser().get().getMentionTag())) {

            user1 = event.getMessageAuthor().asUser().get();
            user2 = event.getMessage().getMentionedUsers().get(0);
            turn = user1;

            sendAcceptMessage(event.getChannel());

            addAll();
        } else {
            event.getMessage().toMessageBuilder().setContent("Veuillez mentionner quelqu'un").replyTo(event.getMessage()).send(event.getChannel());
        }

    }

    private void addAll(){
        for(int i = 0; i < 9; i++){
            cases.put(i, Main.api.getYourself());
        }
    }

    private String getTable(boolean showTurn){
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < 9; i++){
            str.append(getEmoji(cases.get(i)));
            if(i != 2 && i != 5 && i != 8)str.append("|");
            if(i == 2 || i == 5)str.append("\n");
        }
        if(showTurn)str.append("\nC'est au tour de ").append(turn.getMentionTag());
        return str.toString();
    }

    private String getEmoji(User user){
        if(user == user1)return "⭕";
        if(user == user2)return "❌";
        return "◻️";
    }

    private void sendAcceptMessage(TextChannel channel){
        try {
            Message message;
            MessageBuilder messageBuilder = new MessageBuilder()
                    .setContent(user2.getMentionTag() + "," + user1.getMentionTag() + " souhaite faire un morpion avec vous ! L'accepter ?")
                    .addComponents(
                            ActionRow.of(Button.success("valid", "", "✅")),
                            ActionRow.of(Button.danger("unvalid", "", "❎")));

            message = messageBuilder.send(channel).get();
            message.addButtonClickListener(event1 -> {

                if (event1.getButtonInteractionWithCustomId("valid").isPresent()) {
                    if (event1.getInteraction().getUser().getMentionTag().equals(user2.getMentionTag())) {
                        message.delete();
                        play(channel);
                    }
                } else {
                    if (event1.getInteraction().getUser().getMentionTag().equals(user1.getMentionTag()) || event1.getInteraction().getUser().getMentionTag().equals(user2.getMentionTag())) {
                        message.delete();
                        channel.sendMessage(event1.getInteraction().getUser().getMentionTag()+" a annulé le morpion ❌");
                    }
                }

            });
        }catch (InterruptedException | ExecutionException e){
            channel.sendMessage("Une erreur est survenue,nous sommes désolés 😢");
        }
    }

    private void play(TextChannel channel){

        try {

            List<SelectMenuOption> options = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                options.add(SelectMenuOption.create("Case " + (i+1), String.valueOf(i)));
            }

            SelectMenu selectMenu = SelectMenu.create("caseSelection", options);

            Message message;
            MessageBuilder messageBuilder = new MessageBuilder()
                    .setContent(getTable(true))
                    .addComponents(ActionRow.of(selectMenu));

            message = messageBuilder.send(channel).get();
            message.addSelectMenuChooseListener(selectMenuChooseEvent -> {
                if(turn.getMentionTag().equals(selectMenuChooseEvent.getInteraction().getUser().getMentionTag()) && cases.get(Integer.parseInt(selectMenuChooseEvent.getSelectMenuInteraction().getChosenOptions().get(0).getValue())).getMentionTag().equals(Main.api.getYourself().getMentionTag())){
                    cases.replace(Integer.parseInt(selectMenuChooseEvent.getSelectMenuInteraction().getChosenOptions().get(0).getValue()),turn);
                    if(checkWin(turn)){
                        message.delete();
                        channel.sendMessage(getTable(false)+"\nLe joueur **" + turn.getName() + "** a gagné la partie");
                    }
                    changeTurn();
                    message.edit(getTable(true));
                }
            });

        }catch (ExecutionException | InterruptedException e){}
    }

    private void changeTurn(){
        if(turn.getMentionTag().equals(user1.getMentionTag()))turn = user2; else turn = user1;
    }

    private boolean checkWin(User user){
        for(int i = 0;i < 3; i++){
            if(cases.get(i*3).getMentionTag().equals(user.getMentionTag()) && cases.get(i*3+1).getMentionTag().equals(user.getMentionTag()) && cases.get(i*3+2).getMentionTag().equals(user.getMentionTag())){
                return true;
            }
        }
        for(int i = 0;i < 3; i++){
            if(cases.get(i).getMentionTag().equals(user.getMentionTag()) && cases.get(i+3).getMentionTag().equals(user.getMentionTag()) && cases.get(i+6).getMentionTag().equals(user.getMentionTag())){
                return true;
            }
        }
        if(cases.get(0).getMentionTag().equals(user.getMentionTag()) && cases.get(4).getMentionTag().equals(user.getMentionTag()) && cases.get(8).getMentionTag().equals(user.getMentionTag())){
            return true;
        }
        if(cases.get(2).getMentionTag().equals(user.getMentionTag()) && cases.get(4).getMentionTag().equals(user.getMentionTag()) && cases.get(6).getMentionTag().equals(user.getMentionTag())){
            return true;
        }
        return false;
    }

}
