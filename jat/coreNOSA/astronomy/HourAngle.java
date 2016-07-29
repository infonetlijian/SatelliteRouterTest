/* JAT: Java Astrodynamics Toolkit
 *
 * Copyright (c) 2002 National Aeronautics and Space Administration. All rights reserved.
 *
 * This file is part of JAT. JAT is free software; you can 
 * redistribute it and/or modify it under the terms of the 
 * NASA Open Source Agreement 
 * 
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * NASA Open Source Agreement for more details.
 *
 * You should have received a copy of the NASA Open Source Agreement
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package jat.coreNOSA.astronomy;

/**
 * Simple class to store Hour Angle
 * @author Tobias Berthold
 * @version 1.0
 */

public class HourAngle
{
	public boolean positive;
	public int hours;
	public int minutes;
	public double seconds;

	public HourAngle()
	{
	}

	public HourAngle(boolean positive, int hours, int minutes, double seconds)
	{
		this.positive = positive;
		this.hours = hours;
		this.minutes = minutes;
		this.seconds = seconds;
	}
}
