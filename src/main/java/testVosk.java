import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class testVosk {
    public static void main(String[] args) {
        LibVosk.setLogLevel(LogLevel.DEBUG);

        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                16000, 16, 2, 4, 44100, false);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (Model model = new Model("model");
             InputStream ais = AudioSystem.getAudioInputStream(
             new BufferedInputStream(
                     new FileInputStream("10001-90210-01803.wav")));
             Recognizer recognizer = new Recognizer(model, 16000)) {

            int nbytes;
            byte[] b = new byte[4096];
            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    System.out.println(recognizer.getResult());
                } else {
                    System.out.println(recognizer.getPartialResult());
                }
            }

            System.out.println(recognizer.getFinalResult());
        } catch (IOException | UnsupportedAudioFileException e) {
                    e.printStackTrace();
        }
    }
}

