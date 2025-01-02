package summative;

import becker.robots.City;
import becker.robots.Direction;

import java.awt.*;

/**
 * Level 4 - Trained Warrior (Final Version)
 * This robot will participate in the Robot War and battle its opponents in the 20 by 12 battlefield
 * @author Maher Rahman 
 * @version Jan. 21, 2024
 */
public class RahmanFighterRobotV4 extends FighterRobot {
    // Attribute Variables
    private int hp;
    private RahmanOppData[] enhancedData = new RahmanOppData[BattleManagerTest11.NUM_PLAYERS];
    private boolean dataIsNull = true;
    // Constants
    final private int ATTACK = 3;
    final private int DEFENSE = 3;
    final private int NUM_MOVES = 4;
    final private int MAX_HEALTH = 100;
    final private int MAX_ATK_HP_DIFF = 15;
   

    /**
     * Constructor method; just use's the Super Class's one
     * @param c -- City to spawn in
     * @param a -- Avenue to spawn in
     * @param s -- Street to spawn in
     * @param d -- Direction to spawn in
     * @param id -- Robot's ID number
     * @param health -- Health to spawn the robot at
     */
    public RahmanFighterRobotV4(City c, int a, int s, Direction d, int id, int health){
        // the robot has attack 3, defence 3, numMoves 4
        super(c,a,s,d, id, 3, 3, 4);
        this.hp = health;

        // label the robot
        this.setLabel();
    }

    /**
     * This method labels the robot with its current HP and gives it a colour based on its HP value
     * Robot is red if alive and black if dead
     */
    public void setLabel(){
        // display the robot's HP
        this.setLabel("HP: " + this.hp);

        // if the robot is dead, turn it black
        if (this.hp <= 0) {
            this.setColor(Color.black);
        } else {
            this.setColor(Color.red);
        }
    }


    /**
     * Robot uses its AI to figure out what move to make
     * @param energy -- how much energy the Robot currently has
     * @param data -- an array of OppData objects/records, which contain Data about each Opponent
     * @return -- Record which contains the details of the requested move
     */
    public TurnRequest takeTurn(int energy, OppData[] data){
        // adjust the robot's version of OppData[] based off the information provided by Battle Manager
        if (dataIsNull) {
            // create one if necessary
            createEnhancedData(data);
            dataIsNull = false;
        } else {
            // otherwise, update with current information
            updateEnhancedData(data);
        }

        // update the robot's current health
        sortEnhancedDataID();
        this.hp = enhancedData[this.getID()].getHealth();
        // set the label again based off the current health
        this.setLabel();

        // get the average HP of all the opponent robots
        int avgHP = calculateOppAvgHP();

        // determine what move to make
        TurnRequest move;
        if (this.hp <= avgHP - MAX_ATK_HP_DIFF) {
            // if the robot's HP is largely lower than the average HP of the other robots, retreat
            move = retreat(energy);
        } else {
            // otherwise, attack
            move = attack(energy);
        }

        // sort the array by ID for the next turn
        sortEnhancedDataID();

        return move;
    }

    /**
     * Gets the robot from its current position to a specific position
     * @param a -- end Avenue
     * @param s -- end Street
     */
    public void goToLocation(int a, int s){
        // get the difference in the Streets
        int streetDiff = s - this.getStreet();
        // get the difference in the Avenue
        int avenueDiff = a - this.getAvenue();

        // get the total spots that must be travelled
        int totalSpots = Math.abs(streetDiff) + Math.abs(avenueDiff);

        // move in the direction of the end Street
        if (! (streetDiff == 0)) { // ensure that the Robot is not already at the End street
            if (streetDiff < 0){ // if the end Street was a smaller number, turn North
                this.toNorth(this.getDirection());
            } else { // otherwise (streetDiff > 0), turn South
                this.toSouth(this.getDirection());
            }
            // move to the end Street
            this.move(Math.abs(streetDiff));
        }

        // move in the direction of the end avenue
        if (! (avenueDiff == 0)) { // ensure that the Robot is not already at the End avenue
            if (avenueDiff < 0){ // if the end Street was a smaller number, turn West
                this.toWest(this.getDirection());
            } else { // otherwise (streetDiff > 0), turn East
                this.toEast(this.getDirection());
            }
            // move to the end Avenue
            this.move(Math.abs(avenueDiff));
        }
    }

