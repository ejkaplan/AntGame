import processing.core.PApplet;

public class AntGameRunner extends PApplet {

	public static void main(String[] args) {
		PApplet.main("AntGameRunner");
	}

	private Game g;
	private int rows = 50; // Change this to make the grid contain more or
	// fewer cells.

	public void settings() {
		// fullScreen();
		size(3 * displayWidth / 4, 3 * displayHeight / 4);
	}

	public void setup() {
		frameRate(60); // Change this if you want the game to run slower to make
		// it easier to watch.
		float squareWidth = 1f * height / rows;
		int cols = round(width / squareWidth);
		g = new Game(this, rows, cols);
		// Your bots here:
		new RandomHive(g, "Random");
		new RandomHive(g, "Random");
	}

	public void draw() {
		background(255);
		g.update();
	}

}
