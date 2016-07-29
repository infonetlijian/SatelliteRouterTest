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
 */

package jat.coreNOSA.algorithm;
import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.VectorN;

/**
 * Utility class for Numerical Derivatives
 * @author 
 * @version 1.0
 */
public class Jacobian {
    
    /** Compute the Jacobian using central differences.
     * @param xin VectorN containing the current value of x.
     * @param f a VectorFunction whose Jacobian is to be computed.
     * @param eps Accuracy.
     * @return Jacobian matrix (n x n).
     */    
    public static Matrix centralDiff(VectorN xin, VectorFunction f, double eps)
    // computes the Jacobian using central differences
    {
        int n = xin.length;
        VectorN nu = new VectorN(xin);
        VectorN nxf = new VectorN(nu);
        VectorN nxb = new VectorN(nu);
        double [][] cx = new double[n][n];
        
        for (int i = 0; i < n; i++) {
            double dnx = Math.abs(eps*nu.x[i]);
            if (Math.abs(nu.x[i]) <= eps) {
                dnx = eps*eps;
            }
            
            nxf.x[i] = nu.x[i] + dnx;
            dnx = nxf.x[i] - nu.x[i];
            
            VectorN cf = f.evaluate(nxf);
            nxf.x[i] = nu.x[i];
            
            nxb.x[i] = nu.x[i] - dnx;
            VectorN cb = f.evaluate(nxb);
            nxb.x[i] = nu.x[i];
            
            for (int j = 0; j < n; j++) {
                cx[j][i] = 0.5 * (cf.x[j] - cb.x[j])/dnx;
            }
        }
        Matrix Jac = new Matrix(cx, n, n);
        return Jac;
    }

    /** Compute the Jacobian using 4th order central differences.
     * @param xin VectorN containing the current value of x.
     * @param f a VectorFunction whose Jacobian is to be computed.
     * @param eps Accuracy.
     * @return Jacobian matrix (n x n).
     */       
    public static Matrix centralDiff4th(VectorN xin, VectorFunction f, double eps)
    // computes the Jacobian using 4th order central differences
    {
        
        int n = xin.length;
        VectorN nu = new VectorN(xin);
        VectorN nxf = new VectorN(nu);
        VectorN nxb = new VectorN(nu);
        VectorN nxfh = new VectorN(nu);
        VectorN nxbh = new VectorN(nu);
        
        double [][] cx = new double[n][n];
        
        for (int i = 0; i < n; i++) {
            double dnx = Math.abs(eps*nu.x[i]);
            if (Math.abs(nu.x[i]) <= eps) {
                dnx = eps*eps;
            }
            
            nxf.x[i] = nu.x[i] + dnx;
            dnx = nxf.x[i] - nu.x[i];
            
            VectorN cf = f.evaluate(nxf);
            nxf.x[i] = nu.x[i];
            
            nxb.x[i] = nu.x[i] - dnx;
            VectorN cb = f.evaluate(nxb);
            nxb.x[i] = nu.x[i];
            
            nxfh.x[i] = nu.x[i] + 2.0*dnx;
            VectorN cfh = f.evaluate(nxb);
            nxfh.x[i] = nu.x[i];
            
            nxbh.x[i] = nu.x[i] - 2.0*dnx;
            VectorN cbh = f.evaluate(nxb);
            nxbh.x[i] = nu.x[i];
            
            
            for (int j = 0; j < n; j++) {
                cx[j][i] = (8.0*(cf.x[j] - cb.x[j])-(cfh.x[j] - cbh.x[j]))/(12.0*dnx);
            }
        }
        
        Matrix Jac = new Matrix(cx, n, n);
        return Jac;
    }
    
    
}