    /**
     * Gets information from the most recent battle the Robot was involved in, stores this data to be used for
     * enhanced decision-making.
     * @param healthLost -- Robot's Health lost in the battle
     * @param oppID -- ID of the Opponent the robot was fighting
     * @param oppHealthLost -- Opponent's Health lost in the battle
     * @param numRoundsFought -- the number of rounds the two robots battled for
     */
    public void battleResult(int healthLost, int oppID, int oppHealthLost, int numRoundsFought){
        // update the robot's health
        this.hp -= healthLost;

        // set the label of the robot (may change if the robot died in battle)
        this.setLabel();


        // have the robot create a default array if it has not created one already
        if (dataIsNull) {
            createDefaultEnhancedData();
            dataIsNull = false;
        }

        // update the robot's version of OppData[] with the battle results
        if (oppID != -1) {
            sortEnhancedDataID();
            if (oppHealthLost > healthLost) {
                // if the opponent lost more health, the robot won
                enhancedData[oppID].addFightsWon();
            } else if (healthLost > oppHealthLost) {
                // if the robot lost more health, it lost
                enhancedData[oppID].addFightsLost();
            }
        }
    }

    // MY METHODS:

    /**
     * Creates a version of OppData[] which contains default values for all Opponents
     */
    private void createDefaultEnhancedData() {
        for (int i = 0; i < enhancedData.length; i ++) {
            // unique ID, default data
            int id = i;
            int avenue = -1;
            int street = -1;
            int health = 100;

            // create a RahmanOppData with the default data
            RahmanOppData defaultRec = new RahmanOppData(id, avenue, street, health);

            // put the default record in the enhancedData array
            enhancedData[i] = defaultRec;
        }
    }

    /**
     * Creates a version of OppData[] which contains the same data provided by the Battle Manager, but has other
     * useful attribute variables as well
     * @param data -- OppData[] provided by the Battle Manager
     */
    private void createEnhancedData(OppData[] data) {
        // the Battle Manager's OppData[] is initially sorted by ID
        for (int i = 0; i < enhancedData.length; i ++) {
            // get the original data
            int id = data[i].getID();
            int avenue = data[i].getAvenue();
            int street = data[i].getStreet();
            int health = data[i].getHealth();

            // create a RahmanOppData with the same data
            RahmanOppData copy = new RahmanOppData(id, avenue, street, health);

            // put the copy in the enhanced array
            enhancedData[i] = copy;
        }

        /*
        // display
        System.out.println("Creating");
        for (int i = 0; i < enhancedData.length; i ++) {
            System.out.println("ID: " + enhancedData[i].getID());
            System.out.println("A: " + enhancedData[i].getAvenue());
            System.out.println("S: " + enhancedData[i].getStreet());
            System.out.println("HP: " + enhancedData[i].getHealth());
        }
         */
    }

    /**
     * Updates the robot's version of OppData[] with the newest information from the Battle Manager
     * @param data -- OppData[] provided by the Battle Manager
     */
    private void updateEnhancedData(OppData[] data) {
        // Battle Manager's OppData[] is sorted by ID
        sortEnhancedDataID();

        // update the values
        for (int i = 0; i < enhancedData.length; i ++) {
            // get the updated data
            int id = data[i].getID();
            int avenue = data[i].getAvenue();
            int street = data[i].getStreet();
            int health = data[i].getHealth();

            // update the robot's version
            // ensure that the information is being assigned to the correct robot
            if (id == i) {
                enhancedData[i].setAvenue(avenue);
                enhancedData[i].setStreet(street);
                enhancedData[i].setHealth(health);
            }
        }

        // display
        /*
        System.out.println("Updating");
        for (int i = 0; i < enhancedData.length; i ++) {
            System.out.println("ID: " + enhancedData[i].getID());
            System.out.println("A: " + enhancedData[i].getAvenue());
            System.out.println("S: " + enhancedData[i].getStreet());
            System.out.println("HP: " + enhancedData[i].getHealth());
        }
         */

    }

