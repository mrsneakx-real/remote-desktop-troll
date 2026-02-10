package main;

public class WindowsNativeTextToSpeech {
    public static void main(String[] args) {
        String text = "Hello, my Name is Simon Corbi und ich habe Durchfall.";
        String command = String.format("powershell -Command \"Add-Type -AssemblyName System.Speech; " +
                "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$speak.Speak('%s');\"", text);

        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor(); // Wait for speech to finish
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}