package jat.coreNOSA.algorithm.solver;

import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.VectorN;

class LSSDP_FORTRAN
{

	static double DABS(double d)
	{
		return Math.abs(d);
	}

	// Solve y=Ax by Gaussian Elimination
	// C= [A | y]

	static void LSSDP_solve(Matrix C, VectorN X)
	{
		// IMPLICIT DOUBLE PRECISION (A-H,O-Z)
		//     SOLUTION OF A LINEAR SYSTEM BY GAUSS ELIMINATION
		// NR=number of rows
		// NPM=number of columns
		//  DIMENSION C(NR,NPM),X(NR)
		//  EQUIVALENCE (R,S),(RATIO,V)
		double R, S, Z, V, EX, AER, RATIO;
		int I, II, IIP1, J, K, L, KP, LP1;
		int NR, NPM;

		System.out.println("" + C.m);
		NR = C.m - 1;
		NPM = C.n - 1;
		// NORM OF MATRIX C
		R = 0.0;
		for (J = 1; J <= NR; J++)
		{
			Z = 0.0;
			for (K = 1; K <= NR; K++)
			{
				V = C.A[J][K];
				Z = Z + DABS(V);
			}
			if (R < Z)
				R = Z;
		}
		EX = (1.0e-30) * R;
		// TRIANGULARIZATION
		for (L = 1; L <= NR; L++)
		{
			Z = 0.0;
			KP = 0;
			// FIND ELEMENT FOR ROW PIVOT
			for (K = L; K <= NR; K++)
			{
				AER = C.A[K][L];
				AER = DABS(AER);
				if (Z < AER)
				{
					Z = AER;
					KP = K;
				}
			}
			if (L < KP)
			{
				// INTERCHANGE ROWS
				for (J = L; J <= NPM; J++)
				{
					S = C.A[L][J];
					C.A[L][J] = C.A[KP][J];
					C.A[KP][J] = S;
				}
			}
			// TEST FOR A SINGULAR MATRIX
			AER = C.A[L][L];
			AER = DABS(AER);
			if (AER <= EX)
			{
				System.out.println(" MATRIX SINGULAR IN SUBROUTINE LSSSP");
				System.exit(0);
			}
			if (!(L < NR))
			{
				break;
			}
			LP1 = L + 1;
			for (K = LP1; K <= NR; K++)
			{
				AER = C.A[K][L];
				AER = DABS(AER);
				if (AER == 0)
				{
					RATIO = C.A[K][L] / C.A[L][L];
					for (J = LP1; J <= NPM; J++)
					{
						C.A[K][J] = C.A[K][J] - RATIO * C.A[L][J];
					}
				}
			}
		} //34
		C.print(" before back subst");
		// BACK SUBSTITUTION
		for (I = 1; I <= NR; I++)
		{
			S = 0.0;
			II = NPM - I;
			if (II < NR)
			{
				IIP1 = II + 1;
				for (K = IIP1; K <= NR; K++)
				{
					S = S + C.A[II][K] * X.x[K];
				}
			}
			RATIO = C.A[II][NPM];
			X.x[II] = (RATIO - S) / C.A[II][II];
		}
	}

	public static void main(String argv[])
	{
		double[][] C_data = { 
				{ 0, 0, 0, 0, 0 }, 
				{ 0, 1, 0, 1, 1}, 
				{ 0, 0, 3, 0, 2 }, 
				{ 0, 1, 0, 1, 3 }
		};

		System.out.println("Gaussian Elimination Test");
		System.out.println("FORTRAN Arrays starting at 1");
		Matrix C = new Matrix(C_data);
		C.print();
		VectorN X = new VectorN(4);
		LSSDP_solve(C, X);
		C.print();
		X.print();
	}
}
/*
// Set A
C.set(1,1,1.);
C.set(1,2,2.);
C.set(2,1,3.);
C.set(2,2,4.);
// set y
C.set(1,3,5.);
C.set(2,3,6.);
*/