    /**
     * Sorts the robot's version of OppData[] by ID. Uses an Insertion Sort.
     */
    private void sortEnhancedDataID() {
        // go through each ID starting from the second (the 1st will automatically be sorted)
        for (int i = 1; i < enhancedData.length; i ++) {
            int currID = enhancedData[i].getID();
            // compare the current ID to every ID before it
            int currIndex = i;
            int checkIndex = i - 1;
            while (currID < enhancedData[checkIndex].getID()) {
                // swap the records if the current ID is less than the ID before it
                swapRecords(enhancedData, currIndex, checkIndex);
                // update the index variables
                currIndex = checkIndex;
                if (checkIndex > 0) {
                    checkIndex -= 1;
                }
            }
        }
    }


    /**
     * This method has the robot attack, or try to attack, the best opponent
     * @param energy -- the robot's energy level
     * @return -- a TurnRequest for what the robot should do in order to attack the best opponent
     */
    public TurnRequest attack(int energy) {
        // required variables
        int endAvenue;
        int endStreet;
        int fightID = -1; // may be changed
        int numRounds = 0; // may be changed

        // 1: find the best opponent  and make that opponent's location the end position
        int bestOpp = findBestOpp(); // index of best opp in enhancedData[]
        // set end location to the position of the best opponent
        int oppAve = enhancedData[bestOpp].getAvenue();
        int oppStr = enhancedData[bestOpp].getStreet();

        // 2: move the robot towards the best opponent (valid position, enough moves, enough energy)
        // get the end position
        int[] endPos = moveTowards(energy, oppAve, oppStr);
        endAvenue = endPos[0];
        endStreet = endPos[1];

        // 3: if the robot can get to the best opponent, determine if it can attack it or not
        boolean canReach = false;
        if (endAvenue == oppAve && endStreet == oppStr) {
            canReach = true;
            boolean canAttack = checkCanAttack(energy, oppAve, oppStr);
            if (canAttack) {
                // if the robot is able to attack, initiate a fight  with the max amount of rounds
                fightID = enhancedData[bestOpp].getID();
                numRounds = this.getAttack();
            } else {
                // if the robot won't be able to attack, stay at the current position instead
                endAvenue = this.getAvenue();
                endStreet = this.getStreet();
            }
        }

        // 4: if the robot cannot reach the best opponent, maintain a certain distance from it
        // this aims to prevent the best opponent from attacking the robot if it gets too close
        if (! canReach) {
            // maintain a distance 1 less than the max moves, so the robot can reach the opponent next round
            int dist = this.getNumMoves() - 1;
            int[] adjustedPos = maintainDistance(oppAve, oppStr, endAvenue, endStreet, dist, false);
            endAvenue = adjustedPos[0];
            endStreet = adjustedPos[1];
        }

        // 5: check that the final move is valid
        boolean canMove = checkCanMove(energy, endAvenue, endStreet);
        // if the robot can't move there, keep it where it is and don't do anything
        if (! canMove) {
            endAvenue = this.getAvenue();
            endStreet = this.getStreet();
            fightID = -1;
            numRounds = 0;
        }

        return new TurnRequest(endAvenue, endStreet, fightID, numRounds);
    }

