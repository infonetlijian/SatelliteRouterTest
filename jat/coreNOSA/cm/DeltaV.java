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
 * File Created on May 19, 2003
 */
 
package jat.coreNOSA.cm;
import jat.coreNOSA.math.MatrixVector.data.VectorN;

import java.io.Serializable;


/**
* The DeltaV.java Class represents a single Delta-V.
*
* @author 
* @version 1.0
*/
public class DeltaV implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2858149764313424783L;

	/** Epoch time of delta-v in sim time (seconds) */
	public double t;
		
	/** Delta-v vector */
	public VectorN dv;
	
	
	/** Constructor
	 * @param tsim Time of the measurement in sim time (sec).
	 * @param deltav Delta-V vector (m/s)
	 */
	public DeltaV(double tsim, VectorN deltav) {
		this.t = tsim;
		this.dv = deltav;
	}
	

}
