import java.util.Random;

public class RandomHive extends Hive {

	private Random rand;

	public RandomHive(Game game, String name) {
		super(game, name);
		rand = new Random();
	}

	@Override
	public String orders() {
		String out = "";
		char[] dirs = new char[] { 'n', 's', 'w', 'e' };
		for (int[] ant : this.getAntLocations()) {
			out += ant[0] + " " + ant[1] + " " + dirs[rand.nextInt(dirs.length)] + "\n";
		}
		return out;
	}

}
