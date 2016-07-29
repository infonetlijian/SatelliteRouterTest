/* JAT: Java Astrodynamics Toolkit
 *
 * Copyright (c) 2003 National Aeronautics and Space Administration. All rights reserved.
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
 * 
 * File Created on Aug 28, 2003
 */

package jat.coreNOSA.cm;
import jat.coreNOSA.math.MatrixVector.data.VectorN;

import java.io.Serializable;


/**
* The FiniteBurn.java Class represents a single finite burn.
*
* @author 
* @version 1.0
*/
public class FiniteBurn implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8812477331675719590L;

	/** Burn start time in sim time (seconds) */
	public double tstart;
	
	/** Burn stop time in sim time (seconds) */
	public double tstop;
	
	/** Burn acceleration in m/s^2 */
	public double accel;
		
	/** Thrust direction unit vector */
	public VectorN unitVector;
	
	
	/** Constructor
	 * @param t0 Time of burn initiation in sim time (sec).
	 * @param tf time of burn cutoff in sim time (sec)
	 * @param unitv thrust direction unit vector
	 */
	public FiniteBurn(double t0, double tf, double acc, VectorN unitv) {
		this.tstart = t0;
		this.tstop = tf;
		this.accel = acc;
		this.unitVector = unitv;
	}
	

}
