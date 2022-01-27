import com.sun.jna.Library;
import org.vosk.Recognizer;

import java.io.IOException;
import java.io.InputStream;

public class SpeechStreamService {

    private final Recognizer recognizer;
    private final InputStream inputStream;
    private final int sampleRate;
    private final static float BUFFER_SIZE_SECONDS = 0.2f;
    private final int bufferSize;

    private Thread recognizerThread;


    /**
     * Creates speech service.
     * @param recognizer
     * @param inputStream
     * @param sampleRate
     */
    public SpeechStreamService(Recognizer recognizer, InputStream inputStream, int sampleRate) {
        this.recognizer = recognizer;
        this.inputStream = inputStream;
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

            byte[] buffer = new byte[bufferSize];

            while (!interrupted()
                    && ((timeoutSamples == NO_TIMEOUT) || (remainingSamples > 0))) {
                try {
                    int nread = inputStream.read(buffer, 0, buffer.length);
                    if (nread < 0) {
                        break;
                    } else {
                        boolean isSilence = recognizer.acceptWaveForm(buffer, nread);
                        if (isSilence) {
                            final String result = recognizer.getResult();
                            System.out.println(result);

//                            mainHandler.post(() -> listener.onResult(result));
                        } else {
                            final String partialResult = recognizer.getPartialResult();
                            System.out.println(partialResult);
//                            mainHandler.post(() -> listener.onPartialResult(partialResult));
                        }
                    }

                    if (timeoutSamples != NO_TIMEOUT) {
                        remainingSamples = remainingSamples - nread;
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // If we met timeout signal that speech ended
            if (timeoutSamples != NO_TIMEOUT && remainingSamples <= 0) {
//                mainHandler.post(() -> listener.onTimeout());
            } else {
                final String finalResult = recognizer.getFinalResult();
//                mainHandler.post(() -> listener.onFinalResult(finalResult));
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


}