    /**
     * This method has the robot retreat from the closest opponent
     * @param energy -- the robot's energy level
     * @return -- a Turn Request for what the robot should do to retreat from the nearest opponent
     */
    private TurnRequest retreat(int energy) {
        // required variables
        int endAvenue = this.getAvenue(); // may be changed
        int endStreet = this.getStreet(); // may be changed
        int fightID = -1; // may be changed
        int numRounds = 0; // may be changed

        // 1: find the closest enemy robot
        int closestOpp = findClosestOpp(); // index of closest opp in enhancedData[]

        // 2: maintain a distance from the closest opponent
        int distance = calculateDist(enhancedData[closestOpp].getAvenue(), enhancedData[closestOpp].getStreet(), endAvenue, endStreet);
        if (distance < this.getNumMoves()) {
            // if the closest opponent is within the robot's movable range, maintain the max movable range
            int[] adjustedPos = maintainDistance(enhancedData[closestOpp].getAvenue(), enhancedData[closestOpp].getStreet(), this.getAvenue(), this.getStreet(), this.getNumMoves(), true);
            // adjust the end coordinates to maintain the distance
            endAvenue = adjustedPos[0];
            endStreet = adjustedPos[1];
        }

        // 3: move the robot towards the safe spot (valid position, enough moves, enough energy)
        // get the end position
        int[] endPos = moveTowards(energy, endAvenue, endStreet);
        endAvenue = endPos[0];
        endStreet = endPos[1];

        // 4: check that the final move is valid
        boolean canMove = checkCanMove(energy, endAvenue, endStreet);
        // if the robot can't move there, keep it where it is and don't do anything
        if (! canMove) {
            endAvenue = this.getAvenue();
            endStreet = this.getStreet();
            fightID = -1;
            numRounds = 0;
        }

        // 5: if the robot cannot move anywhere and the closest enemy is on top of it, retaliate by attacking back
        // only if the robot has more than 0 energy
        if (endAvenue == this.getAvenue() && endStreet == this.getStreet()) {
            if (endAvenue == enhancedData[closestOpp].getAvenue() && endStreet == enhancedData[closestOpp].getStreet() && energy > 0) {
                fightID = enhancedData[closestOpp].getID();
                numRounds = this.getAttack();
            }
        }

        return new TurnRequest(endAvenue, endStreet, fightID, numRounds);
    }

    /**
     * This method provides a location for the robot to go to which is a certain distance away from a specified location
     * @param enemyAve -- Avenue of the location to maintain distance from
     * @param enemyStr -- Street of the location to maintain distance from
     * @param destAve -- Avenue where the robot is currently heading
     * @param destStr -- Street where the robot is currently heading
     * @param distance -- Distance to maintain
     * @return -- location which is specified distance away from the location
     */
    private int[] maintainDistance(int enemyAve, int enemyStr, int destAve, int destStr, int distance, boolean retreating) {
        // get the distance from where the robot is currently headed to the bad spot
        int currDistance = calculateDist(enemyAve, enemyStr, destAve, destStr);
        // following variables will hold the final destination that maintains the distance
        int adjustedAve = destAve;
        int adjustedStr = destStr;
        // get the difference between the robot's current avenue and street with the opponent's avenue and street
        int aveDiff = Math.abs(enemyAve - this.getAvenue());
        int strDiff = Math.abs(enemyStr - this.getStreet());
        // dimensions of arena
        int arenaWidth = BattleManagerTest11.WIDTH;
        int arenaHeight = BattleManagerTest11.HEIGHT;

        // if the robot is too close to the bad spot, adjust the location to maintain the distance
        if (currDistance < distance) {
            int difference = distance - currDistance;

            // directions the robot can move in without going out of bounds
            boolean canMoveRight = (destAve + difference <= arenaWidth - 1);
            boolean canMoveLeft = (destAve - difference >= 0);
            boolean canMoveDown = (destStr + difference <= arenaHeight - 1);
            boolean canMoveUp = (destStr - difference >= 0);

            // the robot is on the bad spot, move in any valid direction
            if (currDistance == 0) {
                if (canMoveRight) {
                    // right
                    adjustedAve += difference;
                } else if (canMoveLeft) {
                    // left
                    adjustedAve -= difference;
                } else if (canMoveDown) {
                    // down
                    adjustedStr += difference;
                } else if (canMoveUp) {
                    // up
                    adjustedStr -= difference;
                }
            } else { // move the robot in a viable direction to maintain the distance
                // if retreating, robot can move in any direction without the final position being too far
                // otherwise, only adjust the bigger difference so that the final position is not too far
                boolean canAdjustAve = aveDiff > strDiff;
                boolean canAdjustStr = strDiff > aveDiff;
                // check where the robot currently is compared to the bad spot (enemy)
                boolean robotIsLeft = this.getAvenue() < enemyAve;
                boolean robotIsRight = this.getAvenue() > enemyAve;
                boolean robotIsAbove = this.getStreet() < enemyStr;
                boolean robotIsBelow = this.getStreet() > enemyStr;

                if (destAve < enemyAve) {
                    // if the current spot is left of the bad spot, move anywhere viable BUT right
                    if (canMoveLeft && (retreating || canAdjustAve && robotIsLeft)) {
                        // left
                        adjustedAve -= difference;
                    } else if (canMoveDown && (retreating || canAdjustStr && robotIsBelow)) {
                        // down
                        adjustedStr += difference;
                    } else if (canMoveUp && (retreating || canAdjustStr && robotIsAbove)) {
                        // up
                        adjustedStr -= difference;
                    }
                } else if (destAve > enemyAve) {
                    // if the current spot is right of the bad spot, move anywhere viable BUT left
                    if (canMoveRight && (retreating || canAdjustAve && robotIsRight)) {
                        // right
                        adjustedAve += difference;
                    } else if (canMoveDown && (retreating || canAdjustStr && robotIsBelow)) {
                        // down
                        adjustedStr += difference;
                    } else if (canMoveUp && (retreating || canAdjustStr && robotIsAbove)) {
                        // up
                        adjustedStr -= difference;
                    }
                } else if (destStr < enemyStr) {
                    // if the current spot is above the bad spot, move anywhere viable BUT down
                    if (canMoveRight && (retreating || canAdjustAve && robotIsRight)) {
                        // right
                        adjustedAve += difference;
                    } else if (canMoveLeft && (retreating|| canAdjustAve || robotIsLeft)) {
                        // left
                        adjustedAve -= difference;
                    } else if (canMoveUp && (retreating || canAdjustStr && robotIsAbove)) {
                        // up
                        adjustedStr -= difference;
                    }
                } else { // destStr > enemyStr
                    // if the current spot is below the bad spot, move anywhere viable BUT up
                    if (canMoveRight && (retreating || canAdjustAve && robotIsRight)) {
                        // right
                        adjustedAve += difference;
                    } else if (canMoveLeft && (retreating || canAdjustAve && robotIsLeft)) {
                        // left
                        adjustedAve -= difference;
                    } else if (canMoveDown && (retreating || canAdjustStr && robotIsBelow)) {
                        // down
                        adjustedStr += difference;
                    }
                }
            }
        }

        // return the adjusted position which maintains the distance
        int[] distPos = {adjustedAve, adjustedStr};
        return distPos;
    }

