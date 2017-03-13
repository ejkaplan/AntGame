import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import processing.core.PApplet;
import processing.core.PConstants;

public class Game {

	// true = wall, false = corridor

	private int frameCount;

	private PApplet parent;
	private boolean[][] maze;
	private boolean[][] food;
	private int[] foodReserves;
	private int[][] hills;
	private int[][] ants; // 1,2 = player ants, 3 = dead ant pile, 0 = empty
	private Random rand;
	private ArrayList<Integer> foodPriority;
	private ArrayList<Hive> players;
	private int foodStep = 0;
	private int[][] spawnCount;
	private Hive winner;
	private long turnClock;

	private final float pStartAlive = 0.465f;
	private final int birthLimit = 4;
	private final int deathLimit = 3;
	private final int steps = 9;
	private final int hillsPerPlayer = 3;
	private ArrayList<ArrayList<int[]>> hillLocs;
	private final float minOpen = 0.45f;
	private final float maxOpen = 0.7f;

	private int[] timer;
	private final int startTime = 200;
	private final int maxTime = 500;
	private final int incrTime = 50;

	public Game(int rows, int cols) {
		this(null, rows, cols);
	}

	public Game(PApplet parent, int rows, int cols) {
		this(parent, rows, cols, new Random().nextInt());
	}

	public Game(PApplet parent, int rows, int cols, int seed) {
		frameCount = 0;
		this.parent = parent;
		winner = null;
		players = new ArrayList<Hive>();
		rand = new Random(seed);
		foodReserves = new int[] { hillsPerPlayer * 3, hillsPerPlayer * 3 };
		maze = new boolean[rows][cols];
		hills = new int[rows][cols];
		food = new boolean[rows][cols];
		ants = new int[rows][cols];
		foodPriority = new ArrayList<Integer>();
		spawnCount = new int[2][hillsPerPlayer];
		for (int i = 0; i < 9; i++) {
			foodPriority.add(i);
		}
		timer = new int[2];
		for (int i = 0; i < timer.length; i++)
			timer[i] = startTime;
		genMaze();
	}

	public boolean addPlayer(Hive player) {
		if (players.size() < 2 && !players.contains(player)) {
			players.add(player);
			return true;
		}
		return false;
	}

	private void display() {
		parent.ellipseMode(PConstants.CORNER);
		float cellWidth = 1f * parent.width / maze[0].length;
		float cellHeight = 1f * parent.height / maze.length;
		for (int r = 0; r < maze.length; r++) {
			for (int c = 0; c < maze[0].length; c++) {
				float x = c * cellWidth;
				float y = r * cellHeight;
				int col;
				parent.noStroke();
				if (maze[r][c]) {
					col = parent.color(0, 0, 0);
					parent.stroke(col);
				} else
					col = parent.color(255, 255, 255);
				parent.fill(col);
				parent.rect(x, y, cellWidth, cellHeight);
				if (food[r][c]) {
					parent.fill(parent.color(150, 230, 110));
					parent.noStroke();
					parent.rect(x, y, cellWidth, cellHeight);
				}
				if (ants[r][c] == 1) {
					parent.fill(parent.color(255, 0, 0));
					parent.noStroke();
					parent.ellipse(x, y, cellWidth, cellHeight);
				}
				if (ants[r][c] == 2) {
					parent.fill(parent.color(0, 0, 255));
					parent.noStroke();
					parent.ellipse(x, y, cellWidth, cellHeight);
				}
			}
		}
		parent.strokeWeight(1.5f);
		parent.ellipseMode(PConstants.CENTER);
		for (int i = 0; i < hillLocs.size(); i++) {
			for (int[] loc : hillLocs.get(i)) {
				float x = (loc[1] + 0.5f) * cellWidth;
				float y = (loc[0] + 0.5f) * cellHeight;
				if (hills[loc[0]][loc[1]] == 1) {
					parent.stroke(255, 0, 0);
					parent.noFill();
					parent.ellipse(x, y, 1.5f * cellWidth, 1.5f * cellHeight);
				} else if (hills[loc[0]][loc[1]] == 2) {
					parent.stroke(0, 0, 255);
					parent.noFill();
					parent.ellipse(x, y, 1.5f * cellWidth, 1.5f * cellHeight);
				}
			}
		}
		parent.strokeWeight(1);
		if (winner != null) {
			parent.textAlign(PConstants.CENTER, PConstants.CENTER);
			parent.textSize(30);
			if (players.indexOf(winner) == 0)
				parent.fill(255, 0, 0);
			else
				parent.fill(0, 0, 255);
			parent.text(winner.getName() + " wins!", parent.width / 2, parent.height / 2);
		}
	}

