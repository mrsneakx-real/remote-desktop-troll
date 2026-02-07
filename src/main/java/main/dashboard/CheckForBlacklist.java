package main.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

// ... full class code as above ...
public class CheckForBlacklist {
    private Set<String> blockedCommands;

    public CheckForBlacklist(String path) throws Exception {
        blockedCommands = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();

        File localFile = new File(path);
        JsonNode root = null;
        if (localFile.exists()) {
            root = mapper.readTree(localFile);
        } else {
            // Remove leading slash for resource lookup
            String resPath = path.startsWith("/") ? path.substring(1) : path;
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resPath);
            if (resourceStream == null) {
                throw new Exception("Failed to load blacklist: " + path);
            }
            root = mapper.readTree(resourceStream);
        }
        if (root != null && root.has("blocked")) {
            for (JsonNode n : root.get("blocked")) {
                blockedCommands.add(n.asText());
            }
        }
    }

    public boolean isCommandAllowed(String input) {
        String lowerInput = input.toLowerCase();
        for (String cmd : blockedCommands) {
            if (lowerInput.matches(".*\\b" + cmd.toLowerCase() + "\\b.*")) {
                return false;
            }
        }
        return true;
    }
}