package fBot;
import battlecode.common.*;
import java.lang.Math;
import java.util.ArrayList;

public strictfp class RobotPlayer {
	static RobotController rc;

	static Direction[] directions = {
			Direction.NORTH,
			Direction.NORTHEAST,
			Direction.EAST,
			Direction.SOUTHEAST,
			Direction.SOUTH,
			Direction.SOUTHWEST,
			Direction.WEST,
			Direction.NORTHWEST
	};
	static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
			RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

	static int teamCode = 1;

	static int turnCount;

	static MapLocation hqLoc;

	static MapLocation hqLocEnemy;


	/*
	 * List of information know by the HQ
	 */
	//soup location keep track by HQ
	static boolean initiation = false;
	static ArrayList<MapLocation> soupLocList;
	//list of Commands; 1 is used to broadcast HQ location
	static int sendMinerToMine = 2;
	static int sendMinerToEnemy = 3;


	/*
	 * List of information know by the miners
	 */
	static MapLocation soupLoc;
	static MapLocation refineryLoc;
	static int wantSoup  = 20;
	static int foundSoup = 21;
	static int removeSoup = 22;
	static int builtRefinery = 23;
	static int builtSchool = 24;
	static int builtFullFillment = 25;
	static int builtGun = 26;


	static int numMiners = 0;

	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {

		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;

		turnCount = 0;

		System.out.println("I'm a " + rc.getType() + " and I just got created!");
		while (true) {
			turnCount += 1;
			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				// Here, we've separated the controls into a different method for each RobotType.
				// You can add the missing ones or rewrite this into your own control structure.
				System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());

				switch (rc.getType()) {
				case HQ:                 runHQ();                break;
				case MINER:              runMiner();             break;
				case REFINERY:           runRefinery();          break;
				case VAPORATOR:          runVaporator();         break;
				case DESIGN_SCHOOL:      runDesignSchool();      break;
				case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
				case LANDSCAPER:         runLandscaper();        break;
				case DELIVERY_DRONE:     runDeliveryDrone();     break;
				case NET_GUN:            runNetGun();            break;
				}

				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
		}
	}

	static void findHQ() throws GameActionException {

		if (hqLoc == null) {
			// search surroundings for HQ
			RobotInfo[] robots = rc.senseNearbyRobots();
			for (RobotInfo robot : robots) {
				if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
					hqLoc = robot.location;
				}
			}
			if(hqLoc == null) {
				// if still null, search the blockchain
				for (int i = 1; i < rc.getRoundNum(); i++){
					for(Transaction tx : rc.getBlock(i)) {
						int[] mess = tx.getMessage();
						if(mess[2] == teamCode){
							System.out.println("found the HQ!");
							hqLoc = new MapLocation(mess[0], mess[1]);
						}
					}
				}
			}
		}

	}

	static void runHQ() throws GameActionException {
		if (rc.getRoundNum() == 1)
			tryBuild(RobotType.MINER, Direction.NORTHEAST);
	}

	//find soup location
	static boolean askSoupFromHQ() throws GameActionException {

		if (rc.getCooldownTurns() < 3) {
			broadcastMessage(1, 0, 0, teamCode, wantSoup, 0, 0, 0);
		}

		while(soupLoc == null && !rc.isReady()) {
			for(Transaction i : rc.getBlock(rc.getRoundNum()-1)) {
				int[] message_soup = i.getMessage();
				if(message_soup[2]== teamCode && message_soup[3]==sendMinerToMine) {
					soupLoc = new MapLocation(message_soup[0],message_soup[1]);

					return true;
				}
			}

			Clock.yield();
		}

		return false;

	}


	//Miner code
	static void runMiner() throws GameActionException {
		MapLocation enemyHQ = new MapLocation(37, 37);
		if (!rc.getLocation().isAdjacentTo(enemyHQ))
			goTo(enemyHQ);
		else if (!tryBuild(RobotType.FULFILLMENT_CENTER, Direction.NORTHEAST))
			if (!tryBuild(RobotType.FULFILLMENT_CENTER, Direction.NORTH))
				if (!tryBuild(RobotType.FULFILLMENT_CENTER, Direction.EAST))
					System.out.println("oof");
	}

	static void runRefinery() throws GameActionException {
		// System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
	}

	static void runVaporator() throws GameActionException {

	}

	static void runDesignSchool() throws GameActionException {
		for (Direction dir : directions)
			if(tryBuild(RobotType.LANDSCAPER, dir))
				System.out.println("made a landscaper");
	}

	static void runFulfillmentCenter() throws GameActionException {
		for (Direction dir : directions)
			tryBuild(RobotType.DELIVERY_DRONE, dir);
	}

	static void runLandscaper() throws GameActionException {
		
		for(RobotInfo robot: rc.senseNearbyRobots()) {
			if(!robot.team.equals(rc.getTeam())&&robot.getType().equals(RobotType.HQ)) {
				hqLocEnemy = robot.getLocation();
			}
				
		}
		
		if(rc.getLocation().isAdjacentTo(hqLocEnemy)) {
			
			if(rc.getDirtCarrying() == 0){
				if(rc.canDigDirt(rc.getLocation().directionTo(hqLocEnemy).opposite())) {
					rc.digDirt(rc.getLocation().directionTo(hqLocEnemy).opposite());
				}
			}
			
			if(rc.canDepositDirt(rc.getLocation().directionTo(hqLocEnemy))){
				rc.depositDirt(rc.getLocation().directionTo(hqLocEnemy));
			}
			
		}else {
			goTo(rc.getLocation().directionTo(hqLocEnemy));
		}
//		if(hqLoc == null) {
//			findHQ();
//		}
//
//		if(rc.getDirtCarrying() == 0){
//			tryDig();
//		}
//
//		MapLocation bestPlaceToBuildWall = null;
//		// find best place to build
//		if(hqLoc != null) {
//			int lowestElevation = 9999999;
//			for (Direction dir : directions) {
//				MapLocation tileToCheck = hqLoc.add(dir);
//				if(rc.getLocation().distanceSquaredTo(tileToCheck) < 4
//						&& rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
//					if (rc.senseElevation(tileToCheck) < lowestElevation) {
//						lowestElevation = rc.senseElevation(tileToCheck);
//						bestPlaceToBuildWall = tileToCheck;
//					}
//				}
//			}
//		}
//
//		if (Math.random() < 0.4){
//			// build the wall
//			if (bestPlaceToBuildWall != null) {
//				rc.depositDirt(rc.getLocation().directionTo(bestPlaceToBuildWall));
//				System.out.println("building a wall");
//			}
//		}
//
//		// otherwise try to get to the hq
//		if(hqLoc != null){
//			goTo(hqLoc);
//		} else {
//			tryMove(randomDirection());
//		}
	}

	static void runDeliveryDrone() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		if (!rc.isCurrentlyHoldingUnit()) {
			// See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
			RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

			if (robots.length > 0) {
				// Pick up a first robot within range
				rc.pickUpUnit(robots[0].getID());
				System.out.println("I picked up " + robots[0].getID() + "!");
			}
		} else {
			// No close robots, so search for robots within sight radius
			tryMove(randomDirection());
		}
	}

	static void runNetGun() throws GameActionException {

	}

	/**
	 * Returns a random Direction.
	 *
	 * @return a random Direction
	 */
	static Direction randomDirection() {
		return directions[(int) (Math.random() * directions.length)];
	}

	/**
	 * Returns a random RobotType spawned by miners.
	 *
	 * @return a random RobotType
	 */
	static RobotType randomSpawnedByMiner() {
		return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
	}

	static boolean nearbyRobot(RobotType target) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots();
		for(RobotInfo r : robots) {
			if(r.getType() == target) {
				return true;
			}
		}
		return false;
	}

	static boolean tryDig() throws GameActionException {
		Direction dir = randomDirection();
		if(rc.canDigDirt(dir)){
			rc.digDirt(dir);
			return true;
		}
		return false;
	}

	static boolean broadcastMessage(int soupCost, int m0, int m1, int m2, int m3, int m4, int m5, int m6) throws GameActionException {
		int[] message = new int[7];
		message[0] = m0; // xLoc
		message[1] = m1; // yLoc
		message[2] = m2; // teamSecret
		message[3] = m3; // messageType
		message[4] = m4; // val1
		message[5] = m5; // val2
		message[6] = m6; // val3
		if (rc.canSubmitTransaction(message, soupCost)) {
			rc.submitTransaction(message, soupCost);
			return true;
		}
		return false;
	}

	static boolean tryMove() throws GameActionException {
		for (Direction dir : directions)
			if (tryMove(dir))
				return true;
		return false;
		// MapLocation loc = rc.getLocation();
		// if (loc.x < 10 && loc.x < loc.y)
		//     return tryMove(Direction.EAST);
		// else if (loc.x < 10)
		//     return tryMove(Direction.SOUTH);
		// else if (loc.x > loc.y)
		//     return tryMove(Direction.WEST);
		// else
		//     return tryMove(Direction.NORTH);
	}

	// tries to move in the general direction of dir
	static boolean goTo(Direction dir) throws GameActionException {
		Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(),dir.rotateRight(), dir.rotateRight().rotateRight()};
		for (Direction d : toTry){
			if(tryMove(d))
				return true;
		}
		return false;
	}

	// navigate towards a particular location
	static boolean goTo(MapLocation destination) throws GameActionException {
		return goTo(rc.getLocation().directionTo(destination));
	}

	/**
	 * Attempts to move in a given direction.
	 *
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMove(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
			rc.move(dir);
			return true;
		} else return false;
	}

	/**
	 * Attempts to build a given robot in a given direction.
	 *
	 * @param type The type of the robot to build
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canBuildRobot(type, dir)) {
			rc.buildRobot(type, dir);
			return true;
		} else return false;
	}

	/**
	 * Attempts to mine soup in a given direction.
	 *
	 * @param dir The intended direction of mining
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMine(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canMineSoup(dir)) {
			rc.mineSoup(dir);
			return true;
		} else return false;
	}

	/**
	 * Attempts to refine soup in a given direction.
	 *
	 * @param dir The intended direction of refining
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryRefine(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canDepositSoup(dir)) {
			rc.depositSoup(dir, rc.getSoupCarrying());
			return true;
		} else return false;
	}

	static void checkIfSoupGone() throws GameActionException {
		if(soupLoc != null) {
			if(rc.canSenseLocation(soupLoc)&&rc.senseSoup(soupLoc) ==0) {
				soupLoc = null;
				broadcastMessage(1, soupLoc.x, soupLoc.y , teamCode, removeSoup, 1, 1, 1);
			}
		}
	}

	static boolean floodFront(Direction dir) throws GameActionException{
		Direction[] front = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};

		for(Direction dire: front) {
			if(rc.senseFlooding(rc.getLocation().add(dire))) {
				return true;
			}
		}

		return false;
	}

	static boolean searchSurrondingForSoup(int rad) throws GameActionException{
		rad = (int) Math.sqrt(rad);
		for (int i=-rad; i<=rad; i++) {
			for (int j=-rad; j<=rad; j++) {
				MapLocation loc = rc.getLocation().translate(i,j);

				if (rc.canSenseLocation(loc) && rc.senseSoup(loc) != 0) {

					if(rc.getType() == RobotType.MINER) {
						soupLoc = loc;
						broadcastMessage(1, loc.x, loc.y, teamCode, foundSoup, 0, 0, 0);
						return true;
					}

					if(rc.getType() == RobotType.HQ) {
						soupLocList.add(loc);
						return true;
					}
				}
			}
		}
		return false;
	}

	static void spawnMiningLocation() throws GameActionException{
		for(Transaction transaction: rc.getBlock(rc.getRoundNum()-1)) {
			int[] message =  transaction.getMessage();
			if(message[2]==teamCode && message[3]==sendMinerToMine) {
				soupLoc = new MapLocation(message[0],message[1]);
			}

		}
	}

	static void tryBlockchain() throws GameActionException {

	}

	static boolean zigzag = false;
	/**
	 * Zig-zag motion
	 * @return
	 * @throws GameActionException
	 */
	static void exploreMove(MapLocation loc) throws GameActionException{
		if(rc.senseFlooding(rc.adjacentLocation(rc.getLocation().directionTo(loc)))) {
			goTo(rc.getLocation().directionTo(loc));
		}else if(!zigzag) {
			goTo(rc.getLocation().directionTo(loc).rotateLeft());
			zigzag = !zigzag;
		}else if(zigzag) {
			goTo(rc.getLocation().directionTo(loc).rotateRight());
			zigzag = !zigzag;
		}
	}
}