	public List<int[]> getAnts(Hive hive) {
		if (!players.contains(hive))
			return null;
		ArrayList<int[]> visAnts = new ArrayList<int[]>();
		int playerNum = players.indexOf(hive) + 1;
		for (int r = 0; r < ants.length; r++) {
			for (int c = 0; c < ants[0].length; c++) {
				if (ants[r][c] == playerNum) {
					visAnts.add(new int[] { r, c });
				}
			}
		}
		return visAnts;
	}

	public List<int[]> getEnemiesVisible(Hive hive) {
		if (!players.contains(hive))
			return null;
		ArrayList<int[]> visEnemies = new ArrayList<int[]>();
		int playerNum = players.indexOf(hive) + 1;
		for (int r = 0; r < ants.length; r++) {
			for (int c = 0; c < ants[0].length; c++) {
				if (ants[r][c] != playerNum) { // Enemy at (r,c)
					sightLoop: for (int ar = r - Hive.sightDistance; ar <= r + Hive.sightDistance; ar++) {
						if (ar < 0 || ar >= ants.length)
							continue;
						for (int ac = c - Hive.sightDistance; ac <= c + Hive.sightDistance; ac++) {
							if (ac < 0 || ac >= ants[0].length)
								continue;
							if (ants[ar][ac] == playerNum && AntGameRunner.dist(r, c, ar, ac) <= Hive.sightDistance) {
								visEnemies.add(new int[] { r, c });
								break sightLoop;
							}
						}
					}
				}
			}
		}
		return visEnemies;
	}

	public List<int[]> getFoodVisible(Hive hive) {
		if (!players.contains(hive))
			return null;
		ArrayList<int[]> visFood = new ArrayList<int[]>();
		int playerNum = players.indexOf(hive) + 1;
		for (int r = 0; r < food.length; r++) {
			for (int c = 0; c < food[0].length; c++) {
				if (food[r][c]) {
					sightLoop: for (int ar = r - Hive.sightDistance; ar <= r + Hive.sightDistance; ar++) {
						if (ar < 0 || ar >= ants.length)
							continue;
						for (int ac = c - Hive.sightDistance; ac <= c + Hive.sightDistance; ac++) {
							if (ac < 0 || ac >= ants[0].length || ants[ar][ac] != playerNum)
								continue;
							if (AntGameRunner.dist(r, c, ar, ac) <= Hive.sightDistance) {
								visFood.add(new int[] { r, c });
								break sightLoop;
							}
						}
					}
				}
			}
		}
		return visFood;
	}

	public List<int[]> getHills(Hive hive, boolean me) {
		if (!players.contains(hive))
			return null;
		int playerNum = players.indexOf(hive);
		if (!me)
			playerNum = 1 - playerNum;
		ArrayList<int[]> hills = new ArrayList<int[]>();
		for (int i = 0; i < hillLocs.get(playerNum).size(); i++) {
			hills.add(Arrays.copyOf(hillLocs.get(playerNum).get(i), 2));
		}
		return hills;
	}

	public boolean[][] getMapCopy() {
		boolean[][] out = new boolean[maze.length][maze[0].length];
		for (int i = 0; i < maze.length; i++) {
			System.arraycopy(maze[i], 0, out[i], 0, maze[i].length);
		}
		return out;
	}

	public Hive playSilent() {
		while (winner == null) {
			update();
		}
		return winner;
	}

