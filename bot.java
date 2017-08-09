package bot.jda;

import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class bot extends ListenerAdapter
{
    MessageChannel ahchannel;
    List<Member> memvoters;
    List<User> voters = new ArrayList<User>(); 
    List<String> answers = new ArrayList<String>();
    User Referender;
	int refestep = 0;
	int repeat = 0;
    String question;
    String answerslist = new String();
    HashMap<User, String> results = new HashMap<User, String>();
    boolean selfwrite = false;
    
    
    public static void main(String[] args)
    {
        try
        {
            JDA jda = new JDABuilder(AccountType.BOT)
                    .setToken("MzM3MzI0NjQ1NzY4NDk1MTA0.DFQJrw.TqPxUKzqdvN26C9xd8irffZprTc")           
                    .addEventListener(new bot()) 
                    .buildBlocking();  
        }
        catch (LoginException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (RateLimitedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        JDA jda = event.getJDA();                       //JDA, the core of the api.
        long responseNumber = event.getResponseNumber();//The amount of discord events that JDA has received since the last reconnect.
        User author = event.getAuthor();                //The user that sent the message
        Message message = event.getMessage();           //The message that was received.
        MessageChannel channel = event.getChannel();    //This is the MessageChannel that the message was sent to.
                                                        //  This could be a TextChannel, PrivateChannel, or Group!

        String msg = message.getContent();              //This returns a human readable version of the Message. Similar to
                                                        // what you would see in the client.

        boolean bot = author.isBot();                   //This boolean is useful to determine if the User that
                                                        // sent the Message is a BOT or not!

        if (event.isFromType(ChannelType.TEXT))         //If this message was sent to a Guild TextChannel
        {
            Guild guild = event.getGuild();             //The Guild that this message was sent in. (note, in the API, Guilds are Servers)
            TextChannel textChannel = event.getTextChannel(); //The TextChannel that this message was sent to.
            Member member = event.getMember();          //This Member that sent the message. Contains Guild specific information about the User!

            String name;
            if (message.isWebhookMessage())
            {
                name = author.getName();                //If this is a Webhook message, then there is no Member associated
            }                                           // with the User, thus we default to the author for name.
            else
            {
                name = member.getEffectiveName();       //This will either use the Member's nickname if they have one,
            }                                           // otherwise it will default to their username. (User#getName())
        }
        else if (event.isFromType(ChannelType.PRIVATE)) //If this message was sent to a PrivateChannel
        {
            PrivateChannel privateChannel = event.getPrivateChannel();
        }
        else if (event.isFromType(ChannelType.GROUP))   //If this message was sent to a Group. This is CLIENT only!
        {
            Group group = event.getGroup();
            String groupName = group.getName() != null ? group.getName() : "";  //A group name can be null due to it being unnamed.
        }
        if (msg.equals("!help"))
        {
        	channel.sendMessage("Список комманд: \n 1) !ping -- бот отвечает \"pong!\" \n 2) !help -- список комманд бота").queue();
        }
        else if (msg.equals("!ping"))
        {
        	channel.sendMessage("pong!").queue();
        }
        else if (msg.equals("!референдум"))
        {
            if (message.isFromType(ChannelType.TEXT))
            {
            	if (refestep == 0) 
            	{
                    Referender = author;
                    ahchannel = channel;
                    channel.sendMessage("Введите вопрос референдума.").queue();
                    refestep = 1;
            	}
            	else
            	{
            		channel.sendMessage("Уже идет один референдум").queue();
            	}
            }
            else
            {
                channel.sendMessage("Эта комманда только для сервера!").queue();
            }
        }
        else if (author == Referender && channel == ahchannel && refestep == 1)
        {
        	question = msg;
        	channel.sendMessage("Вопрос: "+question+"\nЗадайте список ответов.").queue();
        	refestep = 2;
        }
        else if (author == Referender && channel == ahchannel && refestep == 2)
        {
            answerslist = new String();
        	int i = 0;
        	for (String answer : msg.split(", ")) //разделяем список ответов, на отдельные ответы
        	{
        		if (answers.contains(answer)==false) //защита от повторяющихся ответов
        		{
        			i=i+1;
        			answers.add(answer);
            		answerslist = answerslist + i +"й вариант: "+answer+"\n";
        		}
        	}
        	if (i==1)
        	{
        		channel.sendMessage("Вариантов ответа нет, участникам будет предложено самим вписать ответ. \nЗадайте список избирателей").queue();
        		answers = new ArrayList<String>();
        		selfwrite = true;
        		refestep = 3;
        	}
        	else 
        	{
        		channel.sendMessage(answerslist+"Задайте список избирателей").queue();
        		refestep = 3;
        	}
        }
        else if (author == Referender && channel == ahchannel && refestep == 3)
        {
        	Guild guild = event.getGuild();
        	if (message.getMentionedRoles().isEmpty()==false)
        	{
        		List<Role> votersRoles = message.getMentionedRoles(); //список ролей memberов с правом голоса
                memvoters = guild.getMembersWithRoles(votersRoles); //список memberов с правом голоса
                for (Member memvoter : memvoters) //список userов с правом голоса
                {
                	if (voters.contains(memvoter.getUser())==false) //на всякий случай
        			{
                		voters.add(memvoter.getUser());
        			}
                }
        	}
        	if(message.getMentionedUsers().isEmpty()==false)
        	{
        		List<User> votersUsers = message.getMentionedUsers(); //список упомянутых userов
        		for (User voter : votersUsers) //список userов с правом голоса
                {
        			if (voters.contains(voter)==false) //на всякий случай
        			{
                		voters.add(voter); //добавляем userов в общий список
        			}
                }
        	}
        	if (voters.isEmpty())
        	{
        		if (repeat == 2) //защита от неуказания списка избирателей
        		{
        			channel.sendMessage("Вы трижды не указали список избирателей. Процесс создания референдума отменен.").queue();
        			repeat = 0;
        			refestep = 0;
        		}
        		else
        		{
            		channel.sendMessage("Вы никого не указали").queue();
            		repeat++;
        		}
        	}
        	else
        	{
        		String text = new String();
                for (User voter : voters)
                {
                    text = text + guild.getMember(voter).getEffectiveName() +" записан как избиратель \n";
                    if (selfwrite == false)
                    {
                        voter.openPrivateChannel().complete().sendMessage("Здравствйте, " + guild.getMember(voter).getEffectiveName() +". Прошу Вас проголосовать по следующему вопросу:\n"+question+"\nВарианты ответа:\n"+answerslist).queue();

                    }
                    else
                    {
                        voter.openPrivateChannel().complete().sendMessage("Здравствйте, " + guild.getMember(voter).getEffectiveName() +". Прошу Вас проголосовать по следующему вопросу:\n"+question+"\nВарианты ответа не предложены, Вы можете вписать любой ответ").queue();
                    }
                }
                channel.sendMessage(text).queue();
                refestep = 4;
        	}
        }
        
        if (event.isFromType(ChannelType.PRIVATE) && voters.contains(author) && refestep == 4) //в личке, имеет право голоса, сообщение является вариантом ответа, завершающий этам референдума
        {
        	if(selfwrite == true)
        	{
        		answers.remove(results.get(author));
        		if (answers.contains(msg)==false)
        		{
        			answers.add(msg);
        		}
        		results.put(author, msg);
            	channel.sendMessage("Ваш голос учтен. Вы выбрали вариант: "+msg+". Чтобы переголосовать, Вы можете просто написать другой ответ").queue();
        	}
        	else if (answers.contains(msg))
        	{
        		results.put(author, msg);
            	channel.sendMessage("Ваш голос учтен. Вы выбрали вариант: "+msg+". Чтобы переголосовать, Вы можете просто написать другой вариант ответа").queue();
        	}
        	else if (Integer.parseInt(msg)-1 <= answers.size())
        	{
        		results.put(author, answers.get(Integer.parseInt(msg)-1));
        		channel.sendMessage("Ваш голос учтен. Вы выбрали вариант: "+answers.get(Integer.parseInt(msg)-1)+". Чтобы переголосовать, Вы можете просто написать другой вариант ответа").queue();
        	}
        }
        if (msg.equals("!сколько") && refestep == 4)
        {
        	channel.sendMessage("Проголосовало " + results.size() + " участников.").queue();
        }
        
        if (msg.equals("!результат") && refestep == 4 && author == Referender)
        {
        	refestep = 0;
        	String text = new String();
        	ArrayList<String> resultslist = new ArrayList<String>(results.values()); //создание списка с ответами участников
        	for(String answer : answers) //для каждого варианта ответа
        	{
        		double a = Collections.frequency(resultslist, answer); // сколько человек выбрало этот вариант ответа
        		double v = resultslist.size(); //сколько всего члеовек проголосовало
        		double perc = Math.round(a/v*100); //процент проголосовавших
        		text = text +answer+": "+ Collections.frequency(resultslist, answer) +" чел. ("+perc+"%)\n";
        	}
        	channel.sendMessage("Голосование по вопросу '"+question+"' завершено.\n"+"Проголосовало " + results.size() + " участников. Голоса разделились следующим образом:\n"+text).queue();
        	voters = new ArrayList<User>(); 
            answers = new ArrayList<String>();
            results = new HashMap<User, String>(); //очистка списков
        }
    }
}
