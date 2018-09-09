# AntGame
Recreation of the [2011 Google AI Challenge](http://ants.aichallenge.org/ "Google Ant Game"). The rules are the same, reimplemented to run locally in java for advanced programming class. Students write a bot as a subclass of Hive, which decides moves for all ants.

## Level generation
My main contribution was adding procedurally generated levels, with an algorithm based on [this blog post](https://gamedevelopment.tutsplus.com/tutorials/generate-random-cave-levels-using-cellular-automata--gamedev-9664 "Cellular Automata Cave Generation"), including quality checking to make sure that the maps meet some basic requirements. Also makes sure that all mazes will be rotationally symmetrical so that neither player is at a disadvantage.

![alt text](https://github.com/ejkaplan/AntGame/blob/master/antgif.gif "Two random bots fight it out")
