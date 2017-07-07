package util;

/**
 * Enum type used to represent the directions within the local neighbourhood of a pixel, ultimately to define the
 * connectedness of neighbouring pixels in source detection algorithms.
 *
 * Neighbour directions:
 * 
 * 
 *       N                ____________________
 *       ^             ^ |                    |	
 *       |             | |     Science        |
 * W <---|---> E    AC | |      window        |
 *       |             | |                    |
 *       V             0 |____________________|
 *       S               0-------> AL
 * 
 *
 * @author nrowell
 * @version $Id: Direction.java 470593 2015-12-09 18:54:47Z mdavidso $
 */
public enum Direction {

    /**
     * North
     */
    N(0, 1),
    /**
     * Northeast
     */
    NE(1, 1),
    /**
     * East
     */
    E(1, 0),
    /**
     * Southeast
     */
    SE(1, -1),
    /**
     * South
     */
    S(0, -1),
    /**
     * Southwest
     */
    SW(-1, -1),
    /**
     * West
     */
    W(-1, 0),
    /**
     * Northwest
     */
    NW(-1, 1);

    /**
     * Search step in AL direction
     */
    public int dal;

    /**
     * Search step in AC direction
     */
    public int dac;

    /**
     * Constructor for a {@link Direction}.
     *
     * @param dal
     *            Step in the AL direction.
     * @param dac
     *            Step in the AC direction.
     */
    Direction(int dal, int dac) {
        this.dal = dal;
        this.dac = dac;
    }

    /**
     * Defines 8-pixel neighbourhood
     */
    public static final Direction[] EIGHT_NEIGHBOURS = new Direction[] {
            Direction.N, Direction.NE, Direction.E, Direction.SE, Direction.S, Direction.SW, Direction.W,
            Direction.NW, };

    /**
     * Defines 4-pixel neighbourhood
     */
    public static final Direction[] FOUR_NEIGHBOURS = new Direction[] {
            Direction.N, Direction.E, Direction.S, Direction.W, };

    /**
     * Defines 2-pixel neighbourhood for 1D windows with no extent in AL direction
     */
    public static final Direction[] NORTH_SOUTH = new Direction[] { Direction.N, Direction.S };

    /**
     * Defines 2-pixel neighbourhood for 1D windows with no extent in AC direction
     */
    public static final Direction[] EAST_WEST = new Direction[] { Direction.E, Direction.W };

}
