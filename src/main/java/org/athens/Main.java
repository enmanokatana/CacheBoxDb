package org.athens;

import org.athens.cli.CliMain;
import org.athens.network.Server;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar _code-1.0-SNAPSHOT.jar [server|cli]");
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "server":
                System.out.println("Starting CacheBox server...");
                Server.main();
                break;
            case "cli":
                System.out.println("Starting CacheBox CLI...");
                CliMain.main(new String[0]);
                break;
            default:
                System.out.println("Invalid mode. Use 'server' or 'cli'.");
                break;
        }
    }
}