    /**
     * Checks if the robot will have enough energy left after reaching the opponent to start a battle
     * @param energy -- robot's energy level
     * @param oppAve -- Avenue of the opponent, used to determine energy necessary to get there
     * @param oppStr -- Street of the opponent, used to determine energy necessary to get there
     * @return -- whether or not the robot will be able to attack the opponent
     */
    private boolean checkCanAttack(int energy,  int oppAve, int oppStr) {
        // determine the energy required to reach the opponent
        int energyRequired = findEnergyRequired(oppAve, oppStr);

        // check if the robot will be able to fight the opponent
        boolean canFight = false;
        if (energy - energyRequired > 0) {
            // if the robot still has Energy left after reaching, it can fight
            canFight = true;
        }

        return canFight;
    }

    /**
     * Moves the robot towards a desired end position. Gets as close as possible.
     * @param energy -- the robot's current amount of energy
     * @param desiredAve -- the Avenue the robot wants to get to
     * @param desiredStr -- the Street the robot wants to get to
     * @return -- coordinates (Avenue, Street) of a position close or equal to the desired end position
     */
    private int[] moveTowards(int energy, int desiredAve, int desiredStr) {
        // return values
        int endAve = desiredAve;
        int endStr = desiredStr;

        // INFORMATION:
        int arenaWidth = BattleManagerTest11.WIDTH;
        int arenaHeight = BattleManagerTest11.HEIGHT;
        int totalSpots = calculateDist(desiredAve, desiredStr);
        int energyRequired = totalSpots * BattleManagerTest11.MOVES_ENERGY_COST;

        while (this.getNumMoves() < totalSpots || energy < energyRequired) {
            if (endAve != this.getAvenue()) {
                // reduce the number of Avenues to travel
                if (endAve < this.getAvenue()) { // endAve is left of currAve
                    endAve += 1;
                } else { // endAve is right of currAve
                    endAve -= 1;
                }
            } else if (endStr != this.getStreet()) {
                // reduce the number of Streets to travel
                if (endStr < this.getStreet()) { // endStr is above currStr
                    endStr += 1;
                } else { // endStr is below currStr
                    endStr -= 1;
                }
            }

            // recalculate information based off the new end position
            totalSpots = calculateDist(endAve, endStr);
            energyRequired = totalSpots * BattleManagerTest11.MOVES_ENERGY_COST;
        }

        // ensure that the end position is within the arena
        if (endAve > arenaWidth - 1 || endStr > arenaHeight - 1 || endAve < 0 || endStr < 0) {
            // if it is not, then the robot stays where it is
            endAve = this.getAvenue();
            endStr = this.getStreet();
        }

        // return the end position
        int[] endPos = {endAve, endStr};

        return endPos;
    }

