import com.sun.jna.Library;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;

public class TestService {

    private final Recognizer recognizer;
    private final Queue<byte[]> queue;
    private final int sampleRate;
    private final static float BUFFER_SIZE_SECONDS = 0.2f;
    private final int bufferSize;

    private Thread recognizerThread;


    /**
     * Creates speech service.
     * @param recognizer
     * @param queue
     * @param sampleRate
     */
    public TestService(Recognizer recognizer, Queue<byte[]> queue, int sampleRate) {
        this.recognizer = recognizer;
        this.queue = queue;
        this.sampleRate = sampleRate;
        bufferSize = Math.round(this.sampleRate * BUFFER_SIZE_SECONDS * 2);
    }

    public boolean start(RecognitionListener listener) {
        if (null != recognizerThread)
            return false;

        recognizerThread = new RecognizerThread(listener);
        recognizerThread.start();
        return true;
    }

    /**
     * Starts recognition. After specified timeout listening stops and the
     * endOfSpeech signals about that. Does nothing if recognition is active.
     * <p>
     * timeout - timeout in milliseconds to listen.
     *
     * @return true if recognition was actually started
     */
    public boolean start(RecognitionListener listener, int timeout) {
        if (null != recognizerThread)
            return false;

        recognizerThread = new RecognizerThread(listener, timeout);
        recognizerThread.start();
        return true;
    }


    private final class RecognizerThread extends Thread {

        private int remainingSamples;
        private final int timeoutSamples;
        private final static int NO_TIMEOUT = -1;
        RecognitionListener listener;

        public RecognizerThread(RecognitionListener listener, int timeout) {
            this.listener = listener;
            if (timeout != NO_TIMEOUT)
                this.timeoutSamples = timeout * sampleRate / 1000;
            else
                this.timeoutSamples = NO_TIMEOUT;
            this.remainingSamples = this.timeoutSamples;
        }

        public RecognizerThread(RecognitionListener listener) {
            this(listener, NO_TIMEOUT);
        }

        @Override
        public void run() {
            AudioInputStream inputStream;
            AudioFormat target = new AudioFormat(
                    16000, 16, 1, true, false);


            byte[] buffer = new byte[4096];

            while (!interrupted()
                    && ((timeoutSamples == NO_TIMEOUT) || (remainingSamples > 0))) {

                byte[] data = queue.poll();
                if (data != null) {
                    inputStream = AudioSystem.getAudioInputStream(target,
                            new AudioInputStream(
                                    new ByteArrayInputStream(data), AudioReceiveHandler.OUTPUT_FORMAT, data.length));
                    try {
                        int nbytes;
                        while ((nbytes = inputStream.read(buffer)) >= 0) {
                            boolean isSilence = recognizer.acceptWaveForm(buffer, nbytes);
                            if (isSilence) {
                                final String result = recognizer.getResult();
                                System.out.println(result);

//                            mainHandler.post(() -> listener.onResult(result));
                            } else {
                                final String partialResult = recognizer.getPartialResult();
                                System.out.println(partialResult);
//                            mainHandler.post(() -> listener.onPartialResult(partialResult));
                            }

                            if (timeoutSamples != NO_TIMEOUT) {
                                remainingSamples = remainingSamples - nbytes;
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // If we met timeout signal that speech ended
                    if (timeoutSamples != NO_TIMEOUT && remainingSamples <= 0) {
//                mainHandler.post(() -> listener.onTimeout());
                    } else {
                        final String finalResult = recognizer.getFinalResult();
//                    System.out.println(finalResult);
//                mainHandler.post(() -> listener.onFinalResult(finalResult));
                    }
                }
            }
        }
    }
}



    //    public static void main(String[] argv) throws IOException, UnsupportedAudioFileException {
//        LibVosk.setLogLevel(LogLevel.DEBUG);


//        try (Model model = new Model("model");
//             InputStream ais = AudioSystem.getAudioInputStream(
//                     new BufferedInputStream(
//                             new FileInputStream("10001-90210-01803.wav")));
//             Recognizer recognizer = new Recognizer(model, 16000)) {
//
//            int nbytes;
//            byte[] b = new byte[4096];
//            while ((nbytes = ais.read(b)) >= 0) {
//                if (recognizer.acceptWaveForm(b, nbytes)) {
//                    System.out.println(recognizer.getResult());
//                } else {
//                    System.out.println(recognizer.getPartialResult());
//                }
//            }
//
//            System.out.println(recognizer.getFinalResult());
//        }
//    }