	public void update() {
		// Spawn, move, fight, destroy hills, collect, repeat.
		if (winner == null) {
			spawnAnts();
			if (frameCount % 10 == 9)
				spawnFood();
			moveAnts();
			fightAnts();
			destroyHills();
			checkWinner();
			collectFood();
			frameCount++;
		}
		if (parent != null)
			display();
	}

	private void checkWinner() {
		for (int i = 0; i < players.size(); i++) {
			if (hillLocs.get(i).size() == 0 || getAnts(players.get(i)).size() == 0) {
				winner = players.get(1 - i);
				return;
			}
		}
	}

	private void fightAnts() {
		int[][] newAnts = new int[ants.length][ants[0].length];
		for (int r = 0; r < ants.length; r++) {
			for (int c = 0; c < ants[0].length; c++) {
				int ant = ants[r][c];
				if (ant == 0)
					continue;
				boolean dead = false;
				int enemies = enemiesAround(r, c);
				enemyLoop: for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						int other = ants[r + i][c + j];
						if (other == 0 || other == ant)
							continue;
						if (enemies <= enemiesAround(r + i, c + j)) {
							dead = true;
							break enemyLoop;
						}
					}
				}
				if (!dead)
					newAnts[r][c] = ants[r][c];
			}
		}
		ants = newAnts;
	}

	private void collectFood() {
		for (int r = 0; r < food.length; r++) {
			for (int c = 0; c < food[0].length; c++) {
				if (!food[r][c])
					continue;
				int score = 0;
				boolean found = false;
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						int ant = ants[r + i][c + j];
						if (ant != 0) {
							found = true;
							score += ant == 1 ? 1 : -1;
						}
					}
				}
				if (found)
					food[r][c] = false;
				if (score > 0)
					foodReserves[0]++;
				else if (score < 0)
					foodReserves[1]++;
			}
		}
	}

	private int countAliveNeighbors(int row, int col) {
		int out = 0;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (i == 0 && j == 0)
					continue;
				int nr = row + i;
				int nc = col + j;
				if (nr < 0 || nr >= maze.length || nc < 0 || nc >= maze[0].length || maze[nr][nc])
					out++;
			}
		}
		return out;
	}

	private void destroyHills() {
		ArrayList<int[]> deadHills = new ArrayList<int[]>();
		for (int player = 0; player < hillLocs.size(); player++) {
			for (int hill = 0; hill < hillLocs.get(player).size(); hill++) {
				int[] loc = hillLocs.get(player).get(hill);
				if (ants[loc[0]][loc[1]] != 0 && ants[loc[0]][loc[1]] != player + 1) {
					deadHills.add(loc);
					hills[loc[0]][loc[1]] = 0;
				}
			}
			deadLoop: for (int[] x : deadHills) {
				for (int i = 0; i < hillLocs.get(player).size(); i++) {
					if (Arrays.equals(x, hillLocs.get(player).get(i))) {
						hillLocs.get(player).remove(i);
						continue deadLoop;
					}
				}
			}
		}
	}

	private int enemiesAround(int r, int c) {
		if (ants[r][c] == 0)
			return 0;
		int n = 0;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (ants[r + i][c + j] != 0 && ants[r + i][c + j] != ants[r][c])
					n++;
			}
		}
		return 0;
	}

	private boolean floodCheck() {
		float pOpen = 0;
		boolean[][] newMaze = new boolean[maze.length][maze[0].length];
		for (int i = 0; i < newMaze.length; i++) {
			for (int j = 0; j < newMaze[0].length; j++) {
				newMaze[i][j] = true;
			}
		}
		int[] start = new int[2];
		x: for (int i = 0; i < maze.length; i++) {
			for (int j = 0; j < maze[0].length; j++) {
				if (hills[i][j] != 0) {
					start = new int[] { i, j };
					break x;
				}
			}
		}
		LinkedList<int[]> frontier = new LinkedList<int[]>();
		frontier.add(start);
		LinkedList<int[]> visited = new LinkedList<int[]>();
		while (frontier.size() > 0) {
			int[] curr = frontier.pop();
			visited.add(curr);
			newMaze[curr[0]][curr[1]] = false;
			pOpen++;
			int[] n = new int[] { curr[0] - 1, curr[1] };
			int[] s = new int[] { curr[0] + 1, curr[1] };
			int[] w = new int[] { curr[0], curr[1] - 1 };
			int[] e = new int[] { curr[0], curr[1] + 1 };
			if (n[0] - 1 >= 0 && !maze[n[0]][n[1]] && !listContains(visited, n) && !listContains(frontier, n))
				frontier.add(n);
			if (s[0] + 1 < maze.length && !maze[s[0]][s[1]] && !listContains(visited, s) && !listContains(frontier, s))
				frontier.add(s);
			if (w[1] - 1 >= 0 && !maze[w[0]][w[1]] && !listContains(visited, w) && !listContains(frontier, w))
				frontier.add(w);
			if (e[1] + 1 < maze[0].length && !maze[e[0]][e[1]] && !listContains(visited, e)
					&& !listContains(frontier, e))
				frontier.add(e);
		}
		maze = newMaze;
		for (int r = 0; r < maze.length; r++) {
			for (int c = 0; c < maze[0].length; c++) {
				if (maze[r][c] && hills[r][c] != 0)
					return false;
			}
		}
		pOpen /= (maze.length * maze[0].length);
		return pOpen >= minOpen && pOpen <= maxOpen;
	}

	private void genMaze() {
		genMaze(false);
	}

	private void genMaze(boolean empty) {
		do {
			if (empty) {
				for (int r = 0; r < maze.length; r++) {
					for (int c = 0; c < maze[0].length; c++) {
						if (r == 0 || c == 0 || r == maze.length - 1 || c == maze[0].length - 1)
							maze[r][c] = true;
					}
				}
			} else {
				for (int r = 0; r < maze.length; r++) {
					for (int c = 0; c < maze[0].length; c++) {
						maze[r][c] = rand.nextFloat() < pStartAlive;
						hills[r][c] = 0;
					}
				}
				mirror();
				for (int i = 0; i < steps; i++) {
					simulationStep();
				}
			}
			hillLocs = new ArrayList<ArrayList<int[]>>();
			hillLocs.add(new ArrayList<int[]>());
			hillLocs.add(new ArrayList<int[]>());
			for (int h = 0; h < hillsPerPlayer; h++) {
				int[] hLoc;
				do {
					hLoc = new int[] { h * maze.length / hillsPerPlayer + rand.nextInt(maze.length / hillsPerPlayer),
							rand.nextInt(4 * maze[0].length / 9) };
				} while (maze[hLoc[0]][hLoc[1]] || hills[hLoc[0]][hLoc[1]] != 0);
				int[] otherHLoc = new int[] { maze.length - 1 - hLoc[0], maze[0].length - 1 - hLoc[1] };
				hills[hLoc[0]][hLoc[1]] = 1;
				hillLocs.get(0).add(hLoc);
				hills[otherHLoc[0]][otherHLoc[1]] = 2;
				hillLocs.get(1).add(otherHLoc);
			}
		} while (!empty && !floodCheck());
		for (int i = 0; i < 18; i++) {
			spawnFood();
		}
	}

	private boolean listContains(LinkedList<int[]> data, int[] val) {
		for (int[] x : data) {
			if (Arrays.equals(x, val))
				return true;
		}
		return false;
	}

	private void mirror() {
		for (int r = 0; r < maze.length; r++) {
			for (int c = 0; c < maze[0].length; c++) {
				maze[r][c] = maze[maze.length - 1 - r][maze[0].length - 1 - c];
			}
		}
	}

	private void moveAnts() {
		int[][] newAnts = new int[ants.length][ants[0].length];
		boolean[][] moved = new boolean[ants.length][ants[0].length];
		for (int i = 0; i < players.size(); i++) {
			try {
				turnClock = System.currentTimeMillis();
				String rawOrders = players.get(i).orders();
				turnClock = System.currentTimeMillis() - turnClock;
				timer[i] -= turnClock;
				if (timer[i] < 0) {
					winner = players.get(players.size() - 1 - i);
					return;
				}
				timer[i] += incrTime;
				timer[i] = Math.min(timer[i], maxTime);
				ArrayList<int[]> orders = parseOrders(rawOrders);
				for (int[] move : orders) {
					if (ants[move[0]][move[1]] != i + 1 || moved[move[0]][move[1]])
						continue;
					int[] newLoc = new int[] { move[0] + move[2], move[1] + move[3] };
					if (maze[newLoc[0]][newLoc[1]] || food[newLoc[0]][newLoc[1]])
						continue;
					moved[move[0]][move[1]] = true;
					if (newAnts[newLoc[0]][newLoc[1]] != 0)
						newAnts[newLoc[0]][newLoc[1]] = -1;
					else {
						newAnts[newLoc[0]][newLoc[1]] = i + 1;
					}
				}
			} catch (Exception e) {

			}
		}
		for (int r = 0; r < ants.length; r++) {
			for (int c = 0; c < ants[0].length; c++) {
				if (!moved[r][c] && newAnts[r][c] == 0 && ants[r][c] != 0)
					newAnts[r][c] = ants[r][c];
				if (newAnts[r][c] == -1)
					newAnts[r][c] = 0;
			}
		}
		ants = newAnts;
	}

	private ArrayList<int[]> parseOrders(String orders) {
		String[] info = orders.split("\n");
		ArrayList<int[]> out = new ArrayList<int[]>();
		int[] order = null;
		for (String line : info) {
			try {
				line = line.toLowerCase().trim();
				String[] stuff = line.split(" ");
				order = new int[4];
				for (int i = 0; i < 2; i++) {
					order[i] = Integer.parseInt(stuff[i]);
				}
				if (stuff[2].equals("n"))
					order[2] = -1;
				else if (stuff[2].equals("s"))
					order[2] = 1;
				else if (stuff[2].equals("w"))
					order[3] = -1;
				else if (stuff[2].equals("e"))
					order[3] = 1;
				out.add(order);
			} catch (Exception e) {

			}
		}
		return out;
	}

	private void simulationStep() {
		boolean[][] newMaze = new boolean[maze.length][maze[0].length];
		for (int r = 0; r < maze.length; r++) {
			for (int c = 0; c < maze[0].length; c++) {
				int neighbors = countAliveNeighbors(r, c);
				if (maze[r][c]) {
					newMaze[r][c] = neighbors > deathLimit;
				} else {
					newMaze[r][c] = neighbors > birthLimit;
				}
			}
		}
		maze = newMaze;
	}

	private void spawnAnts() {
		for (int i = 0; i < hillLocs.size(); i++) {
			Collections.shuffle(hillLocs.get(i));
			for (int j = 0; j < hillLocs.get(i).size(); j++) {
				int[] loc = hillLocs.get(i).get(j);
				if (foodReserves[i] == 0)
					break;
				if (ants[loc[0]][loc[1]] == 0) {
					ants[loc[0]][loc[1]] = i + 1;
					foodReserves[i]--;
					spawnCount[i][j]++;
				}
			}
		}
	}

	private void spawnFood() {
		if (foodStep == 0) {
			Collections.shuffle(foodPriority);
		}
		int qr = foodPriority.get(foodStep) / 3;
		int qc = foodPriority.get(foodStep) % 3;
		int qHeight = maze.length / 6;
		int qWidth = maze[0].length / 3;
		int minRow = qr * qHeight;
		int minCol = qc * qWidth;
		int foodR, foodC;
		do {
			foodR = minRow + rand.nextInt(qHeight);
			foodC = minCol + rand.nextInt(qWidth);
		} while (maze[foodR][foodC] || hills[foodR][foodC] != 0 || ants[foodR][foodC] != 0);
		food[foodR][foodC] = true;
		food[maze.length - 1 - foodR][maze[0].length - 1 - foodC] = true;
		foodStep = (foodStep + 1) % foodPriority.size();
	}

	public int getReserveFood(Hive hive) {
		if (!players.contains(hive))
			return 0;
		int i = players.indexOf(hive);
		return foodReserves[i];
	}

	public int getTime(Hive hive) {
		if (!players.contains(hive))
			return 0;
		int i = players.indexOf(hive);
		return timer[i] - (int) (System.currentTimeMillis() - turnClock);
	}

}