    /**
     * Finds the amount of Energy needed to get from the current location to another location
     * @param endAve -- Avenue of desire location
     * @param endStr -- Street of desired location
     * @return
     */
    private int findEnergyRequired(int endAve, int endStr) {
        int totalSpots = calculateDist(endAve, endStr);
        int energyRequired = totalSpots * BattleManagerTest11.MOVES_ENERGY_COST;

        return energyRequired;
    }

    /**
     * Helper method to determine if the Robot can move to a certain spot
     * @param energy -- Robot's energy level
     * @param endAvenue -- requested End Street
     * @param endStreet -- requested End Avenue
     * @return -- whether or not the robot can move to the specified spot
     */
    private boolean checkCanMove(int energy, int endAvenue, int endStreet) {
        // INFORMATION:
        // arena dimensions
        int arenaWidth = BattleManagerTest11.WIDTH;
        int arenaHeight = BattleManagerTest11.HEIGHT;

        // get the total spots that must be travelled
        int totalSpots = calculateDist(endAvenue, endStreet);

        // CHECKS:
        boolean canMove = true;

        // ensure that the end location is valid
        if (endAvenue > arenaWidth - 1 || endStreet > arenaHeight - 1 || endAvenue < 0 || endStreet < 0) {
            canMove = false;
        }

        // cannot move if the amount is not permitted, or there is insufficient energy
        if (totalSpots > this.getNumMoves() || energy < totalSpots * BattleManagerTest11.MOVES_ENERGY_COST) {
            canMove = false;
        }

        return canMove;
    }

    /**
     * Finds the closest enemy to the robot. Uses a selection sort.
     * @return -- index of the Closest Opponent that is not the robot itself and is still alive
     */
    private int findClosestOpp() {
        // must sort at most once for each record in the list
        for (int i = 0; i < enhancedData.length; i ++) {
            int smallestDist = -1;
            int index = -1;

            // find the smallest number after the index of the last number put in
            for (int j = i; j < enhancedData.length; j ++) {
                int currDist = calculateDist(enhancedData[j].getAvenue(), enhancedData[j].getStreet());
                if (smallestDist == -1) {
                    smallestDist = calculateDist(enhancedData[i].getAvenue(), enhancedData[i].getStreet());
                    index = i;
                } else if (currDist < smallestDist) {
                    smallestDist = currDist;
                    index = j;
                }
            }
            // swap the smallest number with the value at the current index
            swapRecords(enhancedData, i, index);
        }

        // display
        /*
        System.out.println("Sorted Distance");
        for (int i = 0; i < enhancedData.length; i ++) {
            System.out.println(enhancedData[i].getID());
        }
         */

        // find the closest opponent (check that they are not the robot itself, and that they are alive)
        int closeIndex = 0;
        while(enhancedData[closeIndex].getID() == this.getID() || enhancedData[closeIndex].getHealth() <= 0) {
            closeIndex += 1;
        }

        return closeIndex;
    }


    /**
     * This method finds the best opponent for the robot to fight. It uses an Insertion Sort to find the best one.
     * @return -- index of the best opponent that is not the robot itself and is still alive
     */

