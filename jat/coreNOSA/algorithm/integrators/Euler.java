/* JAT: Java Astrodynamics Toolkit
 *
 * Copyright (c) 2007 National Aeronautics and Space Administration. All rights reserved.
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

package jat.coreNOSA.algorithm.integrators;

/**
 * <P>
 * Euler is a simple fixed stepsize Euler integrator.
 *
 * @author 
 * @version 1.0
 */

public class Euler {
	
	private double dt;

 
    /** Default constructor
     */
    public Euler() {
    	dt = 1.0;
    }

    /** Default constructor
     */
    public Euler(double step_size) {
    	dt = step_size;
    }

    /** Take a single integration step. Note: it is assumed that any printing
     * will be done by the calling function.
     * @param x     time or independent variable
     * @param y     double[] containing needed inputs (usually the state)
     * @param derivs   Object containing the Equations of Motion
     * @return      double[] containing the new state
     */
    public double[] step(double x, double[] y, Derivatives derivs) {
        int n = y.length;
        double[] yout = new double[n];
        double[] f = derivs.derivs(x, y);

        // construct solution
        for (int i = 0; i < n; i++) {
            yout[i] = y[i] + dt*f[i];
        }

        return yout;
    }

}
