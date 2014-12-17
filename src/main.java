import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class main {

    static Scanner in;
    static ArrayList<Player> players;
    static ArrayList<Match> matchHistory;
    static int kfactor;
    static int ratingDisparity;
    static BufferedReader filereader;
    static PrintWriter updater;
    static PrintWriter matchRecorder;
    static int startElo;
    static ArrayList<Match> newMatches;
    static BufferedReader commandReader;
    static ArrayList<String> commands;

    public static void main(String[] args) throws FileNotFoundException, IOException {

        //Set initial values
        matchHistory = new ArrayList<Match>();
        newMatches = new ArrayList<Match>();
        startElo = 50;
        matchRecorder = new PrintWriter(new FileWriter("matches.txt", true));
        filereader = new BufferedReader(new FileReader("players.txt"));
        commandReader = new BufferedReader(new FileReader("input.txt"));
        commands = new ArrayList<String>();

        kfactor = 32;
        ratingDisparity = 400;
        players = new ArrayList<Player>();
        in = new Scanner(System.in);
        String buffer;
        StringTokenizer com;

        //Add all the names inside the previous txt file into the current ArrayList
        buffer = filereader.readLine();
        while (buffer != null) {
            com = new StringTokenizer(buffer);
            players.add(new Player(com.nextToken(), Integer.parseInt(com.nextToken())));
            buffer = filereader.readLine();
        }
        updater = new PrintWriter(new FileWriter("players.txt"));

        //WARNING: THERE IS NO ERROR HANDLING SYSTEM...
        //BAD THINGS WILL HAPPEN IF THE INPUT IS NOT ENTERED CORRECTLY
        System.out.println("PLEASE DO NOT ENTER IMPROPER INPUT");

        //Commands
        while (true) {
            System.out.println("Please enter the command...");
            if (commands.size() > 0) {
                buffer = commands.get(0);
                commands.remove(0);
            } else {
                buffer = in.nextLine();
            }
            String[] reader = buffer.split(" ");
            if (reader[0].charAt(0) == '-') {
                switch (reader[0]) {
                    case "-h":
                        help();
                        break;
                    case "-print":
                        print();
                        break;
                    case "-end":
                        print();
                        updater.close();
                        matchRecorder.close();
                        System.exit(0);
                        break;
                    case "-clear":
                        matchHistory.clear();
                        players.clear();
                        break;
                    case "-read":
                        read();
                        break;
                }
            } else if (reader[1].charAt(0) == '-') {
                switch (reader[1]) {
                    case "-beat":
                        updater(reader[0], reader[2]);
                        break;
                    case "-add":
                        if (reader.length == 3) {
                            players.add(new Player(reader[0], Integer.parseInt(reader[2])));
                        } else {
                            players.add(new Player(reader[0], startElo));
                        }
                        break;
                    case "-setK":
                        if (isInteger(reader[1])) {
                            kfactor = Integer.parseInt(reader[1]);
                        }
                        break;
                    case "-setD":
                        if (isInteger(reader[1])) {
                            ratingDisparity = Integer.parseInt(reader[1]);
                        }
                        break;
                    case "-read":
                        commandReader = new BufferedReader(new FileReader(reader[0]));
                }
            }
        }
    }

    static void read() {
        try {
            String buffer = commandReader.readLine();
            while (buffer != null) {
                commands.add(buffer);
                buffer = commandReader.readLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(main.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Reading failure");
        } finally {
            try {
                PrintWriter clearer = new PrintWriter(new FileWriter("input.txt"));
                clearer.println("");
                clearer.close();
            } catch (IOException ex) {
                Logger.getLogger(main.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Unable to clear input file");
            }
        }
    }

    static void print() {
        Collections.sort(players, new eloScoreSorter());
        for (int counter = 0; counter < players.size(); counter++) {
            updater.println(players.get(counter).name + " " + players.get(counter).score);
        }
        if (matchHistory.size() <= 0) {
            return;
        }
        for (int counter = 0; counter < matchHistory.size(); counter++) {
            matchRecorder.println(matchHistory.get(counter).winner.name + " -beat " + matchHistory.get(counter).loser.name);
        }
    }

    //First guy always is assumed to beat second guy
    static void updater(String name1, String name2) {
        Collections.sort(players, new nameSorter());
        int position1 = Collections.binarySearch(players, new Player(name1, 0), new nameSorter());
        int position2 = Collections.binarySearch(players, new Player(name2, 0), new nameSorter());
        int elo1 = players.get(position1).score;
        int elo2 = players.get(position2).score;
        int change = eloCalculator(elo1, elo2);
        elo1 += change;
        if (elo2 - change < 0) {
            elo2 = 0;
        } else {
            elo2 = elo2 - change;
        }
        players.set(position1, new Player(name1, elo1));
        players.set(position2, new Player(name2, elo2));
        matchHistory.add(new Match(new Player(name1, elo1), new Player(name2, elo2)));
    }

    static boolean isInteger(String str) {
//        try{
//            Integer.parseInt(test);
//            return true;
//        }
//        catch(Exception e){
//            return false;
//        }
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c <= '/' || c >= ':') {
                return false;
            }
        }
        return true;
    }

    //Assuming that x won against y...
    static int eloCalculator(int x, int y) {
        int change;
        int difference = Math.abs(x - y);
        change = (int) Math.round(kfactor * (1 - (1 / (Math.pow(10, (difference * -1 / ratingDisparity) + 1)))));
        return change;
    }

    static void help() {
        System.out.print("This is the help menu\n"
                + "-h will bring this menu up\n"
                + "-add will add a name to the list of names\n"
                + "    already in the list\n"
                + "    The format is 'name' then 'score' seperated by a space\n"
                + "-beat after one name will update the manager\n"
                + "    to it's total calculations of all the elo\n"
                + "-print will simply update all the elo within\n"
                + "    the text file\n"
                + "-set will set the elo of a player\n"
                + "-setK will set the kfactor\n"
                + "-setD will set the disparity\n"
                + "-end will end the program\n"
                + "-clear will clear all data\n");
    }
}
// E.g. max -beat joe
// max -set 239
//max -add 300
//max -add
//213 -setK

class eloScoreSorter implements Comparator<Player> {

    @Override
    public int compare(Player t, Player t1) {
        return t.score - t1.score;
    }

}

class nameSorter implements Comparator<Player> {

    @Override
    public int compare(Player t, Player t1) {
        return t.name.compareTo(t1.name);
    }

}

class Player {

    public String name;
    public int score;

    public Player(String name, int score) {
        this.name = name;
        this.score = score;
    }
}

class Match {

    public Player winner;
    public Player loser;

    public Match(Player winner, Player loser) {
        this.winner = winner;
        this.loser = loser;
    }
}