    private int findBestOpp() {
        // go through each Opponent starting from the second (the 1st will automatically be sorted)
        for (int i = 1; i < enhancedData.length; i ++) {
            int currFV = calculateFightValue(i);
            // compare the current opponent's Fight Value to the Fight Value of every opponent before it
            int currIndex = i;
            int checkIndex = i - 1;
            while (currFV < calculateFightValue(checkIndex)) {
                // swap the robots if the current Fight Value is less than the Fight Value before it
                swapRecords(enhancedData, currIndex, checkIndex);
                // update the index variables
                currIndex = checkIndex;
                if (checkIndex > 0) {
                    checkIndex -= 1;
                }
            }
        }

        /*
        // display the IDs of the sorted array
        System.out.println("Best Opps");
        for (int i = 0; i < enhancedData.length; i++) {
            System.out.println("ID: " + enhancedData[i].getID() + " FV: " + calculateFightValue(i));
        }
         */

        // find the closest opponent (check that they are not the robot itself, and that they are alive)
        int closeIndex = 0;
        while(enhancedData[closeIndex].getID() == this.getID() || enhancedData[closeIndex].getHealth() <= 0) {
            closeIndex += 1;
        }

        return closeIndex;
    }


    /**
     * This method finds the distance from the robot's current position to another point
     * @param endAvenue -- avenue of point(x-coordinate)
     * @param endStreet -- street of point (y-coordinate)
     * @return -- the total number of spots between the robot's current position and the point
     */
    private int calculateDist(int endAvenue, int endStreet) {
        // if an Avenue or Street value is -1,that means the robot is dead - it's Distance will then automatically be greater
        int totalSpots;
        if (endAvenue == -1 || endStreet == -1) {
            totalSpots = 100;
        } else {
            // get the difference in the Avenue
            int avenueDiff = endAvenue - this.getAvenue();
            // get the difference in the Streets
            int streetDiff = endStreet - this.getStreet();

            // get the total spots that must be travelled
            totalSpots = Math.abs(streetDiff) + Math.abs(avenueDiff);
        }

        return totalSpots;
    }

    /**
     * This method finds the distance from one point to another
     * @param endAvenue -- avenue to get to (x-coordinate)
     * @param endStreet -- street to get to (y-coordinate)
     * @param startAvenue -- avenue the robot is starting from
     * @param startStreet -- street the robot is starting from
     * @return -- the total number of spots the robot needs to move
     */
    private int calculateDist(int endAvenue, int endStreet, int startAvenue, int startStreet) {
        // if an Avenue or Street value is -1,that means the robot is dead - it's Distance will then automatically be greater
        int totalSpots;
        if (endAvenue == -1 || endStreet == -1) {
            totalSpots = 100;
        } else {
            // get the difference in the Avenue
            int avenueDiff = endAvenue - startAvenue;
            // get the difference in the Streets
            int streetDiff = endStreet - startStreet;

            // get the total spots that must be travelled
            totalSpots = Math.abs(streetDiff) + Math.abs(avenueDiff);
        }

        return totalSpots;
    }

    /**
     * Determines a value which represents if it would be good to fight an opponent or not based off their
     * distance, health, the robot's loss rate against the opponent, and the difference between the robot's
     * wins and losses against that opponent. A smaller number is a better Fight Value.
     * @param index -- the index in OppData[] of the robot to calculate the FightValue for
     * @return -- the FightValue for a certain opponent
     */
    private int calculateFightValue(int index) {
        int fightValue;
        // values used to calculate fightValue
        final int STRONGER_BONUS = 5; // bonus if stronger, demerit if weaker
        final double DISTANCE_WEIGHT = 0.3;
        final double HEALTH_WEIGHT = 0.1;
        final double LOSS_RATE_WEIGHT = 0.6;

        // get necessary information
        int distance = calculateDist(enhancedData[index].getAvenue(), enhancedData[index].getStreet());
        int health = enhancedData[index].getHealth();
        float lossRate = calculateLossRate(index);
        // how many more rounds the robot has won vs. lost against the opponent
        int amntStronger = enhancedData[index].getFightsWon() - enhancedData[index].getFightsLost();

        // make distance on the same scale as health and lossRate
        // health and lossRate are out of 100, distance is usually from 0 to 20, 100 / 20 = 5
        distance *= 5;

        // calculate fight value
        // if amntStronger > 0 fightValue decreases, if amntStronger < 0 fightValue increases
        fightValue = (int) (DISTANCE_WEIGHT * distance +  HEALTH_WEIGHT * health + LOSS_RATE_WEIGHT * lossRate);

        // apply the bonus: if amntStronger > 0 fightValue decreases, if amntStronger < 0 fightValue increases
        fightValue -= (amntStronger * STRONGER_BONUS);


        // do not want the robot to fight itself, give itself a very large fightValue
        if (enhancedData[index].getID() == this.getID()) {
            fightValue = 1000000;
        }

        return fightValue;
    }

