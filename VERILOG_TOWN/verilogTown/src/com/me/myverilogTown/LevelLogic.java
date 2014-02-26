package com.me.myverilogtown;

import java.util.*;

import com.badlogic.gdx.math.Vector2;

public class LevelLogic
{
	private verilogTownMap clevel;
	private Car cars[];
	private int num_cars;
	
	public LevelLogic()
	{
		/* init current level map data structure */
		this.clevel = new verilogTownMap(20, 20); // firts_map

		/* this might be where the XML read map goes */
		/* hard coded */
		clevel.verilogTownMapHardCode_first_map();

		/* after reading the number of cars from level */
		num_cars = 1; // hard coded
		cars = new Car[num_cars];

		/* initialize cars */
		for (int i = 0; i < num_cars; i++)
		{
			cars[i] = new Car(clevel.grid[7][0], clevel.grid[4][21], -1, clevel,null,0,0,0,0);
		}
	}

	public void update()
	{
	}
}
