package logic;

import discord.TwoDee;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;

public class StatisticsGenerator {

    private ArrayList<DiceResult> resultList = new ArrayList<>();
    private HashMap<Integer, Double> statisticsMap;
    private HashMap<Integer, Double> doomMap;
    private boolean validCombo = false;
    private boolean overloaded = false;
    private ArrayList<Integer> diceList;
    private ArrayList<Integer> plotDice;

    public StatisticsGenerator(String message) {
        //Add all of the dice to the ArrayLists based on dice type
        diceList = new ArrayList<>();
        plotDice = new ArrayList<>();
        ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
        DiceParameterHandler diceParameterHandler = new DiceParameterHandler(args, diceList, plotDice);
        diceParameterHandler.addDiceToPools();

        //Check if command is valid
        if (!(diceList.isEmpty() && plotDice.isEmpty())){
            validCombo = true;
        }
        else {
            return;
        }
        if (diceList.size() + plotDice.size() > 6){
            overloaded = true;
            return;
        }
        generateResults(diceList, plotDice);
        
        HashMap<Integer, Integer> resultHash = generateStatisticsTable();
        statisticsMap = generateProbabilityHash(diceList, plotDice, resultHash);

        HashMap<Integer, Integer> doomHash = generateDoomChance();
        doomMap = generateProbabilityHash(diceList, plotDice, doomHash);
    }

    //Generate a HashMap with the roll as the keys and the percent as the values
    private HashMap<Integer,Double> generateProbabilityHash(ArrayList<Integer> diceList, ArrayList<Integer> plotDice, HashMap<Integer, Integer> resultHash) {
        int totalCombos = getTotalCombos(diceList, plotDice);
        HashMap<Integer, Double> probHash = new HashMap<>();
        for (Object o : resultHash.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            probHash.put((Integer) pair.getKey(), (Integer)pair.getValue() / (double)totalCombos * 100);
        }
        return probHash;
    }

    //Get the total number of combinations by finding the product of all of the number of faces in all of the dice
    private int getTotalCombos(ArrayList<Integer> diceList, ArrayList<Integer> plotDice) {
        int totalCombos = 1;
        for (int combo : diceList) {
            totalCombos *= combo;
        }

        /*Plot dice have a minimum value of the die size / 2
        //This means that you need to divide it by two and add one to get the number of combinations from it if the
        value is greater than 2 */
        for (int pdCombo : plotDice) {
            if (pdCombo > 2){
                totalCombos *= pdCombo / 2 + 1 ;
            }
        }
        return totalCombos;
    }

    public static void main(String[] args) {
        new StatisticsGenerator("d10 d12 d12");
    }

    private HashMap<Integer, Integer> generateDoomChance(){
        //Add information to a <Integer, Double> hashmap
        HashMap<Integer, Integer> doomHash = new HashMap<>();
        for (DiceResult result: resultList) {
            int key = result.getDoom();
            if (!doomHash.containsKey(key)){
                doomHash.put(key, 1);
            }
            else {
                doomHash.put(key, doomHash.get(key) + 1);
            }
        }
        return doomHash;
    }

    /**
     * Generate a HashMap with keys of a number being rolled with the values as the number of combinations that would
     * result in that roll
     */
    private HashMap<Integer, Integer> generateStatisticsTable() {
        //Add information to a <Integer, Double> hashmap
        HashMap<Integer, Integer> resultHash = new HashMap<>();
        for (DiceResult result: resultList) {
            int key = result.getResult();
            if (!resultHash.containsKey(key)){
                resultHash.put(key, 1);
            }
            else {
                resultHash.put(key, resultHash.get(key) + 1);
            }
        }
        return resultHash;
    }

    //Prep method for generateResults to copy the dice list to prevent it from being modified
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice){
        ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            generateResults(diceListCopy, plotDice, new DiceResult());
    }

    //Recursive method to generate an ArrayList of results
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice, DiceResult result){
        if (diceList.isEmpty()){
            generatePDResults(plotDice, result);
        }
        else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            int diceNum = diceListCopy.remove(0);
            for (int i = 1; i <= diceNum; i++){
                DiceResult resultCopy = result.copy();
                resultCopy.addDiceToResult(i);
                generateResults(diceListCopy, plotDice, resultCopy);
            }
        }
    }

    //Recursive method for handling plot dice
    private void generatePDResults(ArrayList<Integer> plotDice, DiceResult result){
        if (plotDice.isEmpty()){
            resultList.add(result);
        }
        else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(plotDice);
            int diceNum = diceListCopy.remove(0);
            for (int i = diceNum / 2; i <= diceNum; i++){
                DiceResult resultCopy = result.copy();
                resultCopy.addPlotDice(i);
                generatePDResults(diceListCopy, resultCopy);
            }
        }
    }

    //Generates a message that combines the probability of possible rolls and the probability of making a difficulty
    public EmbedBuilder generateStatistics(MessageAuthor author){
        if (overloaded){
            return new EmbedBuilder().setTitle("That's way too many dice for me to handle. Try using less dice.");
        }
        if (!validCombo){
            return new EmbedBuilder().setTitle("I can't find any dice in your command. Try again.");
        }
        String result = generateIndividualStatistics(statisticsMap);
        String difficulties = generateMeetingDifficulty();
        String doom = generateIndividualStatistics(doomMap);

        Random random = new Random();
        return new EmbedBuilder()
                .setTitle(TwoDee.getRollTitleMessage())
                .setAuthor(author)
                .setColor(new Color(random.nextFloat() , random.nextFloat(), random.nextFloat()))
                .addField("Chance to roll a", result, true)
                .addField("Chance to meet", difficulties, true)
                .addField("Chance to generate doom", doom, true);
    }

    //Loop through HashMap, check for rolls greater than difficulty, and sum their values to calculate the chance of
    //beating that difficulty. Rounds the probability to 4 decimal places.
    private String generateMeetingDifficulty() {
        StringBuilder result = new StringBuilder();
        DecimalFormat df = new DecimalFormat("0.##");
        String[] difficultyNames = {"Easy", "Average", "Hard", "Formidable", "Heroic", "Incredible", "Ridiculous", "Impossible"};
        int[] difficulty = {3, 7, 11, 15, 19, 23, 27, 31};
        double[] prob = new double[8];
        String[] probString = new String[8];
        for (int i = 0; i < difficulty.length; i++) {
            prob[i] = 0.0;
            for (Object o : statisticsMap.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                if (difficulty[i] <= (Integer) pair.getKey()){
                    prob[i] += (double) pair.getValue();
                }
            }
            probString[i] = df.format(prob[i]);
        }
        for (int i = 0; i < difficultyNames.length; i++){
            result.append(difficultyNames[i]).append(": ").append(probString[i]).append("%\n");
        }
        return result.toString();
    }

    //Generate the probability of each possible roll and rounds it to two decimal places.
    private String generateIndividualStatistics(HashMap map) {
        //Iterate through HashMap to generate message
        StringBuilder result = new StringBuilder();
        for (Object o : map.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            DecimalFormat df = new DecimalFormat("0.#####");
            String roundedChance = df.format(Double.valueOf(pair.getValue().toString()));
            result.append(pair.getKey()).append(": ").append(roundedChance).append("%\n");
        }
        return result.toString();
    }
}