    /**
     * Calculates the robot's loss rate against a specific opponent
     * @param index -- index of the opponent in the robot's information array
     * @return -- loss rate against that opponent
     */
    private float calculateLossRate(int index) {
        // get necessary information
        int numWins = enhancedData[index].getFightsWon();
        int numLoss = enhancedData[index].getFightsLost();
        int totalFights = numWins + numLoss;

        // calculate loss rate against the opponent
        float lossRate = 0;
        // can't have a loss rate unless at least 1 fight has occured
        if (totalFights > 0) {
            lossRate = ((float) numLoss / totalFights) * 100;
        }
        // reduce the impact of the first fight
        if (totalFights == 1) {
            lossRate /= 2;
        }

        return lossRate;
    }

    /**
     * This method finds the average HP of all the living opponents
     * @return -- average HP of all living robots
     */
    private int calculateOppAvgHP() {
        // check every robot's HP
        int numAdded = 0;
        int totalHP = 0;
        for (int i = 0; i < enhancedData.length; i ++) {
            // only getting the avg HP of the opponent robots
            if (enhancedData[i].getID() != this.getID()) {
                int hp = enhancedData[i].getHealth();
                // only consider the robot's HP if it is not dead
                if (hp > 0) {
                    totalHP += hp;
                    numAdded += 1;
                }
            }
        }

        // find and return the average
        int avg;
        // check that there was at least one living opponent
        if (numAdded > 0) {
            avg = totalHP / numAdded;
        } else {
            // if there were no living opponents, the avg would be 0 (totalHP = 0)
            avg = totalHP;
        }
        return avg;
    }

    /**
     * This method swaps the positions of 2 Records in OppData[], or any of its child classes' arrays
     * @param data -- OppData[] array which contains records
     * @param ind1 -- index of the first record
     * @param ind2 -- index of the second record
     */
    private void swapRecords(OppData[] data, int ind1, int ind2) {
        // swap the two records by saving a copy of the first one
        OppData orig1 = data[ind1];
        data[ind1] = data[ind2];
        data[ind2] = orig1;
    }

    /**
     * This method turns the robot to the North direction based off
     * the direction it is currently facing.
     * @param direction - direction robot is currently facing
     */
    public void toNorth(Direction direction) {
        // turn left until facing North
        while (this.getDirection() != Direction.NORTH) {
            this.turnLeft();
        }
    }

    /**
     * This method turns the robot to the East direction based off
     * the direction it is currently facing.
     * @param direction - direction robot is currently facing
     */
    public void toEast(Direction direction) {
        // turn left until facing East
        while (this.getDirection() != Direction.EAST) {
            this.turnLeft();
        }
    }

    /**
     * This method turns the robot to the South direction based off
     * the direction it is currently facing.
     * @param direction - direction robot is currently facing
     */
    public void toSouth(Direction direction) {
        // turn left until facing South
        while (this.getDirection() != Direction.SOUTH) {
            this.turnLeft();
        }
    }

    /**
     * This method turns the robot to the West direction based off
     * the direction it is currently facing.
     * @param direction - direction robot is currently facing
     */
    public void toWest(Direction direction) {
        // turn left until facing West
        while (this.getDirection() != Direction.WEST) {
            this.turnLeft();
        }
    }
}
