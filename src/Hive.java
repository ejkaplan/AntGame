import java.util.List;

public abstract class Hive {

	public final static int sightDistance = 5;
	private Game game;
	private String name;

	public Hive(Game game, String name) {
		this.game = game;
		this.name = name;
		game.addPlayer(this);
	}

	/**
	 * 
	 * @return A list of arrays of integers indicating the locations of all
	 *         friendly ants, in (row, column) format.
	 */
	public final List<int[]> getAntLocations() {
		return game.getAnts(this);
	}

	/**
	 * 
	 * @return A list of arrays of integers indicating the locations of all
	 *         visible enemies, in (row, column) format.
	 */
	public final List<int[]> getEnemyLocations() {
		return game.getEnemiesVisible(this);
	}

	/**
	 * 
	 * @return A list of arrays of integers indicating the locations of all
	 *         visible food, in (row, column) format.
	 */
	public final List<int[]> getFoodLocations() {
		return game.getFoodVisible(this);
	}

	/**
	 * 
	 * @return A list of arrays of integers indicating the locations of all
	 *         surviving friendly ant hills, in (row, column) format.
	 */
	public final List<int[]> getMyHillLocations() {
		return game.getHills(this, true);
	}

	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return A list of arrays of integers indicating the locations of all
	 *         surviving enemy ant hills, in (row, column) format.
	 */
	public final List<int[]> getOppHillLocations() {
		return game.getHills(this, false);
	}

	/**
	 * 
	 * @return The amount of food remaining in your reserve. This is the number
	 *         of ants you would be able to spawn if you stopped collecting
	 *         food.
	 */
	public final int getReserveFood() {
		return game.getReserveFood(this);
	}

	/**
	 * 
	 * @return The number of milliseconds before you auto-forfeit.
	 */
	public final int getTimeRemaining() {
		return game.getTime(this);
	}

	/**
	 * 
	 * @return A 2D array of booleans, where each sub-array represents a row of
	 *         the maze. True represents the presence of a wall, false
	 *         represents navigable space.
	 */
	public final boolean[][] getWalls() {
		return game.getMapCopy();
	}

	/**
	 * Gives orders for how all of your ants are supposed to move. You should
	 * return a String of the format "row col dir\nrow col dir\nrow col..." Each
	 * ant should have its own line of the orders. You specify the ant to move
	 * by specifying its position, and use one of the letters "n", "s", "e", or
	 * "w" to indicate direction. If you don't want an ant to move, just do not
	 * reference that ant in the orders.
	 * 
	 * @return A String representing the orders for all of your ants.
	 */
	public abstract String orders();

}
