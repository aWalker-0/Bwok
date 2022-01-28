import com.awok.bwok.Config;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavaplayer.PlayerManager;
import lavaplayer.TrackScheduler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.Nullable;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.security.auth.login.LoginException;
import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class DiscoBot extends ListenerAdapter {
    private static Recognizer recognizer;
    private static final Queue<byte[]> queue = new ConcurrentLinkedQueue<>();
    private static final RecognizerTimer recognizerTimer = new RecognizerTimer();
//    private static User user;

    private static TextChannel currentChannel;

    private static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private static AudioPlayer audioPlayer = playerManager.createPlayer();
    private static TrackScheduler trackScheduler = new TrackScheduler(audioPlayer);

    public static void main(String[] args) throws LoginException {

        String token = Config.get("TOKEN");


        // We only need 2 gateway intents enabled for this example:
        EnumSet<GatewayIntent> intents = EnumSet.of(
                // We need messages in guilds to accept commands from users
                GatewayIntent.GUILD_MESSAGES,
                // We need voice states to connect to the voice channel
                GatewayIntent.GUILD_VOICE_STATES
        );

        // Build bot entity with these characteristics
        JDA jda = JDABuilder.createDefault(token, intents)
                .addEventListeners(new DiscoBot())
                .setActivity(Activity.listening("perpetual yeah'ing"))
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .enableCache(CacheFlag.VOICE_STATE)
                .build();

        // Load model and configure recognizer
        try {
            Model model = new Model("model");
            recognizer = new Recognizer(model, 16000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     static class RecognizerTimer {
        private boolean isOn;
        private static Timer timer;
        private long timerLength;

        public RecognizerTimer() {
            this.isOn = false;
        }

        private void start(long numSeconds) {
            timer = new Timer();
            // Assign # of seconds to variable
            this.timerLength = numSeconds;
            // Schedule a new timer task
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // Reset and send string data from recognizer on end of timer
                    interpretStringData(recognizer.getPartialResult());
                    recognizer.reset();
                }
                // Timer counts in milliseconds, so multiply by 100 to get seconds
            }, this.timerLength);
            isOn = true;
        }

        private boolean isCountingDown() {
            return isOn;
        }

        private void stop() {
            timer.cancel();
            this.timerLength = 0;
            this.isOn = false;
            recognizer.reset();
        }

        private void reset() {
            timer.cancel();
            this.start(this.timerLength);
        }

        private void reset(long numSeconds) {
            timer.cancel();
            this.start(numSeconds);
        }

        private void fullReset() {

        }
    }


    private static void interpretStringData(String stringData) {
        System.out.println(stringData);
        if (stringData.contains("my wife")) {
            PlayerManager.getInstance()
                    .loadAndPlay(currentChannel,
                            "C:\\DiscoBot Audio Files\\my_wife.wav");
            recognizer.reset();
        }
        if (stringData.contains("yeah")) {
            PlayerManager.getInstance()
                    .loadAndPlay(currentChannel,
                            "C:\\DiscoBot Audio Files\\_yea_.wav");
            recognizer.reset();
        }
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = message.getAuthor();
        String content = message.getContentRaw();
        Guild guild = event.getGuild();

        // Ignore message if bot
        if (author.isBot())
            return;

        // We only want to handle messages in Guilds
        if (!event.isFromGuild()) {
            return;
        }

        // echo command
        if (content.startsWith("!echo ")) {
            String arg = content.substring("!echo ".length());
            onEchoCommand(event, guild, arg);
        } else if (content.equals("!echo")) {
            onEchoCommand(event);
        }

        if (content.equals("!leave")) {
            onLeaveCommand(event);
        }

        if (content.equals("!play")) {
            onPlayCommand(event);
        }

        if (content.equals("!join")) {
            onJoinCommand(event);
        }

        if (content.equals("!!say")) {
            onSayCommand(event);
        }
    }


    private void onPlayCommand(MessageReceivedEvent event) {
        Member member = event.getMember();                              // Member is the context of the user for the specific guild, containing voice state and roles
        GuildVoiceState voiceState = member.getVoiceState();            // Check the current voice state of the user
        AudioChannel voiceChannel = voiceState.getChannel();
        TextChannel textChannel = event.getTextChannel();
        PlayerManager.getInstance()
                .loadAndPlay(textChannel, "C:\\Users\\syrco\\IdeaProjects\\DiscoBot\\10001-90210-01803.wav");
    }


    private void onLeaveCommand(MessageReceivedEvent event) {
        // Get audio channel
        AudioChannel channel = event.getGuild().getSelfMember().getVoiceState().getChannel();
        currentChannel = null;
        if (channel != null) {
            event.getGuild().getAudioManager().closeAudioConnection();

        }
    }


    private void onJoinCommand(MessageReceivedEvent event) {
        // Note: None of these can be null due to our configuration with the JDABuilder!

        Member member = event.getMember();                              // Member is the context of the user for the specific guild, containing voice state and roles
        GuildVoiceState voiceState = member.getVoiceState();            // Check the current voice state of the user
        AudioChannel channel = voiceState.getChannel();                 // Use the channel the user is currently connected to
        currentChannel = event.getTextChannel();
//        user = event.getAuthor();
        if (channel != null) {
            connectTo(channel);                                         // Join the channel of the user
            onConnecting(channel, event.getChannel());                  // Tell the user about our success
        } else {
            onUnknownChannel(event.getChannel(), "your voice channel"); // Tell the user about our failure
        }
    }


    private void onEchoCommand(MessageReceivedEvent event) {
        // Note: None of these can be null due to our configuration with the JDABuilder!
        Member member = event.getMember();                              // Member is the context of the user for the specific guild, containing voice state and roles
        GuildVoiceState voiceState = member.getVoiceState();            // Check the current voice state of the user
        AudioChannel channel = voiceState.getChannel();                 // Use the channel the user is currently connected to
        if (channel != null) {
            connectTo(channel);                                         // Join the channel of the user
            onConnecting(channel, event.getChannel());                  // Tell the user about our success
        } else {
            onUnknownChannel(event.getChannel(), "your voice channel"); // Tell the user about our failure
        }
    }


    private void onEchoCommand(MessageReceivedEvent event, Guild guild, String arg) {
        boolean isNumber = arg.matches("\\d+"); // This is a regular expression that ensures the input consists of digits
        VoiceChannel channel = null;
        if (isNumber)                           // The input is an id?
        {
            channel = guild.getVoiceChannelById(arg);
        }
        if (channel == null)                    // Then the input must be a name?
        {
            List<VoiceChannel> channels = guild.getVoiceChannelsByName(arg, true);
            if (!channels.isEmpty())            // Make sure we found at least one exact match
                channel = channels.get(0);      // We found a channel! This cannot be null.
        }

        MessageChannel messageChannel = event.getChannel();
        if (channel == null)                    // I have no idea what you want mr user
        {
            onUnknownChannel(messageChannel, arg); // Let the user know about our failure
            return;
        }
        connectTo(channel);                     // We found a channel to connect to!
        onConnecting(channel, messageChannel);     // Let the user know, we were successful!
    }


    private void onSayCommand(MessageReceivedEvent event) {
        TextChannel textChannel = event.getGuild().getTextChannelsByName("d2-discussion", true).get(0);
        textChannel.sendMessage("I can riad tomarow if yu nead anethor human.").queue();
    }


    private void onConnecting(AudioChannel channel, MessageChannel messageChannel) {
        messageChannel.sendMessage("Connecting to " + channel.getName()).queue(); // never forget to queue()!
    }


    private void onUnknownChannel(MessageChannel channel, String comment) {
        channel.sendMessage("Unable to connect to ''" + comment + "'', no such channel!").queue();
    }


    private void onAnnoyCommand(MessageReceivedEvent event) {

    }


    private void connectTo(AudioChannel channel) {
        Guild guild = channel.getGuild();
        // Get an audio manager for this guild, this will be created upon first use for each guild
        AudioManager audioManager = guild.getAudioManager();
        // Create our Send/Receive handler for the audio connection
        EchoHandler handler = new EchoHandler();

        // The order of the following instructions does not matter!

        // Set the sending handler to our echo system
//        audioManager.setSendingHandler(handler);
        // Set the receiving handler to the same echo system, otherwise we can't echo anything
        audioManager.setReceivingHandler(handler);
        // Connect to the voice channel
        audioManager.openAudioConnection(channel);


    }

    private static void recognizeQueueData() {

        AudioFormat target = new AudioFormat(
                16000, 16, 1, true, false);
        while (!queue.isEmpty()) {
            byte[] data = queue.poll();
            if (data != null) {
                // Create audio stream that uses the target format and the byte array input stream from discord
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(target,
                        new AudioInputStream(
                                new ByteArrayInputStream(data), AudioReceiveHandler.OUTPUT_FORMAT, data.length));
                try {
                    int nbytes;
                    byte[] b = new byte[4096];
                    while ((nbytes = inputStream.read(b)) >= 0) {
                        if (!recognizer.acceptWaveForm(b, nbytes)) {
                            String result = recognizer.getPartialResult();
                            interpretStringData(result);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // Reset the timer if we hear speech again before countdown end
        if (recognizerTimer.isCountingDown()) {
            recognizerTimer.reset();
        } else {
            // Otherwise, start a timer
            recognizerTimer.start(200);
        }

    }


    public static class EchoHandler implements AudioReceiveHandler {

        /*
            All methods in this class are called by JDA threads when resources are available/ready for processing.
            The receiver will be provided with the latest 20ms of PCM stereo audio
            Note you can receive even while setting yourself to deafened!
            The sender will provide 20ms of PCM stereo audio (pass-through) once requested by JDA
            When audio is provided JDA will automatically set the bot to speaking!
         */
//        private final Queue<byte[]> queue = new ConcurrentLinkedQueue<>();

        /* Receive Handling */

        // We want to look at specific users, so use both receive and handle user
        @Override // give audio separately for each user that is speaking
        public boolean canReceiveUser() {
            // this is not useful if we want to echo the audio of the voice channel, thus disabled for this purpose
            return queue.size() < 10;
        }

        @Override
        public void handleUserAudio(UserAudio userAudio) {
//            System.out.println(userAudio.getUser());

//            User audioFromUser = userAudio.getUser();
//            if (audioFromUser == user) {
                // Start grabbing data
                byte[] data = userAudio.getAudioData(1.0f);
                queue.add(data);

                recognizeQueueData();
//            }
        }


        /* Send Handling */
//
//        @Override
//        public boolean canProvide() {
//            // If we have something in our buffer we can provide it to the send system
//            return !queue.isEmpty();
//        }
//
//        @Nullable
//        @Override
//        public ByteBuffer provide20MsAudio() {
//            return null;
//        }
//
//        @Override
//        public boolean isOpus() {
//            // since we send audio that is received from discord we don't have opus but PCM
//            return false;
//        }
    }
}